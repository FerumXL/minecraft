package ru.codex.offhand1710;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public final class OffhandMessages {
    /** What the player was looking at when the main hand did nothing and the offhand gets its turn. */
    public static final int USE_AIR = 0;
    public static final int USE_BLOCK = 1;
    public static final int USE_ENTITY = 2;

    /** Bits of UseResult: which first-person animation the client plays for the offhand. */
    public static final int RESULT_SWING = 1;
    public static final int RESULT_EQUIP = 2;

    private OffhandMessages() {
    }

    /** Client -> server: swap the selected hotbar item with the offhand (key F, as in 1.12.2). */
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

    /** Client -> server: right click with the offhand after the main hand did nothing. */
    public static final class UseRequest implements IMessage {
        /** Echoed in UseResult so the client can skip animations it already played when predicting. */
        public int sequence;
        public int kind;
        public int entityId;
        public int x;
        public int y;
        public int z;
        public int side;
        public float hitX;
        public float hitY;
        public float hitZ;

        public UseRequest() {
        }

        public static UseRequest air() {
            UseRequest request = new UseRequest();
            request.kind = USE_AIR;
            return request;
        }

        public static UseRequest entity(int entityId) {
            UseRequest request = new UseRequest();
            request.kind = USE_ENTITY;
            request.entityId = entityId;
            return request;
        }

        public static UseRequest block(int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
            UseRequest request = new UseRequest();
            request.kind = USE_BLOCK;
            request.x = x;
            request.y = y;
            request.z = z;
            request.side = side;
            request.hitX = hitX;
            request.hitY = hitY;
            request.hitZ = hitZ;
            return request;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sequence = buffer.readInt();
            kind = buffer.readByte();
            entityId = buffer.readInt();
            x = buffer.readInt();
            y = buffer.readInt();
            z = buffer.readInt();
            side = buffer.readByte();
            hitX = buffer.readFloat();
            hitY = buffer.readFloat();
            hitZ = buffer.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(sequence);
            buffer.writeByte(kind);
            buffer.writeInt(entityId);
            buffer.writeInt(x);
            buffer.writeInt(y);
            buffer.writeInt(z);
            buffer.writeByte(side);
            buffer.writeFloat(hitX);
            buffer.writeFloat(hitY);
            buffer.writeFloat(hitZ);
        }
    }

    /** Server -> client: the offhand action happened; play the swing and/or re-equip animation. */
    public static final class UseResult implements IMessage {
        public int sequence;
        public int flags;

        public UseResult() {
        }

        public UseResult(int sequence, int flags) {
            this.sequence = sequence;
            this.flags = flags;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sequence = buffer.readInt();
            flags = buffer.readByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(sequence);
            buffer.writeByte(flags);
        }
    }

    /**
     * Client -> server: the creative inventory changed the offhand slot. Vanilla's creative slot packet only
     * accepts the 45 vanilla slots, so the offhand slot needs its own message (creative mode only).
     */
    public static final class CreativeSet implements IMessage {
        public ItemStack stack;

        public CreativeSet() {
        }

        public CreativeSet(ItemStack stack) {
            this.stack = stack == null ? null : stack.copy();
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            stack = ByteBufUtils.readItemStack(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            ByteBufUtils.writeItemStack(buffer, stack);
        }
    }

    /** Server -> clients: the offhand item of a player, for third-person rendering. */
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

    // Network handlers run on the netty thread; the work is queued for the server thread.

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
            OffhandEvents.queueUse(context.getServerHandler().playerEntity.getUniqueID(), message);
            return null;
        }
    }

    public static final class CreativeSetHandler implements IMessageHandler<CreativeSet, IMessage> {
        @Override
        public IMessage onMessage(CreativeSet message, MessageContext context) {
            OffhandEvents.queueCreativeSet(context.getServerHandler().playerEntity.getUniqueID(), message);
            return null;
        }
    }

    public static void synchronize(EntityPlayerMP player) {
        MC112Bridge.NETWORK.channel.sendToAllAround(new Sync(player),
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 128.0D));
    }

    public static void sendResult(EntityPlayerMP player, int sequence, int flags) {
        MC112Bridge.NETWORK.channel.sendTo(new UseResult(sequence, flags), player);
    }
}
