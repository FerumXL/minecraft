package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** EntityStray of 1.12.2: a skeleton of cold biomes whose arrows slow the target for 30 seconds. */
public class EntityStray extends EntitySkeleton {
    public EntityStray(World world) {
        super(world);
    }

    @Override
    protected String getLivingSound() {
        return MC112Bridge.MOD_ID + ":entity.stray.ambient";
    }

    @Override
    protected String getHurtSound() {
        return MC112Bridge.MOD_ID + ":entity.stray.hurt";
    }

    @Override
    protected String getDeathSound() {
        return MC112Bridge.MOD_ID + ":entity.stray.death";
    }

    /** 1.7.10 has no tipped arrows: the Slowness of the stray's arrow is applied when it hits. */
    public static final class Arrows {
        @SubscribeEvent
        public void onHurt(LivingHurtEvent event) {
            if (event.source.getSourceOfDamage() instanceof EntityArrow
                    && ((EntityArrow) event.source.getSourceOfDamage()).shootingEntity instanceof EntityStray) {
                event.entityLiving.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 600));
            }
        }
    }
}
