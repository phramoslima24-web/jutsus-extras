package com.shinobiextras.client;

import com.shinobiextras.ShinobiExtrasAddon;
import com.shinobiextras.network.ActionPacket;
import com.shinobiextras.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShinobiExtrasAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (ClientKeys.MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new ExtraJutsuScreen(null));
        }
        while (ClientKeys.USE.consumeClick()) {
            if (mc.screen == null) ModNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.USE, 0));
        }
        while (ClientKeys.CYCLE.consumeClick()) {
            if (mc.screen == null) ModNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.CYCLE, 0));
        }
    }

    /** Botao "Jutsus Extras" nas telas de jutsu do Narutoto (abaixo do botao do Edo Tensei, se existir). */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        String name = screen.getClass().getName();
        if (!name.equals("net.mcreator.narutoto.client.gui.JutsuBoardScreen")
                && !name.equals("net.mcreator.narutoto.client.gui.OtherBoardScreen")) return;
        event.addListener(Button.builder(
                        Component.translatable("gui.shinobiextras.title"),
                        b -> Minecraft.getInstance().setScreen(new ExtraJutsuScreen(screen)))
                .bounds(screen.width - 130, 32, 122, 20)
                .build());
    }

    /** Plano B: /jutsusextras abre a mesma tela. */
    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jutsusextras").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> mc.setScreen(new ExtraJutsuScreen(null)));
            return 1;
        }));
    }
}
