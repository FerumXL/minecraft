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

    /** EntityPlayerSP.openEditStructure of 1.12.2; nothing on a server. */
    public void openStructure(StructureBlock112.Tile tile) {
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == Items112.GUI_LLAMA) {
            net.minecraft.entity.Entity e = world.getEntityByID(x);
            return e instanceof MoreMobs.Llama ? new LlamaContainer(player.inventory, (MoreMobs.Llama) e, y) : null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return id == Items112.GUI_BOX && tile instanceof BlockShulkerBox.Tile ? new BlockShulkerBox.BoxContainer(player.inventory, (BlockShulkerBox.Tile) tile) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    /** True for the player of this client (never on a server). */
    public boolean isLocalPlayer(net.minecraft.entity.Entity e) {
        return false;
    }
}
