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

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (OLD || event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityCreature)) {
            return;
        }
        EntityCreature e = (EntityCreature) event.entityLiving;
        boolean chasing = e.getEntityToAttack() != null;
        double base;
        if (e instanceof EntitySpider) {
            base = chasing ? 0.9D : 0.576D;
        } else if (e instanceof EntityPigZombie) {
            base = chasing ? 0.784D - 0.45D : 0.529D;
        } else if (e instanceof EntitySilverfish) {
            base = 0.625D;
        } else {
            return;
        }
        IAttributeInstance a = e.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (a.getBaseValue() != base) {
            a.setBaseValue(base);
        }
    }
}
