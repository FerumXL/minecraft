package ru.codex.offhand1710;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * ContainerHorseInventory of 1.12.2 for a llama: the carpet slot at (8, 36), with a chest 3 rows of strength columns
 * from (80, 18), the player's inventory from (8, 84).
 */
public final class LlamaContainer extends Container {
    public final MoreMobs.Llama llama;
    private final int llamaSlots;
    /** Chest columns (0 without a chest); sent with the window, as the client may not have the llama's data yet. */
    public final int cols;

    public LlamaContainer(InventoryPlayer player, final MoreMobs.Llama llama) {
        this(player, llama, llama.chested() ? llama.strength() : 0);
    }

    public LlamaContainer(InventoryPlayer player, final MoreMobs.Llama llama, int cols) {
        this.cols = cols;
        this.llama = llama;
        llama.inv.openInventory();
        addSlotToContainer(new Slot(llama.inv, 0, 8, 36) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.carpet);
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        if (cols > 0) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < cols; col++) {
                    addSlotToContainer(new Slot(llama.inv, 1 + row * cols + col, 80 + col * 18, 18 + row * 18));
                }
            }
        }
        llamaSlots = inventorySlots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(player, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return llama.isEntityAlive() && llama.getDistanceToEntity(player) < 8.0F;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < llamaSlots) {
            if (!mergeItemStack(stack, llamaSlots, inventorySlots.size(), true)) {
                return null;
            }
        } else if (((Slot) inventorySlots.get(0)).isItemValid(stack) && !((Slot) inventorySlots.get(0)).getHasStack()) {
            // mergeItemStack of 1.7.10 ignores the slot limit: one carpet only
            ((Slot) inventorySlots.get(0)).putStack(stack.splitStack(1));
        } else if (llamaSlots <= 1 || !mergeItemStack(stack, 1, llamaSlots, false)) {
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        llama.inv.closeInventory();
    }
}
