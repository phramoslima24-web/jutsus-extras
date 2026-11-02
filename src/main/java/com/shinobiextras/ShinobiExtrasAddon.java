package com.shinobiextras;

import com.shinobiextras.jutsu.JutsuScheduler;
import com.shinobiextras.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ShinobiExtrasAddon.MODID)
public class ShinobiExtrasAddon {
    public static final String MODID = "shinobiextras";

    public ShinobiExtrasAddon() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.ITEMS.register(bus);
        ModRegistry.ENTITIES.register(bus);
        ModRegistry.TABS.register(bus);
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(JutsuScheduler.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::init);
    }
}
