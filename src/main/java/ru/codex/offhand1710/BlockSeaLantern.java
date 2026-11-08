package ru.codex.offhand1710;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.MathHelper;

/**
 * Sea lantern of 1.12.2: glass, full light, drops 2-3 prismarine crystals (fortune adds more, at most 5);
 * silk touch gives the block.
 */
public final class BlockSeaLantern extends Block {
    public BlockSeaLantern() {
        super(Material.glass);
        setBlockName(MC112Bridge.MOD_ID + ".sea_lantern");
        setBlockTextureName(MC112Bridge.MOD_ID + ":sea_lantern");
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(0.3F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return PortedBlocks.prismarineCrystals;
    }

    @Override
    public int quantityDropped(Random random) {
        return 2 + random.nextInt(2);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return MathHelper.clamp_int(quantityDropped(random) + random.nextInt(fortune + 1), 1, 5);
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return MapColor.quartzColor;
    }
}
