package ru.codex.offhand1710;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/** The dragon head of 1.9: decorates the bow of end ships. Metadata is the direction it looks (2 to 5). */
public final class BlockDragonHead extends BlockContainer {
    public static final class Tile extends TileEntity {
    }

    public BlockDragonHead() {
        super(Material.circuits);
        setBlockName(MC112Bridge.MOD_ID + ".dragon_head");
        setBlockTextureName("minecraft:bedrock");
        setCreativeTab(CreativeTabs.tabDecorations);
        setHardness(1.0F);
        setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new Tile();
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
    public int getRenderType() {
        return -1;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int quarter = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, new int[] {2, 5, 3, 4}[quarter], 2);
    }
}
