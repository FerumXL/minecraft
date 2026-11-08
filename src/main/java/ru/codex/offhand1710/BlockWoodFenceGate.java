package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.material.MapColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/** Fence gate of another wood type (1.8+); the vanilla gate always draws oak planks, this one its own wood. */
public final class BlockWoodFenceGate extends BlockFenceGate {
    private final int planksMeta;
    private final MapColor mapColor;

    public BlockWoodFenceGate(String wood, int planksMeta, MapColor mapColor) {
        this.planksMeta = planksMeta;
        this.mapColor = mapColor;
        setBlockName(MC112Bridge.MOD_ID + "." + wood + "_fence_gate");
        setCreativeTab(CreativeTabs.tabRedstone);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return Blocks.planks.getIcon(side, planksMeta);
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
