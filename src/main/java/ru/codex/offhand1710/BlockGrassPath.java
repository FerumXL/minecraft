package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * Grass path of 1.12.2: 15/16 of a block high, made with a shovel on grass, drops dirt, turns back into
 * dirt when a solid block is above it. Sides use the top 15 rows of grass_path_side, as the 1.12.2 model.
 */
public final class BlockGrassPath extends Block {
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockGrassPath() {
        super(Material.ground);
        setBlockName(MC112Bridge.MOD_ID + ".grass_path");
        setBlockTextureName(MC112Bridge.MOD_ID + ":grass_path_side");
        setHardness(0.65F);
        setStepSound(soundTypeGrass);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.9375F, 1.0F);
        setLightOpacity(255);
        useNeighborBrightness = true;
        disableStats();
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(MC112Bridge.MOD_ID + ":grass_path_side");
        top = register.registerIcon(MC112Bridge.MOD_ID + ":grass_path_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        return face == 1 ? top : face == 0 ? Blocks.dirt.getIcon(0, 0) : blockIcon;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(Blocks.dirt);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        revertIfCovered(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        super.onNeighborBlockChange(world, x, y, z, neighbor);
        revertIfCovered(world, x, y, z);
    }

    private void revertIfCovered(World world, int x, int y, int z) {
        if (!world.isRemote && world.getBlock(x, y + 1, z).getMaterial().isSolid()) {
            world.setBlock(x, y, z, Blocks.dirt);
        }
    }
}
