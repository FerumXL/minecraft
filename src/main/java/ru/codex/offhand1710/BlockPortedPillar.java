package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

/** A block placed along an axis like a log (purpur pillar). */
public final class BlockPortedPillar extends BlockRotatedPillar {
    private final MapColor mapColor;
    private final String sideTexture;
    private final String topTexture;
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockPortedPillar(Material material, MapColor mapColor, String name, String sideTexture, String topTexture,
            float hardness, float resistance, Block.SoundType sound) {
        super(material);
        this.mapColor = mapColor;
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        setBlockName(MC112Bridge.MOD_ID + "." + name);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(sound);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(MC112Bridge.MOD_ID + ":" + sideTexture);
        top = register.registerIcon(MC112Bridge.MOD_ID + ":" + topTexture);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected IIcon getSideIcon(int metadata) {
        return blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected IIcon getTopIcon(int metadata) {
        return top;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
