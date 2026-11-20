package com.goodbird.npcgecko.client.model;

import com.goodbird.npcgecko.entity.EntityCustomModel;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.List;

public class ModelCustom extends AnimatedGeoModel<EntityCustomModel> {
    private static final ResourceLocation NO_ANIMATION = new ResourceLocation("npcgecko", "animations/none.animations.json");
    private static final ResourceLocation MISSING_MODEL = new ResourceLocation("npcgecko", "geo/modelnotfound.geo.json");
    private static final ResourceLocation MISSING_MODEL_TEXTURE = new ResourceLocation("npcgecko", "textures/model/alphabet.png");

    @Override
    public ResourceLocation getAnimationFileLocation(EntityCustomModel entity) {
        if(!GeckoLibCache.getInstance().getAnimations().containsKey(entity.animResLoc)){
            return NO_ANIMATION;
        }
        return entity.animResLoc;
    }

    @Override
    public ResourceLocation getModelLocation(EntityCustomModel entity) {
        if(!GeckoLibCache.getInstance().getGeoModels().containsKey(entity.modelResLoc)){
            return MISSING_MODEL;
        }
        return entity.modelResLoc;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCustomModel entity) {
        if(!GeckoLibCache.getInstance().getGeoModels().containsKey(entity.modelResLoc)){
            return MISSING_MODEL_TEXTURE;
        }
        return entity.textureResLoc;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setLivingAnimations(EntityCustomModel entity, Integer uniqueID, AnimationEvent animationEvent) {
        super.setLivingAnimations(entity, uniqueID, animationEvent);
        IBone head = getAnimationProcessor().getBone(entity.headBoneName);
        if (head == null) return;
        if (entity.dialogueMovesHead()) {
            entity.rememberAnimatedHeadPose(head.getRotationY(), head.getRotationX());
            return;
        }
        if (animationEvent == null) return;
        List<?> poses = animationEvent.getExtraDataOfType(EntityModelData.class);
        if (poses.isEmpty()) return;
        EntityModelData pose = (EntityModelData) poses.get(0);
        entity.applyHeadLook(pose);
        head.setRotationX(pose.headPitch * ((float) Math.PI / 180F));
        head.setRotationY(pose.netHeadYaw * ((float) Math.PI / 180F));
    }
}
