package com.goodbird.npcgecko.network;

import com.goodbird.npcgecko.entity.EntityCustomModel;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.function.Consumer;

@SideOnly(Side.CLIENT)
final class ClientNpcAnimation {
    private ClientNpcAnimation() {
    }

    static void forModel(int entityId, Consumer<EntityCustomModel> action) {
        Minecraft client = Minecraft.getMinecraft();
        World world = client.theWorld;
        if (world == null) return;
        client.func_152344_a(() -> {
            if (client.theWorld != world) return;
            Entity entity = world.getEntityByID(entityId);
            if (!(entity instanceof EntityCustomNpc)) return;
            EntityCustomNpc npc = (EntityCustomNpc) entity;
            Entity model = npc.modelData == null ? null : npc.modelData.getEntity(npc);
            if (model instanceof EntityCustomModel) action.accept((EntityCustomModel) model);
        });
    }
}
