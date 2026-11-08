package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Shield blocking of 1.12.2 (EntityLivingBase.attackEntityFrom, canBlockDamageSource, damageShield...). */
public final class ShieldMechanics {
    /** Ticks a shield stays unusable after an axe hit (1.12.2 cooldown 100). */
    public static final int AXE_COOLDOWN = 100;

    /** The raised shield of the player: offhand or main hand, raised at least 5 ticks ago; or null. */
    public static ItemStack activeShield(EntityPlayer player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack used = offhand.useStack;
        if (offhand.isUsing() && used != null && used.getItem() instanceof ItemShield
                && used.getMaxItemUseDuration() - offhand.useCount >= 5) {
            return used;
        }
        ItemStack main = player.getItemInUse();
        if (player.isUsingItem() && main != null && main.getItem() instanceof ItemShield
                && player.getItemInUseDuration() >= 5) {
            return main;
        }
        return null;
    }

    /**
     * A blockable source in front of the player: not unblockable, coming from an entity (for projectiles the
     * projectile itself) whose horizontal direction to the player points against the look vector.
     */
    private static boolean canBlock(EntityPlayer player, DamageSource source) {
        if (source.isUnblockable()) {
            return false;
        }
        Entity from = source.getSourceOfDamage();
        if (from == null) {
            return false;
        }
        Vec3 look = player.getLook(1.0F);
        Vec3 toPlayer = Vec3.createVectorHelper(player.posX - from.posX, player.posY - from.posY,
                player.posZ - from.posZ).normalize();
        return toPlayer.xCoord * look.xCoord + toPlayer.zCoord * look.zCoord < 0.0D;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttack(LivingAttackEvent event) {
        if (!(event.entityLiving instanceof EntityPlayerMP) || event.ammount <= 0.0F) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (player.capabilities.disableDamage) {
            return;
        }
        ItemStack shield = activeShield(player);
        if (shield == null || !canBlock(player, event.source)) {
            return;
        }
        // Blocked: no damage at all; an arrow bounces off because the attack reports failure.
        event.setCanceled(true);
        player.hurtResistantTime = player.maxHurtResistantTime;
        player.worldObj.playSoundAtEntity(player, MC112Bridge.MOD_ID + ":item.shield.block", 1.0F,
                0.8F + player.worldObj.rand.nextFloat() * 0.4F);
        damageShield(player, shield, event.ammount);
        Entity direct = event.source.getSourceOfDamage();
        if (!event.source.isProjectile() && direct instanceof EntityLivingBase) {
            blockUsingShield(player, (EntityLivingBase) direct);
        }
    }

    /**
     * 1.7.10 halves all damage while the main hand item with the block action is in use (sword blocking).
     * A shield blocks fully from the front and not at all from behind, so for a shield raised in the main
     * hand the halving is undone before it happens.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer) || event.source.isUnblockable()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        ItemStack main = player.getItemInUse();
        if (player.isUsingItem() && main != null && main.getItem() instanceof ItemShield && event.ammount > 1.0F) {
            event.ammount = event.ammount * 2.0F - 1.0F;
        }
    }

    /** EntityPlayer.damageShield: hits of 3 or more cost 1 + damage durability; a broken shield is gone. */
    private static void damageShield(EntityPlayerMP player, ItemStack shield, float amount) {
        if (amount < 3.0F) {
            return;
        }
        shield.damageItem(1 + MathHelper.floor_float(amount), player);
        if (shield.stackSize > 0) {
            return;
        }
        OffhandInventory offhand = OffhandInventory.get(player);
        if (offhand.useStack == shield) {
            offhand.setStack(null);
            OffhandEvents.stopOffhandUse(player);
        } else {
            int slot = player.inventory.currentItem;
            if (player.inventory.mainInventory[slot] == shield) {
                player.inventory.setInventorySlotContents(slot, null);
            }
            player.clearItemInUse();
            ForgeEventFactory.onPlayerDestroyItem(player, shield);
        }
        player.worldObj.playSoundAtEntity(player, "random.break", 0.8F, 0.8F + player.worldObj.rand.nextFloat() * 0.4F);
    }

    /** EntityPlayer.blockUsingShield: push the attacker back; an axe disables the shield. */
    private static void blockUsingShield(EntityPlayerMP player, EntityLivingBase attacker) {
        knockBack(attacker, 0.5F, player.posX - attacker.posX, player.posZ - attacker.posZ);
        ItemStack weapon = attacker.getHeldItem();
        if (weapon != null && weapon.getItem() instanceof ItemAxe) {
            disableShield(player);
        }
    }

    /** EntityPlayer.disableShield(true): 5 seconds without the shield, the raised shield is lowered. */
    private static void disableShield(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        offhand.shieldCooldown = AXE_COOLDOWN;
        if (offhand.isUsing() && offhand.useStack != null && offhand.useStack.getItem() instanceof ItemShield) {
            OffhandEvents.stopOffhandUse(player);
        }
        ItemStack main = player.getItemInUse();
        if (player.isUsingItem() && main != null && main.getItem() instanceof ItemShield) {
            player.clearItemInUse();
        }
        OffhandMessages.synchronize(player);
        OffhandMessages.sendResult(player, 0, OffhandMessages.RESULT_SHIELD_DISABLED, AXE_COOLDOWN);
        player.worldObj.playSoundAtEntity(player, "random.break", 0.8F, 0.8F + player.worldObj.rand.nextFloat() * 0.4F);
    }

    /** EntityLivingBase.knockBack of 1.12.2 (strength used, vertical push only on the ground). */
    private static void knockBack(EntityLivingBase target, float strength, double xRatio, double zRatio) {
        if (target.getRNG().nextDouble()
                < target.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue()) {
            return;
        }
        float length = MathHelper.sqrt_double(xRatio * xRatio + zRatio * zRatio);
        if (length < 1.0E-4F) {
            return;
        }
        target.isAirBorne = true;
        target.motionX /= 2.0D;
        target.motionZ /= 2.0D;
        target.motionX -= xRatio / (double) length * (double) strength;
        target.motionZ -= zRatio / (double) length * (double) strength;
        if (target.onGround) {
            target.motionY /= 2.0D;
            target.motionY += (double) strength;
            if (target.motionY > 0.4D) {
                target.motionY = 0.4D;
            }
        }
        target.velocityChanged = true;
    }
}
