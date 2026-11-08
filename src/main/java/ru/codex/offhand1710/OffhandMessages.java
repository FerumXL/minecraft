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
    /** The offhand started a held use (food, potion, bow...); UseResult.useCount holds its duration. */
    public static final int RESULT_START_USE = 4;
    /** The held use ended (released, finished or cancelled). */
    public static final int RESULT_STOP_USE = 8;
    /** The held use finished (food eaten, potion drunk): final burst of eating particles. */
    public static final int RESULT_FINISHED = 16;
    /** An axe knocked the shield down; UseResult.useCount holds the cooldown in ticks. */
    public static final int RESULT_SHIELD_DISABLED = 32;

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
        public int useCount;

        public UseResult() {
        }

        public UseResult(int sequence, int flags, int useCount) {
            this.sequence = sequence;
            this.flags = flags;
            this.useCount = useCount;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sequence = buffer.readInt();
            flags = buffer.readByte();
            useCount = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(sequence);
            buffer.writeByte(flags);
            buffer.writeInt(useCount);
        }
    }

    /** Client -> server: the use button was released while the offhand item was in use (shoot the bow). */
    public static final class Release implements IMessage {
        public Release() {
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
        }

        @Override
        public void toBytes(ByteBuf buffer) {
        }
    }

    public static final class ReleaseHandler implements IMessageHandler<Release, IMessage> {
        @Override
        public IMessage onMessage(Release message, MessageContext context) {
            OffhandEvents.queueRelease(context.getServerHandler().playerEntity.getUniqueID());
            return null;
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
        /** Ticks left of the offhand held use, so other players see the eating / bow animation. */
        public int useCount;
        public int shieldCooldown;

        public Sync() {
        }

        public Sync(EntityPlayerMP player) {
            entityId = player.getEntityId();
            OffhandInventory offhand = OffhandInventory.get(player);
            ItemStack stack = offhand.getStack();
            this.stack = stack == null ? null : stack.copy();
            this.useCount = offhand.useCount;
            this.shieldCooldown = offhand.shieldCooldown;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            stack = ByteBufUtils.readItemStack(buffer);
            useCount = buffer.readInt();
            shieldCooldown = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            ByteBufUtils.writeItemStack(buffer, stack);
            buffer.writeInt(useCount);
            buffer.writeInt(shieldCooldown);
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

    /** Server -> clients: a player swung the offhand (the offhand version of the vanilla swing animation packet). */
    public static final class Swing implements IMessage {
        public int entityId;

        public Swing() {
        }

        public Swing(int entityId) {
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

    /** Client -> server: jump pressed while falling with an elytra. */
    public static final class ElytraStart implements IMessage {
        public ElytraStart() {
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
        }

        @Override
        public void toBytes(ByteBuf buffer) {
        }
    }

    public static final class ElytraStartHandler implements IMessageHandler<ElytraStart, IMessage> {
        @Override
        public IMessage onMessage(ElytraStart message, MessageContext context) {
            // Network thread: the server tick of this player picks it up.
            OffhandInventory.get(context.getServerHandler().playerEntity).elytraStartRequested = true;
            return null;
        }
    }

    /** Server -> clients: whether a player is gliding, and the length of a firework boost that just started. */
    public static final class ElytraState implements IMessage {
        public int entityId;
        public boolean flying;
        public int boost;

        public ElytraState() {
        }

        public ElytraState(int entityId, boolean flying, int boost) {
            this.entityId = entityId;
            this.flying = flying;
            this.boost = boost;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            flying = buffer.readBoolean();
            boost = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeBoolean(flying);
            buffer.writeInt(boost);
        }
    }

    /** Server -> client: a shulker bullet hit this player. */
    public static final class Levitate implements IMessage {
        public int ticks;

        public Levitate() {
        }

        public Levitate(int ticks) {
            this.ticks = ticks;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            ticks = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(ticks);
        }
    }

    public static final int PARTICLE_SWEEP = 0;
    public static final int PARTICLE_DAMAGE = 1;

    /** Server -> clients: the 1.12.2 particle packet (SPacketParticles) for the particles 1.7.10 does not have. */
    public static final class Particle implements IMessage {
        public int kind;
        public double x;
        public double y;
        public double z;
        public int count;
        public float offsetX;
        public float offsetY;
        public float offsetZ;
        public float speed;

        public Particle() {
        }

        public Particle(int kind, double x, double y, double z, int count, float offsetX, float offsetY, float offsetZ,
                float speed) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            kind = buffer.readByte();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            count = buffer.readInt();
            offsetX = buffer.readFloat();
            offsetY = buffer.readFloat();
            offsetZ = buffer.readFloat();
            speed = buffer.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeByte(kind);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeInt(count);
            buffer.writeFloat(offsetX);
            buffer.writeFloat(offsetY);
            buffer.writeFloat(offsetZ);
            buffer.writeFloat(speed);
        }
    }

    /** Sent to everyone around; the swinging player's own client ignores it (it has animated already). */
    public static void broadcastSwing(EntityPlayerMP player) {
        MC112Bridge.NETWORK.channel.sendToAllAround(new Swing(player.getEntityId()),
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64.0D));
    }

    public static void sendResult(EntityPlayerMP player, int sequence, int flags) {
        sendResult(player, sequence, flags, 0);
    }

    public static void sendResult(EntityPlayerMP player, int sequence, int flags, int useCount) {
        MC112Bridge.NETWORK.channel.sendTo(new UseResult(sequence, flags, useCount), player);
    }
}
