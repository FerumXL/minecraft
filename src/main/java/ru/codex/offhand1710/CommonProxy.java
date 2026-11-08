package ru.codex.offhand1710;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CommonProxy implements IGuiHandler {
    public void preInitialize() {
    }

    /** After the blocks and items exist (FML init). */
    public void initialize() {
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return id == Items112.GUI_BOX && tile instanceof BlockShulkerBox.Tile ? new BlockShulkerBox.BoxContainer(player.inventory, (BlockShulkerBox.Tile) tile) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
