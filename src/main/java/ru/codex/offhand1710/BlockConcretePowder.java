package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.World;

/**
 * Concrete powder of 1.12.2: falls like sand and turns into concrete when water touches it from a side or
 * from above (water below does not count). Any block with the water material counts, not only vanilla water.
 */
public final class BlockConcretePowder extends BlockFalling {
    private final Block concrete;
    private final int colorIndex;

    public BlockConcretePowder(Block concrete, String color, int colorIndex) {
        super(Material.sand);
        this.concrete = concrete;
        this.colorIndex = colorIndex;
        setBlockName(MC112Bridge.MOD_ID + ".concrete_powder_" + color);
        setBlockTextureName(MC112Bridge.MOD_ID + ":concrete_powder_" + color);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(0.5F);
        setStepSound(soundTypeSand);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!tryTouchWater(world, x, y, z)) {
            super.onBlockAdded(world, x, y, z);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!tryTouchWater(world, x, y, z)) {
            super.onNeighborBlockChange(world, x, y, z, neighbor);
        }
    }

    /** Called when a falling powder block lands (1.12.2 onEndFalling). */
    @Override
    public void func_149828_a(World world, int x, int y, int z, int metadata) {
        tryTouchWater(world, x, y, z);
    }

    private boolean tryTouchWater(World world, int x, int y, int z) {
        if (world.isRemote || world.getBlock(x, y, z) != this) {
            return false;
        }
        if (isWater(world, x, y + 1, z) || isWater(world, x - 1, y, z) || isWater(world, x + 1, y, z)
                || isWater(world, x, y, z - 1) || isWater(world, x, y, z + 1)) {
            world.setBlock(x, y, z, concrete, 0, 3);
            return true;
        }
        return false;
    }

    private static boolean isWater(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).getMaterial() == Material.water;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return MapColor.getMapColorForBlockColored(colorIndex);
    }
}
