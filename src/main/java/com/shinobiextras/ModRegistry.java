package com.shinobiextras;

import com.shinobiextras.entity.ShinobiEntity;
import com.shinobiextras.item.JutsuScrollItem;
import com.shinobiextras.jutsu.ExtraJutsu;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ShinobiExtrasAddon.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ShinobiExtrasAddon.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShinobiExtrasAddon.MODID);

    /** Um pergaminho por jutsu (clique direito = aprende de graca). */
    public static final Map<ExtraJutsu, RegistryObject<Item>> SCROLLS = new EnumMap<>(ExtraJutsu.class);
    public static final Map<ShinobiEntity.Kind, RegistryObject<EntityType<ShinobiEntity>>> SHINOBIS = new EnumMap<>(ShinobiEntity.Kind.class);

    static {
        for (ExtraJutsu j : ExtraJutsu.values()) {
            SCROLLS.put(j, ITEMS.register("scroll_" + j.id, () -> new JutsuScrollItem(j)));
        }
        for (ShinobiEntity.Kind kind : ShinobiEntity.Kind.values()) {
            RegistryObject<EntityType<ShinobiEntity>> type = ENTITIES.register(kind.id,
                    () -> EntityType.Builder.<ShinobiEntity>of((t, l) -> new ShinobiEntity(t, l, kind), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(ShinobiExtrasAddon.MODID, kind.id).toString()));
            SHINOBIS.put(kind, type);
            ITEMS.register(kind.id + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(type, kind.eggPrimary, kind.eggSecondary, new Item.Properties()));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.shinobiextras"))
                    .icon(() -> new ItemStack(SCROLLS.get(ExtraJutsu.KIRIGAKURE).get()))
                    .displayItems((params, out) -> {
                        for (RegistryObject<Item> item : ITEMS.getEntries()) {
                            out.accept(item.get());
                        }
                    })
                    .build());
}
