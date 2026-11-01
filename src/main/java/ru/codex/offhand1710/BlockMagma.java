package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class BlockMagma extends Block {
    public BlockMagma() {
        super(Material.rock);
        setBlockName(MC112Bridge.MOD_ID + ".magma_block");
        setBlockTextureName(MC112Bridge.MOD_ID + ":magma_block");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabBlock);
        setHardness(0.5F);
        setResistance(2.5F);
        setLightLevel(0.2F);
        setStepSound(soundTypePiston);
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && entity instanceof EntityLivingBase && !entity.isSneaking()) {
            entity.attackEntityFrom(DamageSource.inFire, 1.0F);
        }
        super.onEntityWalking(world, x, y, z, entity);
    }
}

