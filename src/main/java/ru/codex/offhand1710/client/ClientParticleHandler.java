package ru.codex.offhand1710.client;

import java.util.Random;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import ru.codex.offhand1710.OffhandMessages;

/** NetHandlerPlayClient.handleParticles of 1.12.2 for the particles added by the mod. */
public final class ClientParticleHandler implements IMessageHandler<OffhandMessages.Particle, IMessage> {
    @Override
    public IMessage onMessage(final OffhandMessages.Particle message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                spawnAll(message);
            }
        });
        return null;
    }

    private static void spawnAll(OffhandMessages.Particle message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        if (message.count == 0) {
            spawn(mc, message.kind, message.x, message.y, message.z, (double) (message.speed * message.offsetX),
                    (double) (message.speed * message.offsetY), (double) (message.speed * message.offsetZ));
            return;
        }
        Random random = mc.theWorld.rand;
        for (int i = 0; i < message.count; i++) {
            spawn(mc, message.kind, message.x + random.nextGaussian() * (double) message.offsetX,
                    message.y + random.nextGaussian() * (double) message.offsetY,
                    message.z + random.nextGaussian() * (double) message.offsetZ,
                    random.nextGaussian() * (double) message.speed, random.nextGaussian() * (double) message.speed,
                    random.nextGaussian() * (double) message.speed);
        }
    }

    private static void spawn(Minecraft mc, int kind, double x, double y, double z, double motionX, double motionY, double motionZ) {
        EntityLivingBase viewer = mc.renderViewEntity;
        if (viewer == null || mc.gameSettings.particleSetting > 1 || viewer.getDistanceSq(x, y, z) > 1024.0D) {
            return;
        }
        World world = mc.theWorld;
        EntityFX particle = kind == OffhandMessages.PARTICLE_SWEEP
                ? new EntitySweepFX(mc, world, x, y, z, motionX)
                : new EntityDamageIndicatorFX(world, x, y, z, motionX, motionY, motionZ);
        mc.effectRenderer.addEffect(particle);
    }
}
