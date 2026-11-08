package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

/** A full block with separate side, top and bottom textures (the cube_bottom_top / cube_column models). */
public class BlockSidedTextures extends BlockSimple {
    private final String sideTexture;
    private final String topTexture;
    private final String bottomTexture;
    @SideOnly(Side.CLIENT)
    private IIcon side;
    @SideOnly(Side.CLIENT)
    private IIcon top;
    @SideOnly(Side.CLIENT)
    private IIcon bottom;

    public BlockSidedTextures(Material material, MapColor mapColor, String name, String sideTexture,
            String topTexture, String bottomTexture, CreativeTabs tab, float hardness, float resistance,
            Block.SoundType sound) {
        super(material, mapColor, name, sideTexture, tab, hardness, resistance, sound);
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon(MC112Bridge.MOD_ID + ":" + sideTexture);
        top = register.registerIcon(MC112Bridge.MOD_ID + ":" + topTexture);
        bottom = register.registerIcon(MC112Bridge.MOD_ID + ":" + bottomTexture);
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        return face == 0 ? bottom : face == 1 ? top : side;
    }
}
