package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/** A plain full block with the 1.12.2 properties given to it: stone variants, prismarine, red nether brick... */
public class BlockSimple extends Block {
    private final MapColor mapColor;

    /**
     * @param resistance 1.12.2 setResistance value, or a negative number when 1.12.2 does not set one
     *                   (then hardness * 5 applies, as in both versions)
     */
    public BlockSimple(Material material, MapColor mapColor, String name, String texture, CreativeTabs tab,
            float hardness, float resistance, Block.SoundType sound) {
        super(material);
        this.mapColor = mapColor;
        setBlockName(MC112Bridge.MOD_ID + "." + name);
        setBlockTextureName(MC112Bridge.MOD_ID + ":" + texture);
        setCreativeTab(tab);
        setHardness(hardness);
        if (resistance >= 0.0F) {
            setResistance(resistance);
        }
        setStepSound(sound);
    }

    public BlockSimple tool(String tool) {
        setHarvestLevel(tool, 0);
        return this;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
