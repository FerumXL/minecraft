package ru.codex.offhand1710.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.EntityElderGuardian;
import ru.codex.offhand1710.EntityGuardian;

/** RenderGuardian and RenderElderGuardian of 1.12.2, with the laser beam. */
public final class RenderGuardian extends RenderLiving {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", "textures/entity/guardian.png");
    private static final ResourceLocation ELDER = new ResourceLocation("mc112bridge", "textures/entity/guardian_elder.png");
    private static final ResourceLocation BEAM = new ResourceLocation("mc112bridge", "textures/entity/guardian_beam.png");

    public RenderGuardian() {
        super(new ModelGuardian(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityElderGuardian ? ELDER : TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partial) {
        if (entity instanceof EntityElderGuardian) {
            GL11.glScalef(2.35F, 2.35F, 2.35F);
        }
    }

    private static Vec3 position(EntityLivingBase entity, double offsetY, float partial) {
        return Vec3.createVectorHelper(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partial,
                offsetY - (double) entity.yOffset + entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partial,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partial);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partial) {
        super.doRender(entity, x, y, z, yaw, partial);
        EntityGuardian guardian = (EntityGuardian) entity;
        EntityLivingBase target = guardian.targeted();
        if (target != null) {
            renderBeam(guardian, target, x, y, z, partial);
        }
    }

    private void renderBeam(EntityGuardian guardian, EntityLivingBase target, double x, double y, double z, float partial) {
        float progress = guardian.attackProgress(partial);
        bindTexture(BEAM);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        float time = (float) guardian.worldObj.getTotalWorldTime() + partial;
        float scroll = time * 0.5F % 1.0F;
        float eye = guardian.getEyeHeight();
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + eye, (float) z);
        Vec3 to = position(target, (double) target.height * 0.5D, partial);
        Vec3 from = position(guardian, (double) eye, partial);
        Vec3 dir = Vec3.createVectorHelper(to.xCoord - from.xCoord, to.yCoord - from.yCoord, to.zCoord - from.zCoord);
        double length = dir.lengthVector() + 1.0D;
        dir = dir.normalize();
        float pitch = (float) Math.acos(dir.yCoord);
        float yaw = (float) Math.atan2(dir.zCoord, dir.xCoord);
        GL11.glRotatef((1.5707964F + -yaw) * 57.295776F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch * 57.295776F, 1.0F, 0.0F, 0.0F);
        double angle = (double) time * 0.05D * -1.5D;
        float charge = progress * progress;
        int red = 64 + (int) (charge * 191.0F);
        int green = 32 + (int) (charge * 191.0F);
        int blue = 128 - (int) (charge * 64.0F);
        double v0 = (double) (-1.0F + scroll);
        double v1 = length * 2.5D + v0;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA(red, green, blue, 255);
        double[][] side = {{angle + Math.PI, angle}, {angle + Math.PI / 2.0D, angle + 4.71238898038469D}};
        for (int i = 0; i < 2; i++) {
            double ax = Math.cos(side[i][0]) * 0.2D;
            double az = Math.sin(side[i][0]) * 0.2D;
            double bx = Math.cos(side[i][1]) * 0.2D;
            double bz = Math.sin(side[i][1]) * 0.2D;
            t.addVertexWithUV(ax, length, az, 0.4999D, v1);
            t.addVertexWithUV(ax, 0.0D, az, 0.4999D, v0);
            t.addVertexWithUV(bx, 0.0D, bz, 0.0D, v0);
            t.addVertexWithUV(bx, length, bz, 0.0D, v1);
        }
        double v = guardian.ticksExisted % 2 == 0 ? 0.5D : 0.0D;
        t.addVertexWithUV(Math.cos(angle + 2.356194490192345D) * 0.282D, length, Math.sin(angle + 2.356194490192345D) * 0.282D, 0.5D, v + 0.5D);
        t.addVertexWithUV(Math.cos(angle + 0.7853981633974483D) * 0.282D, length, Math.sin(angle + 0.7853981633974483D) * 0.282D, 1.0D, v + 0.5D);
        t.addVertexWithUV(Math.cos(angle + 5.497787143782138D) * 0.282D, length, Math.sin(angle + 5.497787143782138D) * 0.282D, 1.0D, v);
        t.addVertexWithUV(Math.cos(angle + 3.9269908169872414D) * 0.282D, length, Math.sin(angle + 3.9269908169872414D) * 0.282D, 0.5D, v);
        t.draw();
        GL11.glPopMatrix();
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
    }
}
