package ru.codex.offhand1710;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Shield of 1.12.2 (without banner patterns): 336 durability, raised with the use button and held like an
 * item in use (block action, 72000 ticks), repaired with planks. It cannot be raised during its 5 second
 * cooldown after an axe hit. Blocking itself is in ShieldMechanics; drawing in client.ShieldRenderer.
 */
public final class ItemShield extends Item {
    /** ItemShield.getItemStackDisplayName: a decorated shield is named after its colour. */
    @Override
    public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag")) {
            int base = stack.getTagCompound().getCompoundTag("BlockEntityTag").getInteger("Base") & 15;
            return net.minecraft.util.StatCollector.translateToLocal("item.shield." + Banner112.COLOR_KEYS[15 - base] + ".name");
        }
        return super.getItemStackDisplayName(stack);
    }

    public ItemShield() {
        setMaxStackSize(1);
        setMaxDamage(336);
        setCreativeTab(CreativeTabs.tabCombat);
        setUnlocalizedName(MC112Bridge.MOD_ID + ".shield");
        setTextureName(MC112Bridge.MOD_ID + ":shield");
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.block;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (OffhandInventory.get(player).shieldCooldown <= 0) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        }
        return stack;
    }

    @Override
    public boolean getIsRepairable(ItemStack shield, ItemStack material) {
        return material != null && material.getItem() == Item.getItemFromBlock(Blocks.planks)
                || super.getIsRepairable(shield, material);
    }
}
