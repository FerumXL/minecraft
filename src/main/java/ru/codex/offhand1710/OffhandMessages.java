package ru.codex.offhand1710;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayerMP;

public final class OffhandMessages {
    private OffhandMessages() {
    }

    public static final class SwapRequest implements IMessage {
        public SwapRequest() {
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
        }

        @Override
        public void toBytes(ByteBuf buffer) {
        }
    }

    public static final class UseRequest implements IMessage {
        public int entityId = -1;

        public UseRequest() {
        }

        public UseRequest(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
        }
    }

    public static final class Sync implements IMessage {
        public int entityId;
        public ItemStack stack;

        public Sync() {
        }

        public Sync(EntityPlayerMP player) {
            entityId = player.getEntityId();
            ItemStack stack = OffhandInventory.get(player).getStack();
            this.stack = stack == null ? null : stack.copy();
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            stack = ByteBufUtils.readItemStack(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            ByteBufUtils.writeItemStack(buffer, stack);
        }
    }

    public static final class SwapHandler implements IMessageHandler<SwapRequest, IMessage> {
        @Override
        public IMessage onMessage(SwapRequest message, MessageContext context) {
            OffhandEvents.queueSwap(context.getServerHandler().playerEntity.getUniqueID());
            return null;
        }
    }

    public static final class UseHandler implements IMessageHandler<UseRequest, IMessage> {
        @Override
        public IMessage onMessage(UseRequest message, MessageContext context) {
            OffhandEvents.queueUse(context.getServerHandler().playerEntity.getUniqueID(), message.entityId);
            return null;
        }
    }

    public static void synchronize(EntityPlayerMP player) {
        MC112Bridge.NETWORK.channel.sendToAllAround(
                new Sync(player),
                new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                        player.dimension, player.posX, player.posY, player.posZ, 128.0D));
    }
}

