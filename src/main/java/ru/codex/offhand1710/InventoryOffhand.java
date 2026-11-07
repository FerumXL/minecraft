package ru.codex.offhand1710;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** One-slot inventory view of a player's offhand, used by the offhand slot in the player's container. */
public final class InventoryOffhand implements IInventory {
    private final EntityPlayer player;

    public InventoryOffhand(EntityPlayer player) {
        this.player = player;
    }

    private OffhandInventory data() {
        return OffhandInventory.get(player);
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? data().getStack() : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = getStackInSlot(slot);
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            setInventorySlotContents(slot, null);
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.stackSize == 0) {
            setInventorySlotContents(slot, null);
        } else {
            markDirty();
        }
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            data().setStack(stack);
            markDirty();
        }
    }

    @Override
    public String getInventoryName() {
        return "container.mc112bridge.offhand";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        // Other players see the offhand item through the sync packet; the owner gets it through the container.
        if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) {
            OffhandMessages.synchronize((EntityPlayerMP) player);
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer other) {
        return other == player;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
