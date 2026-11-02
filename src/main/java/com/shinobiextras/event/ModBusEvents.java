package com.shinobiextras.event;

import com.shinobiextras.ModRegistry;
import com.shinobiextras.ShinobiExtrasAddon;
import com.shinobiextras.entity.ShinobiEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShinobiExtrasAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {
    private ModBusEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        for (ShinobiEntity.Kind kind : ShinobiEntity.Kind.values()) {
            event.put(ModRegistry.SHINOBIS.get(kind).get(), ShinobiEntity.createAttributes(kind).build());
        }
    }

    @SubscribeEvent
    public static void onSpawnPlacement(SpawnPlacementRegisterEvent event) {
        for (ShinobiEntity.Kind kind : ShinobiEntity.Kind.values()) {
            event.register(ModRegistry.SHINOBIS.get(kind).get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ShinobiEntity::checkSpawn,
                    SpawnPlacementRegisterEvent.Operation.AND);
        }
    }
}
