package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

/** Bone block of 1.12.2: placed along an axis like a log, hardness 2, stone sounds, needs a pickaxe. */
public final class BlockBone extends BlockRotatedPillar {
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockBone() {
        super(Material.rock);
        setBlockName(MC112Bridge.MOD_ID + ".bone_block");
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(2.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(MC112Bridge.MOD_ID + ":bone_block_side");
        top = register.registerIcon(MC112Bridge.MOD_ID + ":bone_block_top");
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
        return MapColor.sandColor;
    }
}
