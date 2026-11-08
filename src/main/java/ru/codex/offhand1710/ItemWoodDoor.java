package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Door item for another wood type. Same placement as the vanilla 1.7.10 ItemDoor (which only knows the oak
 * and iron doors), stacking to 64 as in 1.12.2.
 */
public final class ItemWoodDoor extends Item {
    private final Block door;

    public ItemWoodDoor(Block door, String wood) {
        this.door = door;
        setMaxStackSize(64);
        setCreativeTab(CreativeTabs.tabRedstone);
        setUnlocalizedName(MC112Bridge.MOD_ID + "." + wood + "_door");
        setTextureName(MC112Bridge.MOD_ID + ":door_" + wood);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (side != 1) {
            return false;
        }
        ++y;
        if (!player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x, y + 1, z, side, stack)) {
            return false;
        }
        if (!door.canPlaceBlockAt(world, x, y, z)) {
            return false;
        }
        int facing = MathHelper.floor_double((double) ((player.rotationYaw + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3;
        ItemDoor.placeDoorBlock(world, x, y, z, facing, door);
        --stack.stackSize;
        return true;
    }
}
