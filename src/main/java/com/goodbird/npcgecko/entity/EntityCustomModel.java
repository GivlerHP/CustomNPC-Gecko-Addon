package com.goodbird.npcgecko.entity;

import com.goodbird.npcgecko.constants.EnumSyncAutoAnim;
import com.goodbird.npcgecko.data.CustomModelData;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.EntityCreature;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import noppes.npcs.controllers.data.TintData;
import noppes.npcs.client.gui.player.DialogCamera;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.resource.GeckoLibCache;

public class EntityCustomModel extends EntityCreature implements IAnimatable, IAnimationTickable {
    private final AnimationFactory factory = new AnimationFactory(this);
    public ResourceLocation modelResLoc = new ResourceLocation(CustomModelData.DEFAULT_MODEL);
    public ResourceLocation animResLoc = new ResourceLocation(CustomModelData.DEFAULT_ANIMATION);
    public ResourceLocation textureResLoc = new ResourceLocation(CustomModelData.DEFAULT_TEXTURE);
    public String idleAnimName = "";
    public String walkAnimName = "";
    public String hurtAnimName = "";
    public String meleeAttackAnimName = "";
    public String rangedAttackAnimName = "";
    public String headBoneName = "head";
    public AnimationBuilder dialogAnim = null;
    private AnimationController<EntityCustomModel> movement;
    private AnimationBuilder ownedGesture;
    private ResourceLocation gestureFile;
    private Animation gestureClip;
    private Object gestureResources;
    private boolean gestureMovesHead;
    public AnimationBuilder manualAnim = null;
    public AnimationBuilder attackAnim = null;
    public AnimationBuilder hurtAnim = null;
    public ItemStack leftHeldItem;
    public boolean isSemiVisible = false;
    public TintData tintData;
    public float renderHeadYaw = Float.NaN;
    public float renderHeadPitch = Float.NaN;
    public DialogCamera.HeadLook dialogueHeadLook;
    private final NpcHeadPose headPose = new NpcHeadPose();

    private <E extends IAnimatable> PlayState predicateMovement(AnimationEvent<E> event) {
        if (ownedGesture != null && (dialogAnim != ownedGesture || manualAnim != null || attackAnim != null
            || hurtAnim != null || hurtTime > 0 || deathTime > 0 || !animResLoc.equals(gestureFile)
            || !gestureResourcesAvailable())) {
            endDialogueGesture(ownedGesture);
        }
        if (!GeckoLibCache.getInstance().getAnimations().containsKey(animResLoc)) return PlayState.STOP;
        if (manualAnim != null) {
            if (event.getController().currentAnimationBuilder == manualAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                manualAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != manualAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(manualAnim);
                return PlayState.CONTINUE;
            }
        }
        if (dialogAnim != null) {
            if (event.getController().currentAnimationBuilder == dialogAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                if (dialogAnim == ownedGesture) endDialogueGesture(ownedGesture);
                else dialogAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != dialogAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(dialogAnim);
                return PlayState.CONTINUE;
            }
        }
        if (attackAnim != null) {
            if (event.getController().currentAnimationBuilder == attackAnim
                && event.getController().getAnimationState() == AnimationState.Stopped) {
                attackAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != attackAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(attackAnim);
                return PlayState.CONTINUE;
            }
        }
        if (hurtAnim != null) {
            if (event.getController().currentAnimationBuilder == hurtAnim
                && event.getController().getAnimationState() == AnimationState.Stopped) {
                hurtAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != hurtAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(hurtAnim);
                return PlayState.CONTINUE;
            }
        }
        if (!event.isMoving() || walkAnimName.isEmpty()) {
            if (!idleAnimName.isEmpty()) {
                event.getController().setAnimation(new AnimationBuilder().loop(idleAnimName));
            } else {
                return PlayState.STOP;
            }
        } else {
            event.getController().setAnimation(new AnimationBuilder().loop(walkAnimName));
        }
        return PlayState.CONTINUE;
    }

    private boolean gestureResourcesAvailable() {
        GeckoLibCache cache = GeckoLibCache.getInstance();
        Object revision = cache.getResourceRevision();
        if (revision == gestureResources) return true;
        gestureResources = revision;
        AnimationFile file = cache.getAnimations().get(gestureFile);
        return file != null && file.getAnimation(gestureClip.animationName) == gestureClip
            && cache.getGeoModels().containsKey(modelResLoc);
    }

    public DialoguePlayback beginDialogueGesture(Animation clip, Object resources, boolean movesHead, boolean loop) {
        if (manualAnim != null || attackAnim != null || hurtAnim != null || hurtTime > 0 || deathTime > 0) return null;
        AnimationBuilder builder = new AnimationBuilder();
        AnimationBuilder gesture = loop ? builder.loop(clip.animationName) : builder.playOnce(clip.animationName);
        ownedGesture = dialogAnim = gesture;
        gestureFile = animResLoc;
        gestureClip = clip;
        gestureResources = resources;
        gestureMovesHead = movesHead;
        return new DialoguePlayback(gesture);
    }

    public final class DialoguePlayback {
        private final AnimationBuilder gesture;

        private DialoguePlayback(AnimationBuilder gesture) {
            this.gesture = gesture;
        }

        public boolean canRebind() {
            return ownedGesture == gesture && dialogAnim == gesture && (movement == null
                || movement.currentAnimationBuilder != gesture || movement.getAnimationState() == AnimationState.Transitioning);
        }

        public void stop() {
            endDialogueGesture(gesture);
        }
    }

    private void endDialogueGesture(AnimationBuilder gesture) {
        if (gesture == null || ownedGesture != gesture) return;
        if (dialogAnim == gesture) dialogAnim = null;
        if (movement != null && movement.currentAnimationBuilder == gesture) movement.markNeedsReload();
        ownedGesture = null;
        gestureResources = null;
        gestureFile = null;
        gestureClip = null;
        gestureMovesHead = false;
    }

    public boolean dialogueMovesHead() {
        return gestureMovesHead && movement != null && movement.currentAnimationBuilder == ownedGesture
            && movement.getAnimationState() != AnimationState.Stopped;
    }

    /** Applies the render's head direction while leaving the entity's rotations alone. */
    public void applyHeadLook(EntityModelData pose) {
        float yaw = Float.isFinite(renderHeadYaw) ? renderHeadYaw : pose.netHeadYaw;
        float pitch = Float.isFinite(renderHeadPitch) ? renderHeadPitch : pose.headPitch;
        DialogCamera.HeadLook look = dialogueHeadLook;
        if (look != null
            && manualAnim == null && attackAnim == null && hurtAnim == null && hurtTime <= 0 && deathTime <= 0) {
            // Gecko's bone axes use the opposite sign to the vanilla model angles.
            yaw = -look.yaw;
            pitch = -look.pitch;
        }
        headPose.track(yaw, pitch, System.nanoTime());
        pose.netHeadYaw = headPose.yaw;
        pose.headPitch = headPose.pitch;
    }

    public void rememberAnimatedHeadPose(float yawRadians, float pitchRadians) {
        headPose.remember((float) Math.toDegrees(yawRadians), (float) Math.toDegrees(pitchRadians), System.nanoTime());
    }

    public void setManualAnimation(AnimationBuilder builder) {
        endDialogueGesture(ownedGesture);
        manualAnim = builder;
    }

    public void setDialogAnim(String name) {
        endDialogueGesture(ownedGesture);
        dialogAnim = new AnimationBuilder().playOnce(name);
    }
    public void activateReceivedAnim(EnumSyncAutoAnim type) {
        endDialogueGesture(ownedGesture);
        if(type == EnumSyncAutoAnim.HURT){
            if(!hurtAnimName.isEmpty())
                hurtAnim = new AnimationBuilder().playOnce(hurtAnimName);
        }else if(type == EnumSyncAutoAnim.MELEE_ATTACK){
            if(!meleeAttackAnimName.isEmpty())
                attackAnim = new AnimationBuilder().playOnce(meleeAttackAnimName);
        }else if(type == EnumSyncAutoAnim.RANGED_ATTACK){
            if(!rangedAttackAnimName.isEmpty())
                attackAnim = new AnimationBuilder().playOnce(rangedAttackAnimName);
        }
    }

    public EntityCustomModel(World worldIn) {
        super(worldIn);
        this.ignoreFrustumCheck = true;
        this.setSize(0.7F, 2F);
    }

    @Override
    public void registerControllers(AnimationData data) {
        movement = new AnimationController<>(this, "movement", 10, this::predicateMovement);
        data.addAnimationController(movement);
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    public void releasePreviewTicker() {
        AnimationData data = factory.getAnimationData(getUniqueID().hashCode());
        if (data != null && data.ticker != null) {
            FMLCommonHandler.instance().bus().unregister(data.ticker);
            data.ticker = null;
        }
    }

    @Override
    public int tickTimer() {
        return ticksExisted;
    }
}
