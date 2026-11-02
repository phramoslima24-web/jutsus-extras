package com.shinobiextras.jutsu;

import com.shinobiextras.Bridge;
import com.shinobiextras.network.ModNetwork;
import com.shinobiextras.network.SyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/** Aprendizado, selecao, recarga e custo dos jutsus extras (lado servidor). */
public final class JutsuManager {
    private JutsuManager() {}

    public static final String KEY = "shinobiextras_data";

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static int mask(Player p) {
        return data(p).getInt("learned");
    }

    public static boolean isLearned(Player p, ExtraJutsu j) {
        return (mask(p) & j.bit()) != 0;
    }

    public static int selected(Player p) {
        return data(p).getInt("selected");
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPacket(mask(p), selected(p)));
    }

    /** Aprende de graca (usado pelos pergaminhos). Devolve false se ja sabia. */
    public static boolean learn(ServerPlayer p, ExtraJutsu j) {
        if (isLearned(p, j)) return false;
        CompoundTag d = data(p);
        d.putInt("learned", d.getInt("learned") | j.bit());
        if (Integer.bitCount(d.getInt("learned")) == 1) d.putInt("selected", j.ordinal());
        sync(p);
        p.sendSystemMessage(Component.translatable("message.shinobiextras.learned", Component.translatable(j.langKey())));
        return true;
    }

    /** Clique na tela: aprende (paga XP) se ainda nao sabe; senao seleciona. */
    public static void requestLearnOrSelect(ServerPlayer p, int index) {
        ExtraJutsu j = ExtraJutsu.byIndex(index);
        if (j == null) return;
        if (isLearned(p, j)) {
            data(p).putInt("selected", j.ordinal());
            sync(p);
            return;
        }
        if (!p.isCreative() && p.experienceLevel < j.xpCost) {
            p.sendSystemMessage(Component.translatable("message.shinobiextras.not_enough_levels", j.xpCost));
            return;
        }
        if (!p.isCreative()) p.giveExperienceLevels(-j.xpCost);
        learn(p, j);
    }

    /** Proximo jutsu aprendido. */
    public static void cycle(ServerPlayer p) {
        int m = mask(p);
        if (m == 0) {
            p.displayClientMessage(Component.translatable("message.shinobiextras.none_learned"), true);
            return;
        }
        int n = ExtraJutsu.values().length;
        int cur = selected(p);
        for (int i = 1; i <= n; i++) {
            int idx = (cur + i) % n;
            if ((m & (1 << idx)) != 0) {
                data(p).putInt("selected", idx);
                break;
            }
        }
        sync(p);
        ExtraJutsu now = ExtraJutsu.byIndex(selected(p));
        if (now != null) {
            p.displayClientMessage(Component.translatable("message.shinobiextras.selected", Component.translatable(now.langKey())), true);
        }
    }

    private static String cdKey(ExtraJutsu j) {
        return "cd_" + j.id;
    }

    public static void use(ServerPlayer p) {
        ExtraJutsu j = ExtraJutsu.byIndex(selected(p));
        if (j == null || !isLearned(p, j)) {
            p.displayClientMessage(Component.translatable("message.shinobiextras.none_learned"), true);
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        long now = p.level().getGameTime();
        long end = data(p).getLong(cdKey(j));
        if (!creative && now < end) {
            long secs = (end - now + 19) / 20;
            p.displayClientMessage(Component.translatable("message.shinobiextras.cooldown", secs), true);
            return;
        }
        boolean pay = j.chakraCost > 0 && !creative;
        if (pay) {
            double chakra = Bridge.getDouble(p, "chakra", -1);
            if (chakra >= 0 && chakra < j.chakraCost) {
                p.displayClientMessage(Component.translatable("message.shinobiextras.no_chakra", j.chakraCost), true);
                return;
            }
        }
        if (JutsuActions.run(j, p)) {
            if (!creative) data(p).putLong(cdKey(j), now + j.cooldownTicks);
            if (pay) Bridge.addChakra(p, -j.chakraCost);
        }
    }

    /** Copia os dados para o novo corpo (morte). */
    public static void copy(Player from, Player to) {
        CompoundTag old = from.getPersistentData().getCompound(KEY);
        to.getPersistentData().put(KEY, old.copy());
    }
}
