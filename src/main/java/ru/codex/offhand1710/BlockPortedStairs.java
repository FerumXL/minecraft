package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.creativetab.CreativeTabs;

/** Stairs of 1.8+ blocks; hardness, resistance, sounds, textures and map color come from the full block. */
public final class BlockPortedStairs extends BlockStairs {
    public BlockPortedStairs(Block model, String name) {
        super(model, 0);
        setBlockName(MC112Bridge.MOD_ID + "." + name);
        setCreativeTab(CreativeTabs.tabBlock);
        // Vanilla only sets this for its own stairs when the block list is built; without it the stairs are dark.
        useNeighborBrightness = true;
    }
}
