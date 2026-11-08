package ru.codex.offhand1710.client;

import net.minecraft.client.particle.EntityCritFX;
import net.minecraft.world.World;

/** ParticleCrit.DamageIndicatorFactory of 1.12.2: the dark hearts of a strong hit (sprite 67 of the 1.12.2 particles.png). */
public final class EntityDamageIndicatorFX extends EntityCritFX {
    public EntityDamageIndicatorFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY + 1.0D, motionZ, 1.0F);
        particleMaxAge = 20;
        setParticleTextureIndex(67);
    }
}
