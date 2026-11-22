package ru.codex.offhand1710.client;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.RenderEnderCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.End112;

/**
 * The end crystal of 1.7.10 plus the beam of RenderEnderCrystal 1.12.2: a crystal with a beam target (the respawn of
 * the dragon) draws the twisting beam texture from itself to the middle of that block.
 */
public final class CrystalRender extends RenderEnderCrystal {
    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/endercrystal/endercrystal_beam.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
        super.doRender(entity, x, y, z, yaw, partial);
        if (!(entity instanceof End112.Crystal)) {
            return;
        }
        int[] target = ((End112.Crystal) entity).beam();
        if (target == null) {
            return;
        }
        End112.Crystal c = (End112.Crystal) entity;
        float time = (float) c.innerRotation + partial;
        float bob = MathHelper.sin(time * 0.2F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.2F;
        // the crystal of this mod stands two blocks lower than its model shows (End112: it sits on the bedrock)
        double fromY = y + 2.0D - 0.3D + bob * 0.4F;
        double dx = target[0] + 0.5D - c.posX;
        double dy = target[1] + 0.5D - (c.posY + 2.0D - 0.3D + bob * 0.4F);
        double dz = target[2] + 0.5D - c.posZ;
        beam(x, fromY, z, dx, dy, dz, c.innerRotation, partial);
    }

    /** A tube of eight sides, dark at the start and light at the end, its texture running along it with time. */
    static void beam(double x, double y, double z, double dx, double dy, double dz, int ticks, float partial) {
        float horizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float length = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef((float) (-Math.atan2(dz, dx)) * 180.0F / (float) Math.PI - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef((float) (-Math.atan2(horizontal, dy)) * 180.0F / (float) Math.PI - 90.0F, 1.0F, 0.0F, 0.0F);
        Tessellator t = Tessellator.instance;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_CULL_FACE);
        net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(BEAM);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float v0 = 0.0F - ((float) ticks + partial) * 0.01F;
        float v1 = length / 32.0F - ((float) ticks + partial) * 0.01F;
        t.startDrawing(5);
        for (int i = 0; i <= 8; i++) {
            float sx = MathHelper.sin((float) (i % 8) * (float) Math.PI * 2.0F / 8.0F) * 0.75F;
            float sy = MathHelper.cos((float) (i % 8) * (float) Math.PI * 2.0F / 8.0F) * 0.75F;
            float u = (float) (i % 8) / 8.0F;
            t.setColorOpaque_I(0);
            t.addVertexWithUV(sx * 0.2F, sy * 0.2F, 0.0D, u, v1);
            t.setColorOpaque_I(16777215);
            t.addVertexWithUV(sx, sy, length, u, v0);
        }
        t.draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();
    }
}
