package ru.codex.offhand1710;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 * Iron trapdoor of 1.12.2. The 1.7.10 trapdoor already ignores right clicks when its material is iron, so
 * only redstone opens it, as in 1.12.2.
 */
public final class BlockIronTrapdoor extends BlockTrapDoor {
    public BlockIronTrapdoor() {
        super(Material.iron);
        setBlockName(MC112Bridge.MOD_ID + ".iron_trapdoor");
        setBlockTextureName(MC112Bridge.MOD_ID + ":iron_trapdoor");
        setCreativeTab(CreativeTabs.tabRedstone);
        setHardness(5.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 0);
    }
}
