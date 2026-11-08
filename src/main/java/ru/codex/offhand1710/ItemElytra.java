package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.EnumHelper;

/** ItemElytra of 1.12.2: worn in the chest slot, no armor, 432 durability, repaired with leather, unusable at 1 left. */
public final class ItemElytra extends ItemArmor {
    private static final ArmorMaterial MATERIAL = EnumHelper.addArmorMaterial("MC112_ELYTRA", 27, new int[] {0, 0, 0, 0}, 0);

    @SideOnly(Side.CLIENT)
    private IIcon brokenIcon;

    public ItemElytra() {
        super(MATERIAL, 0, 1);
        setMaxDamage(432);
        setUnlocalizedName(MC112Bridge.MOD_ID + ".elytra");
        setTextureName(MC112Bridge.MOD_ID + ":elytra");
        setCreativeTab(CreativeTabs.tabTransport);
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getItemDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean getIsRepairable(ItemStack stack, ItemStack material) {
        return material.getItem() == Items.leather;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(getIconString());
        brokenIcon = register.registerIcon(MC112Bridge.MOD_ID + ":broken_elytra");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return damage < getMaxDamage() - 1 ? itemIcon : brokenIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        return MC112Bridge.MOD_ID + ":textures/entity/elytra.png";
    }

    /** The wings are drawn by ElytraRenderer; the armor pass draws nothing. */
    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, int slot) {
        return ru.codex.offhand1710.client.ElytraRenderer.EMPTY_ARMOR;
    }
}
