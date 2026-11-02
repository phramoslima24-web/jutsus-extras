package com.shinobiextras.network;

import com.shinobiextras.jutsu.JutsuManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor. USE: usar o jutsu selecionado. CYCLE: trocar. LEARN_OR_SELECT: clique na tela (value = indice). */
public class ActionPacket {
    public static final int USE = 0, CYCLE = 1, LEARN_OR_SELECT = 2;

    private final int type;
    private final int value;

    public ActionPacket(int type, int value) {
        this.type = type;
        this.value = value;
    }

    public static void encode(ActionPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.type);
        buf.writeByte(m.value);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readByte(), buf.readByte());
    }

    public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;
            switch (m.type) {
                case USE -> JutsuManager.use(sp);
                case CYCLE -> JutsuManager.cycle(sp);
                case LEARN_OR_SELECT -> JutsuManager.requestLearnOrSelect(sp, m.value);
                default -> { }
            }
        });
        c.setPacketHandled(true);
    }
}
