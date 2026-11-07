package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.util.IIcon;

/** The offhand slot of the player inventory (the shield slot of 1.12.2). Accepts any item. */
public final class SlotOffhand extends Slot {
    /** Shield silhouette shown while the slot is empty; registered on the client when the item atlas is stitched. */
    @SideOnly(Side.CLIENT)
    public static IIcon emptyIcon;

    public SlotOffhand(IInventory inventory, int x, int y) {
        super(inventory, 0, x, y);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBackgroundIconIndex() {
        return emptyIcon;
    }
}
