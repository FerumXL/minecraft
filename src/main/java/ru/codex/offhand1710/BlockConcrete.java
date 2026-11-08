package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** Concrete of 1.12.2: rock, hardness 1.8 (explosion resistance follows from it), stone sounds, needs a pickaxe. */
public final class BlockConcrete extends Block {
    private final int colorIndex;

    public BlockConcrete(String color, int colorIndex) {
        super(Material.rock);
        this.colorIndex = colorIndex;
        setBlockName(MC112Bridge.MOD_ID + ".concrete_" + color);
        setBlockTextureName(MC112Bridge.MOD_ID + ":concrete_" + color);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(1.8F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
    }

    /** Map color of the dye, as in 1.12.2. */
    @Override
    public MapColor getMapColor(int metadata) {
        return MapColor.getMapColorForBlockColored(colorIndex);
    }
}
