package ru.codex.offhand1710;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/** EntityHusk of 1.12.2: a desert zombie that does not burn in daylight and makes its target hungry. */
public class EntityHusk extends EntityZombie {
    public EntityHusk(World world) {
        super(world);
    }

    /** 1.7.10 zombies set themselves on fire in EntityZombie.onLivingUpdate; a real fire or lava is still respected. */
    @Override
    public void setFire(int seconds) {
        if (seconds != 8 || worldObj.func_147470_e(boundingBox.contract(0.001D, 0.001D, 0.001D))) {
            super.setFire(seconds);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && getHeldItem() == null && target instanceof EntityLivingBase) {
            int difficulty = worldObj.difficultySetting.getDifficultyId();
            ((EntityLivingBase) target).addPotionEffect(new PotionEffect(Potion.hunger.id, 7 * difficulty * 20));
        }
        return hit;
    }

    @Override
    protected String getLivingSound() {
        return MC112Bridge.MOD_ID + ":entity.husk.ambient";
    }

    @Override
    protected String getHurtSound() {
        return MC112Bridge.MOD_ID + ":entity.husk.hurt";
    }

    @Override
    protected String getDeathSound() {
        return MC112Bridge.MOD_ID + ":entity.husk.death";
    }
}
