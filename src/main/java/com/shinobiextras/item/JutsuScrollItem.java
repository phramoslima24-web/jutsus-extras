package com.shinobiextras.item;

import com.shinobiextras.jutsu.ExtraJutsu;
import com.shinobiextras.jutsu.JutsuManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Pergaminho: clique direito ensina o jutsu de graca e consome o item. */
public class JutsuScrollItem extends Item {
    private final ExtraJutsu jutsu;

    public JutsuScrollItem(ExtraJutsu jutsu) {
        super(new Item.Properties().stacksTo(1));
        this.jutsu = jutsu;
    }

    public ExtraJutsu getJutsu() {
        return jutsu;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.shinobiextras.scroll", Component.translatable(jutsu.langKey()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (JutsuManager.learn(sp, jutsu)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
            } else {
                sp.displayClientMessage(Component.translatable("message.shinobiextras.already_known"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
