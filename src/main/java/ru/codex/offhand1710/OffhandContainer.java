package ru.codex.offhand1710;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Adds the offhand slot to the player's own inventory container (ContainerPlayer), on both sides.
 *
 * The slot is appended after the 45 vanilla slots, so vanilla slot numbers do not move. Because both the
 * server and the client container have it, the normal container sync (window 0) keeps the owner's offhand
 * in step, and every vanilla click works on it: pick up, place, split, drag, number keys, shift-click out.
 * Position 77,62 in the inventory screen is where 1.12.2 draws its offhand slot.
 */
public final class OffhandContainer {
    public static final int INVENTORY_X = 77;
    public static final int INVENTORY_Y = 62;

    private OffhandContainer() {
    }

    @SuppressWarnings("unchecked")
    public static void addSlot(EntityPlayer player) {
        Container container = player.inventoryContainer;
        if (container == null || findSlot(container) != null) {
            return;
        }
        // Same steps as the protected Container.addSlotToContainer.
        SlotOffhand slot = new SlotOffhand(new InventoryOffhand(player), INVENTORY_X, INVENTORY_Y);
        slot.slotNumber = container.inventorySlots.size();
        container.inventorySlots.add(slot);
        container.inventoryItemStacks.add(null);
    }

    public static SlotOffhand findSlot(Container container) {
        if (container == null) {
            return null;
        }
        for (Object slot : container.inventorySlots) {
            if (slot instanceof SlotOffhand) {
                return (SlotOffhand) slot;
            }
        }
        return null;
    }
}
