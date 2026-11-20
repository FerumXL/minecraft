package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * 1.12.2 shoulder parrots (EntityAILandOnOwnersShoulder, EntityPlayer.addShoulderEntity / spawnShoulderEntities):
 * a tamed standing parrot that touches its owner on the ground lands on the free shoulder (left first) and is kept
 * as NBT in the player data; it jumps off when the player falls more than 0.5, is in water, rides, sleeps or is hurt.
 */
public final class Shoulder112 {
    public static final String LEFT = "mc112ShoulderLeft", RIGHT = "mc112ShoulderRight";

    /** From Parrot.onLivingUpdate on the server. */
    public static void tryLand(MoreMobs.Parrot parrot) {
        if (!parrot.isTamed() || parrot.isSitting() || parrot.getLeashed() || parrot.ticksExisted < 100) {
            return;
        }
        Entity owner = parrot.getOwner();
        if (!(owner instanceof EntityPlayerMP) || owner.worldObj != parrot.worldObj) {
            return;
        }
        EntityPlayerMP p = (EntityPlayerMP) owner;
        if (p.capabilities.isFlying || p.isInWater() || p.ridingEntity != null || !p.onGround || p.isPlayerSleeping()) {
            return;
        }
        NBTTagCompound data = p.getEntityData();
        String slot = !data.hasKey(LEFT) ? LEFT : !data.hasKey(RIGHT) ? RIGHT : null;
        if (slot == null) {
            return;
        }
        if (parrot.ticksExisted % 10 == 0 && parrot.getDistanceSqToEntity(p) > 1.0D) {
            parrot.getNavigator().tryMoveToEntityLiving(p, 1.0D);
        }
        // 1.12 lands on touching the owner; the 1.7.10 follow-owner AI stops the parrot 1 block away, so next to the
        // owner (under 1.41 blocks) is enough here
        if (parrot.getDistanceSqToEntity(p) < 2.0D) {
            NBTTagCompound tag = new NBTTagCompound();
            if (parrot.writeToNBTOptional(tag)) {
                data.setTag(slot, tag);
                parrot.setDead();
                sync(p);
            }
        }
    }

    public static int variant(NBTTagCompound data, String slot) {
        return data.hasKey(slot) ? data.getCompoundTag(slot).getInteger("Variant") : -1;
    }

    static void dropAll(EntityPlayer p) {
        NBTTagCompound data = p.getEntityData();
        boolean changed = false;
        for (String slot : new String[] {LEFT, RIGHT}) {
            if (data.hasKey(slot)) {
                Entity e = EntityList.createEntityFromNBT(data.getCompoundTag(slot), p.worldObj);
                if (e != null) {
                    e.setPosition(p.posX + (slot == LEFT ? 0.5D : -0.5D), p.posY + 0.7D, p.posZ);
                    e.ticksExisted = 0;
                    p.worldObj.spawnEntityInWorld(e);
                }
                data.removeTag(slot);
                changed = true;
            }
        }
        if (changed && p instanceof EntityPlayerMP) {
            sync((EntityPlayerMP) p);
        }
    }

    static void sync(EntityPlayerMP p) {
        Msg m = new Msg(p.getEntityId(), variant(p.getEntityData(), LEFT), variant(p.getEntityData(), RIGHT));
        MC112Bridge.NETWORK.channel.sendToAllAround(m, new NetworkRegistry.TargetPoint(p.dimension, p.posX, p.posY, p.posZ, 128.0D));
        MC112Bridge.NETWORK.channel.sendTo(m, p);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer p = event.player;
        if (event.phase != TickEvent.Phase.END || p.worldObj.isRemote) {
            return;
        }
        NBTTagCompound data = p.getEntityData();
        if ((data.hasKey(LEFT) || data.hasKey(RIGHT))
            && (p.fallDistance > 0.5F || p.isInWater() || p.ridingEntity != null || p.isPlayerSleeping())) {
            dropAll(p);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            dropAll((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            dropAll((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onTrack(PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayerMP && event.entityPlayer instanceof EntityPlayerMP) {
            EntityPlayerMP t = (EntityPlayerMP) event.target;
            MC112Bridge.NETWORK.channel.sendTo(new Msg(t.getEntityId(), variant(t.getEntityData(), LEFT), variant(t.getEntityData(), RIGHT)),
                (EntityPlayerMP) event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            sync((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            sync((EntityPlayerMP) event.player);
        }
    }

    /** Server -> clients: parrot variants on a player's shoulders (-1 = none). */
    public static final class Msg implements IMessage {
        public int entityId, left, right;

        public Msg() {
        }

        public Msg(int entityId, int left, int right) {
            this.entityId = entityId;
            this.left = left;
            this.right = right;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            entityId = b.readInt();
            left = b.readByte();
            right = b.readByte();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(entityId);
            b.writeByte(left);
            b.writeByte(right);
        }
    }
}
