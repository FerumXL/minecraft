package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** EntityArmorStand of 1.8: holds armor, the part you click on decides the slot, the pose comes from NBT as in the original. */
public class EntityArmorStand112 extends EntityLiving {
    private static final int WATCH_FLAGS = 12;
    private static final int WATCH_POSE = 13;
    private static final String[] PARTS = {"Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg"};
    /** Default rotations of EntityArmorStand. */
    private static final float[] DEFAULT = {0, 0, 0, 0, 0, 0, -10, 0, -10, -15, 0, 10, -1, 0, -1, 1, 0, 1};
    private static final int FLAG_ARMS = 1;
    private static final int FLAG_NO_PLATE = 2;
    private long lastHit;

    public EntityArmorStand112(World world) {
        super(world);
        setSize(0.5F, 1.975F);
        stepHeight = 0.0F;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_FLAGS, Byte.valueOf((byte) 0));
        for (int i = 0; i < 18; i++) {
            dataWatcher.addObject(WATCH_POSE + i, Float.valueOf(DEFAULT[i]));
        }
    }

    public float pose(int part, int axis) {
        return dataWatcher.getWatchableObjectFloat(WATCH_POSE + part * 3 + axis);
    }

    public boolean showArms() {
        return (dataWatcher.getWatchableObjectByte(WATCH_FLAGS) & FLAG_ARMS) != 0;
    }

    public boolean showPlate() {
        return (dataWatcher.getWatchableObjectByte(WATCH_FLAGS) & FLAG_NO_PLATE) == 0;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("ShowArms", showArms());
        tag.setBoolean("NoBasePlate", !showPlate());
        NBTTagCompound pose = new NBTTagCompound();
        for (int part = 0; part < 6; part++) {
            NBTTagList list = new NBTTagList();
            for (int axis = 0; axis < 3; axis++) {
                list.appendTag(new NBTTagFloat(pose(part, axis)));
            }
            pose.setTag(PARTS[part], list);
        }
        tag.setTag("Pose", pose);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        int flags = (tag.getBoolean("ShowArms") ? FLAG_ARMS : 0) | (tag.getBoolean("NoBasePlate") ? FLAG_NO_PLATE : 0);
        dataWatcher.updateObject(WATCH_FLAGS, Byte.valueOf((byte) flags));
        NBTTagCompound pose = tag.getCompoundTag("Pose");
        for (int part = 0; part < 6; part++) {
            if (pose.hasKey(PARTS[part])) {
                NBTTagList list = pose.getTagList(PARTS[part], 5);
                for (int axis = 0; axis < 3; axis++) {
                    dataWatcher.updateObject(WATCH_POSE + part * 3 + axis, Float.valueOf(list.func_150308_e(axis)));
                }
            }
        }
    }

    @Override
    protected void updateEntityActionState() {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entity) {
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = rotationYaw;
        rotationYawHead = rotationYaw;
        prevRotationYawHead = rotationYaw;
        limbSwingAmount = 0.0F;
        prevLimbSwingAmount = 0.0F;
    }

    /** EntityArmorStand.getClickedSlot. */
    private int clickedSlot(double y) {
        if (y >= 0.1D && y < 0.55D && getEquipmentInSlot(1) != null) {
            return 1;
        }
        if (y >= 0.9D && y < 1.6D && getEquipmentInSlot(3) != null) {
            return 3;
        }
        if (y >= 0.4D && y < 1.2D && getEquipmentInSlot(2) != null) {
            return 2;
        }
        if (y >= 1.6D && getEquipmentInSlot(4) != null) {
            return 4;
        }
        return 0;
    }

    @Override
    protected boolean interact(EntityPlayer player) {
        if (worldObj.isRemote) {
            return true;
        }
        ItemStack held = player.getCurrentEquippedItem();
        int slot;
        if (held != null) {
            slot = EntityLiving.getArmorPosition(held);
            if (slot == 0 && !showArms()) {
                return true;
            }
        } else {
            slot = clickedSlot(hitHeight(player));
        }
        ItemStack old = getEquipmentInSlot(slot);
        if (held == null && old == null) {
            return true;
        }
        if (held != null && old != null && held.stackSize > 1) {
            return true;
        }
        ItemStack placed = null;
        if (held != null) {
            placed = held.copy();
            placed.stackSize = 1;
            if (!player.capabilities.isCreativeMode) {
                held.stackSize--;
                if (held.stackSize <= 0) {
                    player.inventory.mainInventory[player.inventory.currentItem] = null;
                }
            }
        }
        setCurrentItemOrArmor(slot, placed);
        equipmentDropChances[slot] = 2.0F;
        if (old != null) {
            if (player.inventory.getCurrentItem() == null) {
                player.inventory.mainInventory[player.inventory.currentItem] = old;
            } else if (!player.inventory.addItemStackToInventory(old)) {
                entityDropItem(old, 1.0F);
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        return true;
    }

    /** 1.7.10 does not pass the hit position to entities: the player's line of sight is intersected with the box. */
    private double hitHeight(EntityPlayer player) {
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.boundingBox.minY + (double) player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eye.addVector(look.xCoord * 6.0D, look.yCoord * 6.0D, look.zCoord * 6.0D);
        AxisAlignedBB box = boundingBox.expand(0.1D, 0.0D, 0.1D);
        MovingObjectPosition hit = box.calculateIntercept(eye, end);
        return hit == null ? 1.0D : hit.hitVec.yCoord - boundingBox.minY;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (worldObj.isRemote || isDead || source.isFireDamage()) {
            return false;
        }
        if (source == DamageSource.outOfWorld) {
            setDead();
            return false;
        }
        boolean byPlayer = source.getEntity() instanceof EntityPlayer;
        if (byPlayer && ((EntityPlayer) source.getEntity()).capabilities.isCreativeMode) {
            setDead();
            return false;
        }
        if (byPlayer && worldObj.getTotalWorldTime() - lastHit > 5L) {
            lastHit = worldObj.getTotalWorldTime();
            playSound(MC112Bridge.MOD_ID + ":entity.armorstand.hit", 1.0F, 1.0F);
            return false;
        }
        if (byPlayer || source.isProjectile() || source.isExplosion()) {
            breakApart();
        }
        return false;
    }

    private void breakApart() {
        playSound(MC112Bridge.MOD_ID + ":entity.armorstand.break", 1.0F, 1.0F);
        entityDropItem(new ItemStack(Items112.armorStand), 0.0F);
        for (int slot = 0; slot < 5; slot++) {
            ItemStack stack = getEquipmentInSlot(slot);
            if (stack != null) {
                entityDropItem(stack, 1.0F);
                setCurrentItemOrArmor(slot, null);
            }
        }
        setDead();
    }

    @Override
    protected void dropEquipment(boolean byPlayer, int looting) {
    }

    public static final class StandItem extends Item {
        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
            if (side == 0) {
                return false;
            }
            int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
            Block clicked = world.getBlock(x, y, z);
            int px = x;
            int py = y;
            int pz = z;
            if (!clicked.isReplaceable(world, x, y, z)) {
                px += offsets[side][0];
                py += offsets[side][1];
                pz += offsets[side][2];
            }
            if (!world.isAirBlock(px, py, pz) && !world.getBlock(px, py, pz).isReplaceable(world, px, py, pz) || !world.isAirBlock(px, py + 1, pz)) {
                return false;
            }
            if (!world.isRemote) {
                EntityArmorStand112 stand = new EntityArmorStand112(world);
                float yaw = (float) MathHelper.floor_float((MathHelper.wrapAngleTo180_float(player.rotationYaw - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                stand.setLocationAndAngles((double) px + 0.5D, (double) py, (double) pz + 0.5D, yaw, 0.0F);
                world.spawnEntityInWorld(stand);
                world.playSoundAtEntity(stand, MC112Bridge.MOD_ID + ":entity.armorstand.place", 0.75F, 0.8F);
                if (!player.capabilities.isCreativeMode) {
                    stack.stackSize--;
                }
            }
            return true;
        }
    }

}
