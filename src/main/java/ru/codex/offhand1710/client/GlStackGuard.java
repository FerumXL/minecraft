package ru.codex.offhand1710.client;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Repairs OpenGL matrix stacks that something left unbalanced during a frame.
 *
 * Seen in a real game with OptiFine and HBM: every frame one glPushMatrix lands on the texture matrix stack
 * while the matching glPopMatrix lands on the modelview stack. The texture stack then grows by one per frame
 * until it overflows (GL error 1283 "Stack overflow" every frame, texture matrices no longer restored) and
 * Minecraft's own glPopMatrix after the frame underflows (1284).
 *
 * At the end of each render tick a balanced frame always has texture stack depth 1 on units 0 and 1, the
 * matrix mode GL_MODELVIEW and the same modelview depth as at the start of the tick. Only when one of these
 * is wrong the stacks are put back (extra texture matrices popped, missing modelview entries pushed, extra
 * ones popped) and the first repair is logged with the depths found, so the problem stays visible in the log.
 */
public final class GlStackGuard {
    private int startModelview = -1;
    private int repairs;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            startModelview = Minecraft.getMinecraft().theWorld == null ? -1
                    : GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || startModelview < 1) {
            return;
        }
        int expected = startModelview;
        startModelview = -1;
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int modelview = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int tex0 = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int tex1 = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        GL13.glActiveTexture(active);
        if (mode == GL11.GL_MODELVIEW && modelview == expected && tex0 <= 1 && tex1 <= 1) {
            return;
        }

        if (tex0 > 1 || tex1 > 1) {
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            popTexture(GL13.GL_TEXTURE0, tex0);
            popTexture(GL13.GL_TEXTURE1, tex1);
            GL13.glActiveTexture(active);
        }
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        for (int i = modelview; i < expected; i++) {
            GL11.glPushMatrix();
        }
        for (int i = modelview; i > expected; i--) {
            GL11.glPopMatrix();
        }

        repairs++;
        if (repairs == 1 || repairs == 1000 || repairs % 100000 == 0) {
            FMLLog.warning("[mc112bridge] OpenGL matrix stacks were left unbalanced by a frame (repair #%d): "
                    + "matrixMode=%d modelview=%d (expected %d) texture0=%d texture1=%d (expected 1). "
                    + "Restored; the cause is another mod or OptiFine.", repairs, mode, modelview, expected, tex0, tex1);
        }
    }

    private static void popTexture(int unit, int depth) {
        if (depth <= 1) {
            return;
        }
        GL13.glActiveTexture(unit);
        for (int i = depth; i > 1; i--) {
            GL11.glPopMatrix();
        }
    }
}
