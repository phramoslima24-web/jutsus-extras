package com.shinobiextras.client;

import com.shinobiextras.ModRegistry;
import com.shinobiextras.ShinobiExtrasAddon;
import com.shinobiextras.entity.ShinobiEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShinobiExtrasAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ShinobiEntity.Kind kind : ShinobiEntity.Kind.values()) {
            event.registerEntityRenderer(ModRegistry.SHINOBIS.get(kind).get(), ShinobiRenderer::new);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.USE);
        event.register(ClientKeys.CYCLE);
        event.register(ClientKeys.MENU);
    }
}
