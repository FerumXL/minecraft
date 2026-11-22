package ru.codex.offhand1710;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor;

/**
 * CombatRules.getDamageAfterAbsorb of 1.9 - 1.12.2: armor stops less of a big hit,
 * damage * (1 - clamp(armor - damage / (2 + toughness / 4), armor / 5, 20) / 25), and diamond armor has a toughness of
 * 2 a piece. Armor of other mods (ISpecialArmor, as HBM's) keeps the reckoning of Forge. -Dmc112.armor17 turns it off.
 */
public final class Armor112 {
    public static final boolean OFF = System.getProperty("mc112.armor17") != null;

    private Armor112() {
    }

    public static float afterAbsorb(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float g = Math.min(Math.max(armor - damage / f, armor * 0.2F), 20.0F);
        return damage * (1.0F - g / 25.0F);
    }

    /** ArmorMaterial.getToughness of 1.12.2: 2 for diamond, 0 for the rest. */
    public static float toughness(ItemArmor armor) {
        return armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND ? 2.0F : 0.0F;
    }

    /** ArmorProperties.ApplyArmor (players); -1 leaves it to Forge. */
    public static float player(EntityLivingBase entity, ItemStack[] armor, DamageSource source, double damage) {
        if (OFF) {
            return -1.0F;
        }
        for (ItemStack s : armor) {
            if (s != null && s.getItem() instanceof ISpecialArmor) {
                return -1.0F;
            }
        }
        if (source.isUnblockable()) {
            return (float) damage;
        }
        float total = 0.0F;
        float tough = 0.0F;
        for (ItemStack s : armor) {
            if (s != null && s.getItem() instanceof ItemArmor) {
                total += ((ItemArmor) s.getItem()).damageReduceAmount;
                tough += toughness((ItemArmor) s.getItem());
            }
        }
        // InventoryPlayer.damageArmor of 1.12.2: each piece loses damage / 4, at least 1
        int wear = Math.max(1, (int) (damage / 4.0D));
        for (int i = 0; i < armor.length; i++) {
            ItemStack s = armor[i];
            if (s != null && s.getItem() instanceof ItemArmor) {
                s.damageItem(wear, entity);
                if (s.stackSize <= 0) {
                    armor[i] = null;
                }
            }
        }
        return afterAbsorb((float) damage, total, tough);
    }

    /**
     * EnchantmentProtection.calcModifierDamage of 1.12.2: protection gives its level, fire, blast and projectile
     * protection twice it against their kind, feather falling three times against falls; -1 leaves it to 1.7.10.
     */
    public static int protection(net.minecraft.enchantment.EnchantmentProtection e, int level, DamageSource source) {
        if (OFF) {
            return -1;
        }
        if (source.canHarmInCreative()) {
            return 0;
        }
        switch (e.protectionType) {
            case 0:
                return level;
            case 1:
                return source.isFireDamage() ? level * 2 : 0;
            case 2:
                return source == DamageSource.fall ? level * 3 : 0;
            case 3:
                return source.isExplosion() ? level * 2 : 0;
            case 4:
                return source.isProjectile() ? level * 2 : 0;
            default:
                return 0;
        }
    }

    /** EnchantmentHelper.getEnchantmentModifierDamage of 1.12.2: the plain sum, without the random half of 1.7.10. */
    public static int modifierSum(ItemStack[] armor, DamageSource source) {
        if (OFF) {
            return -1;
        }
        int sum = 0;
        for (ItemStack s : armor) {
            if (s == null) {
                continue;
            }
            net.minecraft.nbt.NBTTagList list = s.getEnchantmentTagList();
            if (list == null) {
                continue;
            }
            for (int i = 0; i < list.tagCount(); i++) {
                int id = list.getCompoundTagAt(i).getShort("id");
                int lvl = list.getCompoundTagAt(i).getShort("lvl");
                if (id >= 0 && id < net.minecraft.enchantment.Enchantment.enchantmentsList.length && net.minecraft.enchantment.Enchantment.enchantmentsList[id] != null) {
                    sum += net.minecraft.enchantment.Enchantment.enchantmentsList[id].calcModifierDamage(lvl, source);
                }
            }
        }
        return sum;
    }

    /** EntityLivingBase.applyArmorCalculations (mobs); -1 leaves it to 1.7.10. */
    public static float mob(EntityLivingBase entity, DamageSource source, float damage) {
        if (OFF || entity instanceof net.minecraft.entity.player.EntityPlayer) {
            return -1.0F;
        }
        if (source.isUnblockable()) {
            return damage;
        }
        float tough = 0.0F;
        ItemStack[] items = entity.getLastActiveItems();
        for (int i = 1; i < items.length; i++) {
            if (items[i] != null && items[i].getItem() instanceof ItemArmor) {
                tough += toughness((ItemArmor) items[i].getItem());
            }
        }
        return afterAbsorb(damage, entity.getTotalArmorValue(), tough);
    }
}
