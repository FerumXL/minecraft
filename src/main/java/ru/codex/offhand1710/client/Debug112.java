package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import ru.codex.offhand1710.SkeletonTrap112;
import ru.codex.offhand1710.Spectator112;
import ru.codex.offhand1710.States112;

/**
 * The debug screen of 1.12.2 (GuiOverlayDebug): its left and right columns drawn on grey bars, the axes in place of the
 * crosshair, the chunk borders of F3 + G (DebugRendererChunkBorder) and the F3 key combinations with their chat
 * feedback (Minecraft.processKeyF3). The figures 1.7.10 has no source for (renderer statistics) stay those of 1.7.10.
 * -Dmc112.debug17 leaves the debug screen of 1.7.10.
 */
public final class Debug112 {
    public static final boolean OFF = System.getProperty("mc112.debug17") != null;
    /** F3 + G. */
    public static boolean chunkBorders;
    /** A combination was used while F3 was held: F3 does not toggle the screen (1.12.2 toggles on release). */
    private static boolean f3Used;
    private static volatile String cpu;
    /** Translation keys of the last messages printed (autotest). */
    public static final List<String> PRINTED = new java.util.concurrent.CopyOnWriteArrayList<String>();
    /** GL_VENDOR, GL_RENDERER, GL_VERSION, read on the render thread. */
    private static String[] gl = {"", "", ""};

    // ---- the columns ----

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onText(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OFF || !mc.gameSettings.showDebugInfo || event.left == null || event.right == null) {
            return;
        }
        if (gl[0].isEmpty()) {
            gl = new String[] {GL11.glGetString(GL11.GL_VENDOR), GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION)};
        }
        List<String> left = new ArrayList<String>();
        List<String> right = new ArrayList<String>();
        if (!layout(event.left, event.right, RulesClient.reduced, left, right)) {
            return;
        }
        event.setCanceled(true);
        FontRenderer font = mc.fontRenderer;
        int h = font.FONT_HEIGHT;
        for (int i = 0; i < left.size(); i++) {
            String s = left.get(i);
            if (s != null && !s.isEmpty()) {
                int w = font.getStringWidth(s);
                int y = 2 + h * i;
                Gui.drawRect(1, y - 1, 2 + w + 1, y + h - 1, 0x90505050);
                font.drawString(s, 2, y, 0xE0E0E0);
            }
        }
        int width = event.resolution.getScaledWidth();
        if (lagometer) {
            drawLagometer(event.resolution.getScaledHeight());
        }
        for (int i = 0; i < right.size(); i++) {
            String s = right.get(i);
            if (s != null && !s.isEmpty()) {
                int w = font.getStringWidth(s);
                int x = width - 2 - w;
                int y = 2 + h * i;
                Gui.drawRect(x - 1, y - 1, x + w + 1, y + h - 1, 0x90505050);
                font.drawString(s, x, y, 0xE0E0E0);
            }
        }
    }

    /**
     * The columns of 1.12.2 from the ones GuiIngameForge of 1.7.10 made. Lines other mods added after those of Forge
     * are kept at the end. False when the lists are not the debug screen of 1.7.10.
     */
    public static boolean layout(List<String> left17, List<String> right17, boolean reduced, List<String> left, List<String> right) {
        Minecraft mc = Minecraft.getMinecraft();
        int ws = -1;
        for (int i = 0; i < left17.size(); i++) {
            String s = left17.get(i);
            if (s != null && s.startsWith("ws: ")) {
                ws = i;
                break;
            }
        }
        int mem = -1;
        for (int i = 0; i < right17.size(); i++) {
            String s = right17.get(i);
            if (s != null && s.startsWith("Used memory: ")) {
                mem = i;
                break;
            }
        }
        if (ws < 5 || mem < 0 || mc.renderViewEntity == null || mc.theWorld == null) {
            return false;
        }
        int leftEnd = ws + 1;
        if (leftEnd < left17.size() && left17.get(leftEnd) != null && left17.get(leftEnd).startsWith("shader: ")) {
            leftEnd++;
        }
        int rightEnd = Math.min(right17.size(), mem + 3 + cpw.mods.fml.common.FMLCommonHandler.instance().getBrandings(false).size());

        EntityLivingBase e = mc.renderViewEntity;
        World world = mc.theWorld;
        int x = MathHelper.floor_double(e.posX);
        int y = MathHelper.floor_double(e.boundingBox.minY);
        int z = MathHelper.floor_double(e.posZ);

        left.add("Minecraft " + cpw.mods.fml.common.Loader.MC_VERSION + " (" + cpw.mods.fml.common.Loader.MC_VERSION + "/" + net.minecraft.client.ClientBrandRetriever.getClientModName() + ")");
        left.add(fps(mc));
        for (int i = 1; i <= 4; i++) {
            left.add(left17.get(i));
        }
        left.add("");
        if (reduced) {
            left.add(String.format("Chunk-relative: %d %d %d", x & 15, y & 15, z & 15));
        } else {
            left.add(String.format("XYZ: %.3f / %.5f / %.3f", e.posX, e.boundingBox.minY, e.posZ));
            left.add(String.format("Block: %d %d %d", x, y, z));
            left.add(String.format("Chunk: %d %d %d in %d %d %d", x & 15, y & 15, z & 15, x >> 4, y >> 4, z >> 4));
            int facing = MathHelper.floor_double(e.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            String[] names = {"south", "west", "north", "east"};
            String[] towards = {"Towards positive Z", "Towards negative X", "Towards negative Z", "Towards positive X"};
            left.add(String.format("Facing: %s (%s) (%.1f / %.1f)", names[facing], towards[facing], MathHelper.wrapAngleTo180_float(e.rotationYaw),
                MathHelper.wrapAngleTo180_float(e.rotationPitch)));
            if (y >= 0 && y < 256 && world.blockExists(x, y, z)) {
                Chunk chunk = world.getChunkFromBlockCoords(x, z);
                left.add("Biome: " + chunk.getBiomeGenForWorldCoords(x & 15, z & 15, world.getWorldChunkManager()).biomeName);
                left.add("Light: " + chunk.getBlockLightValue(x & 15, y, z & 15, 0) + " (" + chunk.getSavedLightValue(EnumSkyBlock.Sky, x & 15, y, z & 15) + " sky, "
                    + chunk.getSavedLightValue(EnumSkyBlock.Block, x & 15, y, z & 15) + " block)");
                // World.getDifficultyForLocation: the integrated server's world when there is one (inhabited time)
                World diff = world;
                MinecraftServer server = MinecraftServer.getServer();
                if (mc.isIntegratedServerRunning() && server != null) {
                    World w = server.worldServerForDimension(world.provider.dimensionId);
                    if (w != null) {
                        diff = w;
                    }
                }
                left.add(String.format("Local Difficulty: %.2f // %.2f (Day %d)", SkeletonTrap112.additionalDifficulty(diff, x, z), SkeletonTrap112.clamped(diff, x, z),
                    world.getWorldTime() / 24000L));
            } else {
                left.add("Outside of world...");
            }
            if (mc.entityRenderer != null && mc.entityRenderer.isShaderActive()) {
                left.add("Shader: " + mc.entityRenderer.getShaderGroup().getShaderGroupName());
            }
            MovingObjectPosition hit = mc.objectMouseOver;
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                left.add(String.format("Looking at: %d %d %d", hit.blockX, hit.blockY, hit.blockZ));
            }
        }
        for (int i = leftEnd; i < left17.size(); i++) {
            left.add(left17.get(i));
        }

        for (int i = 0; i < mem; i++) {
            right.add(right17.get(i));
        }
        long max = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long used = total - Runtime.getRuntime().freeMemory();
        right.add(String.format("Java: %s %dbit", System.getProperty("java.version"), mc.isJava64bit() ? 64 : 32));
        right.add(String.format("Mem: % 2d%% %03d/%03dMB", used * 100L / max, used / 1024L / 1024L, max / 1024L / 1024L));
        right.add(String.format("Allocated: % 2d%% %03dMB", total * 100L / max, total / 1024L / 1024L));
        right.add("");
        right.add(String.format("CPU: %s", cpu()));
        right.add("");
        right.add(String.format("Display: %dx%d (%s)", mc.displayWidth, mc.displayHeight, gl[0]));
        right.add(gl[1]);
        right.add(gl[2]);
        MovingObjectPosition hit = mc.objectMouseOver;
        if (!reduced && hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block b = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            int meta = world.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
            right.add("");
            String n112 = States112.name112(b, meta);
            right.add(n112 != null ? n112 : String.valueOf(Block.blockRegistry.getNameForObject(b)));
            Map<String, String> props = n112 == null ? null : States112.props(n112, meta);
            if (props != null) {
                actualState(world, hit.blockX, hit.blockY, hit.blockZ, b, props);
                for (Map.Entry<String, String> p : props.entrySet()) {
                    String v = p.getValue();
                    if ("true".equals(v)) {
                        v = EnumChatFormatting.GREEN + v;
                    } else if ("false".equals(v)) {
                        v = EnumChatFormatting.RED + v;
                    }
                    right.add(p.getKey() + ": " + v);
                }
            }
        }
        for (int i = rightEnd; i < right17.size(); i++) {
            right.add(right17.get(i));
        }
        return true;
    }

    /**
     * Block.getActualState of 1.12.2 for the properties it takes from the neighbours, which the metadata of 1.7.10 does
     * not hold: connections of fences, walls, panes, vines and redstone, snowy, the other half of doors and double
     * plants, in_wall of fence gates. The connections are those the 1.7.10 renderer draws. -Dmc112.actualState17 shows
     * the metadata only.
     */
    public static void actualState(net.minecraft.world.IBlockAccess w, int x, int y, int z, Block b, Map<String, String> p) {
        if (System.getProperty("mc112.actualState17") != null) {
            return;
        }
        if (b instanceof net.minecraft.block.BlockFence) {
            net.minecraft.block.BlockFence f = (net.minecraft.block.BlockFence) b;
            put(p, "north", f.canConnectFenceTo(w, x, y, z - 1));
            put(p, "south", f.canConnectFenceTo(w, x, y, z + 1));
            put(p, "west", f.canConnectFenceTo(w, x - 1, y, z));
            put(p, "east", f.canConnectFenceTo(w, x + 1, y, z));
        } else if (b instanceof net.minecraft.block.BlockWall) {
            net.minecraft.block.BlockWall wall = (net.minecraft.block.BlockWall) b;
            boolean n = wall.canConnectWallTo(w, x, y, z - 1);
            boolean s = wall.canConnectWallTo(w, x, y, z + 1);
            boolean we = wall.canConnectWallTo(w, x - 1, y, z);
            boolean e = wall.canConnectWallTo(w, x + 1, y, z);
            boolean straight = n && !e && s && !we || !n && e && !s && we;
            put(p, "north", n);
            put(p, "south", s);
            put(p, "west", we);
            put(p, "east", e);
            put(p, "up", !straight || !w.isAirBlock(x, y + 1, z));
        } else if (b instanceof net.minecraft.block.BlockPane) {
            net.minecraft.block.BlockPane pane = (net.minecraft.block.BlockPane) b;
            put(p, "north", pane.canPaneConnectTo(w, x, y, z - 1, net.minecraftforge.common.util.ForgeDirection.NORTH));
            put(p, "south", pane.canPaneConnectTo(w, x, y, z + 1, net.minecraftforge.common.util.ForgeDirection.SOUTH));
            put(p, "west", pane.canPaneConnectTo(w, x - 1, y, z, net.minecraftforge.common.util.ForgeDirection.WEST));
            put(p, "east", pane.canPaneConnectTo(w, x + 1, y, z, net.minecraftforge.common.util.ForgeDirection.EAST));
        } else if (b instanceof net.minecraft.block.BlockRedstoneWire) {
            // RenderBlocks.renderBlockRedstoneWire of 1.7.10: a side, or up the face of a full block
            boolean coverUp = w.getBlock(x, y + 1, z).isNormalCube();
            int[][] sides = {{0, -1, 2}, {0, 1, 0}, {-1, 0, 1}, {1, 0, 3}};
            String[] names = {"north", "south", "west", "east"};
            for (int i = 0; i < 4; i++) {
                int sx = x + sides[i][0];
                int sz = z + sides[i][1];
                boolean full = w.getBlock(sx, y, sz).isNormalCube();
                String v = "none";
                if (!coverUp && full && net.minecraft.block.BlockRedstoneWire.isPowerProviderOrWire(w, sx, y + 1, sz, -1)) {
                    v = "up";
                } else if (net.minecraft.block.BlockRedstoneWire.isPowerProviderOrWire(w, sx, y, sz, sides[i][2])
                    || !full && net.minecraft.block.BlockRedstoneWire.isPowerProviderOrWire(w, sx, y - 1, sz, -1)) {
                    v = "side";
                }
                if (p.containsKey(names[i])) {
                    p.put(names[i], v);
                }
            }
        } else if (b instanceof net.minecraft.block.BlockVine) {
            Block above = w.getBlock(x, y + 1, z);
            put(p, "up", above.renderAsNormalBlock() && above.getMaterial().blocksMovement());
        } else if (b instanceof net.minecraft.block.BlockDoor) {
            int meta = w.getBlockMetadata(x, y, z);
            boolean upper = (meta & 8) != 0;
            int oy = upper ? y - 1 : y + 1;
            if (w.getBlock(x, oy, z) == b) {
                String n112 = States112.name112(b, meta);
                Map<String, String> other = n112 == null ? null : States112.props(n112, w.getBlockMetadata(x, oy, z));
                if (other != null) {
                    for (String k : upper ? new String[] {"facing", "open"} : new String[] {"hinge", "powered"}) {
                        if (other.containsKey(k) && p.containsKey(k)) {
                            p.put(k, other.get(k));
                        }
                    }
                }
            }
        } else if (b instanceof net.minecraft.block.BlockDoublePlant) {
            int meta = w.getBlockMetadata(x, y, z);
            if ((meta & 8) != 0 && w.getBlock(x, y - 1, z) == b) {
                String n112 = States112.name112(b, w.getBlockMetadata(x, y - 1, z));
                Map<String, String> lower = n112 == null ? null : States112.props(n112, w.getBlockMetadata(x, y - 1, z));
                if (lower != null && lower.containsKey("variant") && p.containsKey("variant")) {
                    p.put("variant", lower.get("variant"));
                }
            }
        } else if (b instanceof net.minecraft.block.BlockFenceGate) {
            int dir = net.minecraft.block.BlockDirectional.getDirection(w.getBlockMetadata(x, y, z));
            boolean alongX = dir == 0 || dir == 2;
            Block a = alongX ? w.getBlock(x - 1, y, z) : w.getBlock(x, y, z - 1);
            Block c = alongX ? w.getBlock(x + 1, y, z) : w.getBlock(x, y, z + 1);
            put(p, "in_wall", a instanceof net.minecraft.block.BlockWall || c instanceof net.minecraft.block.BlockWall);
        }
        if (b instanceof net.minecraft.block.BlockStairs && p.containsKey("shape")) {
            p.put("shape", stairsShape(w, x, y, z, b, p));
        }
        if (p.containsKey("snowy")) {
            Block above = w.getBlock(x, y + 1, z);
            put(p, "snowy", above == net.minecraft.init.Blocks.snow || above == net.minecraft.init.Blocks.snow_layer);
        }
    }

    private static final String[] FACINGS = {"north", "east", "south", "west"};

    private static int[] step(String facing) {
        return "north".equals(facing) ? new int[] {0, -1} : "south".equals(facing) ? new int[] {0, 1} : "east".equals(facing) ? new int[] {1, 0} : new int[] {-1, 0};
    }

    private static String opposite(String f) {
        return "north".equals(f) ? "south" : "south".equals(f) ? "north" : "east".equals(f) ? "west" : "east";
    }

    /** EnumFacing.rotateYCCW. */
    private static String ccw(String f) {
        return "north".equals(f) ? "west" : "west".equals(f) ? "south" : "south".equals(f) ? "east" : "north";
    }

    private static boolean xAxis(String f) {
        return "east".equals(f) || "west".equals(f);
    }

    /** facing and half of a stairs block, or null. */
    private static Map<String, String> stairs(net.minecraft.world.IBlockAccess w, int x, int y, int z) {
        Block b = w.getBlock(x, y, z);
        if (!(b instanceof net.minecraft.block.BlockStairs)) {
            return null;
        }
        int meta = w.getBlockMetadata(x, y, z);
        String n = States112.name112(b, meta);
        return n == null ? null : States112.props(n, meta);
    }

    /** BlockStairs.getStairsShape of 1.12.2. */
    static String stairsShape(net.minecraft.world.IBlockAccess w, int x, int y, int z, Block b, Map<String, String> self) {
        String facing = self.get("facing");
        String half = self.get("half");
        if (facing == null || half == null) {
            return "straight";
        }
        int[] f = step(facing);
        Map<String, String> front = stairs(w, x + f[0], y, z + f[1]);
        if (front != null && half.equals(front.get("half"))) {
            String f1 = front.get("facing");
            if (f1 != null && xAxis(f1) != xAxis(facing) && different(w, x, y, z, self, opposite(f1))) {
                return f1.equals(ccw(facing)) ? "outer_left" : "outer_right";
            }
        }
        Map<String, String> back = stairs(w, x - f[0], y, z - f[1]);
        if (back != null && half.equals(back.get("half"))) {
            String f2 = back.get("facing");
            if (f2 != null && xAxis(f2) != xAxis(facing) && different(w, x, y, z, self, f2)) {
                return f2.equals(ccw(facing)) ? "inner_left" : "inner_right";
            }
        }
        return "straight";
    }

    /** BlockStairs.isDifferentStairs. */
    private static boolean different(net.minecraft.world.IBlockAccess w, int x, int y, int z, Map<String, String> self, String side) {
        int[] s = step(side);
        Map<String, String> o = stairs(w, x + s[0], y, z + s[1]);
        return o == null || !self.get("facing").equals(o.get("facing")) || !self.get("half").equals(o.get("half"));
    }

    private static void put(Map<String, String> p, String key, boolean value) {
        if (p.containsKey(key)) {
            p.put(key, String.valueOf(value));
        }
    }

    /** Minecraft.debug of 1.12.2 from that of 1.7.10 ("60 fps, 3 chunk updates"). */
    private static String fps(Minecraft mc) {
        String[] p = mc.debug.split(" ");
        if (p.length < 3) {
            return mc.debug;
        }
        String fps = p[0];
        String updates = p[2];
        net.minecraft.client.settings.GameSettings g = mc.gameSettings;
        return fps + " fps (" + updates + " chunk update" + ("1".equals(updates) ? "" : "s") + ") T: "
            + (g.limitFramerate >= 260 ? "inf" : String.valueOf(g.limitFramerate)) + (g.enableVsync ? " vsync" : "") + (g.fancyGraphics ? "" : " fast")
            + (g.clouds ? (g.fancyGraphics ? " fancy-clouds" : " fast-clouds") : "");
    }

    /** OpenGlHelper.getCpu of 1.12.2: "<logical processors>x <name>"; the name is read once, off the render thread. */
    private static String cpu() {
        if (cpu == null) {
            cpu = "<unknown>";
            Thread t = new Thread("mc112-cpu") {
                @Override
                public void run() {
                    String name = cpuName();
                    if (name != null) {
                        cpu = Runtime.getRuntime().availableProcessors() + "x " + name;
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
        return cpu;
    }

    private static String cpuName() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Process p = new ProcessBuilder("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString").redirectErrorStream(true).start();
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while ((line = r.readLine()) != null) {
                    int i = line.indexOf("REG_SZ");
                    if (i >= 0) {
                        return line.substring(i + 6).trim();
                    }
                }
            } else if (new java.io.File("/proc/cpuinfo").exists()) {
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("/proc/cpuinfo"));
                try {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.startsWith("model name")) {
                            return line.substring(line.indexOf(':') + 1).trim();
                        }
                    }
                } finally {
                    r.close();
                }
            } else if (os.contains("mac")) {
                Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
                return new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream())).readLine();
            }
        } catch (Exception ignored) {
            // the name stays <unknown>
        }
        return null;
    }

    // ---- the axes in place of the crosshair (GuiIngame.renderAttackIndicator, OpenGlHelper.renderDirections) ----

    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OFF || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !mc.gameSettings.showDebugInfo || mc.gameSettings.hideGUI
            || RulesClient.reduced || mc.gameSettings.thirdPersonView != 0 || mc.renderViewEntity == null) {
            return;
        }
        event.setCanceled(true);
        EntityLivingBase e = mc.renderViewEntity;
        float pt = event.partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslatef(event.resolution.getScaledWidth() / 2, event.resolution.getScaledHeight() / 2, 0.0F);
        GL11.glRotatef(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt, -1.0F, 0.0F, 0.0F);
        GL11.glRotatef(e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(-1.0F, -1.0F, -1.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;
        int size = 10;
        GL11.glLineWidth(4.0F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA(0, 0, 0, 255);
        t.addVertex(0, 0, 0);
        t.addVertex(size, 0, 0);
        t.addVertex(0, 0, 0);
        t.addVertex(0, size, 0);
        t.addVertex(0, 0, 0);
        t.addVertex(0, 0, size);
        t.draw();
        GL11.glLineWidth(2.0F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA(255, 0, 0, 255);
        t.addVertex(0, 0, 0);
        t.addVertex(size, 0, 0);
        t.setColorRGBA(0, 255, 0, 255);
        t.addVertex(0, 0, 0);
        t.addVertex(0, size, 0);
        t.setColorRGBA(127, 127, 255, 255);
        t.addVertex(0, 0, 0);
        t.addVertex(0, 0, size);
        t.draw();
        GL11.glLineWidth(1.0F);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    // ---- F3 + G (DebugRendererChunkBorder) ----

    @SubscribeEvent
    public void onWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OFF || !chunkBorders || mc.renderViewEntity == null) {
            return;
        }
        EntityLivingBase e = mc.renderViewEntity;
        float pt = event.partialTicks;
        double d0 = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt;
        double d1 = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt;
        double d2 = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt;
        double d3 = 0.0D - d1;
        double d4 = 256.0D - d1;
        double d5 = (double) (MathHelper.floor_double(e.posX) >> 4 << 4) - d0;
        double d6 = (double) (MathHelper.floor_double(e.posZ) >> 4 << 4) - d2;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        GL11.glLineWidth(1.0F);
        t.startDrawing(GL11.GL_LINE_STRIP);
        for (int i = -16; i <= 32; i += 16) {
            for (int j = -16; j <= 32; j += 16) {
                fade(t, d5 + i, d3, d6 + j, d4, 1.0F, 0.0F, 0.0F, 0.5F);
            }
        }
        for (int k = 2; k < 16; k += 2) {
            fade(t, d5 + k, d3, d6, d4, 1.0F, 1.0F, 0.0F, 1.0F);
            fade(t, d5 + k, d3, d6 + 16.0D, d4, 1.0F, 1.0F, 0.0F, 1.0F);
        }
        for (int l = 2; l < 16; l += 2) {
            fade(t, d5, d3, d6 + l, d4, 1.0F, 1.0F, 0.0F, 1.0F);
            fade(t, d5 + 16.0D, d3, d6 + l, d4, 1.0F, 1.0F, 0.0F, 1.0F);
        }
        for (int m = 0; m <= 256; m += 2) {
            ring(t, d5, m - d1, d6, 1.0F, 1.0F, 0.0F);
        }
        t.draw();
        GL11.glLineWidth(2.0F);
        t.startDrawing(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= 16; i += 16) {
            for (int j = 0; j <= 16; j += 16) {
                fade(t, d5 + i, d3, d6 + j, d4, 0.25F, 0.25F, 1.0F, 1.0F);
            }
        }
        for (int m = 0; m <= 256; m += 16) {
            ring(t, d5, m - d1, d6, 0.25F, 0.25F, 1.0F);
        }
        t.draw();
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** A vertical line from y0 to y1, joined to the strip by invisible ends. */
    private static void fade(Tessellator t, double x, double y0, double z, double y1, float r, float g, float b, float a) {
        t.setColorRGBA_F(r, g, b, 0.0F);
        t.addVertex(x, y0, z);
        t.setColorRGBA_F(r, g, b, a);
        t.addVertex(x, y0, z);
        t.addVertex(x, y1, z);
        t.setColorRGBA_F(r, g, b, 0.0F);
        t.addVertex(x, y1, z);
    }

    /** The square around the chunk at one height. */
    private static void ring(Tessellator t, double x, double y, double z, float r, float g, float b) {
        t.setColorRGBA_F(r, g, b, 0.0F);
        t.addVertex(x, y, z);
        t.setColorRGBA_F(r, g, b, 1.0F);
        t.addVertex(x, y, z);
        t.addVertex(x, y, z + 16.0D);
        t.addVertex(x + 16.0D, y, z + 16.0D);
        t.addVertex(x + 16.0D, y, z);
        t.addVertex(x, y, z);
        t.setColorRGBA_F(r, g, b, 0.0F);
        t.addVertex(x, y, z);
    }

    // ---- F3 combinations (Minecraft.processKeyF3 of 1.12.2) ----

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OFF) {
            return;
        }
        int key = Keyboard.getEventKey();
        boolean down = Keyboard.getEventKeyState();
        if (key == Keyboard.KEY_F3) {
            if (down) {
                // 1.7.10 has just toggled the screen on the press (when no screen was open): undo it, 1.12.2 waits
                // for the release and toggles only if F3 was not used with another key (Minecraft.runTick)
                if (mc.currentScreen == null) {
                    mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
                    f3Pending = true;
                }
                f3Used = false;
            } else if (f3Pending) {
                f3Pending = false;
                if (!f3Used && mc.currentScreen == null) {
                    mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
                    mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
                    lagometer = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
                }
            }
            return;
        }
        if (!down || mc.currentScreen != null || !Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            return;
        }
        if (combo(key)) {
            f3Used = true;
        }
    }

    /** F3 went down with no screen open; the toggle waits for the release. */
    private static boolean f3Pending;
    /** GameSettings.showLagometer of 1.12.2 (Alt + F3). */
    public static boolean lagometer;

    // ---- Alt + F3 (FrameTimer, GuiOverlayDebug.renderLagometer) ----

    private static final long[] FRAMES = new long[240];
    private static int frameIndex;
    private static int frameCount;
    private static long lastFrame;

    @SubscribeEvent
    public void onRenderTick(cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent event) {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.START) {
            return;
        }
        long now = System.nanoTime();
        if (lastFrame != 0L) {
            FRAMES[frameIndex] = now - lastFrame;
            frameIndex = (frameIndex + 1) % FRAMES.length;
            frameCount = Math.min(frameCount + 1, FRAMES.length);
        }
        lastFrame = now;
    }

    /** FrameTimer.getLagometerValue: 30 pixels for a frame of 1/60 s. */
    private static int lag(long nanos) {
        return (int) ((double) nanos / 1.6666666E7D * 30.0D);
    }

    private static int blend(int c1, int c2, float f) {
        int out = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int a = c1 >> s & 255;
            int b = c2 >> s & 255;
            out |= MathHelper.clamp_int((int) ((float) a + (float) (b - a) * f), 0, 255) << s;
        }
        return out;
    }

    /** GuiOverlayDebug.getFrameColor: green to yellow to red. */
    private static int frameColor(int v) {
        return v < 30 ? blend(-16711936, -256, (float) v / 30.0F) : blend(-256, -65536, (float) (v - 30) / 30.0F);
    }

    private static void vLine(int x, int y1, int y2, int color) {
        Gui.drawRect(x, Math.min(y1, y2) + 1, x + 1, Math.max(y1, y2), color);
    }

    private static void hLine(int x1, int x2, int y, int color) {
        Gui.drawRect(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    /** GuiOverlayDebug.renderLagometer of 1.12.2. */
    static void drawLagometer(int h) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Gui.drawRect(0, h - 60, 240, h, -1873784752);
        int start = (frameIndex - frameCount + FRAMES.length) % FRAMES.length;
        for (int n = 0; n < frameCount; n++) {
            int v = lag(FRAMES[(start + n) % FRAMES.length]);
            vLine(n, h, h - v, frameColor(MathHelper.clamp_int(v, 0, 60)));
        }
        Gui.drawRect(1, h - 30 + 1, 14, h - 30 + 10, -1873784752);
        mc.fontRenderer.drawString("60", 2, h - 30 + 2, 14737632);
        hLine(0, 239, h - 30, -1);
        Gui.drawRect(1, h - 60 + 1, 14, h - 60 + 10, -1873784752);
        mc.fontRenderer.drawString("30", 2, h - 60 + 2, 14737632);
        hLine(0, 239, h - 60, -1);
        hLine(0, 239, h - 1, -1);
        vLine(0, h - 60, h, -1);
        vLine(239, h - 60, h, -1);
        if (mc.gameSettings.limitFramerate <= 120) {
            hLine(0, 239, h - 60 + mc.gameSettings.limitFramerate / 2, -16711681);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /** For the autotest: frames recorded. */
    public static int framesRecorded() {
        return frameCount;
    }

    /** One F3 combination after 1.7.10 did its own part of it; true when the key is one. */
    public static boolean combo(int key) {
        Minecraft mc = Minecraft.getMinecraft();
        switch (key) {
            case Keyboard.KEY_A:
                feedback("debug.reload_chunks.message");
                return true;
            case Keyboard.KEY_B:
                feedback(RenderManager.debugBoundingBox ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
                return true;
            case Keyboard.KEY_D:
                if (mc.ingameGUI != null) {
                    mc.ingameGUI.getChatGUI().clearChatMessages();
                }
                return true;
            case Keyboard.KEY_F:
                feedback("debug.cycle_renderdistance.message", mc.gameSettings.renderDistanceChunks);
                return true;
            case Keyboard.KEY_G:
                chunkBorders = !chunkBorders;
                feedback(chunkBorders ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
                return true;
            case Keyboard.KEY_H:
                feedback(mc.gameSettings.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
                return true;
            case Keyboard.KEY_N:
                gameModeSwitch(mc);
                return true;
            case Keyboard.KEY_P:
                feedback(mc.gameSettings.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
                return true;
            case Keyboard.KEY_Q:
                // Q also drops the held item in 1.7.10; with F3 it only shows the list
                while (mc.gameSettings.keyBindDrop.isPressed()) {
                    // consumed
                }
                feedback("debug.help.message");
                for (String k : new String[] {"reload_chunks", "show_hitboxes", "clear_chat", "cycle_renderdistance", "chunk_boundaries", "advanced_tooltips",
                    "creative_spectator", "pause_focus", "help", "reload_resourcepacks"}) {
                    mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentTranslation("debug." + k + ".help"));
                    PRINTED.add("debug." + k + ".help");
                }
                return true;
            case Keyboard.KEY_T:
                feedback("debug.reload_resourcepacks.message");
                return true;
            case Keyboard.KEY_S:
                return true;
            default:
                return false;
        }
    }

    /** F3 + N: creative to spectator and back, for a player who may use /gamemode. */
    private static void gameModeSwitch(Minecraft mc) {
        if (mc.thePlayer == null) {
            return;
        }
        boolean allowed = true;
        MinecraftServer server = MinecraftServer.getServer();
        if (mc.isIntegratedServerRunning() && server != null) {
            EntityPlayerMP p = server.getConfigurationManager().func_152612_a(mc.thePlayer.getCommandSenderName());
            allowed = p != null && p.canCommandSenderUseCommand(2, "gamemode");
        }
        if (!allowed) {
            feedback("debug.creative_spectator.error");
        } else if (Spectator112.is(mc.thePlayer)) {
            mc.thePlayer.sendChatMessage("/gamemode creative");
        } else if (mc.thePlayer.capabilities.isCreativeMode) {
            mc.thePlayer.sendChatMessage("/gamemode spectator");
        }
    }

    /** Minecraft.debugFeedbackTranslated of 1.12.2: "[Debug]:" in bold yellow, then the message. */
    public static void feedback(String key, Object... args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null) {
            return;
        }
        ChatComponentText line = new ChatComponentText("");
        line.appendSibling(new ChatComponentTranslation("debug.prefix").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW).setBold(true)));
        line.appendText(" ");
        line.appendSibling(new ChatComponentTranslation(key, args));
        mc.ingameGUI.getChatGUI().printChatMessage(line);
        PRINTED.add(key);
    }
}
