package ru.codex.offhand1710.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.RenderEndPortal;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.End112;

/**
 * TileEntityEndGatewayRenderer of 1.12.2: the end portal effect on all six faces, and while the gateway is new
 * (200 ticks, magenta, up to the build limit) or cooling down after a teleport (40 ticks, purple, 50 blocks) a beacon
 * beam up and down whose length follows sin(progress * pi).
 */
public final class GatewayRender extends RenderEndPortal {
    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/beacon_beam.png");
    private static final float[] MAGENTA = {0.69803923F, 0.29803923F, 0.84705883F};
    private static final float[] PURPLE = {0.49803922F, 0.24705882F, 0.69803923F};

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
        // RenderEndPortal draws one horizontal plane at y + 0.75; turn it onto each face of the block
        float[][] turns = {{0, 1, 0, 0}, {180, 1, 0, 0}, {90, 1, 0, 0}, {-90, 1, 0, 0}, {90, 0, 0, 1}, {-90, 0, 0, 1}};
        for (float[] t : turns) {
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
            GL11.glRotatef(t[0], t[1], t[2], t[3]);
            GL11.glTranslated(-x - 0.5D, -y - 0.5D, -z - 0.5D);
            super.renderTileEntityAt(tile, x, y + 0.25D, z, partial);
            GL11.glPopMatrix();
        }
        if (!(tile instanceof End112.GatewayTile)) {
            return;
        }
        End112.GatewayTile g = (End112.GatewayTile) tile;
        boolean spawning = g.spawning();
        if (!spawning && g.cooldown <= 0) {
            return;
        }
        float progress = spawning ? ((float) g.age + partial) / 200.0F : ((float) (40 - g.cooldown) + partial) / 40.0F;
        double reach = spawning ? 256.0D - (double) tile.yCoord : 50.0D;
        float f = MathHelper.sin(Math.min(1.0F, progress) * (float) Math.PI);
        int length = MathHelper.floor_double((double) f * reach);
        float[] color = spawning ? MAGENTA : PURPLE;
        long time = tile.getWorldObj().getTotalWorldTime();
        beam(x, y, z, partial, f, time, length, color);
        beam(x, y, z, partial, f, time, -length, color);
    }

    /** TileEntityBeaconRenderer.renderBeamSegment of 1.12.2 with radii 0.15 and 0.175. */
    private void beam(double x, double y, double z, float partial, float alpha, long time, int height, float[] c) {
        if (height == 0) {
            return;
        }
        bindTexture(BEAM);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        double top = (double) height;
        double t = (double) Math.floorMod(time, 40L) + (double) partial;
        double v1 = height < 0 ? t : -t;
        double raw = v1 * 0.2D - (double) MathHelper.floor_double(v1 * 0.1D);
        double frac = raw - Math.floor(raw) - 1.0D;
        double angle = t * 0.025D * -1.5D;
        double r = 0.15D;
        double ax = 0.5D + Math.cos(angle + 2.356194490192345D) * r, az = 0.5D + Math.sin(angle + 2.356194490192345D) * r;
        double bx = 0.5D + Math.cos(angle + Math.PI / 4.0D) * r, bz = 0.5D + Math.sin(angle + Math.PI / 4.0D) * r;
        double cx = 0.5D + Math.cos(angle + 3.9269908169872414D) * r, cz = 0.5D + Math.sin(angle + 3.9269908169872414D) * r;
        double dx = 0.5D + Math.cos(angle + 5.497787143782138D) * r, dz = 0.5D + Math.sin(angle + 5.497787143782138D) * r;
        double vTop = (double) height * (0.5D / r) + frac, vBottom = frac;
        if (height < 0) {
            double s = vTop;
            vTop = vBottom;
            vBottom = s;
        }
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.setColorRGBA_F(c[0], c[1], c[2], 1.0F);
        quad(tes, x, y, z, ax, az, bx, bz, top, vTop, vBottom);
        quad(tes, x, y, z, dx, dz, cx, cz, top, vTop, vBottom);
        quad(tes, x, y, z, bx, bz, dx, dz, top, vTop, vBottom);
        quad(tes, x, y, z, cx, cz, ax, az, top, vTop, vBottom);
        tes.draw();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        double g = 0.175D;
        tes.startDrawingQuads();
        tes.setColorRGBA_F(c[0], c[1], c[2], 0.125F);
        double lo = 0.5D - g, hi = 0.5D + g;
        quad(tes, x, y, z, lo, lo, hi, lo, top, vTop, vBottom);
        quad(tes, x, y, z, hi, hi, lo, hi, top, vTop, vBottom);
        quad(tes, x, y, z, hi, lo, hi, hi, top, vTop, vBottom);
        quad(tes, x, y, z, lo, hi, lo, lo, top, vTop, vBottom);
        tes.draw();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    private static void quad(Tessellator t, double x, double y, double z, double x1, double z1, double x2, double z2, double h, double vTop, double vBottom) {
        t.addVertexWithUV(x + x1, y + h, z + z1, 1.0D, vTop);
        t.addVertexWithUV(x + x1, y, z + z1, 1.0D, vBottom);
        t.addVertexWithUV(x + x2, y, z + z2, 0.0D, vBottom);
        t.addVertexWithUV(x + x2, y + h, z + z2, 0.0D, vTop);
    }
}
