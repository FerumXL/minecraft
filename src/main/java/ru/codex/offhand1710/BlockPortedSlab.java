package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * Slab of 1.8+ (red sandstone, purpur), as a single/double block pair like the vanilla 1.7.10 slabs.
 * BlockSlab hard-codes the vanilla stone slab for the double slab's drops and pick-block result, so both go
 * to this slab's own single half here.
 */
public final class BlockPortedSlab extends BlockSlab {
    private final MapColor mapColor;
    private final String sideTexture;
    private final String topTexture;
    private final String bottomTexture;
    private BlockPortedSlab single;
    @SideOnly(Side.CLIENT)
    private IIcon side;
    @SideOnly(Side.CLIENT)
    private IIcon top;
    @SideOnly(Side.CLIENT)
    private IIcon bottom;

    public BlockPortedSlab(boolean isDouble, Material material, MapColor mapColor, String name, String sideTexture,
            String topTexture, String bottomTexture, float hardness, float resistance, Block.SoundType sound) {
        super(isDouble, material);
        this.mapColor = mapColor;
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.single = this;
        setBlockName(MC112Bridge.MOD_ID + "." + name);
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(sound);
        useNeighborBrightness = true;
        if (!isDouble) {
            setCreativeTab(CreativeTabs.tabBlock);
        }
    }

    /** For the double slab: the single slab it drops and splits into. */
    void setSingle(BlockPortedSlab single) {
        this.single = single;
    }

    @Override
    public String func_150002_b(int metadata) {
        return single.getUnlocalizedName();
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(single);
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(single, 2, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(single);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon(MC112Bridge.MOD_ID + ":" + sideTexture);
        top = register.registerIcon(MC112Bridge.MOD_ID + ":" + topTexture);
        bottom = register.registerIcon(MC112Bridge.MOD_ID + ":" + bottomTexture);
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        return face == 0 ? bottom : face == 1 ? top : side;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
