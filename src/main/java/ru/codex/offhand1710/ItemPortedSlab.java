package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.item.ItemSlab;

/**
 * ItemSlab with a constructor FML can find: GameRegistry.registerBlock looks the constructor up by the exact
 * classes of the extra arguments, and a Boolean argument never matches ItemSlab's primitive boolean.
 */
public final class ItemPortedSlab extends ItemSlab {
    public ItemPortedSlab(Block block, BlockPortedSlab single, BlockPortedSlab doubleSlab, Boolean isDouble) {
        super(block, single, doubleSlab, isDouble.booleanValue());
    }
}
