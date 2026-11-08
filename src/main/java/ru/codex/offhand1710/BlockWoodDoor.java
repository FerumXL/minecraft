package ru.codex.offhand1710;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/**
 * Door of another wood type (1.8+). Uses the vanilla door logic and render; drops and picks its own item
 * instead of the oak door that BlockDoor hard-codes. Textures DOOR_upper / DOOR_lower as in 1.12.2.
 */
public final class BlockWoodDoor extends BlockDoor {
    private final MapColor mapColor;
    private Item item;

    public BlockWoodDoor(String wood, MapColor mapColor) {
        super(Material.wood);
        this.mapColor = mapColor;
        setBlockName(MC112Bridge.MOD_ID + "." + wood + "_door");
        setBlockTextureName(MC112Bridge.MOD_ID + ":door_" + wood);
        setHardness(3.0F);
        setStepSound(soundTypeWood);
        disableStats();
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return (metadata & 8) != 0 ? null : item;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return item;
    }

    @Override
    public MapColor getMapColor(int metadata) {
        return mapColor;
    }
}
