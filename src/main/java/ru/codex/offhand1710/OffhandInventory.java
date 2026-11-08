package ru.codex.offhand1710;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.IExtendedEntityProperties;

public final class OffhandInventory implements IExtendedEntityProperties {
    public static final String KEY = "mc112bridge.offhand";
    private ItemStack stack;

    /**
     * Ticks left while the offhand item is being used (eating, drinking, drawing a bow), 0 when not in use.
     * Authoritative on the server; on clients it drives the animation. Not saved.
     */
    public int useCount;
    /** The offhand stack the use started with; the use stops if the offhand item is replaced. Server only. */
    public ItemStack useStack;

    public boolean isUsing() {
        return useCount > 0;
    }

    public static void register(Entity entity) {
        if (entity instanceof EntityPlayer && entity.getExtendedProperties(KEY) == null) {
            entity.registerExtendedProperties(KEY, new OffhandInventory());
        }
    }

    public static OffhandInventory get(EntityPlayer player) {
        register(player);
        return (OffhandInventory) player.getExtendedProperties(KEY);
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public void copyFrom(OffhandInventory other) {
        stack = other.stack == null ? null : other.stack.copy();
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        if (stack != null) {
            NBTTagCompound item = new NBTTagCompound();
            stack.writeToNBT(item);
            compound.setTag(KEY, item);
        } else {
            compound.removeTag(KEY);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        stack = compound.hasKey(KEY) ? ItemStack.loadItemStackFromNBT(compound.getCompoundTag(KEY)) : null;
    }

    @Override
    public void init(Entity entity, net.minecraft.world.World world) {
    }
}

