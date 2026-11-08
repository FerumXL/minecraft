package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Glazed terracotta of 1.12.2.
 *
 * Metadata is the facing, in 1.12.2 horizontal index order: 0 south, 1 west, 2 north, 3 east. The block
 * faces the player who placed it (the opposite of the player's horizontal facing).
 *
 * Textures follow models/block/glazed_terracotta.json of 1.12.2: the pattern is rotated clockwise by 90 on
 * the north face, 270 on south, 0 on west, 180 on east and 0 on top and bottom; the blockstate turns that
 * model clockwise (seen from above) by facing * 90 degrees. Files glazed_terracotta_COLOR_K hold the pattern
 * turned K * 90 degrees clockwise. 1.7.10 draws the bottom face flipped compared with 1.12.2, so the bottom
 * uses glazed_terracotta_COLOR_bottom_K: the pattern flipped vertically, then turned K * 90 clockwise.
 */
public final class BlockGlazedTerracotta extends Block {
    /** 1.7.10 side (2 north, 3 south, 4 west, 5 east) to clockwise order north 0, east 1, south 2, west 3. */
    private static final int[] CLOCKWISE_INDEX = {-1, -1, 0, 2, 3, 1};

    private final String color;
    private final int colorIndex;
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;
    @SideOnly(Side.CLIENT)
    private IIcon[] bottomIcons;

    public BlockGlazedTerracotta(String color, int colorIndex) {
        super(Material.rock);
        this.color = color;
        this.colorIndex = colorIndex;
        setBlockName(MC112Bridge.MOD_ID + ".glazed_terracotta_" + color);
        setBlockTextureName(MC112Bridge.MOD_ID + ":glazed_terracotta_" + color + "_0");
        setCreativeTab(CreativeTabs.tabDecorations);
        setHardness(1.4F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[4];
        bottomIcons = new IIcon[4];
        String base = MC112Bridge.MOD_ID + ":glazed_terracotta_" + color;
        for (int k = 0; k < 4; k++) {
            icons[k] = register.registerIcon(base + "_" + k);
            bottomIcons[k] = register.registerIcon(base + "_bottom_" + k);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int facing = metadata & 3;
        if (side == 0) {
            return bottomIcons[facing];
        }
        if (side == 1) {
            return icons[facing];
        }
        int worldFace = CLOCKWISE_INDEX[side & 7];
        if (worldFace < 0) {
            return icons[0];
        }
        // The model face that the blockstate rotation brought to this side, and that face's texture rotation
        // (north 90, east 180, south 270, west 0 = (clockwise index + 1) * 90).
        int modelFace = (worldFace - facing + 4) & 3;
        return icons[(modelFace + 1) & 3];
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int playerFacing = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, (playerFacing + 2) & 3, 2);
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return MapColor.getMapColorForBlockColored(colorIndex);
    }
}
