package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class BlockGlazedTerracotta extends Block {
    private final String color;
    private final IIcon[] icons = new IIcon[4];

    public BlockGlazedTerracotta(String color) {
        super(Material.rock);
        this.color = color;
        setBlockName(MC112Bridge.MOD_ID + ".glazed_terracotta_" + color);
        setBlockTextureName(MC112Bridge.MOD_ID + ":glazed_terracotta_" + color + "_0");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabBlock);
        setHardness(1.4F);
        setResistance(7.0F);
        setStepSound(soundTypePiston);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        for (int rotation = 0; rotation < icons.length; rotation++) {
            icons[rotation] = register.registerIcon(
                    MC112Bridge.MOD_ID + ":glazed_terracotta_" + color + "_" + rotation);
        }
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return icons[metadata & 3];
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        int rotation = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, rotation, 2);
    }
}

