package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import ru.codex.offhand1710.Particles112;

/** The particles of Particles112 on the client: ParticleDragonBreath and ParticleFallingDust of 1.12.2. */
public final class Particles112Client implements Particles112.Factory {
    static void register() {
        Particles112.factory = new Particles112Client();
    }

    private static boolean far(World world, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderViewEntity == null || mc.effectRenderer == null) {
            return true;
        }
        // RenderGlobal.spawnParticle0: not beyond 32 blocks, fewer with the particle option lowered
        double dx = mc.renderViewEntity.posX - x;
        double dy = mc.renderViewEntity.posY - y;
        double dz = mc.renderViewEntity.posZ - z;
        int setting = mc.gameSettings.particleSetting;
        return dx * dx + dy * dy + dz * dz > 1024.0D || setting > 1 || setting == 1 && world.rand.nextInt(3) == 0;
    }

    @Override
    public void dragonBreath(World world, double x, double y, double z, double mx, double my, double mz) {
        if (!far(world, x, y, z)) {
            Minecraft.getMinecraft().effectRenderer.addEffect(new Breath(world, x, y, z, mx, my, mz));
        }
    }

    @Override
    public void fallingDust(World world, double x, double y, double z, int rgb) {
        if (!far(world, x, y, z)) {
            float r = (float) (rgb >> 16 & 255) / 255.0F;
            float g = (float) (rgb >> 8 & 255) / 255.0F;
            float b = (float) (rgb & 255) / 255.0F;
            Minecraft.getMinecraft().effectRenderer.addEffect(new Dust(world, x, y, z, r, g, b));
        }
    }

    /** ParticleDragonBreath of 1.12.2. */
    public static final class Breath extends EntityFX {
        private final float oSize;
        private boolean hasHitGround;

        Breath(World world, double x, double y, double z, double mx, double my, double mz) {
            super(world, x, y, z, mx, my, mz);
            motionX = mx;
            motionY = my;
            motionZ = mz;
            particleRed = next(0.7176471F, 0.8745098F);
            particleGreen = 0.0F;
            particleBlue = next(0.8235294F, 0.9764706F);
            particleScale *= 0.75F;
            oSize = particleScale;
            particleMaxAge = (int) (20.0D / ((double) rand.nextFloat() * 0.8D + 0.2D));
            noClip = true;
        }

        private float next(float min, float max) {
            return min + rand.nextFloat() * (max - min);
        }

        @Override
        public void onUpdate() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            if (particleAge++ >= particleMaxAge) {
                setDead();
                return;
            }
            setParticleTextureIndex(3 * particleAge / particleMaxAge + 5);
            if (onGround) {
                motionY = 0.0D;
                hasHitGround = true;
            }
            if (hasHitGround) {
                motionY += 0.002D;
            }
            moveEntity(motionX, motionY, motionZ);
            if (posY == prevPosY) {
                motionX *= 1.1D;
                motionZ *= 1.1D;
            }
            motionX *= 0.9599999785423279D;
            motionZ *= 0.9599999785423279D;
            if (hasHitGround) {
                motionY *= 0.9599999785423279D;
            }
        }

        @Override
        public void renderParticle(Tessellator t, float pt, float rx, float rxz, float rz, float ryz, float rxy) {
            particleScale = oSize * MathHelper.clamp_float(((float) particleAge + pt) / (float) particleMaxAge * 32.0F, 0.0F, 1.0F);
            super.renderParticle(t, pt, rx, rxz, rz, ryz, rxy);
        }
    }

    /** ParticleFallingDust of 1.12.2: drifts down slowly, spinning. */
    public static final class Dust extends EntityFX {
        private final float oSize;
        private final float rotSpeed;
        private float angle;
        private float prevAngle;

        Dust(World world, double x, double y, double z, float r, float g, float b) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            motionX = 0.0D;
            motionY = 0.0D;
            motionZ = 0.0D;
            particleRed = r;
            particleGreen = g;
            particleBlue = b;
            particleScale *= 0.75F;
            particleScale *= 0.9F;
            oSize = particleScale;
            particleMaxAge = (int) (32.0D / (Math.random() * 0.8D + 0.2D));
            particleMaxAge = (int) ((float) particleMaxAge * 0.9F);
            rotSpeed = ((float) Math.random() - 0.5F) * 0.1F;
            angle = (float) Math.random() * ((float) Math.PI * 2F);
        }

        @Override
        public void onUpdate() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            if (particleAge++ >= particleMaxAge) {
                setDead();
            }
            prevAngle = angle;
            angle += (float) Math.PI * rotSpeed * 2.0F;
            if (onGround) {
                prevAngle = angle = 0.0F;
            }
            setParticleTextureIndex(7 - particleAge * 8 / particleMaxAge);
            moveEntity(motionX, motionY, motionZ);
            motionY -= 0.003000000026077032D;
            motionY = Math.max(motionY, -0.14000000059604645D);
        }

        /** Particle.renderParticle of 1.12.2 with the quad turned by the particle angle around the view direction. */
        @Override
        public void renderParticle(Tessellator t, float pt, float rx, float rxz, float rz, float ryz, float rxy) {
            particleScale = oSize * MathHelper.clamp_float(((float) particleAge + pt) / (float) particleMaxAge * 32.0F, 0.0F, 1.0F);
            float u0 = (float) particleTextureIndexX / 16.0F;
            float u1 = u0 + 0.0624375F;
            float v0 = (float) particleTextureIndexY / 16.0F;
            float v1 = v0 + 0.0624375F;
            float s = 0.1F * particleScale;
            float x = (float) (prevPosX + (posX - prevPosX) * (double) pt - interpPosX);
            float y = (float) (prevPosY + (posY - prevPosY) * (double) pt - interpPosY);
            float z = (float) (prevPosZ + (posZ - prevPosZ) * (double) pt - interpPosZ);
            double[][] c = {
                {-rx * s - ryz * s, -rxz * s, -rz * s - rxy * s},
                {-rx * s + ryz * s, rxz * s, -rz * s + rxy * s},
                {rx * s + ryz * s, rxz * s, rz * s + rxy * s},
                {rx * s - ryz * s, -rxz * s, rz * s - rxy * s}};
            if (angle != 0.0F) {
                EntityLivingBase view = Minecraft.getMinecraft().renderViewEntity;
                Vec3 dir = view == null ? Vec3.createVectorHelper(0, 0, 1) : view.getLook(pt);
                float a = angle + (angle - prevAngle) * pt;
                float cos = MathHelper.cos(a * 0.5F);
                float sin = MathHelper.sin(a * 0.5F);
                double qx = sin * dir.xCoord;
                double qy = sin * dir.yCoord;
                double qz = sin * dir.zCoord;
                double qq = qx * qx + qy * qy + qz * qz;
                for (int l = 0; l < 4; l++) {
                    double vx = c[l][0];
                    double vy = c[l][1];
                    double vz = c[l][2];
                    double dot = vx * qx + vy * qy + vz * qz;
                    double k = (double) (cos * cos) - qq;
                    // q * 2(v.q) + v (cos^2 - q.q) + (q x v) * 2cos
                    c[l][0] = qx * 2.0D * dot + vx * k + (qy * vz - qz * vy) * 2.0D * cos;
                    c[l][1] = qy * 2.0D * dot + vy * k + (qz * vx - qx * vz) * 2.0D * cos;
                    c[l][2] = qz * 2.0D * dot + vz * k + (qx * vy - qy * vx) * 2.0D * cos;
                }
            }
            t.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);
            t.addVertexWithUV(x + c[0][0], y + c[0][1], z + c[0][2], u1, v1);
            t.addVertexWithUV(x + c[1][0], y + c[1][1], z + c[1][2], u1, v0);
            t.addVertexWithUV(x + c[2][0], y + c[2][1], z + c[2][2], u0, v0);
            t.addVertexWithUV(x + c[3][0], y + c[3][1], z + c[3][2], u0, v1);
        }
    }
}
