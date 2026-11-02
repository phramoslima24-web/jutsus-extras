package com.shinobiextras.client;

import com.shinobiextras.ShinobiExtrasAddon;
import com.shinobiextras.entity.ShinobiEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShinobiRenderer extends HumanoidMobRenderer<ShinobiEntity, HumanoidModel<ShinobiEntity>> {
    public ShinobiRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ShinobiEntity entity) {
        return new ResourceLocation(ShinobiExtrasAddon.MODID, "textures/entity/" + entity.getKind().id + ".png");
    }
}
