package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/**
 * Walking speeds of 1.12.2 for the mobs that still run the old AI in 1.7.10 (spiders, cave spiders, zombie pigmen,
 * silverfish). The old AI walks at moveForward = movementSpeed with landMovementFactor 0.1, a steady 0.1 x 0.98 x speed
 * / 0.454 blocks a tick (measured in stage group2: 0.1727 for 0.8). The AI of 1.12.2 walks at aiMoveSpeed = taskSpeed x
 * movementSpeed, steady a^2 x 0.98 / 0.454. So the 1.7.10 value is 10 a^2, chosen by what the mob does, as the old AI
 * walks and chases at one speed:
 * spider chase 0.3 x 1.0 -> 0.9, wander 0.3 x 0.8 -> 0.576; pigman angry (0.23 + 0.05) x 1.0 -> 0.784 (0.45 of it is the
 * 1.7.10 attack boost), calm 0.23 x 1.0 -> 0.529; silverfish 0.25 x 1.0 -> 0.625.
 */
public final class Speeds112 {
    public static final boolean OLD = "1".equals(System.getProperty("mc112.speeds17"));
    /** -Dmc112.blaze17=1: the 1.7.10 blaze movement only. */
    public static final boolean BLAZE_OLD = "1".equals(System.getProperty("mc112.blaze17"));

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (OLD || event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityCreature)) {
            return;
        }
        EntityCreature e = (EntityCreature) event.entityLiving;
        if (Mobs19.handles(e)) {
            return; // the task AI of 1.12.2 with its own speeds (Mobs19)
        }
        boolean chasing = e.getEntityToAttack() != null;
        double base;
        if (e instanceof EntitySpider) {
            base = chasing ? 0.9D : 0.576D;
        } else if (e instanceof EntityPigZombie) {
            base = chasing ? 0.784D - 0.45D : 0.529D;
        } else if (e instanceof EntitySilverfish) {
            base = 0.625D;
        } else if (e instanceof net.minecraft.entity.monster.EntityBlaze && !BLAZE_OLD) {
            // EntityBlaze.AIFireballAttack of 1.12.2: closer than 2 blocks it moves in (speed 1.0), from 2 to 48
            // (followRange) it hovers and shoots without moving, further away it flies towards the target.
            // In the air both versions push by speed x jumpMovementFactor 0.02, so the attribute stays 0.23 there;
            // on the ground the 1.12.2 walk needs 10 a^2 = 0.529 under the old AI.
            net.minecraft.entity.Entity t = e.getEntityToAttack();
            // EntityAINearestAttackableTarget of 1.12.2: players within followRange 48 that it can see (1.7.10: 16)
            if (t == null && e.ticksExisted % 10 == 0) {
                net.minecraft.entity.player.EntityPlayer p = e.worldObj.getClosestVulnerablePlayerToEntity(e, 48.0D);
                if (p != null && e.canEntityBeSeen(p) && !Spectator112.is(p)) {
                    e.setTarget(p);
                    t = p;
                }
            }
            double d2 = t == null ? 0.0D : e.getDistanceSqToEntity(t);
            if (t != null && d2 >= 4.0D && d2 < 48.0D * 48.0D) {
                base = 0.0D;
            } else {
                base = e.onGround ? 0.529D : 0.23D;
            }
        } else {
            return;
        }
        IAttributeInstance a = e.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (a.getBaseValue() != base) {
            a.setBaseValue(base);
        }
    }
}
