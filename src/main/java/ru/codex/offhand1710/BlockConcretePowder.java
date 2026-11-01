package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public final class BlockConcretePowder extends BlockFalling {
    private final Block concrete;

    public BlockConcretePowder(Block concrete, String unlocalizedName, String textureName) {
        this.concrete = concrete;
        setBlockName(unlocalizedName);
        setBlockTextureName(textureName);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabBlock);
        setHardness(0.5F);
        setResistance(0.5F);
        setStepSound(soundTypeSand);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        solidifyIfWet(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        solidifyIfWet(world, x, y, z);
    }

    private void solidifyIfWet(World world, int x, int y, int z) {
        if (world.isRemote) return;

        if (isWater(world, x - 1, y, z) || isWater(world, x + 1, y, z)
                || isWater(world, x, y - 1, z) || isWater(world, x, y + 1, z)
                || isWater(world, x, y, z - 1) || isWater(world, x, y, z + 1)) {
            world.setBlock(x, y, z, concrete, 0, 3);
        }
    }

    private boolean isWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == Blocks.water || block == Blocks.flowing_water;
    }
}

