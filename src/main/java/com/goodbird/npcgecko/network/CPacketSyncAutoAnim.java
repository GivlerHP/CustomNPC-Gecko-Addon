package com.goodbird.npcgecko.network;

import com.goodbird.npcgecko.constants.EnumSyncAutoAnim;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import noppes.npcs.entity.EntityNPCInterface;

public final class CPacketSyncAutoAnim implements IMessage, IMessageHandler<CPacketSyncAutoAnim, IMessage> {
    public EnumSyncAutoAnim animType;
    public int entityId;
    public CPacketSyncAutoAnim(){

    }
    public CPacketSyncAutoAnim(EntityNPCInterface npc, EnumSyncAutoAnim animType) {
        this.animType = animType;
        this.entityId = npc.getEntityId();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(animType.ordinal());
        buf.writeInt(entityId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        animType = EnumSyncAutoAnim.values()[buf.readInt()];
        entityId = buf.readInt();
    }

    @Override
    public IMessage onMessage(CPacketSyncAutoAnim message, MessageContext ctx) {
        ClientNpcAnimation.forModel(message.entityId, model -> model.activateReceivedAnim(message.animType));
        return null;
    }
}
