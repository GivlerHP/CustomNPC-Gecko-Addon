package com.goodbird.npcgecko.mixin.impl;

import com.goodbird.npcgecko.client.gui.SubGuiModelAnimation;
import com.goodbird.npcgecko.client.gui.SubGuiModelExtras;
import com.goodbird.npcgecko.data.CustomModelData;
import com.goodbird.npcgecko.entity.EntityCustomModel;
import com.goodbird.npcgecko.mixin.IDataDisplay;
import kamkeel.npcs.addon.client.GeckoAddonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import noppes.npcs.client.gui.model.GuiCreationScreen;
import noppes.npcs.client.gui.player.DialogCamera;
import noppes.npcs.client.gui.player.GuiDialogInteract;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.model.ModelMPM;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib3.core.IAnimatable;

@Mixin(GeckoAddonClient.class)
public class MixinGeckoAddonClient {

    @Shadow(remap = false) public boolean supportEnabled;

    /**
     * @author Goodbird
     * @reason List the model explicitly in the NPC type selector.
     */
    @Overwrite(remap = false)
    public Class<? extends EntityLivingBase> geoModelType() {
        return supportEnabled ? EntityCustomModel.class : null;
    }

    /**
     * @author Goodbird
     * @reason Use the built-in texture when switching to the Gecko model type.
     */
    @Overwrite(remap = false)
    public String geoModelDefaultTexture() {
        return CustomModelData.DEFAULT_TEXTURE;
    }

    /**
     * @author Goodbird
     * @reason Add Gecko-specific settings for the selected model.
     */
    @Overwrite(remap = false)
    public void showGeckoButtons(GuiCreationScreen creationScreen, EntityLivingBase entity){
        if (!supportEnabled || !(entity instanceof EntityCustomModel)) return;
        int x = creationScreen.guiLeft;
        int width = 190;
        creationScreen.addButton(new GuiNpcButton(204, x, creationScreen.guiTop + 22, width, 20, "gecko.modelAnimation"));
        creationScreen.addButton(new GuiNpcButton(203, x, creationScreen.guiTop + 45, width, 20, "gecko.modelExtras"));
    }

    /**
     * @author Goodbird
     * @reason Detach the animation ticker when the model preview closes.
     */
    @Overwrite(remap = false)
    public void releaseModelPreview(EntityLivingBase entity) {
        if (entity instanceof EntityCustomModel) ((EntityCustomModel) entity).releasePreviewTicker();
    }

    /**
     * @author Goodbird
     * @reason Open Gecko-specific settings from the model editor.
     */
    @Overwrite(remap = false)
    public void geckoGuiCreationScreenActionPerformed(GuiCreationScreen creationScreen, GuiNpcButton button){
        if(!supportEnabled)
            return;

        if(button.id == 203){
            creationScreen.setSubGui(new SubGuiModelExtras(creationScreen.npc));
        }
        if (button.id == 204) creationScreen.setSubGui(new SubGuiModelAnimation(creationScreen.npc));
    }

    /**
     * @author Goodbird
     * @reason Init Model Animation Button for NPCs
     */
    @Overwrite(remap = false)
    public void geckoNpcDisplayInitGui(GuiNPCInterface2 gui){
        if(!supportEnabled)
            return;

        int y = gui.guiTop + 188;
        gui.addLabel(new GuiNpcLabel(212, "gecko.modelAnimation", gui.guiLeft + 185, y + 5));
        gui.addButton(new GuiNpcButton(212, gui.guiLeft + 300, y, 100, 20, "selectServer.edit"));
        if (!((IDataDisplay)gui.npc.display).hasCustomModel()) {
            gui.getLabel(212).enabled=false;
            gui.getButton(212).setVisible(false);
            gui.getButton(212).setEnabled(false);
        }
    }

    /**
     * @author Goodbird
     * @reason Add Support for Model Animation button on Npc Display
     */
    @Overwrite(remap = false)
    public void geckoNpcDisplayActionPerformed(GuiNPCInterface2 gui, GuiNpcButton btn){
        if(!supportEnabled)
            return;

        if(btn.id == 212){
            gui.setSubGui(new SubGuiModelAnimation(gui.npc));
        }
    }

    /**
     * @author KAMKEEEL
     * @reason Check if Entity is a Gecko Model
     */
    @Overwrite(remap = false)
    public boolean isGeckoModel(ModelBase mainModel){
        if(!supportEnabled)
            return false;

        return (mainModel instanceof ModelMPM && ((ModelMPM) mainModel).entity instanceof IAnimatable);
    }

    /**
     * @author Goodbird
     * @reason Render Gecko Model Support
     */
    @Overwrite(remap = false)
    public void geckoRenderModel(ModelMPM mainModel, EntityNPCInterface npc, float rot, float partial) {
        geckoRenderModel(mainModel, npc, rot, partial, Float.NaN, Float.NaN);
    }

    /**
     * @author Goodbird
     * @reason Use the same head angles as the renderer drawing the NPC's body
     */
    @Overwrite(remap = false)
    public void geckoRenderModel(ModelMPM mainModel, EntityNPCInterface npc, float rot, float partial,
                                 float headYaw, float headPitch) {
        GL11.glRotatef(180, 1,0,0);
        GL11.glTranslated(0, -1.5,0);

        Minecraft mc = Minecraft.getMinecraft();
        boolean invisible = npc.isInvisible();
        if (invisible && npc.isInvisibleToPlayer(mc.thePlayer)) return;

        if (mainModel.entity instanceof EntityCustomModel) {
            EntityCustomModel model = (EntityCustomModel) mainModel.entity;
            model.isSemiVisible = invisible;
            boolean speaking = GuiDialogInteract.prepareSpeakerRender(mc, npc, model);
            DialogCamera.HeadLook previousLook = model.dialogueHeadLook;
            float previousYaw = model.renderHeadYaw;
            float previousPitch = model.renderHeadPitch;
            model.renderHeadYaw = -headYaw;
            model.renderHeadPitch = -headPitch;
            model.dialogueHeadLook = speaking
                ? DialogCamera.headLook(npc, RenderNPCInterface.renderBodyYaw(npc, partial), partial) : null;
            try {
                RenderManager.instance.renderEntityWithPosYaw(model, 0, 0, 0, rot, partial);
            } finally {
                model.dialogueHeadLook = previousLook;
                model.renderHeadYaw = previousYaw;
                model.renderHeadPitch = previousPitch;
            }
        } else if (!invisible) {
            RenderManager.instance.renderEntityWithPosYaw(mainModel.entity, 0, 0, 0, rot, partial);
        } else {
            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.15F);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
            try {
                RenderManager.instance.renderEntityWithPosYaw(mainModel.entity, 0, 0, 0, rot, partial);
            } finally {
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glPopMatrix();
                GL11.glDepthMask(true);
            }
        }
    }
}
