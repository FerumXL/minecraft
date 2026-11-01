package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

public final class BlockConcrete extends Block {
    private final String texture;
    private IIcon icon;

    public BlockConcrete(String color, boolean powder) {
        super(powder ? Material.sand : Material.rock);
        this.texture = powder ? "concrete_powder_" + color : "concrete_" + color;
        setBlockName(MC112Bridge.MOD_ID + "." + texture);
        // The block texture atlas adds the "blocks/" directory automatically.
        setBlockTextureName(MC112Bridge.MOD_ID + ":" + texture);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(powder ? 0.5F : 1.8F);
        setResistance(powder ? 0.5F : 6.0F);
        if (powder) setStepSound(soundTypeSand);
        else setStepSound(soundTypePiston);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon(getTextureName());
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return icon;
    }
}

