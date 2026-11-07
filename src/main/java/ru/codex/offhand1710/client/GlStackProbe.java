package ru.codex.offhand1710.client;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

/**
 * Diagnostic only. Enabled by the presence of the file mc112bridge-glprobe.txt in the game directory.
 *
 * At every render stage it measures the depth of all OpenGL matrix/attribute stacks and drains the
 * GL error flag, so a GL error is attributed to the gap between two stages ("GL probe" lines in the
 * log). Frames 3 and 4 are logged completely as a timeline. Stops itself after MAX_FRAMES in-world
 * frames. Note: while active it consumes GL errors, so the in-game "OpenGL error" chat spam stops.
 */
public final class GlStackProbe {
    private static final int MAX_FRAMES = 3000;
    private static final int MAX_ERROR_REPORTS = 60;
    private static final int MAX_DELTA_REPORTS_PER_SOURCE = 5;
    private static final int MAX_TRACE_LINES_PER_FRAME = 700;

    private static final int[] PARAMS = {
            GL11.GL_MODELVIEW_STACK_DEPTH,
            GL11.GL_PROJECTION_STACK_DEPTH,
            GL11.GL_TEXTURE_STACK_DEPTH,
            GL11.GL_ATTRIB_STACK_DEPTH,
            GL11.GL_CLIENT_ATTRIB_STACK_DEPTH
    };
    private static final String[] NAMES = {"modelview", "projection", "texture", "attrib", "clientAttrib"};

    private final Map<String, Integer> deltaCounts = new HashMap<String, Integer>();
    private final Map<String, Integer> errorPairCounts = new HashMap<String, Integer>();
    private final Map<String, int[]> open = new HashMap<String, int[]>();
    private int frames;
    private int traceLines;
    private int errorReports;
    private boolean active = true;
    private String prevStage = "(none)";
    private int[] frameStart = new int[PARAMS.length];

    private static int[] snapshot() {
        int[] values = new int[PARAMS.length];
        for (int i = 0; i < PARAMS.length; i++) {
            values[i] = GL11.glGetInteger(PARAMS[i]);
        }
        return values;
    }

    private static String format(int[] s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(NAMES[i]).append('=').append(s[i]);
        }
        return out.toString();
    }

    private boolean ready() {
        return active && Minecraft.getMinecraft().thePlayer != null;
    }

    private boolean tracing() {
        // Early frames: the texture stack is still growing there, so the stage that grows it is visible.
        return (frames == 3 || frames == 4) && traceLines < MAX_TRACE_LINES_PER_FRAME;
    }

    /** Measures stacks, drains GL errors and attributes them to the gap since the previous stage. */
    private int[] stage(String name) {
        int[] s = snapshot();
        int err = GL11.glGetError();
        for (int i = 0; i < 8 && GL11.glGetError() != 0; i++) {
            // drain any further flags
        }
        if (tracing()) {
            traceLines++;
            FMLLog.info("[mc112bridge GL probe] T%d %s | %s | err=%d", frames, name, format(s), err);
        }
        if (err != 0) {
            String key = prevStage + " -> " + name + " err " + err;
            Integer seen = errorPairCounts.get(key);
            int count = seen == null ? 0 : seen.intValue();
            if (count < 2 && errorReports < MAX_ERROR_REPORTS) {
                errorPairCounts.put(key, Integer.valueOf(count + 1));
                errorReports++;
                FMLLog.info("[mc112bridge GL probe] frame %d: GL error %d first seen at '%s' "
                        + "(previous stage '%s' was clean) | %s",
                        frames, err, name, prevStage, format(s));
            }
        }
        prevStage = name;
        return s;
    }

    private void reportDelta(String source, int delta) {
        String key = source + (delta > 0 ? "+" : "-");
        Integer seen = deltaCounts.get(key);
        int count = seen == null ? 0 : seen.intValue();
        if (count >= MAX_DELTA_REPORTS_PER_SOURCE) {
            return;
        }
        deltaCounts.put(key, Integer.valueOf(count + 1));
        FMLLog.info("[mc112bridge GL probe] frame %d: %s changed by %d", frames, source, delta);
    }

    private void compare(String label, int[] before, int[] after) {
        for (int i = 0; i < before.length; i++) {
            int delta = after[i] - before[i];
            if (delta != 0) {
                reportDelta(label + " [" + NAMES[i] + " stack]", delta);
            }
        }
    }

    private void begin(String key, String stageName) {
        open.put(key, stage(stageName));
    }

    private void end(String key, String label, String stageName) {
        int[] after = stage(stageName);
        int[] before = open.remove(key);
        if (before != null) {
            compare(label, before, after);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!active || Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            frames++;
            traceLines = 0;
            frameStart = stage("frame start");
            if (frames <= 3 || frames % 100 == 0) {
                FMLLog.info("[mc112bridge GL probe] frame %d start: %s", frames, format(frameStart));
            }
            if (frames > MAX_FRAMES) {
                active = false;
                FMLLog.info("[mc112bridge GL probe] finished after %d frames", MAX_FRAMES);
            }
        } else {
            compare("whole frame", frameStart, stage("frame end"));
        }
    }

    /** First and last handler of RenderWorldLastEvent: anything in between is another mod's handler. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldLastFirst(RenderWorldLastEvent event) {
        if (ready()) {
            stage("world render last: BEFORE other mods' handlers");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldLastFinal(RenderWorldLastEvent event) {
        if (ready()) {
            compare("frame start -> end of world render", frameStart,
                    stage("world render last: AFTER other mods' handlers"));
        }
    }

    /** Fired right before the vanilla first-person hand is drawn. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHand(RenderHandEvent event) {
        if (ready()) {
            stage("just before vanilla hand render");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (ready()) {
            begin("overlay:" + event.type, "pre overlay " + event.type);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (ready()) {
            end("overlay:" + event.type, "HUD element " + event.type, "post overlay " + event.type);
        }
    }

    @SubscribeEvent
    public void onLivingPre(RenderLivingEvent.Pre event) {
        if (ready()) {
            begin("living:" + event.entity.getEntityId(),
                    "pre entity " + event.entity.getClass().getSimpleName());
        }
    }

    @SubscribeEvent
    public void onLivingPost(RenderLivingEvent.Post event) {
        if (ready()) {
            end("living:" + event.entity.getEntityId(),
                    "rendering entity " + event.entity.getClass().getName(),
                    "post entity " + event.entity.getClass().getSimpleName());
        }
    }

    @SubscribeEvent
    public void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (ready()) {
            begin("player:" + event.entityPlayer.getEntityId(), "pre player");
        }
    }

    @SubscribeEvent
    public void onPlayerPost(RenderPlayerEvent.Post event) {
        if (ready()) {
            end("player:" + event.entityPlayer.getEntityId(), "rendering player", "post player");
        }
    }

    @SubscribeEvent
    public void onSpecialsPre(RenderPlayerEvent.Specials.Pre event) {
        if (ready()) {
            begin("specials:" + event.entityPlayer.getEntityId(), "pre player specials");
        }
    }

    @SubscribeEvent
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        if (ready()) {
            end("specials:" + event.entityPlayer.getEntityId(),
                    "player specials (held item, armor, cape)", "post player specials");
        }
    }
}
