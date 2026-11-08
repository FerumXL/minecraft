package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** ParticleSweepAttack of 1.12.2. Drawn in the normal particle pass, switching the texture for its own quad. */
public final class EntitySweepFX extends EntityFX {
    private static final ResourceLocation SWEEP = new ResourceLocation("mc112bridge", "textures/entity/sweep.png");
    private static final ResourceLocation PARTICLES = new ResourceLocation("textures/particle/particles.png");
    private static final int LIFETIME = 4;

    private final Minecraft mc;
    private final float size;
    private int life;

    public EntitySweepFX(Minecraft mc, World world, double x, double y, double z, double sizeOffset) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.mc = mc;
        float shade = rand.nextFloat() * 0.6F + 0.4F;
        particleRed = shade;
        particleGreen = shade;
        particleBlue = shade;
        size = 1.0F - (float) sizeOffset * 0.5F;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        life++;
        if (life == LIFETIME) {
            setDead();
        }
    }

    @Override
    public void renderParticle(Tessellator t, float partial, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        int frame = (int) (((float) life + partial) * 3.0F / (float) LIFETIME);
        if (frame > 7) {
            return;
        }
        float u0 = (float) (frame % 4) / 4.0F;
        float u1 = u0 + 0.24975F;
        float v0 = (float) (frame / 2) / 2.0F;
        float v1 = v0 + 0.4995F;
        double x = prevPosX + (posX - prevPosX) * (double) partial - interpPosX;
        double y = prevPosY + (posY - prevPosY) * (double) partial - interpPosY;
        double z = prevPosZ + (posZ - prevPosZ) * (double) partial - interpPosZ;

        t.draw();
        mc.getTextureManager().bindTexture(SWEEP);
        t.startDrawingQuads();
        t.setBrightness(240);
        t.setColorRGBA_F(particleRed, particleGreen, particleBlue, 1.0F);
        t.addVertexWithUV(x - (double) (rotX * size) - (double) (rotYZ * size), y - (double) (rotXZ * size * 0.5F),
                z - (double) (rotZ * size) - (double) (rotXY * size), (double) u1, (double) v1);
        t.addVertexWithUV(x - (double) (rotX * size) + (double) (rotYZ * size), y + (double) (rotXZ * size * 0.5F),
                z - (double) (rotZ * size) + (double) (rotXY * size), (double) u1, (double) v0);
        t.addVertexWithUV(x + (double) (rotX * size) + (double) (rotYZ * size), y + (double) (rotXZ * size * 0.5F),
                z + (double) (rotZ * size) + (double) (rotXY * size), (double) u0, (double) v0);
        t.addVertexWithUV(x + (double) (rotX * size) - (double) (rotYZ * size), y - (double) (rotXZ * size * 0.5F),
                z + (double) (rotZ * size) - (double) (rotXY * size), (double) u0, (double) v1);
        t.draw();
        mc.getTextureManager().bindTexture(PARTICLES);
        t.startDrawingQuads();
    }
}
