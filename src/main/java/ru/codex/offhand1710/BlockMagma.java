package ru.codex.offhand1710;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.DamageSource;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Magma block of 1.12.2. */
public final class BlockMagma extends Block {
    /** 1.12.2 DamageSource.HOT_FLOOR: fire damage with its own death message ("discovered floor was lava"). */
    public static final DamageSource HOT_FLOOR = new DamageSource("hotFloor").setFireDamage();

    public BlockMagma() {
        super(Material.rock);
        setBlockName(MC112Bridge.MOD_ID + ".magma_block");
        setBlockTextureName(MC112Bridge.MOD_ID + ":magma_block");
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(0.5F);
        setLightLevel(0.2F);
        setStepSound(soundTypePiston);
        setTickRandomly(true);
        setHarvestLevel("pickaxe", 0);
    }

    /** Walking on it hurts, except fire-immune entities; sneaking players do not trigger walking in 1.7.10. */
    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && !entity.isImmuneToFire() && entity instanceof EntityLivingBase && !Content119.hasFrostWalker(entity)) {
            entity.attackEntityFrom(HOT_FLOOR, 1.0F);
        }
        super.onEntityWalking(world, x, y, z, entity);
    }

    /** 1.12.2: water directly above evaporates on a random tick, with a fizz and large smoke. */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.getBlock(x, y + 1, z).getMaterial() != Material.water) {
            return;
        }
        world.setBlockToAir(x, y + 1, z);
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.fizz", 0.5F,
                2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        if (world instanceof WorldServer) {
            ((WorldServer) world).func_147487_a("largesmoke", x + 0.5D, y + 1.25D, z + 0.5D, 8, 0.5D, 0.25D, 0.5D, 0.0D);
        }
    }

    /** Drawn at full brightness, as 1.12.2 does (getPackedLightmapCoords). */
    @Override
    @SideOnly(Side.CLIENT)
    public int getMixedBrightnessForBlock(IBlockAccess world, int x, int y, int z) {
        return 15728880;
    }

    /** 1.12.2 lets only fire-immune mobs spawn on it; 1.7.10 cannot tell the mob here, so none spawn. */
    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return MapColor.netherrackColor;
    }
}
