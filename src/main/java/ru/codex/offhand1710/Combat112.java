package ru.codex.offhand1710;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * The combat of 1.9-1.12.2: attack speed and cooldown, the damage and speed of weapons, the sweep of swords, crits
 * only without sprinting and the attack sounds. The whole attack replaces EntityPlayer.attackTargetEntityWithCurrentItem
 * through AttackEntityEvent, following the 1.12.2 method, with the 1.7.10 calls where the API differs.
 */
public final class Combat112 {
    private static final String ATTACK_SPEED_NAME = "generic.attackSpeed";
    public static final IAttribute ATTACK_SPEED =
            new RangedAttribute(ATTACK_SPEED_NAME, 4.0D, 0.0D, 1024.0D).setShouldWatch(true);
    /** Item.ATTACK_SPEED_MODIFIER of 1.12.2. */
    private static final UUID SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    private static final float RAD = (float) Math.PI / 180.0F;
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.player.attack.";

    /** Weapon numbers of 1.12.2 for the vanilla items. Mod items keep their own damage. */
    public static void applyVanillaValues() {
        Potion.digSpeed.func_111184_a(ATTACK_SPEED, "AF8B6E3F-3328-4C0A-AA36-5BA2BB9DBEF3", 0.10000000149011612D, 2);
        Potion.digSlowdown.func_111184_a(ATTACK_SPEED, "55FCED67-E92A-486E-9800-B47F202C4386", -0.10000000149011612D, 2);
        try {
            sword(Items.wooden_sword, 3.0F);
            sword(Items.stone_sword, 4.0F);
            sword(Items.iron_sword, 5.0F);
            sword(Items.diamond_sword, 6.0F);
            sword(Items.golden_sword, 3.0F);
            tool(Items.wooden_axe, 6.0F);
            tool(Items.stone_axe, 8.0F);
            tool(Items.iron_axe, 8.0F);
            tool(Items.diamond_axe, 8.0F);
            tool(Items.golden_axe, 6.0F);
            tool(Items.wooden_pickaxe, 1.0F);
            tool(Items.stone_pickaxe, 2.0F);
            tool(Items.iron_pickaxe, 3.0F);
            tool(Items.diamond_pickaxe, 4.0F);
            tool(Items.golden_pickaxe, 1.0F);
            tool(Items.wooden_shovel, 1.5F);
            tool(Items.stone_shovel, 2.5F);
            tool(Items.iron_shovel, 3.5F);
            tool(Items.diamond_shovel, 4.5F);
            tool(Items.golden_shovel, 1.5F);
        } catch (RuntimeException e) {
            FMLLog.warning("[mc112bridge] weapon damage values not changed: %s", e);
        }
    }

    private static void sword(Item item, float damage) {
        ReflectionHelper.setPrivateValue(ItemSword.class, (ItemSword) item, Float.valueOf(damage), "field_150934_a");
    }

    private static void tool(Item item, float damage) {
        ReflectionHelper.setPrivateValue(ItemTool.class, (ItemTool) item, Float.valueOf(damage), "field_77865_bY",
                "damageVsEntity");
    }

    /** The attack speed modifier of an item as in 1.12.2, or null when the item has none (speed stays 4). */
    public static Double attackSpeedModifier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        Item item = stack.getItem();
        if (item instanceof ItemSword) {
            return Double.valueOf(-2.4000000953674316D);
        }
        if (item instanceof ItemHoe) {
            return Double.valueOf(hoeSpeed((ItemHoe) item));
        }
        if (item instanceof ItemAxe) {
            return Double.valueOf(axeSpeed((ItemTool) item));
        }
        if (item instanceof ItemPickaxe) {
            return Double.valueOf(-2.799999952316284D);
        }
        if (item instanceof ItemSpade) {
            return Double.valueOf(-3.0D);
        }
        if (item instanceof ItemTool) {
            // Tools of other mods that do not extend the vanilla classes.
            java.util.Set<String> classes = item.getToolClasses(stack);
            if (classes.contains("axe")) {
                return Double.valueOf(axeSpeed((ItemTool) item));
            }
            if (classes.contains("pickaxe")) {
                return Double.valueOf(-2.799999952316284D);
            }
            return Double.valueOf(-3.0D);
        }
        return null;
    }

    private static double axeSpeed(ItemTool item) {
        switch (item.func_150913_i()) {
            case WOOD:
            case STONE:
                return -3.200000047683716D;
            case IRON:
                return -3.0999999046325684D;
            default:
                return -3.0D;
        }
    }

    private static double hoeSpeed(ItemHoe item) {
        try {
            return (double) (Item.ToolMaterial.valueOf(item.getToolMaterialName()).getDamageVsEntity() + 1.0F - 4.0F);
        } catch (IllegalArgumentException e) {
            return -3.0D;
        }
    }

    private static IAttributeInstance speedInstance(EntityPlayer player) {
        IAttributeInstance instance = player.getAttributeMap().getAttributeInstanceByName(ATTACK_SPEED_NAME);
        return instance != null ? instance : player.getAttributeMap().registerAttribute(ATTACK_SPEED);
    }

    public static double attackSpeed(EntityPlayer player) {
        return speedInstance(player).getAttributeValue();
    }

    /** EntityPlayer.getCooldownPeriod. */
    public static float cooldownPeriod(EntityPlayer player) {
        return (float) (1.0D / attackSpeed(player) * 20.0D);
    }

    /** EntityPlayer.getCooledAttackStrength. */
    public static float attackStrength(EntityPlayer player, float adjustTicks) {
        float strength = ((float) OffhandInventory.get(player).ticksSinceLastSwing + adjustTicks) / cooldownPeriod(player);
        return MathHelper.clamp_float(strength, 0.0F, 1.0F);
    }

    public static void resetCooldown(EntityPlayer player) {
        OffhandInventory.get(player).ticksSinceLastSwing = 0;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer) {
            speedInstance((EntityPlayer) event.entity);
        }
    }

    /** EntityPlayer.onUpdate of 1.12.2: the cooldown counter, restarted when another item is taken in the hand. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayer player = event.player;
        OffhandInventory state = OffhandInventory.get(player);
        ItemStack held = player.getCurrentEquippedItem();
        applySpeedModifier(player, held);
        state.ticksSinceLastSwing++;
        if (!sameItem(state.lastMainHand, held)) {
            state.ticksSinceLastSwing = 0;
        }
        state.lastMainHand = held == null ? null : held.copy();
    }

    private static void applySpeedModifier(EntityPlayer player, ItemStack held) {
        IAttributeInstance instance = speedInstance(player);
        Double amount = attackSpeedModifier(held);
        AttributeModifier current = instance.getModifier(SPEED_MODIFIER);
        if (current != null && (amount == null || current.getAmount() != amount.doubleValue())) {
            instance.removeModifier(current);
            current = null;
        }
        if (current == null && amount != null) {
            instance.applyModifier(new AttributeModifier(SPEED_MODIFIER, "Weapon modifier", amount.doubleValue(), 0));
        }
    }

    /** ItemStack.areItemsEqualIgnoreDurability. */
    private static boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getItem() == b.getItem() && (a.isItemStackDamageable() || a.getItemDamage() == b.getItemDamage());
    }

    /** Sharpness has the 1.9 formula; the other damage enchantments did not change. */
    private static float enchantmentDamage(ItemStack stack, EnumCreatureAttribute creature) {
        if (stack == null) {
            return 0.0F;
        }
        float total = 0.0F;
        Map<?, ?> enchantments = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<?, ?> entry : enchantments.entrySet()) {
            int id = ((Integer) entry.getKey()).intValue();
            int level = ((Integer) entry.getValue()).intValue();
            if (id == Enchantment.sharpness.effectId) {
                total += 1.0F + (float) Math.max(0, level - 1) * 0.5F;
            } else if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                total += Enchantment.enchantmentsList[id].func_152376_a(level, creature);
            }
        }
        return total;
    }

    public static float tooltipEnchantmentDamage(ItemStack stack) {
        return enchantmentDamage(stack, EnumCreatureAttribute.UNDEFINED);
    }

    /** Last in the chain, so that protection mods can still cancel the attack and it then stays cancelled. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        event.setCanceled(true);
        EntityPlayer player = event.entityPlayer;
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack != null && stack.getItem().onLeftClickEntity(stack, player, event.target)) {
            return;
        }
        if (player.worldObj.isRemote) {
            resetCooldown(player); // PlayerControllerMP.attackEntity; the hit itself is decided by the server
            return;
        }
        if (event.target.canAttackWithItem() && !event.target.hitByEntity(player)) {
            attack(player, event.target, stack);
        }
    }

    private static void playSound(EntityPlayer player, String name) {
        player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, SOUND + name, 1.0F, 1.0F);
    }

    /** EntityPlayer.attackTargetEntityWithCurrentItem of 1.12.2. */
    private static void attack(EntityPlayer player, Entity target, ItemStack stack) {
        float damage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        EntityLivingBase living = target instanceof EntityLivingBase ? (EntityLivingBase) target : null;
        float bonus = enchantmentDamage(stack, living != null ? living.getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED);
        float strength = attackStrength(player, 0.5F);
        damage *= 0.2F + strength * strength * 0.8F;
        bonus *= strength;
        resetCooldown(player);
        if (damage <= 0.0F && bonus <= 0.0F) {
            return;
        }

        boolean strong = strength > 0.9F;
        boolean sprintHit = false;
        int knockback = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
        if (player.isSprinting() && strong) {
            playSound(player, "knockback");
            knockback++;
            sprintHit = true;
        }
        boolean critical = strong && player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder()
                && !player.isInWater() && !player.isPotionActive(Potion.blindness) && player.ridingEntity == null
                && living != null && !player.isSprinting();
        if (critical) {
            damage *= 1.5F;
        }
        damage += bonus;

        double walked = (double) (player.distanceWalkedModified - player.prevDistanceWalkedModified);
        boolean sweep = strong && !critical && !sprintHit && player.onGround && walked < (double) player.getAIMoveSpeed()
                && stack != null && stack.getItem() instanceof ItemSword;

        float healthBefore = 0.0F;
        boolean ignited = false;
        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        if (living != null) {
            healthBefore = living.getHealth();
            if (fireAspect > 0 && !target.isBurning()) {
                ignited = true;
                target.setFire(1);
            }
        }
        double motionX = target.motionX;
        double motionY = target.motionY;
        double motionZ = target.motionZ;
        if (!target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)) {
            playSound(player, "nodamage");
            if (ignited) {
                target.extinguish();
            }
            return;
        }

        float sin = MathHelper.sin(player.rotationYaw * RAD);
        float cos = MathHelper.cos(player.rotationYaw * RAD);
        if (knockback > 0) {
            if (living != null) {
                knockBack(living, (float) knockback * 0.5F, (double) sin, (double) -cos);
            } else {
                target.addVelocity((double) (-sin * (float) knockback * 0.5F), 0.1D, (double) (cos * (float) knockback * 0.5F));
            }
            player.motionX *= 0.6D;
            player.motionZ *= 0.6D;
            player.setSprinting(false);
        }
        if (sweep) {
            sweepAround(player, target, sin, cos);
        }
        if (target instanceof EntityPlayerMP && target.velocityChanged) {
            ((EntityPlayerMP) target).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(target));
            target.velocityChanged = false;
            target.motionX = motionX;
            target.motionY = motionY;
            target.motionZ = motionZ;
        }
        if (critical) {
            playSound(player, "crit");
            player.onCriticalHit(target);
        }
        if (!critical && !sweep) {
            playSound(player, strong ? "strong" : "weak");
        }
        if (bonus > 0.0F) {
            player.onEnchantmentCritical(target);
        }
        if (damage >= 18.0F) {
            player.triggerAchievement(AchievementList.overkill);
        }
        player.setLastAttacker(target);
        if (living != null) {
            EnchantmentHelper.func_151384_a(living, player);
        }
        EnchantmentHelper.func_151385_b(player, target);

        ItemStack held = player.getCurrentEquippedItem();
        Object struck = target;
        if (target instanceof EntityDragonPart) {
            IEntityMultiPart dragon = ((EntityDragonPart) target).entityDragonObj;
            if (dragon instanceof EntityLivingBase) {
                struck = dragon;
            }
        }
        if (held != null && struck instanceof EntityLivingBase) {
            held.hitEntity((EntityLivingBase) struck, player);
            if (held.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
        if (living != null) {
            float dealt = healthBefore - living.getHealth();
            player.addStat(StatList.damageDealtStat, Math.round(dealt * 10.0F));
            if (fireAspect > 0) {
                target.setFire(fireAspect * 4);
            }
            if (dealt > 2.0F) {
                sendParticle(player, OffhandMessages.PARTICLE_DAMAGE, target.posX, target.posY + (double) (target.height * 0.5F),
                        target.posZ, (int) ((double) dealt * 0.5D), 0.1F, 0.0F, 0.1F, 0.2F);
            }
        }
        player.addExhaustion(0.1F);
    }

    private static void sweepAround(EntityPlayer player, Entity target, float sin, float cos) {
        List<?> nearby = player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(1.0D, 0.25D, 1.0D));
        for (Iterator<?> iterator = nearby.iterator(); iterator.hasNext();) {
            EntityLivingBase other = (EntityLivingBase) iterator.next();
            if (other != player && other != target && !player.isOnSameTeam(other) && player.getDistanceSqToEntity(other) < 9.0D) {
                knockBack(other, 0.4F, (double) sin, (double) -cos);
                other.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F); // 1 + Sweeping Edge, absent in 1.7.10
            }
        }
        playSound(player, "sweep");
        double x = (double) -sin;
        double z = (double) cos;
        sendParticle(player, OffhandMessages.PARTICLE_SWEEP, player.posX + x, player.posY + (double) player.height * 0.5D,
                player.posZ + z, 0, (float) x, 0.0F, (float) z, 0.0F);
    }

    /** EntityLivingBase.knockBack of 1.12.2, where the strength is a parameter and only grounded targets rise. */
    private static void knockBack(EntityLivingBase target, float strength, double x, double z) {
        if (target.getRNG().nextDouble() < target.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue()) {
            return;
        }
        target.isAirBorne = true;
        float length = MathHelper.sqrt_double(x * x + z * z);
        target.motionX /= 2.0D;
        target.motionZ /= 2.0D;
        target.motionX -= x / (double) length * (double) strength;
        target.motionZ -= z / (double) length * (double) strength;
        if (target.onGround) {
            target.motionY /= 2.0D;
            target.motionY += (double) strength;
            if (target.motionY > 0.4000000059604645D) {
                target.motionY = 0.4000000059604645D;
            }
        }
    }

    private static void sendParticle(EntityPlayer player, int kind, double x, double y, double z, int count, float offsetX,
            float offsetY, float offsetZ, float speed) {
        MC112Bridge.NETWORK.channel.sendToAllAround(
                new OffhandMessages.Particle(kind, x, y, z, count, offsetX, offsetY, offsetZ, speed),
                new NetworkRegistry.TargetPoint(player.dimension, x, y, z, 32.0D));
    }
}
