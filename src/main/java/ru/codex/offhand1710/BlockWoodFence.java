package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.IBlockAccess;

/**
 * Fence of another wood type (1.8+), textured with the vanilla planks of that wood.
 *
 * Connects like 1.12.2: to any fence of the same material (all wooden fences), to any fence gate and to solid
 * full blocks except pumpkins and melons. The vanilla 1.7.10 oak fence only connects to itself and to the oak
 * gate, so an oak fence does not reach out towards these (they do reach towards it).
 */
public final class BlockWoodFence extends BlockFence {
    private final MapColor mapColor;

    public BlockWoodFence(String wood, String planksTexture, MapColor mapColor) {
        super(planksTexture, Material.wood);
        this.mapColor = mapColor;
        setBlockName(MC112Bridge.MOD_ID + "." + wood + "_fence");
        setCreativeTab(CreativeTabs.tabDecorations);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
    }

    @Override
    public boolean canConnectFenceTo(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockFence) {
            return block.getMaterial() == getMaterial();
        }
        if (block instanceof BlockFenceGate) {
            return true;
        }
        Material material = block.getMaterial();
        return material.isOpaque() && block.renderAsNormalBlock() && material != Material.gourd;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
