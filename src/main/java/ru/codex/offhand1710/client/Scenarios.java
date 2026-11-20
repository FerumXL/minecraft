package ru.codex.offhand1710.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.ChestGenHooks;
import ru.codex.offhand1710.Brew112;
import ru.codex.offhand1710.Banner112;
import ru.codex.offhand1710.Content119;
import ru.codex.offhand1710.Content120;
import ru.codex.offhand1710.Content122;
import ru.codex.offhand1710.Data112;
import ru.codex.offhand1710.Maps112;
import ru.codex.offhand1710.PortedBlocks;
import ru.codex.offhand1710.Trades112;

/** Client-driven test scenarios; active only with MC112_AUDIT = more, more2, scene or bench. */
public final class Scenarios {
    private static final String STAGE = System.getenv("MC112_AUDIT");
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ArrayDeque<Step> steps = new ArrayDeque<Step>();
    private FileWriter out;
    private boolean launched;
    private boolean started;
    private int wait;
    private int bx;
    private int bz;
    private double maxMotion;
    private double minMotion;
    private double lastY;
    private boolean flyingNow;
    private double maxItemY;
    private double itemStart;
    private EntityItem flying;
    private double levY;
    private boolean sample;
    private boolean sampleItem;
    private EntityPig target;
    private ItemStack boughtMap;

    private static final class Step {
        int delay;
        boolean server;
        Runnable body;
    }

    private Scenarios() {
    }

    static void start() {
        if ("more".equals(STAGE) || "more2".equals(STAGE) || "scene".equals(STAGE) || "bench".equals(STAGE) || "extra".equals(STAGE) || "gui".equals(STAGE) || "terrain".equals(STAGE) || "backlog1".equals(STAGE) || "villager".equals(STAGE) || "elytra".equals(STAGE) || "group2".equals(STAGE)) {
            FMLCommonHandler.instance().bus().register(new Scenarios());
        }
    }

    private void client(int delay, Runnable r) {
        add(delay, false, r);
    }

    private void server(int delay, Runnable r) {
        add(delay, true, r);
    }

    private void add(int delay, boolean server, Runnable r) {
        Step s = new Step();
        s.delay = delay;
        s.server = server;
        s.body = r;
        steps.add(s);
    }

    private synchronized void log(String line) {
        try {
            if (out == null) {
                out = new FileWriter(new File(mc.mcDataDir, "audit112-" + STAGE + ".txt"));
            }
            out.write(line + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void result(String name, boolean ok, String detail) {
        log((ok ? "PASS " : "FAIL ") + name + " " + detail);
    }

    private EntityPlayerMP p() {
        return (EntityPlayerMP) MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
    }

    private WorldServer w() {
        return MinecraftServer.getServer().worldServers[0];
    }

    private void cmd(String c) {
        MinecraftServer.getServer().getCommandManager().executeCommand(p(), c);
    }

    private void shot(String name) {
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, "s-" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void fly(boolean on) {
        p().capabilities.allowFlying = true;
        p().capabilities.isFlying = on;
        p().sendPlayerAbilities();
    }

    private void tp(double x, double y, double z, float yaw, float pitch) {
        p().playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
    }

    @SuppressWarnings("unchecked")
    private GuiButton button(int id) {
        List<GuiButton> list = ReflectionHelper.getPrivateValue(GuiScreen.class, mc.currentScreen, "buttonList", "field_146292_n");
        for (GuiButton b : list) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    private void click(GuiButton b) {
        try {
            Method m = ReflectionHelper.findMethod(GuiScreen.class, mc.currentScreen, new String[] {"mouseClicked", "func_73864_a"}, int.class, int.class, int.class);
            m.invoke(mc.currentScreen, Integer.valueOf(b.xPosition + 2), Integer.valueOf(b.yPosition + 2), Integer.valueOf(0));
        } catch (Exception e) {
            log("FAIL click " + e);
        }
    }

    private Brew112.Cloud cloudNear(double x, double y, double z, double r) {
        for (Object o : w().getEntitiesWithinAABB(Brew112.Cloud.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r))) {
            return (Brew112.Cloud) o;
        }
        return null;
    }

    @SubscribeEvent
    public void onClient(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched = true;
            mc.gameSettings.pauseOnLostFocus = false;
            String rd = System.getenv("MC112_BENCH_RD");
            mc.gameSettings.renderDistanceChunks = rd == null ? 8 : Integer.parseInt(rd);
            if ("terrain".equals(STAGE)) {
                // MC112_WORLD: save folder (loaded if it exists, else created with MC112_SEED, DEFAULT, structures on)
                String tw = System.getenv("MC112_WORLD") == null ? "audit112t" : System.getenv("MC112_WORLD");
                String seed = System.getenv("MC112_SEED");
                mc.launchIntegratedServer(tw, tw, new WorldSettings(seed == null ? 112L : Long.parseLong(seed), WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
                return;
            }
            if ("elytra".equals(STAGE)) {
                // a fresh default world; MC112_SEED (default 112), render distance MC112_BENCH_RD; MC112_WORLD: an existing save
                if (System.getenv("MC112_WORLD") != null) {
                    String tw = System.getenv("MC112_WORLD");
                    mc.launchIntegratedServer(tw, tw, null);
                    return;
                }
                deleteTree(new File(mc.mcDataDir, "saves/audit112e"));
                String seed = System.getenv("MC112_SEED");
                mc.launchIntegratedServer("audit112e", "audit112e", new WorldSettings(seed == null ? 112L : Long.parseLong(seed), WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
                return;
            }
            if ("backlog1".equals(STAGE) || "villager".equals(STAGE)) {
                // a fresh default world on the seed of the user's world, with its savanna, taiga and plains villages
                String name = "villager".equals(STAGE) ? "audit112v" : "audit112b";
                deleteTree(new File(mc.mcDataDir, "saves/" + name));
                mc.launchIntegratedServer(name, name,new WorldSettings(6968716290812306038L, WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
                return;
            }
            String world = "bench".equals(STAGE) ? "audit112" : "audit112m";
            WorldType type = "bench".equals(STAGE) ? WorldType.DEFAULT : WorldType.FLAT;
            mc.launchIntegratedServer(world, world, new WorldSettings(112L, WorldSettings.GameType.CREATIVE, !"bench".equals(STAGE) ? false : true, false, type).enableCommands());
            return;
        }
        if (mc.thePlayer == null || MinecraftServer.getServer() == null || MinecraftServer.getServer().getConfigurationManager().playerEntityList.isEmpty()) {
            return;
        }
        if (!started) {
            started = true;
            wait = 60;
            build();
        }
        if (sample) {
            maxMotion = Math.max(maxMotion, mc.thePlayer.motionY);
            minMotion = Math.min(minMotion, mc.thePlayer.motionY);
            lastY = mc.thePlayer.posY;
            flyingNow = mc.thePlayer.capabilities.isFlying;
        }
        steerFlight();
        run(false);
    }

    @SubscribeEvent
    public void onServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && started) {
            sampleBacklog();
            sampleSpeeds();
            sampleFlight();
            if (sampleItem && flying != null) {
                maxItemY = Math.max(maxItemY, flying.posY);
            }
            run(true);
        }
    }

    private synchronized void run(boolean serverThread) {
        Step s = steps.peek();
        if (s == null) {
            return;
        }
        if (!serverThread) {
            if (wait > 0) {
                wait--;
                return;
            }
        }
        if (wait > 0 || s.server != serverThread) {
            return;
        }
        steps.poll();
        try {
            s.body.run();
        } catch (Throwable t) {
            log("FAIL step exception " + t);
            t.printStackTrace();
        }
        Step next = steps.peek();
        wait = next == null ? 0 : next.delay;
    }

    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteTree(c);
            }
        }
        f.delete();
    }

    private void finish() {
        client(20, new Runnable() {
            @Override
            public void run() {
                log("DONE");
                mc.shutdown();
            }
        });
    }

    private void build() {
        if ("scene".equals(STAGE)) {
            scene();
        } else if ("terrain".equals(STAGE)) {
            terrain();
        } else if ("bench".equals(STAGE)) {
            bench();
        } else if ("extra".equals(STAGE)) {
            extra();
        } else if ("backlog1".equals(STAGE)) {
            backlog1();
        } else if ("villager".equals(STAGE)) {
            villager();
        } else if ("elytra".equals(STAGE)) {
            elytra();
        } else if ("group2".equals(STAGE)) {
            group2();
        } else if ("gui".equals(STAGE)) {
            gui();
        } else if ("more2".equals(STAGE)) {
            server(40, new Runnable() {
                @Override
                public void run() {
                    boolean found = false;
                    for (ItemStack s : p().inventory.mainInventory) {
                        found |= s != null && s.getItem() == Items.filled_map && s.hasTagCompound() && s.getTagCompound().hasKey("Marker112Type");
                    }
                    result("map survives reload", found, "");
                }
            });
            server(60, new Runnable() {
                @Override
                public void run() {
                    boolean marker = false;
                    for (ItemStack s : p().inventory.mainInventory) {
                        marker |= s != null && s.getItem() == Items.filled_map && s.hasTagCompound() && Maps112.hasMarker(w(), s);
                    }
                    result("map marker after reload", marker, "");
                }
            });
            finish();
        } else {
            more();
        }
    }

    // ---- benchmark ----

    /**
     * Stage terrain (diagnosis): MC112_GEN="cx0,cz0,cx1,cz1" generates and populates that chunk square on the server,
     * then saves; MC112_SHOTS="name,x,y,z,yaw,pitch;..." takes screenshots at those spots (time 6000, flying).
     */
    /**
     * "find:taiga" (or desert, savanna, plains) in MC112_GEN: the nearest village grid cell of that start biome (the
     * check of MapGenVillage.canSpawnStructureAtCoords, no chunk generated), as a 9x9 chunk square around it.
     */
    private String findVillage(String what) {
        int want = "desert".equals(what) ? 1 : "savanna".equals(what) ? 2 : "taiga".equals(what) ? 3 : 0;
        for (int r = 0; r < 40; r++) {
            for (int rx = -r; rx <= r; rx++) {
                for (int rz = -r; rz <= r; rz++) {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != r) {
                        continue;
                    }
                    java.util.Random random = w().setRandomSeed(rx, rz, 10387312);
                    int cx = rx * 32 + random.nextInt(24);
                    int cz = rz * 32 + random.nextInt(24);
                    if (!w().getWorldChunkManager().areBiomesViable(cx * 16 + 8, cz * 16 + 8, 0, net.minecraft.world.gen.structure.MapGenVillage.villageSpawnBiomes)) {
                        continue;
                    }
                    net.minecraft.world.biome.BiomeGenBase biome = w().getWorldChunkManager().getBiomeGenAt(cx * 16 + 2, cz * 16 + 2);
                    if (ru.codex.offhand1710.Village112.typeOf(biome) == want) {
                        log("TERRAIN found " + what + " village cell chunk " + cx + "," + cz + " biome=" + biome.biomeName);
                        return (cx - 4) + "," + (cz - 4) + "," + (cx + 4) + "," + (cz + 4);
                    }
                }
            }
        }
        log("TERRAIN no " + what + " village found");
        return "";
    }

    /** HBM structure starts (NTMWorldGenerator.nbtGen.structureMap) whose box touches the given block square. */
    private String hbmStarts(int bx0, int bz0, int bx1, int bz1) {
        try {
            java.lang.reflect.Field gens = cpw.mods.fml.common.registry.GameRegistry.class.getDeclaredField("worldGenerators");
            gens.setAccessible(true);
            StringBuilder out = new StringBuilder();
            int n = 0;
            for (Object g : (java.util.Set<?>) gens.get(null)) {
                if (!g.getClass().getName().equals("com.hbm.world.gen.NTMWorldGenerator")) {
                    continue;
                }
                java.lang.reflect.Field f = g.getClass().getDeclaredField("nbtGen");
                f.setAccessible(true);
                Object gen = f.get(g);
                java.lang.reflect.Field map = null;
                for (String name : new String[] {"structureMap", "field_75053_d"}) {
                    try {
                        map = net.minecraft.world.gen.structure.MapGenStructure.class.getDeclaredField(name);
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                map.setAccessible(true);
                for (Object s : ((java.util.Map<?, ?>) map.get(gen)).values()) {
                    net.minecraft.world.gen.structure.StructureBoundingBox b = ((net.minecraft.world.gen.structure.StructureStart) s).getBoundingBox();
                    if (b.maxX >= bx0 && b.minX <= bx1 && b.maxZ >= bz0 && b.minZ <= bz1) {
                        n++;
                        out.append(' ').append(b.minX).append(',').append(b.minY).append(',').append(b.minZ);
                    }
                }
            }
            return n + out.toString();
        } catch (Throwable t) {
            return "error " + t;
        }
    }

    private void terrain() {
        String genEnv = System.getenv("MC112_GEN");
        if (genEnv != null && genEnv.startsWith("find:")) {
            StringBuilder squares = new StringBuilder();
            for (String what : genEnv.substring(5).split("\\+")) {
                squares.append(findVillage(what.trim())).append(';');
            }
            genEnv = squares.toString();
        }
        final String gen = genEnv;
        final String shots = System.getenv("MC112_SHOTS");
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                cmd("weather clear");
                fly(true);
                log("TERRAIN seed=" + w().getSeed() + " spawn=" + w().getSpawnPoint().posX + "," + w().getSpawnPoint().posZ + " gen=" + gen);
            }
        });
        // several squares may be given, separated by ';' (stage village uses one per village)
        for (String square : gen == null ? new String[0] : gen.split(";")) {
            if (square.trim().isEmpty()) {
                continue;
            }
            String[] g = square.split(",");
            final int x0 = Integer.parseInt(g[0].trim()), z0 = Integer.parseInt(g[1].trim()), x1 = Integer.parseInt(g[2].trim()), z1 = Integer.parseInt(g[3].trim());
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                final int cx = x;
                server(1, new Runnable() {
                    @Override
                    public void run() {
                        for (int cz = z0 - 1; cz <= z1 + 1; cz++) {
                            w().theChunkProviderServer.loadChunk(cx, cz);
                            // rows two behind are populated already; queue them for unload (saved on unload)
                            w().theChunkProviderServer.unloadChunksIfNotNearSpawn(cx - 3, cz);
                        }
                        if ((cx & 15) == 0) {
                            log("TERRAIN row " + cx + " loaded=" + w().theChunkProviderServer.getLoadedChunkCount());
                        }
                    }
                });
            }
            server(5, new Runnable() {
                @Override
                public void run() {
                    try {
                        w().saveAllChunks(true, null);
                    } catch (Exception e) {
                        log("FAIL save " + e);
                    }
                    log("TERRAIN generated");
                    int hbm = 0, logs = 0, leaves = 0;
                    java.util.Map<String, Integer> names = new java.util.TreeMap<String, Integer>();
                    for (int cx = x0; cx <= x1; cx++) {
                        for (int cz = z0; cz <= z1; cz++) {
                            net.minecraft.world.chunk.Chunk c = w().getChunkFromChunkCoords(cx, cz);
                            for (int bx = 0; bx < 16; bx++) {
                                for (int bz = 0; bz < 16; bz++) {
                                    for (int by = 1; by < 256; by++) {
                                        net.minecraft.block.Block b = c.getBlock(bx, by, bz);
                                        if (b == net.minecraft.init.Blocks.air) {
                                            continue;
                                        }
                                        if (b instanceof net.minecraft.block.BlockLog) {
                                            logs++;
                                        } else if (b instanceof net.minecraft.block.BlockLeavesBase) {
                                            leaves++;
                                        } else {
                                            String n = net.minecraft.block.Block.blockRegistry.getNameForObject(b);
                                            if (n != null && n.startsWith("hbm:") && by >= 60) {
                                                hbm++;
                                                Integer k = names.get(n);
                                                names.put(n, k == null ? 1 : k + 1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    log("TERRAIN hbmStarts " + x0 + "," + z0 + " " + hbmStarts(x0 * 16, z0 * 16, x1 * 16 + 15, z1 * 16 + 15));
                    log("TERRAIN count " + x0 + "," + z0 + " hbmSurface=" + hbm + " logs=" + logs + " leaves=" + leaves + " " + names);
                }
            });
        }
        if (shots != null && !shots.isEmpty()) {
            for (String sp : shots.split(";")) {
                final String[] p = sp.split(",");
                if (p.length < 6) {
                    continue;
                }
                server(20, new Runnable() {
                    @Override
                    public void run() {
                        fly(true);
                        tp(Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
                    }
                });
                client(400, new Runnable() {
                    @Override
                    public void run() {
                        shot("terrain-" + p[0]);
                        log("TERRAIN shot " + p[0] + " at " + mc.thePlayer.posX + "," + mc.thePlayer.posY + "," + mc.thePlayer.posZ + " fps=" + mc.debug);
                    }
                });
            }
        }
        server(20, new Runnable() {
            @Override
            public void run() {
                try {
                    w().saveAllChunks(true, null);
                } catch (Exception e) {
                    log("FAIL save " + e);
                }
            }
        });
        finish();
    }

    private void bench() {
        final long[] frames = new long[2];
        final long[] start = new long[1];
        server(100, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                fly(true);
            }
        });
        final long[] gc = new long[2];
        client(200, new Runnable() {
            @Override
            public void run() {
                log("BENCHENV java=" + System.getProperty("java.version") + " arch=" + System.getProperty("sun.arch.data.model") + "bit vm=" + System.getProperty("java.vm.name")
                        + " args=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
                        + " maxMemMB=" + Runtime.getRuntime().maxMemory() / 1048576L + " cpus=" + Runtime.getRuntime().availableProcessors());
                log("BENCHGL renderer=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER) + " vendor=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR)
                        + " version=" + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
                log("BENCHOPT renderDistance=" + mc.gameSettings.renderDistanceChunks + " fancy=" + mc.gameSettings.fancyGraphics + " vsync=" + mc.gameSettings.enableVsync
                        + " limitFramerate=" + mc.gameSettings.limitFramerate + " ao=" + mc.gameSettings.ambientOcclusion + " mipmap=" + mc.gameSettings.mipmapLevels
                        + " display=" + mc.displayWidth + "x" + mc.displayHeight + " active=" + org.lwjgl.opengl.Display.isActive()
                        + " optifine=" + (net.minecraft.launchwrapper.Launch.classLoader != null && classExists("Config")) + " mods=" + cpw.mods.fml.common.Loader.instance().getActiveModList().size());
                start[0] = System.currentTimeMillis();
                frames[0] = System.nanoTime();
                gc[0] = gcMillis();
                count = 0;
                perSecond.setLength(0);
                secondStart = System.currentTimeMillis();
                secondCount = 0;
                counting = true;
            }
        });
        client(1200, new Runnable() {
            @Override
            public void run() {
                counting = false;
                gc[1] = gcMillis();
                long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
                log("BENCHGC gcMillis=" + (gc[1] - gc[0]) + " usedMB=" + used + " entities=" + mc.theWorld.loadedEntityList.size() + " tiles=" + mc.theWorld.loadedTileEntityList.size());
                log("BENCHSEC " + perSecond);
                log("BENCHDEBUG " + mc.debug);
                double secs = (System.currentTimeMillis() - start[0]) / 1000.0D;
                long[] ticks = MinecraftServer.getServer().tickTimeArray;
                long sum = 0;
                for (long t : ticks) {
                    sum += t;
                }
                log("BENCH fps=" + (long) (count / secs) + " mspt=" + (sum / (double) ticks.length / 1000000.0D) + " seconds=" + secs + " loadedChunks=" + mc.theWorld.getChunkProvider().getLoadedChunkCount());
            }
        });
        finish();
    }

    private int count;
    private boolean counting;
    private final StringBuilder perSecond = new StringBuilder();
    private long secondStart;
    private int secondCount;

    private static long gcMillis() {
        long sum = 0;
        for (java.lang.management.GarbageCollectorMXBean b : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            sum += Math.max(0L, b.getCollectionTime());
        }
        return sum;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, net.minecraft.launchwrapper.Launch.classLoader);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            frames++;
            if (fakeOn) {
                poke(leftFrames > 0, rightFrames > 0);
            }
        } else if (event.phase == TickEvent.Phase.END) {
            if (measuring && GL11.glGetError() != 0) {
                glErrors++;
            }
            if (fakeOn && (leftFrames > 0 || rightFrames > 0)) {
                poke(false, false);
                leftFrames = Math.max(0, leftFrames - 1);
                rightFrames = Math.max(0, rightFrames - 1);
            }
        }
        if (event.phase == TickEvent.Phase.END && counting) {
            count++;
            secondCount++;
            long now = System.currentTimeMillis();
            if (now - secondStart >= 1000L) {
                perSecond.append(secondCount).append(' ');
                secondCount = 0;
                secondStart = now;
            }
        }
    }

    // ---- scene screenshots ----

    private ItemStack patternBanner() {
        ItemStack s = new ItemStack(Banner112.banner, 1, 15);
        NBTTagCompound e = new NBTTagCompound();
        e.setString("Pattern", "ts");
        e.setInteger("Color", 1);
        NBTTagList l = new NBTTagList();
        l.appendTag(e);
        NBTTagCompound b = new NBTTagCompound();
        b.setTag("Patterns", l);
        NBTTagCompound t = new NBTTagCompound();
        t.setTag("BlockEntityTag", b);
        s.setTagCompound(t);
        return s;
    }

    private void hold(ItemStack s) {
        p().inventory.mainInventory[p().inventory.currentItem] = s;
        p().inventoryContainer.detectAndSendChanges();
    }

    private void view(int mode) {
        mc.gameSettings.thirdPersonView = mode;
    }

    private void scene() {
        server(40, new Runnable() {
            @Override
            public void run() {
                bx = 0;
                bz = 0;
                cmd("gamerule doDaylightCycle false");
                cmd("time set 6000");
                cmd("difficulty 0");
                tp(0.5D, 100.0D, 0.5D, 0.0F, 0.0F);
                fly(true);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                cmd("fill -6 99 -6 6 99 6 minecraft:stone");
                clearMobs();
                hold(patternBanner());
                // 1.12.2 stops creative flight on landing, 1.7.10 does not; flight widens the FOV by 10 %,
                // so the reference shots are taken standing.
                fly(false);
            }
        });
        client(40, shoot("banner_fp"));
        view3(40, 2, "banner_3p");
        server(10, holdRun("shield"));
        view3(40, 2, "shield_3p");
        client(10, viewRun(0));
        client(30, shoot("shield_fp"));
        server(10, holdRun("banner"));
        client(10, new Runnable() {
            @Override
            public void run() {
                Client122.leftHand = true;
                mc.thePlayer.sendChatMessage("/say left");
                ru.codex.offhand1710.MC112Bridge.NETWORK.channel.sendToServer(new Content122.HandMessage(true));
            }
        });
        client(40, shoot("left_fp"));
        view3(40, 2, "left_3p");
        client(10, new Runnable() {
            @Override
            public void run() {
                Client122.leftHand = false;
                ru.codex.offhand1710.MC112Bridge.NETWORK.channel.sendToServer(new Content122.HandMessage(false));
                view(0);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                hold(null);
                // 1.7.10 items hang 0.125 below posY (yOffset), 1.12.2 items do not: 100.1 + 0.125 puts the box where /summon puts it.
                // At 100.1 the box cut into the stone and the item was pushed out sideways by about two blocks.
                EntityItem it = new EntityItem(w(), 0.5D, 100.225D, 3.5D, patternBanner());
                it.delayBeforeCanPickup = 32767;
                // /summon in 1.12.2 has no Motion tag, so the item starts still; the 1.7.10 constructor throws it randomly.
                it.motionX = 0.0D;
                it.motionY = 0.0D;
                it.motionZ = 0.0D;
                w().spawnEntityInWorld(it);
                tp(0.5D, 100.0D, 0.5D, 0.0F, 15.0F);
            }
        });
        client(60, new Runnable() {
            @Override
            public void run() {
                for (Object o : mc.theWorld.loadedEntityList) {
                    if (o instanceof EntityItem) {
                        EntityItem e = (EntityItem) o;
                        log("GROUNDITEM " + e.getEntityItem() + " at " + e.posX + "," + e.posY + "," + e.posZ + " motion=" + e.motionX + "," + e.motionY + "," + e.motionZ
                                + " age=" + e.age + " hover=" + e.hoverStart + " player=" + mc.thePlayer.posX + "," + mc.thePlayer.posY + "," + mc.thePlayer.posZ + " yaw=" + mc.thePlayer.rotationYaw
                                + " pitch=" + mc.thePlayer.rotationPitch + " flying=" + mc.thePlayer.capabilities.isFlying + " fov=" + mc.gameSettings.fovSetting + " fovMul=" + mc.thePlayer.getFOVMultiplier());
                    }
                }
                shot("banner_ground");
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                EntityPig pig = new EntityPig(w());
                pig.setLocationAndAngles(0.5D, 100.0D, 4.5D, 0.0F, 0.0F);
                pig.func_110163_bv();
                w().spawnEntityInWorld(pig);
                pig.addPotionEffect(new PotionEffect(Content120.glowing.id, 2000, 0));
            }
        });
        client(60, shoot("glow_pig"));
        server(10, new Runnable() {
            @Override
            public void run() {
                cmd("fill -6 100 3 6 103 3 minecraft:stone");
            }
        });
        client(40, shoot("glow_wall"));
        server(10, new Runnable() {
            @Override
            public void run() {
                cmd("fill -6 100 3 6 103 3 minecraft:air");
                clearMobs();
                placeRow();
                tp(4.5D, 100.5D, -2.5D, 0.0F, 18.0F);
            }
        });
        client(60, shoot("blocks_close"));
        server(10, new Runnable() {
            @Override
            public void run() {
                tp(0.5D, 100.0D, 0.5D, 0.0F, 60.0F);
                // Flat world has no structures: the same marker tag on a fresh map, centre of the map as in the 1.12.2 scene.
                int id = w().getUniqueDataId("map");
                ItemStack map = new ItemStack(Items.filled_map, 1, id);
                net.minecraft.world.storage.MapData data = new net.minecraft.world.storage.MapData("map_" + id);
                w().setItemData(data.mapName, data);
                data.scale = 2;
                data.dimension = 0;
                data.markDirty();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Marker112Type", 9);
                tag.setInteger("Marker112X", 0);
                tag.setInteger("Marker112Z", 0);
                map.setTagCompound(tag);
                hold(map);
            }
        });
        client(60, shoot("map"));
        client(10, new Runnable() {
            @Override
            public void run() {
                cmd("advancement grant " + p().getCommandSenderName() + " everything");
                DataClient.openAdvancements();
            }
        });
        client(40, shoot("advancements"));
        client(5, new Runnable() {
            @Override
            public void run() {
                log("MAPCOORDS done");
            }
        });
        client(10, closeGui());
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                cmd("recipe give " + p().getCommandSenderName() + " *");
                java.util.Arrays.fill(p().inventory.mainInventory, null);
                p().inventory.mainInventory[12] = new ItemStack(Blocks.planks, 8, 0);
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        client(30, new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            }
        });
        client(10, clickId(9300));
        client(30, shoot("book"));
        client(5, closeGui());
        server(5, new Runnable() {
            @Override
            public void run() {
                // lingering potions of slowness, healing, poison, swiftness and a plain splash for comparison, hotbar 0..5
                int[] dmg = {16394, 16389, 16388, 16386, 16384};
                java.util.Arrays.fill(p().inventory.mainInventory, null);
                for (int i = 0; i < dmg.length; i++) {
                    p().inventory.mainInventory[i] = new ItemStack(Brew112.lingering, 1, dmg[i]);
                }
                p().inventory.mainInventory[5] = new ItemStack(Items.potionitem, 1, 16394);
                p().inventory.currentItem = 0;
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            }
        });
        client(20, shoot("lingering"));
        finish();
    }

    /** /kill has no selectors in 1.7.10 and would kill the player. */
    private void clearMobs() {
        for (Object o : w().loadedEntityList) {
            if (!(o instanceof net.minecraft.entity.player.EntityPlayer)) {
                ((net.minecraft.entity.Entity) o).setDead();
            }
        }
    }

    private void placeRow() {
        Object[] row = {Content119.observer, Content119.slime, Content120.wetSponge, PortedBlocks.seaLantern, ru.codex.offhand1710.Items112.BOXES[10], Content119.barrier};
        for (int i = 0; i < row.length; i++) {
            w().setBlock(i * 2, 100, 3, (net.minecraft.block.Block) row[i], i == 0 ? 0 : 1, 2);
        }

    }

    private Runnable openCmd() {
        return new Runnable() {
            @Override
            public void run() {
                TileEntity tile = mc.theWorld.getTileEntity(bx + 40, 4, bz);
                mc.displayGuiScreen(new GuiCommandBlock(((TileEntityCommandBlock) tile).func_145993_a()));
            }
        };
    }

    private Runnable clickId(final int id) {
        return new Runnable() {
            @Override
            public void run() {
                click(button(id));
            }
        };
    }

    private Runnable closeGui() {
        return new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(null);
            }
        };
    }

    private Runnable shoot(final String name) {
        return new Runnable() {
            @Override
            public void run() {
                shot(name);
            }
        };
    }

    private Runnable viewRun(final int mode) {
        return new Runnable() {
            @Override
            public void run() {
                view(mode);
            }
        };
    }

    private void view3(int delay, int mode, String name) {
        client(10, viewRun(mode));
        client(delay, shoot(name));
    }

    private Runnable holdRun(final String what) {
        return new Runnable() {
            @Override
            public void run() {
                if ("banner".equals(what)) {
                    hold(patternBanner());
                } else {
                    ItemStack shield = new ItemStack(PortedBlocks.shield);
                    NBTTagCompound inner = (NBTTagCompound) patternBanner().getTagCompound().getCompoundTag("BlockEntityTag").copy();
                    inner.setInteger("Base", 15);
                    NBTTagCompound t = new NBTTagCompound();
                    t.setTag("BlockEntityTag", inner);
                    shield.setTagCompound(t);
                    hold(shield);
                }
            }
        };
    }

    // ---- backlog1: biome doors of villages, advancement triggers, bounce of every entity ----

    /** An entity dropped onto a block: lands, then the highest feet position after the landing. */
    private static final class Drop {
        String name;
        net.minecraft.entity.Entity e;
        double top;
        float health;
        boolean landed;
        double landY;
        String below;
        double maxAfter = -1.0D;
        int afterTicks;
    }

    private final List<Drop> drops = new java.util.ArrayList<Drop>();
    private int[] doorWatch;
    private boolean doorOpened;
    private double villagerMaxX;
    private EntityVillager doorVillager;

    private void sampleBacklog() {
        for (Drop d : drops) {
            if (!d.landed) {
                d.landed = d.e.onGround;
                d.landY = d.e.posY;
            } else if (d.afterTicks < 60) {
                d.afterTicks++;
                d.maxAfter = Math.max(d.maxAfter, d.e.posY);
            }
        }
        if (doorWatch != null) {
            int meta = w().getBlockMetadata(doorWatch[0], doorWatch[1], doorWatch[2]);
            doorOpened |= (meta & 4) != 0;
            if (doorVillager != null) {
                villagerMaxX = Math.max(villagerMaxX, doorVillager.posX);
            }
        }
    }

    private static final String[] VILLAGE_KINDS = {"savanna", "taiga", "plains"};

    /** Lower door halves in the square around a village centre: oak, spruce, acacia, other; and every half found. */
    private int[] countDoors(int cx, int cz, List<int[]> halves) {
        int[] n = new int[4];
        for (int x = cx - 80; x <= cx + 80; x++) {
            for (int z = cz - 80; z <= cz + 80; z++) {
                for (int y = 40; y < 150; y++) {
                    Block b = w().getBlock(x, y, z);
                    if (!(b instanceof net.minecraft.block.BlockDoor)) {
                        continue;
                    }
                    int meta = w().getBlockMetadata(x, y, z);
                    if (halves != null) {
                        halves.add(new int[] {x, y, z, Block.getIdFromBlock(b), meta});
                    }
                    if ((meta & 8) != 0) {
                        continue;
                    }
                    n[b == Blocks.wooden_door ? 0 : b == PortedBlocks.DOORS[0] ? 1 : b == PortedBlocks.DOORS[3] ? 2 : 3]++;
                }
            }
        }
        return n;
    }

    /** Doors a fresh VillageCollection registers from the positions of these villagers (VillageCollection.tick). */
    private int registeredDoors(List<int[]> villagers) {
        net.minecraft.village.VillageCollection vc = new net.minecraft.village.VillageCollection(w());
        for (int[] v : villagers) {
            vc.addVillagerPosition(v[0], v[1], v[2]);
        }
        for (int i = 0; i < villagers.size() + 3; i++) {
            vc.tick();
        }
        int doors = 0;
        for (Object o : vc.getVillageList()) {
            doors += ((net.minecraft.village.Village) o).getNumVillageDoors();
        }
        return doors;
    }

    private Drop drop(String name, net.minecraft.entity.Entity e, double x, double y, double z, double top) {
        e.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        w().spawnEntityInWorld(e);
        Drop d = new Drop();
        d.name = name;
        d.e = e;
        d.top = top;
        d.below = w().getBlock(net.minecraft.util.MathHelper.floor_double(x), net.minecraft.util.MathHelper.floor_double(top - 0.2D), net.minecraft.util.MathHelper.floor_double(z)).getUnlocalizedName();
        d.health = e instanceof net.minecraft.entity.EntityLivingBase ? ((net.minecraft.entity.EntityLivingBase) e).getHealth() : 0.0F;
        drops.add(d);
        return d;
    }

    private void hand(ItemStack s) {
        p().inventory.setInventorySlotContents(p().inventory.currentItem, s);
    }

    private void done(String name, String id) {
        result(name, Data112.isDone(p(), id), id);
    }

    /** Teleporter that keeps the coordinates (the scaled ones of the dimension change) and builds no portal. */
    private static final class Keep extends net.minecraft.world.Teleporter {
        Keep(WorldServer world) {
            super(world);
        }

        @Override
        public void placeInPortal(net.minecraft.entity.Entity e, double x, double y, double z, float yaw) {
            e.setLocationAndAngles(e.posX, 120.0D, e.posZ, e.rotationYaw, 0.0F);
        }

        @Override
        public boolean placeInExistingPortal(net.minecraft.entity.Entity e, double x, double y, double z, float yaw) {
            return true;
        }

        @Override
        public boolean makePortal(net.minecraft.entity.Entity e) {
            return true;
        }

        @Override
        public void removeStalePortalLocations(long time) {
        }
    }

    // ---- villager: walking speeds against the travel formula of 1.12.2, villagers of a savanna village by day ----

    /** Horizontal blocks per tick of an entity, sampled on the server each tick. */
    private static final class Speed {
        final String name;
        final net.minecraft.entity.Entity e;
        boolean has;
        double lx;
        double lz;
        final List<Double> d = new java.util.ArrayList<Double>();

        Speed(String name, net.minecraft.entity.Entity e) {
            this.name = name;
            this.e = e;
        }

        /** The 90th percentile of the ticks it moved (more than 0.02), its maximum and the count of those ticks. */
        double[] stats() {
            List<Double> moving = new java.util.ArrayList<Double>();
            for (double v : d) {
                if (v > 0.02D) {
                    moving.add(v);
                }
            }
            java.util.Collections.sort(moving);
            if (moving.isEmpty()) {
                return new double[] {0.0D, 0.0D, 0.0D};
            }
            return new double[] {moving.get((int) (moving.size() * 0.9D)), moving.get(moving.size() - 1), moving.size()};
        }
    }

    private final List<Speed> speeds = new java.util.ArrayList<Speed>();
    private final java.util.Map<net.minecraft.entity.EntityLiving, double[]> lanes = new java.util.LinkedHashMap<net.minecraft.entity.EntityLiving, double[]>();

    private void sampleSpeeds() {
        for (Speed s : speeds) {
            if (s.has) {
                s.d.add(Math.sqrt((s.e.posX - s.lx) * (s.e.posX - s.lx) + (s.e.posZ - s.lz) * (s.e.posZ - s.lz)));
            }
            s.has = true;
            s.lx = s.e.posX;
            s.lz = s.e.posZ;
        }
        for (java.util.Map.Entry<net.minecraft.entity.EntityLiving, double[]> en : lanes.entrySet()) {
            double[] v = en.getValue();
            if (en.getKey().getNavigator().noPath()) {
                v[4] = 1.0D - v[4];
                en.getKey().getNavigator().tryMoveToXYZ(v[0], v[1], v[4] == 0.0D ? v[2] : v[3], v[5]);
            }
        }
    }

    /**
     * Steady walking speed on a block of slipperiness 0.6 (EntityLivingBase.travel of 1.12.2, the same numbers as
     * moveEntityWithHeading of 1.7.10): the move helper sets moveForward = aiMoveSpeed = speed * movementSpeed, it is
     * scaled by 0.98, and moveRelative adds forward * aiMoveSpeed while the motion keeps 0.6 * 0.91 of itself.
     */
    private static double walkSpeed(double aiSpeed) {
        float m = (float) aiSpeed;
        return (double) (m * 0.98F * m) / (1.0D - (double) (0.6F * 0.91F));
    }

    private void speedCheck(String what, Speed s, double expected) {
        double[] st = s.stats();
        result(what, Math.abs(st[0] - expected) < 0.006D && st[1] < expected + 0.006D, String.format("p90=%.4f max=%.4f expected=%.4f movingTicks=%d", st[0], st[1], expected, (int) st[2]));
    }

    private net.minecraft.entity.EntityLiving lane(net.minecraft.entity.EntityLiving e, String name, double x, double y, double z, double speed) {
        e.setLocationAndAngles(x, y, z - 6.0D, 0.0F, 0.0F);
        if (e instanceof net.minecraft.entity.monster.EntityZombie) {
            e.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
        }
        w().spawnEntityInWorld(e);
        if (speed > 0.0D) {
            net.minecraft.entity.ai.EntityAITasks t = ReflectionHelper.getPrivateValue(net.minecraft.entity.EntityLiving.class, e, "tasks", "field_70714_bg");
            net.minecraft.entity.ai.EntityAITasks tt = ReflectionHelper.getPrivateValue(net.minecraft.entity.EntityLiving.class, e, "targetTasks", "field_70715_bh");
            t.taskEntries.clear();
            tt.taskEntries.clear();
            lanes.put(e, new double[] {x, y, z + 8.0D, z - 6.0D, 0.0D, speed});
        }
        speeds.add(new Speed(name, e));
        return e;
    }

    private Speed speed(String name) {
        for (Speed s : speeds) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }

    private void endSpeeds() {
        for (Speed s : speeds) {
            if (!(s.e instanceof EntityPlayerMP) && !(s.e instanceof EntityVillager && s.name.startsWith("village"))) {
                s.e.setDead();
            }
        }
        speeds.clear();
        lanes.clear();
    }

    // ---- stage group2: @e and /entitydata, shoulder parrot, llama carpet, illusioner copies, recipe search ----

    private ru.codex.offhand1710.MoreMobs.Parrot g2parrot;
    private ru.codex.offhand1710.MoreMobs.Llama g2llama;
    private ru.codex.offhand1710.Illagers.Illusioner g2ill;

    private void group2() {
        final int dy = 200;
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("difficulty 2");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                bx = w().getSpawnPoint().posX;
                bz = w().getSpawnPoint().posZ;
                for (int x = bx - 12; x <= bx + 12; x++) {
                    for (int z = bz - 12; z <= bz + 12; z++) {
                        w().setBlock(x, dy - 1, z, Blocks.stone, 0, 2);
                        for (int y = dy; y < dy + 4; y++) {
                            w().setBlockToAir(x, y, z);
                        }
                    }
                }
                fly(false);
                tp(bx + 0.5D, dy, bz + 0.5D, 0.0F, 20.0F);
                g2llama = new ru.codex.offhand1710.MoreMobs.Llama(w());
                g2llama.setLocationAndAngles(bx + 3.5D, dy, bz + 3.5D, 0.0F, 0.0F);
                w().spawnEntityInWorld(g2llama);
                g2llama.setCustomNameTag("G2llama");
                g2parrot = new ru.codex.offhand1710.MoreMobs.Parrot(w());
                g2parrot.setLocationAndAngles(bx + 0.5D, dy, bz + 1.5D, 0.0F, 0.0F);
                g2parrot.setTamed(true);
                g2parrot.func_152115_b(p().getUniqueID().toString());
                w().spawnEntityInWorld(g2parrot);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                java.util.List<net.minecraft.entity.Entity> e = ru.codex.offhand1710.Content120.selectEntities(p(), "@e[type=mc112bridge.Llama,r=10]");
                java.util.List<net.minecraft.entity.Entity> named = ru.codex.offhand1710.Content120.selectEntities(p(), "@e[name=G2llama]");
                java.util.List<net.minecraft.entity.Entity> one = ru.codex.offhand1710.Content120.selectEntities(p(), "@e[c=1]");
                result("@e type/r finds the llama", e.size() == 1 && e.get(0) == g2llama, "n=" + e.size());
                result("@e name finds the llama", named.size() == 1 && named.get(0) == g2llama, "n=" + named.size());
                result("@e c=1 nearest", one.size() == 1, "n=" + one.size() + " " + (one.isEmpty() ? "" : one.get(0).getClass().getSimpleName()));
                cmd("entitydata @e[name=G2llama] {Tame:1b,Temper:7}");
                NBTTagCompound tag = new NBTTagCompound();
                g2llama.writeToNBT(tag);
                result("/entitydata merges Tame and Temper", g2llama.isTame() && tag.getInteger("Temper") == 7, "tame=" + g2llama.isTame() + " temper=" + tag.getInteger("Temper"));
                p().inventory.mainInventory[p().inventory.currentItem] = new ItemStack(Blocks.carpet, 3, 14);
                g2llama.interact(p());
                result("tame llama takes a red carpet", g2llama.decor() == 14, "decor=" + g2llama.decor());
                tag = new NBTTagCompound();
                g2llama.writeToNBT(tag);
                result("carpet saved as DecorItem", tag.hasKey("DecorItem") && tag.getCompoundTag("DecorItem").getShort("Damage") == 14, String.valueOf(tag.getCompoundTag("DecorItem")));
                p().inventory.mainInventory[p().inventory.currentItem] = null;
            }
        });
        client(40, new Runnable() {
            @Override
            public void run() {
                net.minecraft.entity.Entity c = mc.theWorld.getEntityByID(g2llama.getEntityId());
                result("client sees the carpet", c instanceof ru.codex.offhand1710.MoreMobs.Llama && ((ru.codex.offhand1710.MoreMobs.Llama) c).decor() == 14, "");
                mc.thePlayer.rotationYaw = 135.0F;
                mc.thePlayer.rotationPitch = 25.0F;
                mc.gameSettings.thirdPersonView = 0;
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                shot("g2-llama");
            }
        });
        // the parrot needs 100 ticks of life, then walks into the owner
        server(140, new Runnable() {
            @Override
            public void run() {
                NBTTagCompound d = p().getEntityData();
                result("tamed parrot lands on the shoulder", d.hasKey(ru.codex.offhand1710.Shoulder112.LEFT) && g2parrot.isDead, "left=" + d.hasKey(ru.codex.offhand1710.Shoulder112.LEFT) + " dead=" + g2parrot.isDead
                    + " dist=" + Math.sqrt(g2parrot.getDistanceSqToEntity(p())) + " age=" + g2parrot.ticksExisted + " owner=" + (g2parrot.getOwner() != null)
                    + " playerGround=" + p().onGround + " flying=" + p().capabilities.isFlying + " sitting=" + g2parrot.isSitting());
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                int[] s = MoreMobsClient.SHOULDERS.get(mc.thePlayer.getEntityId());
                result("client knows the shoulder parrot", s != null && s[0] >= 0, s == null ? "none" : s[0] + "," + s[1]);
                mc.gameSettings.thirdPersonView = 2;
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                shot("g2-shoulder");
                mc.gameSettings.thirdPersonView = 0;
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                p().attackEntityFrom(net.minecraft.util.DamageSource.generic, 0.5F);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                boolean off = !p().getEntityData().hasKey(ru.codex.offhand1710.Shoulder112.LEFT);
                java.util.List<?> parrots = w().getEntitiesWithinAABB(ru.codex.offhand1710.MoreMobs.Parrot.class, p().boundingBox.expand(4, 4, 4));
                result("hurt player drops the parrot", off && !parrots.isEmpty(), "off=" + off + " parrots=" + parrots.size());
                g2ill = new ru.codex.offhand1710.Illagers.Illusioner(w());
                g2ill.setLocationAndAngles(bx + 0.5D, dy, bz + 6.5D, 180.0F, 0.0F);
                w().spawnEntityInWorld(g2ill);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                result("illusioner casts the mirror spell", g2ill.isPotionActive(net.minecraft.potion.Potion.invisibility), "");
                g2ill.attackEntityFrom(net.minecraft.util.DamageSource.generic, 1.0F);
            }
        });
        client(15, new Runnable() {
            @Override
            public void run() {
                net.minecraft.entity.Entity c = mc.theWorld.getEntityByID(g2ill.getEntityId());
                boolean moved = false;
                if (c instanceof ru.codex.offhand1710.Illagers.Illusioner) {
                    for (double[] o : ((ru.codex.offhand1710.Illagers.Illusioner) c).offsets) {
                        moved |= o[0] != 0.0D || o[2] != 0.0D;
                    }
                }
                result("illusioner copies moved after a hit", moved, "invisible=" + (c != null && c.isInvisible()));
                mc.thePlayer.rotationYaw = 0.0F;
                mc.thePlayer.rotationPitch = 15.0F;
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("g2-illusioner");
            }
        });
        // item 8: own spawn entries for donkeys and zombie villagers
        server(5, new Runnable() {
            @Override
            public void run() {
                g2ill.setDead();
                net.minecraft.entity.passive.EntityHorse horse = new ru.codex.offhand1710.Variants112.Horse(w());
                horse.onSpawnWithEgg(null);
                net.minecraft.entity.passive.EntityHorse donkey = new ru.codex.offhand1710.Variants112.Donkey(w());
                donkey.onSpawnWithEgg(null);
                int horses = 0, villagers = 0;
                for (int i = 0; i < 200; i++) {
                    net.minecraft.entity.passive.EntityHorse h = new ru.codex.offhand1710.Variants112.Horse(w());
                    h.onSpawnWithEgg(null);
                    horses += h.getHorseType() == 0 ? 1 : 0;
                    net.minecraft.entity.monster.EntityZombie z = new ru.codex.offhand1710.Variants112.Zombie(w());
                    z.onSpawnWithEgg(null);
                    villagers += z.isVillager() ? 1 : 0;
                }
                net.minecraft.entity.monster.EntityZombie zv = new ru.codex.offhand1710.Variants112.ZombieVillager(w());
                zv.onSpawnWithEgg(null);
                result("spawn classes: horse entry gives horses, donkey entry donkeys", horses == 200 && donkey.getHorseType() == 1, "horses=" + horses + "/200 donkeyType=" + donkey.getHorseType());
                result("spawn classes: zombie entry never a villager, villager entry always", villagers == 0 && zv.isVillager(), "villagers=" + villagers + "/200 zv=" + zv.isVillager());
                donkey.setLocationAndAngles(bx + 0.5D, dy, bz - 3.5D, 0.0F, 0.0F);
                NBTTagCompound tag = new NBTTagCompound();
                boolean saved = donkey.writeToNBTOptional(tag);
                net.minecraft.entity.Entity back = net.minecraft.entity.EntityList.createEntityFromNBT(tag, w());
                result("donkey saved as EntityHorse and loads as a plain donkey", saved && "EntityHorse".equals(tag.getString("id"))
                    && back != null && back.getClass() == net.minecraft.entity.passive.EntityHorse.class && ((net.minecraft.entity.passive.EntityHorse) back).getHorseType() == 1,
                    "saved=" + saved + " id=" + tag.getString("id") + " class=" + (back == null ? null : back.getClass().getSimpleName()));
                StringBuilder plains = new StringBuilder();
                for (Object o : net.minecraft.world.biome.BiomeGenBase.plains.getSpawnableList(net.minecraft.entity.EnumCreatureType.creature)) {
                    net.minecraft.world.biome.BiomeGenBase.SpawnListEntry e = (net.minecraft.world.biome.BiomeGenBase.SpawnListEntry) o;
                    plains.append(e.entityClass.getSimpleName()).append('=').append(e.itemWeight).append(' ');
                }
                for (Object o : net.minecraft.world.biome.BiomeGenBase.plains.getSpawnableList(net.minecraft.entity.EnumCreatureType.monster)) {
                    net.minecraft.world.biome.BiomeGenBase.SpawnListEntry e = (net.minecraft.world.biome.BiomeGenBase.SpawnListEntry) o;
                    plains.append(e.entityClass.getSimpleName()).append('=').append(e.itemWeight).append(' ');
                }
                String s = plains.toString();
                result("plains spawn lists: Horse 5, Donkey 1, Zombie 95, ZombieVillager 5", s.contains("Horse=5") && s.contains("Donkey=1") && s.contains("Zombie=95") && s.contains("ZombieVillager=5"), s);
                w().spawnEntityInWorld(donkey);
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                boolean seen = false;
                for (Object o : mc.theWorld.loadedEntityList) {
                    seen |= o.getClass() == net.minecraft.entity.passive.EntityHorse.class && ((net.minecraft.entity.passive.EntityHorse) o).getHorseType() == 1;
                }
                result("client sees the spawned donkey as a plain EntityHorse of type 1", seen, "");
            }
        });
        // llama chest, inventory window and caravan (AbstractChestHorse, EntityAILlamaFollowCaravan of 1.12.2)
        final ru.codex.offhand1710.MoreMobs.Llama[] train = new ru.codex.offhand1710.MoreMobs.Llama[3];
        server(5, new Runnable() {
            @Override
            public void run() {
                g2llama.setStrength(3);
                g2llama.setLocationAndAngles(bx + 0.5D, dy, bz + 3.5D, 0.0F, 0.0F);
                g2llama.getNavigator().clearPathEntity();
                tp(bx + 0.5D, dy, bz + 1.5D, 0.0F, 20.0F);
                p().inventory.mainInventory[p().inventory.currentItem] = new ItemStack(Blocks.chest, 1);
                g2llama.interact(p());
                p().inventory.mainInventory[p().inventory.currentItem] = null;
                g2llama.inv.setInventorySlotContents(4, new ItemStack(Items.diamond, 5));
                NBTTagCompound tag = new NBTTagCompound();
                g2llama.writeToNBT(tag);
                ru.codex.offhand1710.MoreMobs.Llama copy = new ru.codex.offhand1710.MoreMobs.Llama(w());
                copy.readFromNBT(tag);
                result("llama takes a chest; Strength, ChestedHorse, Items and the carpet survive saving",
                    g2llama.chested() && copy.chested() && copy.strength() == 3 && copy.inv.getStackInSlot(4) != null && copy.inv.getStackInSlot(4).stackSize == 5 && copy.decor() == 14,
                    "chested=" + g2llama.chested() + " copy: chested=" + copy.chested() + " strength=" + copy.strength() + " slot4=" + copy.inv.getStackInSlot(4) + " decor=" + copy.decor());
                int s1 = 0, s5 = 0;
                for (int i = 0; i < 2000; i++) {
                    ru.codex.offhand1710.MoreMobs.Llama l = new ru.codex.offhand1710.MoreMobs.Llama(w());
                    l.onSpawnWithEgg(null);
                    s1 += l.strength() == 1 ? 1 : 0;
                    s5 += l.strength() == 5 ? 1 : 0;
                }
                result("spawn strength 1-5 as 1.12 (about 33 % ones, about 0.8 % fives)", s1 > 560 && s1 < 780 && s5 > 2 && s5 < 40, "ones=" + s1 + "/2000 fives=" + s5 + "/2000");
                p().setSneaking(true);
                g2llama.interact(p());
                p().setSneaking(false);
                boolean server = p().openContainer instanceof ru.codex.offhand1710.LlamaContainer;
                result("sneak + right click opens the llama inventory (carpet + 9 chest slots)", server && p().openContainer.inventorySlots.size() == 1 + 9 + 36,
                    "container=" + p().openContainer.getClass().getSimpleName() + " slots=" + p().openContainer.inventorySlots.size());
                // caravan: a leashed llama and three free ones behind it
                g2llama.setLeashedToEntity(p(), true);
                for (int i = 0; i < 3; i++) {
                    train[i] = new ru.codex.offhand1710.MoreMobs.Llama(w());
                    train[i].setLocationAndAngles(bx - 2.5D - 3.0D * i, dy, bz + 3.5D, 0.0F, 0.0F);
                    w().spawnEntityInWorld(train[i]);
                }
            }
        });
        for (int k = 0; k < 4; k++) {
            final int tick = k;
            client(1, new Runnable() {
                @Override
                public void run() {
                    log("INFO llama window tick " + tick + " client=" + mc.currentScreen + " clientContainer=" + mc.thePlayer.openContainer.getClass().getSimpleName()
                        + " server=" + p().openContainer.getClass().getSimpleName());
                }
            });
        }
        client(1, new Runnable() {
            @Override
            public void run() {
                result("client shows the llama window", mc.currentScreen instanceof LlamaGui, String.valueOf(mc.currentScreen) + " server=" + p().openContainer.getClass().getSimpleName()
                    + " dist=" + g2llama.getDistanceToEntity(p()) + " clientContainer=" + mc.thePlayer.openContainer.getClass().getSimpleName());
                shot("g2-llama-gui");
                mc.thePlayer.closeScreen();
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                int linked = 0;
                StringBuilder chain = new StringBuilder();
                for (ru.codex.offhand1710.MoreMobs.Llama l : train) {
                    linked += l.inCaravan() ? 1 : 0;
                    chain.append(l.inCaravan() ? (l.caravanHead == g2llama ? "lead " : "llama ") : "none ").append(String.format("d=%.1f ", l.getDistanceToEntity(g2llama)));
                }
                result("free llamas form a caravan behind the leashed one", linked == 3 && g2llama.hasCaravanTail(), chain.toString() + " headLeashed=" + g2llama.getLeashed() + " holder=" + g2llama.getLeashedToEntity());
                g2llama.clearLeashed(true, false);
            }
        });
        // @e in /effect, /tp, /execute and /kill (the 1.7.10 commands of these names only took players)
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("effect @e[name=G2llama] speed 10 1");
                boolean speed = g2llama.isPotionActive(net.minecraft.potion.Potion.moveSpeed) && g2llama.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier() == 1;
                double z0 = g2llama.posZ;
                cmd("tp @e[name=G2llama] ~ ~ ~5");
                boolean moved = Math.abs(g2llama.posZ - z0 - 5.0D) < 0.01D;
                cmd("execute @e[name=G2llama] ~ ~ ~ effect @s glowing 10");
                boolean glow = g2llama.isPotionActive(ru.codex.offhand1710.Content120.glowing);
                cmd("tp @p @e[name=G2llama]");
                boolean toEntity = p().getDistanceSqToEntity(g2llama) < 0.01D;
                cmd("kill @e[type=mc112bridge.Llama]");
                boolean killed = g2llama.isDead || g2llama.getHealth() <= 0.0F;
                result("@e in /effect /tp /execute /kill", speed && moved && glow && toEntity && killed,
                    "speed=" + speed + " moved=" + moved + " glow=" + glow + " toEntity=" + toEntity + " killed=" + killed);
                tp(bx + 0.5D, dy, bz + 0.5D, 0.0F, 20.0F);
            }
        });
        // item 6: gateway of a dragon kill, its shape, tile and beam
        final int[] gw = new int[3];
        server(5, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.End112.Gateways g = ru.codex.offhand1710.End112.Gateways.get(w());
                int before = g.left.size();
                int next = g.left.get(before - 1);
                net.minecraft.entity.boss.EntityDragon dragon = new net.minecraft.entity.boss.EntityDragon(w());
                new ru.codex.offhand1710.End112().onDeath(new net.minecraftforge.event.entity.living.LivingDeathEvent(dragon, net.minecraft.util.DamageSource.generic));
                gw[0] = net.minecraft.util.MathHelper.floor_double(96.0D * Math.cos(2.0D * (-Math.PI + 0.15707963267948966D * (double) next)));
                gw[1] = 75;
                gw[2] = net.minecraft.util.MathHelper.floor_double(96.0D * Math.sin(2.0D * (-Math.PI + 0.15707963267948966D * (double) next)));
                boolean shape = w().getBlock(gw[0], 75, gw[2]) == ru.codex.offhand1710.End112.gateway && w().getBlock(gw[0], 77, gw[2]) == Blocks.bedrock
                    && w().getBlock(gw[0], 73, gw[2]) == Blocks.bedrock && w().getBlock(gw[0] + 1, 76, gw[2]) == Blocks.bedrock && w().getBlock(gw[0] + 1, 76, gw[2] + 1) != Blocks.bedrock
                    && w().getBlock(gw[0] + 1, 75, gw[2]) == Blocks.air;
                net.minecraft.tileentity.TileEntity t = w().getTileEntity(gw[0], 75, gw[2]);
                result("dragon kill opens the next of 20 gateways (1.12 position, WorldGenEndGateway shape)", before == 20 && g.left.size() == 19 && shape,
                    "before=" + before + " after=" + g.left.size() + " index=" + next + " at " + gw[0] + ",75," + gw[2] + " shape=" + shape);
                result("gateway has the 1.12 tile entity, new (beam)", t instanceof ru.codex.offhand1710.End112.GatewayTile && ((ru.codex.offhand1710.End112.GatewayTile) t).spawning(), String.valueOf(t));
                cmd("gamemode 1");
                fly(true);
                tp(gw[0] + 0.5D, 80.0D, gw[2] - 12.5D, 0.0F, 10.0F);
            }
        });
        client(60, new Runnable() {
            @Override
            public void run() {
                shot("g2-gateway");
            }
        });
        // item 7: an explorer map hung in a frame gets its marker without being carried
        final net.minecraft.entity.item.EntityItemFrame[] frame = new net.minecraft.entity.item.EntityItemFrame[1];
        final ItemStack[] map = new ItemStack[1];
        server(5, new Runnable() {
            @Override
            public void run() {
                w().setBlock(bx + 5, dy, bz + 5, Blocks.stone, 0, 3);
                map[0] = new ItemStack(Items.filled_map, 1, w().getUniqueDataId("map"));
                net.minecraft.world.storage.MapData data = Items.filled_map.getMapData(map[0], w());
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Marker112Type", 9);
                tag.setInteger("Marker112X", data.xCenter + 20);
                tag.setInteger("Marker112Z", data.zCenter + 20);
                map[0].setTagCompound(tag);
                frame[0] = new net.minecraft.entity.item.EntityItemFrame(w(), bx + 5, dy, bz + 4, 2);
                frame[0].setDisplayedItem(map[0]);
                w().spawnEntityInWorld(frame[0]);
                result("framed map starts without the marker", !ru.codex.offhand1710.Maps112.hasMarker(w(), map[0]), "");
            }
        });
        server(45, new Runnable() {
            @Override
            public void run() {
                result("framed explorer map gets its marker back", ru.codex.offhand1710.Maps112.hasMarker(w(), frame[0].getDisplayedItem()), "");
            }
        });
        // spectator mode: through a wall, no interaction, looking through a mob, back to creative
        final net.minecraft.entity.passive.EntityPig[] pig = new net.minecraft.entity.passive.EntityPig[1];
        server(5, new Runnable() {
            @Override
            public void run() {
                tp(bx + 0.5D, dy, bz - 6.5D, 0.0F, 0.0F);
                for (int x = bx - 2; x <= bx + 2; x++) {
                    for (int y = dy; y < dy + 4; y++) {
                        for (int z = bz - 4; z <= bz - 2; z++) {
                            w().setBlock(x, y, z, Blocks.stone, 0, 2);
                        }
                    }
                }
                cmd("gamemode spectator");
                pig[0] = new net.minecraft.entity.passive.EntityPig(w());
                pig[0].setLocationAndAngles(bx + 0.5D, dy, bz + 6.5D, 0.0F, 0.0F);
                w().spawnEntityInWorld(pig[0]);
                result("/gamemode spectator: flag, flying, creative base", ru.codex.offhand1710.Spectator112.is(p()) && p().capabilities.isFlying && p().capabilities.isCreativeMode,
                    "flying=" + p().capabilities.isFlying);
            }
        });
        for (int k = 0; k < 30; k++) {
            client(1, new Runnable() {
                @Override
                public void run() {
                    mc.thePlayer.rotationYaw = 0.0F;
                    mc.thePlayer.motionZ = 0.5D;
                    mc.thePlayer.motionY = 0.0D;
                }
            });
        }
        server(5, new Runnable() {
            @Override
            public void run() {
                boolean client = mc.thePlayer.posZ > bz - 1.0D && mc.thePlayer.noClip && ru.codex.offhand1710.Spectator112.is(mc.thePlayer);
                boolean server = p().posZ > bz - 1.0D && p().noClip;
                result("spectator flies through a 3-block stone wall (client and server)", client && server,
                    String.format("clientZ=%.1f serverZ=%.1f wall=%d..%d noClip=%s/%s", mc.thePlayer.posZ, p().posZ, bz - 4, bz - 2, mc.thePlayer.noClip, p().noClip));
                net.minecraftforge.event.entity.player.PlayerInteractEvent ev = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p(),
                    net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, bx, dy - 1, bz, 1, w());
                result("spectator cannot use blocks", ev.isCanceled(), "");
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                net.minecraft.entity.Entity c = mc.theWorld.getEntityByID(pig[0].getEntityId());
                if (c instanceof net.minecraft.entity.EntityLivingBase) {
                    SpectatorClient.look((net.minecraft.entity.EntityLivingBase) c);
                }
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                result("left click on a mob looks through its eyes; the server keeps the player at it", mc.renderViewEntity != mc.thePlayer && p().getDistanceToEntity(pig[0]) < 1.5F,
                    "view=" + mc.renderViewEntity + " dist=" + p().getDistanceToEntity(pig[0]));
                shot("g2-spectator");
                mc.thePlayer.movementInput.sneak = true;
                mc.thePlayer.setSneaking(true);
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                mc.thePlayer.setSneaking(false);
                mc.thePlayer.movementInput.sneak = false;
                result("sneaking returns the view", mc.renderViewEntity == mc.thePlayer, String.valueOf(mc.renderViewEntity));
            }
        });
        server(1, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode creative");
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                result("/gamemode creative leaves spectator on both sides", !ru.codex.offhand1710.Spectator112.is(p()) && !p().noClip && !mc.thePlayer.noClip
                    && !ru.codex.offhand1710.Spectator112.is(mc.thePlayer), "serverNoClip=" + p().noClip + " clientNoClip=" + mc.thePlayer.noClip);
            }
        });
        // skeleton trap: waits while no player is within 10 blocks, then four skeleton horsemen
        final net.minecraft.entity.passive.EntityHorse[] trap = new net.minecraft.entity.passive.EntityHorse[1];
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                fly(true);
                tp(bx + 0.5D, dy + 1.0D, bz - 11.5D, 0.0F, 0.0F);
                trap[0] = ru.codex.offhand1710.SkeletonTrap112.spawnTrap(w(), bx + 0.5D, dy, bz + 11.5D);
                log("INFO additional difficulty here " + ru.codex.offhand1710.SkeletonTrap112.additionalDifficulty(w(), bx, bz));
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                boolean fire = false;
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        fire |= w().getBlock(bx + x, dy, bz + 11 + z) == Blocks.fire;
                    }
                }
                result("trap skeleton horse waits (type 4, no fire from the harmless bolt)", trap[0].isEntityAlive() && trap[0].getHorseType() == 4
                    && ru.codex.offhand1710.SkeletonTrap112.isTrap(trap[0]) && trap[0].riddenByEntity == null && !fire, "fire=" + fire + " type=" + trap[0].getHorseType());
                tp(bx + 0.5D, dy + 1.0D, bz + 5.5D, 0.0F, 0.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                int riders = 0, bows = 0, helmets = 0;
                for (Object o : w().getEntitiesWithinAABB(net.minecraft.entity.passive.EntityHorse.class, trap[0].boundingBox.expand(12.0D, 6.0D, 12.0D))) {
                    net.minecraft.entity.passive.EntityHorse h = (net.minecraft.entity.passive.EntityHorse) o;
                    if (h.getHorseType() == 4 && h.riddenByEntity instanceof net.minecraft.entity.monster.EntitySkeleton) {
                        riders++;
                        net.minecraft.entity.monster.EntitySkeleton s = (net.minecraft.entity.monster.EntitySkeleton) h.riddenByEntity;
                        bows += s.getHeldItem() != null && s.getHeldItem().getItem() == Items.bow && s.getHeldItem().isItemEnchanted() ? 1 : 0;
                        helmets += s.getEquipmentInSlot(4) != null && s.getEquipmentInSlot(4).getItem() == Items.iron_helmet ? 1 : 0;
                    }
                }
                result("a player within 10 blocks fires the trap: 4 skeleton horsemen, enchanted bows, iron helmets",
                    !ru.codex.offhand1710.SkeletonTrap112.isTrap(trap[0]) && trap[0].isTame() && riders == 4 && bows == 4 && helmets == 4,
                    "riders=" + riders + " bows=" + bows + " helmets=" + helmets + " tame=" + trap[0].isTame());
                cmd("gamemode 1");
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("g2-skeleton-trap");
            }
        });
        // knowledge book: unlocks its recipes and is used up; a book with an unknown name does nothing
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("recipe take @p minecraft:crafting_table");
                ItemStack book = new ItemStack(ru.codex.offhand1710.Items112.knowledgeBook);
                NBTTagCompound tag = new NBTTagCompound();
                net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
                list.appendTag(new net.minecraft.nbt.NBTTagString("minecraft:crafting_table"));
                tag.setTag("Recipes", list);
                book.setTagCompound(tag);
                book.getItem().onItemRightClick(book, w(), p());
                boolean known = ru.codex.offhand1710.Data112.unlockRecipes(p(), java.util.Collections.singletonList("minecraft:crafting_table")) == 0;
                ItemStack bad = new ItemStack(ru.codex.offhand1710.Items112.knowledgeBook);
                NBTTagCompound badTag = new NBTTagCompound();
                net.minecraft.nbt.NBTTagList badList = new net.minecraft.nbt.NBTTagList();
                badList.appendTag(new net.minecraft.nbt.NBTTagString("minecraft:no_such_recipe"));
                badTag.setTag("Recipes", badList);
                bad.setTagCompound(badTag);
                bad.getItem().onItemRightClick(bad, w(), p());
                result("knowledge book unlocks its recipe and is used up; an invalid one is kept", known && book.stackSize == 0 && bad.stackSize == 1,
                    "known=" + known + " used=" + book.stackSize + " bad=" + bad.stackSize);
            }
        });
        finish();
    }

    private void villager() {
        final int dy = 200;
        final int[] centre = new int[2];
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("difficulty 2");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 1000");
                cmd("weather clear");
                fly(true);
                bx = w().getSpawnPoint().posX;
                bz = w().getSpawnPoint().posZ;
                String square = findVillage("savanna");
                if (!square.isEmpty()) {
                    String[] g = square.split(",");
                    centre[0] = (Integer.parseInt(g[0]) + 4) * 16 + 8;
                    centre[1] = (Integer.parseInt(g[1]) + 4) * 16 + 8;
                }
                tp(bx + 0.5D, dy + 4.0D, bz - 10.5D, 0.0F, 30.0F);
                for (int x = bx - 12; x <= bx + 12; x++) {
                    for (int z = bz - 12; z <= bz + 12; z++) {
                        w().setBlock(x, dy - 1, z, Blocks.stone, 0, 2);
                    }
                }
                // AI cleared, the navigator walks each mob back and forth over 14 blocks with its vanilla wander speed
                lane(new EntityVillager(w()), "villager", bx - 8.5D, dy, bz + 0.5D, 0.6D);
                lane(new net.minecraft.entity.monster.EntityZombie(w()), "zombie", bx - 2.5D, dy, bz + 0.5D, 1.0D);
                lane(new EntityPig(w()), "pig", bx + 3.5D, dy, bz + 0.5D, 1.0D);
                lane(new net.minecraft.entity.passive.EntityCow(w()), "cow", bx + 9.5D, dy, bz + 0.5D, 1.0D);
                EntityVillager v = (EntityVillager) lanes.keySet().iterator().next();
                log("INFO villager movementSpeed=" + v.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).getAttributeValue() + " savanna village centre=" + centre[0] + "," + centre[1]);
            }
        });
        server(300, new Runnable() {
            @Override
            public void run() {
                speedCheck("villager walks at the 1.12.2 speed (AI speed 0.6, movementSpeed 0.5)", speed("villager"), walkSpeed(0.6D * 0.5D));
                speedCheck("zombie walks at the 1.12.2 speed (1.0 x 0.23)", speed("zombie"), walkSpeed(0.23D));
                speedCheck("pig walks at the 1.12.2 speed (1.0 x 0.25)", speed("pig"), walkSpeed(0.25D));
                speedCheck("cow walks at the 1.12.2 speed (1.0 x 0.2)", speed("cow"), walkSpeed(0.2D));
                endSpeeds();
                // natural AI: the villager flees a zombie (EntityAIAvoidEntity 0.6 / 0.6)
                lane(new EntityVillager(w()), "fleeing villager", bx + 0.5D, dy, bz + 6.5D, 0.0D);
                lane(new net.minecraft.entity.monster.EntityZombie(w()), "chasing zombie", bx + 0.5D, dy, bz + 3.5D, 0.0D);
            }
        });
        server(100, new Runnable() {
            @Override
            public void run() {
                speedCheck("villager fleeing a zombie runs at the 1.12.2 speed (0.6)", speed("fleeing villager"), walkSpeed(0.6D * 0.5D));
                speedCheck("zombie chasing a villager walks at the 1.12.2 speed", speed("chasing zombie"), walkSpeed(0.23D));
                endSpeeds();
                fly(false);
                tp(bx + 0.5D, dy, bz - 10.5D, 0.0F, 0.0F);
            }
        });
        client(30, new Runnable() {
            @Override
            public void run() {
                mc.thePlayer.rotationYaw = 0.0F;
                mc.thePlayer.setSprinting(false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            }
        });
        server(15, new Runnable() {
            @Override
            public void run() {
                speeds.add(new Speed("player", p()));
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                // the player's moves come from the client: a server tick may apply two of them (two packets in one
                // tick), so up to 2 ticks of double distance are allowed; the walking speed itself is the p90
                double expected = (double) (0.98F * 0.1F) / (1.0D - (double) (0.6F * 0.91F));
                Speed walk = speed("player");
                double[] st = walk.stats();
                int doubled = 0;
                for (double v : walk.d) {
                    doubled += v > expected * 1.1D ? 1 : 0;
                }
                result("player walks at the vanilla speed (0.1, no sprint)", Math.abs(st[0] - expected) < 0.006D && doubled <= 2 && st[1] < expected * 3.5D,
                        String.format("p90=%.4f max=%.4f expected=%.4f movingTicks=%d ticksAbove=%d", st[0], st[1], expected, (int) st[2], doubled));
                endSpeeds();
            }
        });
        client(1, new Runnable() {
            @Override
            public void run() {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            }
        });
        // the savanna village: 1.7.10 sent its villagers indoors at speed 1.0 all day (EntityAIMoveIndoors)
        for (int r = -5; r <= 5; r++) {
            final int row = r;
            server(1, new Runnable() {
                @Override
                public void run() {
                    if (centre[0] == 0 && centre[1] == 0) {
                        return;
                    }
                    int cx = (centre[0] >> 4) + row;
                    for (int cz = (centre[1] >> 4) - 5; cz <= (centre[1] >> 4) + 5; cz++) {
                        w().theChunkProviderServer.loadChunk(cx, cz);
                    }
                }
            });
        }
        server(20, new Runnable() {
            @Override
            public void run() {
                fly(true);
                tp(centre[0] + 0.5D, w().getHeightValue(centre[0], centre[1]) + 12.0D, centre[1] + 0.5D, 0.0F, 60.0F);
            }
        });
        for (int k = 0; k < 2; k++) {
            final boolean night = k == 1;
            server(night ? 1 : 300, new Runnable() {
                @Override
                public void run() {
                    if (night) {
                        cmd("time set 13000");
                    }
                    for (Object o : w().getEntitiesWithinAABB(EntityVillager.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(centre[0] - 80, 0, centre[1] - 80, centre[0] + 80, 255, centre[1] + 80))) {
                        EntityVillager v = (EntityVillager) o;
                        speeds.add(new Speed("village villager " + v.getEntityId() + (v.isChild() ? " child" : ""), v));
                    }
                }
            });
            server(400, new Runnable() {
                @Override
                public void run() {
                    Speed all = new Speed("all", null);
                    StringBuilder each = new StringBuilder();
                    int over = 0;
                    for (Speed s : speeds) {
                        all.d.addAll(s.d);
                        for (double v : s.d) {
                            over += v > 0.25D ? 1 : 0;
                        }
                        double[] st = s.stats();
                        each.append(String.format(" %s:max=%.3f", s.name.substring("village villager ".length()).replace(' ', '_'), st[1]));
                    }
                    double[] st = all.stats();
                    String detail = String.format("villagers=%d p90=%.4f max=%.4f movingTicks=%d ticksOver0.25=%d walk=%.4f indoorsAt1.0=%.4f;%s", speeds.size(), st[0], st[1], (int) st[2], over, walkSpeed(0.3D), walkSpeed(0.5D), each);
                    if (night) {
                        // 1.12.2 too: at night EntityAIMoveIndoors walks at speed 1.0; only reported
                        log("INFO savanna village at night (time 13000) " + detail);
                    } else {
                        result("savanna village by day: villagers walk at the 1.12.2 speed, under 2% of moving ticks above 0.25 (pushes; 1.7.10 ran indoors at 0.54)", speeds.size() > 0 && st[2] > 0 && over * 50 <= st[2] && st[0] < walkSpeed(0.3D) + 0.006D, detail);
                    }
                    endSpeeds();
                }
            });
        }
        finish();
    }

    // ---- elytra: a long straight flight at speed; chunk holes ahead, server tick time, fps, speed ----

    /** MC112_FLY: "rockets" (pitch -5, a Flight 3 rocket every 25 ticks) or "glide" (pitch 25, no rockets). */
    private static final String FLY = System.getenv("MC112_FLY") == null ? "rockets" : System.getenv("MC112_FLY");
    private boolean flightOn;
    private int flightTick;
    private double flx;
    private double flz;
    private boolean fHas;
    private final StringBuilder flightLog = new StringBuilder();
    private double flightDist;
    private int flightHolesMax;
    private long flightHoleSum;
    private int flightSamples;
    private double flightMaxSpeed;
    private int flightBelowLoaded;
    private int flightRockets;
    /** Pitch for the screenshots (the terrain below, not the sky); 0 = the flight's own pitch. */
    private float shotPitch;
    private final java.util.Map<String, Integer> serverSamples = new java.util.HashMap<String, Integer>();
    private Thread serverThread;

    /** A poor man's profiler: where the server thread is, sampled from the client thread each client tick. */
    private void sampleServerThread() {
        if (serverThread == null) {
            for (Thread th : Thread.getAllStackTraces().keySet()) {
                if ("Server thread".equals(th.getName())) {
                    serverThread = th;
                }
            }
            if (serverThread == null) {
                return;
            }
        }
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (StackTraceElement e : serverThread.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("java.") || c.startsWith("sun.")) {
                continue;
            }
            b.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName()).append(" < ");
            if (++n >= 7) {
                break;
            }
        }
        String key = b.toString();
        Integer old = serverSamples.get(key);
        serverSamples.put(key, old == null ? 1 : old + 1);
    }

    private String topSamples() {
        List<java.util.Map.Entry<String, Integer>> l = new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(serverSamples.entrySet());
        java.util.Collections.sort(l, new java.util.Comparator<java.util.Map.Entry<String, Integer>>() {
            @Override
            public int compare(java.util.Map.Entry<String, Integer> a, java.util.Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(12, l.size()); i++) {
            b.append("\nPROFILE ").append(l.get(i).getValue()).append(" ").append(l.get(i).getKey());
        }
        return b.toString();
    }

    private void steerFlight() {
        if (!flightOn || mc.thePlayer == null) {
            return;
        }
        sampleServerThread();
        mc.thePlayer.rotationYaw = 0.0F;
        mc.thePlayer.rotationPitch = shotPitch != 0.0F ? shotPitch : "glide".equals(FLY) ? 25.0F : -5.0F;
        if (flightTick % 20 == 10) {
            // chunks ahead (z+) within the render distance that the client has not got yet
            int pcx = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posX) >> 4;
            int pcz = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
            int rd = Math.min(mc.gameSettings.renderDistanceChunks, 16);
            int holes = 0;
            for (int dz = 1; dz <= rd; dz++) {
                for (int dx = -2; dx <= 2; dx++) {
                    net.minecraft.world.chunk.Chunk c = mc.theWorld.getChunkFromChunkCoords(pcx + dx, pcz + dz);
                    if (c == null || !c.isChunkLoaded || c instanceof net.minecraft.world.chunk.EmptyChunk) {
                        holes++;
                    }
                }
            }
            net.minecraft.world.chunk.Chunk under = mc.theWorld.getChunkFromChunkCoords(pcx, pcz);
            if (under == null || under instanceof net.minecraft.world.chunk.EmptyChunk) {
                flightBelowLoaded++;
            }
            flightHolesMax = Math.max(flightHolesMax, holes);
            flightHoleSum += holes;
            flightSamples++;
            flightLog.append(String.format(" [c%d holes=%d/%d]", flightTick / 20, holes, rd * 5));
        }
    }

    private void sampleFlight() {
        if (!flightOn) {
            return;
        }
        flightTick++;
        EntityPlayerMP p = p();
        if (fHas) {
            double d = Math.sqrt((p.posX - flx) * (p.posX - flx) + (p.posZ - flz) * (p.posZ - flz));
            flightDist += d;
            flightMaxSpeed = Math.max(flightMaxSpeed, d);
        }
        fHas = true;
        flx = p.posX;
        flz = p.posZ;
        if ("rockets".equals(FLY) && flightTick % 25 == 1) {
            ItemStack rocket = new ItemStack(Items.fireworks, 1);
            NBTTagCompound fw = new NBTTagCompound();
            fw.setByte("Flight", (byte) 3);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Fireworks", fw);
            rocket.setTagCompound(tag);
            ru.codex.offhand1710.Elytra112.useFirework(p, rocket);
        }
        flightRockets = Math.max(flightRockets, w().getEntitiesWithinAABB(net.minecraft.entity.item.EntityFireworkRocket.class, p.boundingBox.expand(4.0D, 4.0D, 4.0D)).size());
        if (flightTick % 20 == 0) {
            long[] ticks = MinecraftServer.getServer().tickTimeArray;
            long sum = 0;
            for (long t : ticks) {
                sum += t;
            }
            flightLog.append(String.format(" [s%d y=%.0f v=%.2f mspt=%.1f srvChunks=%d genQueue=%d sendQueue=%d flying=%s]", flightTick / 20, p.posY, flightMaxSpeed, sum / (double) ticks.length / 1000000.0D,
                    w().theChunkProviderServer.getLoadedChunkCount(), ru.codex.offhand1710.ChunkStream.pending(), p.loadedChunks.size(), ru.codex.offhand1710.OffhandInventory.get(p).elytraFlying));
        }
    }

    private void elytra() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("gamerule doDaylightCycle false");
                cmd("time set 6000");
                cmd("weather clear");
                fly(false);
                p().inventory.armorInventory[2] = new ItemStack(ru.codex.offhand1710.Elytra112.elytra);
                p().inventoryContainer.detectAndSendChanges();
                tp(p().posX, 200.0D, p().posZ, 0.0F, 0.0F);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.Elytra112.start(p());
                log("INFO flight " + FLY + " started=" + ru.codex.offhand1710.OffhandInventory.get(p()).elytraFlying + " renderDistance=" + mc.gameSettings.renderDistanceChunks
                        + " viewDistance=" + MinecraftServer.getServer().getConfigurationManager().getViewDistance() + " from " + (int) p().posX + "," + (int) p().posY + "," + (int) p().posZ);
                flightOn = true;
                flightTick = 0;
            }
        });
        client(1, new Runnable() {
            @Override
            public void run() {
                count = 0;
                perSecond.setLength(0);
                secondStart = System.currentTimeMillis();
                secondCount = 0;
                counting = true;
            }
        });
        client(600, new Runnable() {
            @Override
            public void run() {
                mc.gameSettings.thirdPersonView = 1;
                shotPitch = 35.0F;
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("elytra-" + FLY + "-rd" + mc.gameSettings.renderDistanceChunks + "-back");
                mc.gameSettings.thirdPersonView = 0;
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("elytra-" + FLY + "-rd" + mc.gameSettings.renderDistanceChunks + "-first");
                shotPitch = 0.0F;
                double box = mc.thePlayer.boundingBox.maxY - mc.thePlayer.boundingBox.minY;
                result("glide: client box 0.6 high, yOffset unchanged (stance 1.62)", Math.abs(box - 0.6D) < 0.01D && Math.abs(mc.thePlayer.yOffset - 1.62F) < 0.01F,
                    "box=" + box + " yOffset=" + mc.thePlayer.yOffset + " stance=" + (mc.thePlayer.posY - mc.thePlayer.boundingBox.minY));
                result("glide: camera 1.22 lower (eye 0.4)", Math.abs(ElytraClient.cameraShift(0.0F) - 1.22F) < 0.01F, "shift=" + ElytraClient.cameraShift(0.0F));
            }
        });
        server(1, new Runnable() {
            @Override
            public void run() {
                double box = p().boundingBox.maxY - p().boundingBox.minY;
                result("glide: server box 0.6 high", Math.abs(box - 0.6D) < 0.01D, "box=" + box + " height=" + p().height);
                ru.codex.offhand1710.Elytra112.useFirework(p(), new ItemStack(Items.fireworks, 1));
            }
        });
        client(4, new Runnable() {
            @Override
            public void run() {
                mc.gameSettings.thirdPersonView = 1;
                shotPitch = 20.0F;
            }
        });
        client(4, new Runnable() {
            @Override
            public void run() {
                shot("elytra-rocket-back");
                mc.gameSettings.thirdPersonView = 2;
            }
        });
        client(4, new Runnable() {
            @Override
            public void run() {
                shot("elytra-rocket-front");
                mc.gameSettings.thirdPersonView = 0;
                shotPitch = 0.0F;
            }
        });
        server(580, new Runnable() {
            @Override
            public void run() {
                flightOn = false;
                counting = false;
                EntityPlayerMP p = p();
                log("FLIGHT" + flightLog);
                log("INFO server thread samples:" + topSamples());
                int[] r = ru.codex.offhand1710.ChunkStream.REASONS;
                log("INFO chunk loads: broken " + r[0] + ", loaded " + r[1] + ", on disk " + r[2] + ", near player " + r[3] + ", deferred " + r[4] + ", generated at once " + r[5] + ", unloaded active chunks skipped " + ru.codex.offhand1710.ChunkStream.skippedActive);
                log("FPS per second " + perSecond);
                log(String.format("INFO flight %s: %d ticks, distance %.0f (%.2f blocks/tick), max %.2f, now at %d,%d,%d, flying=%s, holes ahead avg %.1f max %d, chunk under player missing %d of %d samples, held at the chunk edge %d ticks, rockets riding along max %d, elytra damage %d",
                        FLY, flightTick, flightDist, flightDist / Math.max(1, flightTick), flightMaxSpeed, (int) p.posX, (int) p.posY, (int) p.posZ,
                        ru.codex.offhand1710.OffhandInventory.get(p).elytraFlying, flightHoleSum / (double) Math.max(1, flightSamples), flightHolesMax, flightBelowLoaded, flightSamples, ElytraClient.heldAtEdge, flightRockets,
                        p.inventory.armorInventory[2] == null ? -1 : p.inventory.armorInventory[2].getItemDamage()));
                result("elytra flight ran (" + FLY + ")", flightTick >= 600 && flightDist > 100.0D, String.format("distance=%.0f", flightDist));
            }
        });
        // survival: a rocket with one explosion hurts the gliding player by 5 + 2 (EntityFireworkRocket.dealExplosionDamage)
        final float[] health = new float[1];
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                EntityPlayerMP p = p();
                p.setHealth(20.0F);
                p.fallDistance = 0.0F;
                if (!ru.codex.offhand1710.OffhandInventory.get(p).elytraFlying) {
                    tp(p.posX, 220.0D, p.posZ, 0.0F, 0.0F);
                }
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP p = p();
                ru.codex.offhand1710.Elytra112.start(p);
                health[0] = p.getHealth();
                ItemStack rocket = new ItemStack(Items.fireworks, 1);
                NBTTagCompound explosion = new NBTTagCompound();
                explosion.setByte("Type", (byte) 0);
                explosion.setIntArray("Colors", new int[] {0xFF0000});
                NBTTagList list = new NBTTagList();
                list.appendTag(explosion);
                NBTTagCompound fw = new NBTTagCompound();
                fw.setByte("Flight", (byte) 1);
                fw.setTag("Explosions", list);
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("Fireworks", fw);
                rocket.setTagCompound(tag);
                boolean used = ru.codex.offhand1710.Elytra112.useFirework(p, rocket);
                log("INFO explosive rocket used=" + used + " flying=" + ru.codex.offhand1710.OffhandInventory.get(p).elytraFlying + " health=" + health[0]);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                float lost = health[0] - p().getHealth();
                result("survival: a rocket with one explosion hurts the glider by 7 (5 + 2)", Math.abs(lost - 7.0F) < 0.01F, "lost=" + lost + " health=" + p().getHealth());
                cmd("gamemode 1");
                p().inventory.armorInventory[2] = null;
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                double c = mc.thePlayer.boundingBox.maxY - mc.thePlayer.boundingBox.minY, s = p().boundingBox.maxY - p().boundingBox.minY;
                result("glide end: box back to 1.8 on both sides, still connected", Math.abs(c - 1.8D) < 0.01D && Math.abs(s - 1.8D) < 0.01D && mc.theWorld != null,
                    "client=" + c + " server=" + s + " flying=" + ru.codex.offhand1710.OffhandInventory.get(p()).elytraFlying);
            }
        });
        finish();
    }

    private void backlog1() {
        final int[] centre = new int[VILLAGE_KINDS.length * 2];
        final String[] squares = new String[VILLAGE_KINDS.length];
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("difficulty 2");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                cmd("weather clear");
                fly(true);
                bx = w().getSpawnPoint().posX;
                bz = w().getSpawnPoint().posZ;
                for (int i = 0; i < VILLAGE_KINDS.length; i++) {
                    squares[i] = findVillage(VILLAGE_KINDS[i]);
                    if (squares[i].isEmpty()) {
                        continue;
                    }
                    String[] g = squares[i].split(",");
                    centre[i * 2] = (Integer.parseInt(g[0]) + 4) * 16 + 8;
                    centre[i * 2 + 1] = (Integer.parseInt(g[1]) + 4) * 16 + 8;
                }
                log("INFO seed=" + w().getSeed() + " spawn=" + bx + "," + bz);
            }
        });
        // generate the three villages (the chunks stay loaded, their villagers tick)
        for (int k = 0; k < VILLAGE_KINDS.length; k++) {
            final int i = k;
            for (int r = -5; r <= 5; r++) {
                final int row = r;
                server(1, new Runnable() {
                    @Override
                    public void run() {
                        if (squares[i] == null || squares[i].isEmpty()) {
                            return;
                        }
                        int cx = (centre[i * 2] >> 4) + row;
                        for (int cz = (centre[i * 2 + 1] >> 4) - 5; cz <= (centre[i * 2 + 1] >> 4) + 5; cz++) {
                            w().theChunkProviderServer.loadChunk(cx, cz);
                        }
                    }
                });
            }
        }
        for (int k = 0; k < VILLAGE_KINDS.length; k++) {
            final int i = k;
            final String kind = VILLAGE_KINDS[k];
            server(20, new Runnable() {
                @Override
                public void run() {
                    if (squares[i] == null || squares[i].isEmpty()) {
                        result("village " + kind + " found", false, "");
                        return;
                    }
                    tp(centre[i * 2] + 0.5D, w().getHeightValue(centre[i * 2], centre[i * 2 + 1]) + 6.0D, centre[i * 2 + 1] + 0.5D, 0.0F, 30.0F);
                }
            });
            // villagers report their positions to the world's VillageCollection every 70-120 ticks
            server(400, new Runnable() {
                @Override
                public void run() {
                    if (squares[i] == null || squares[i].isEmpty()) {
                        return;
                    }
                    int cx = centre[i * 2];
                    int cz = centre[i * 2 + 1];
                    List<int[]> halves = new java.util.ArrayList<int[]>();
                    int[] n = countDoors(cx, cz, halves);
                    String counts = "oak=" + n[0] + " spruce=" + n[1] + " acacia=" + n[2] + " other=" + n[3];
                    int want = "savanna".equals(kind) ? 2 : "taiga".equals(kind) ? 1 : 0;
                    boolean only = n[want] > 0 && n[0] + n[1] + n[2] + n[3] == n[want];
                    result("village " + kind + " doors are " + (want == 2 ? "acacia" : want == 1 ? "spruce" : "oak"), only, counts + " centre=" + cx + "," + cz);
                    List<int[]> villagers = new java.util.ArrayList<int[]>();
                    for (Object o : w().getEntitiesWithinAABB(EntityVillager.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(cx - 96, 0, cz - 96, cx + 96, 255, cz + 96))) {
                        net.minecraft.entity.Entity v = (net.minecraft.entity.Entity) o;
                        villagers.add(new int[] {net.minecraft.util.MathHelper.floor_double(v.posX), net.minecraft.util.MathHelper.floor_double(v.posY), net.minecraft.util.MathHelper.floor_double(v.posZ)});
                    }
                    int biome = registeredDoors(villagers);
                    // the same layout with oak doors (the metadata kept): the count of the 1.7.10 door code
                    for (int[] h : halves) {
                        w().setBlock(h[0], h[1], h[2], Blocks.wooden_door, h[4], 2);
                    }
                    int oak = registeredDoors(villagers);
                    for (int[] h : halves) {
                        w().setBlock(h[0], h[1], h[2], Block.getBlockById(h[3]), h[4], 2);
                    }
                    result("village " + kind + " door registration equals the oak case", biome == oak && biome > 0, "biomeDoors=" + biome + " oakDoors=" + oak + " villagers=" + villagers.size());
                    net.minecraft.village.Village real = w().villageCollectionObj.findNearestVillage(cx, w().getHeightValue(cx, cz), cz, 96);
                    int realDoors = real == null ? 0 : real.getNumVillageDoors();
                    result("village " + kind + " registered in the world (villages.dat)", realDoors > 0, "doors=" + realDoors + " villages=" + w().villageCollectionObj.getVillageList().size()
                        + (real == null ? "" : " centre=" + real.getCenter().posX + "," + real.getCenter().posZ + " radius=" + real.getVillageRadius() + " villagers=" + real.getNumVillagers()));
                }
            });
        }
        // a villager inside a closed acacia door room walks out: EntityAIOpenDoor opens the door
        final int dy = 200;
        server(20, new Runnable() {
            @Override
            public void run() {
                tp(bx + 3.5D, dy + 3.0D, bz + 4.5D, 0.0F, 60.0F);
                for (int x = bx - 2; x <= bx + 60; x++) {
                    for (int z = bz - 3; z <= bz + 60; z++) {
                        w().setBlock(x, dy - 1, z, Blocks.stone, 0, 2);
                    }
                }
                for (int y = dy; y <= dy + 1; y++) {
                    w().setBlock(bx - 1, y, bz, Blocks.stone, 0, 2);
                    w().setBlock(bx, y, bz - 1, Blocks.stone, 0, 2);
                    w().setBlock(bx, y, bz + 1, Blocks.stone, 0, 2);
                    w().setBlock(bx + 1, y, bz - 1, Blocks.stone, 0, 2);
                    w().setBlock(bx + 1, y, bz + 1, Blocks.stone, 0, 2);
                }
                w().setBlock(bx, dy + 2, bz, Blocks.stone, 0, 2);
                w().setBlock(bx + 1, dy + 2, bz, Blocks.stone, 0, 2);
                net.minecraft.item.ItemDoor.placeDoorBlock(w(), bx + 1, dy, bz, 0, PortedBlocks.DOORS[3]);
                doorVillager = new EntityVillager(w());
                doorVillager.setLocationAndAngles(bx + 0.5D, dy, bz + 0.5D, 0.0F, 0.0F);
                w().spawnEntityInWorld(doorVillager);
                villagerMaxX = doorVillager.posX;
                doorOpened = false;
                doorWatch = new int[] {bx + 1, dy, bz};
                log("INFO door before meta=" + w().getBlockMetadata(bx + 1, dy, bz) + " block=" + w().getBlock(bx + 1, dy, bz).getUnlocalizedName());
            }
        });
        for (int t = 0; t < 30; t++) {
            server(10, new Runnable() {
                @Override
                public void run() {
                    if (!doorOpened || villagerMaxX < bx + 2.5D) {
                        doorVillager.getNavigator().tryMoveToXYZ(bx + 7.5D, dy, bz + 0.5D, 0.6D);
                    }
                }
            });
        }
        server(5, new Runnable() {
            @Override
            public void run() {
                result("villager opens an acacia door (EntityAIOpenDoor) and walks out", doorOpened && villagerMaxX > bx + 2.0D, "opened=" + doorOpened + " villagerMaxX=" + (villagerMaxX - bx));
                doorWatch = null;
                doorVillager.setDead();
            }
        });
        // C: pigs and zombies onto slime blocks (12 blocks) and beds (5 blocks), a pig onto stone, an item onto slime
        final int sz = bz + 10;
        server(10, new Runnable() {
            @Override
            public void run() {
                int[] pads = {20, 26, 32, 38, 44, 50};
                for (int i = 0; i < pads.length; i++) {
                    int x = bx + pads[i];
                    for (int a = -1; a <= 1; a++) {
                        for (int b = -1; b <= 1; b++) {
                            if (i == 0 || i == 1 || i == 5) {
                                w().setBlock(x + a, dy - 1, sz + b, Content119.slime, 0, 2);
                            } else if (i == 4) {
                                w().setBlock(x + a, dy - 1, sz + b, Blocks.stone, 0, 2);
                            }
                        }
                    }
                    if (i == 2 || i == 3) {
                        w().setBlock(x, dy - 1, sz, Blocks.bed, 0, 2);
                        w().setBlock(x, dy - 1, sz + 1, Blocks.bed, 8, 2);
                    }
                }
                tp(bx + 35.5D, dy + 8.0D, sz - 8.5D, 0.0F, 30.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                double slimeTop = dy;
                double bedTop = dy - 1 + 0.5625D;
                drop("pig slime", new EntityPig(w()), bx + 20.5D, dy + 12.0D, sz + 0.5D, slimeTop);
                net.minecraft.entity.monster.EntityZombie z1 = new net.minecraft.entity.monster.EntityZombie(w());
                z1.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
                drop("zombie slime", z1, bx + 26.5D, dy + 12.0D, sz + 0.5D, slimeTop);
                drop("pig bed", new EntityPig(w()), bx + 32.5D, dy + 5.0D, sz + 0.5D, bedTop);
                net.minecraft.entity.monster.EntityZombie z2 = new net.minecraft.entity.monster.EntityZombie(w());
                z2.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
                drop("zombie bed", z2, bx + 38.5D, dy + 5.0D, sz + 0.5D, bedTop);
                drop("pig stone", new EntityPig(w()), bx + 44.5D, dy + 5.0D, sz + 0.5D, dy);
                EntityItem item = new EntityItem(w(), 0, 0, 0, new ItemStack(Items.diamond));
                item.motionX = 0.0D;
                item.motionZ = 0.0D;
                item.delayBeforeCanPickup = 1000;
                drop("item slime", item, bx + 50.5D, dy + 10.0D, sz + 0.5D, slimeTop);
            }
        });
        server(160, new Runnable() {
            @Override
            public void run() {
                for (Drop d : drops) {
                    double up = d.maxAfter - d.top;
                    boolean onTop = d.landed && Math.abs(d.landY - d.top) < 0.1D;
                    float health = d.e instanceof net.minecraft.entity.EntityLivingBase ? ((net.minecraft.entity.EntityLivingBase) d.e).getHealth() : 0.0F;
                    String detail = "bounce=" + String.format("%.2f", up) + " health " + d.health + "->" + health + " landed=" + d.landed + " landY=" + String.format("%.2f", d.landY) + " below=" + d.below + " now=" + String.format("%.1f", d.e.posY) + " dead=" + d.e.isDead + " difficulty=" + w().difficultySetting;
                    if (d.name.endsWith("slime") && d.e instanceof net.minecraft.entity.EntityLivingBase) {
                        result(d.name + " bounces, no fall damage", onTop && up > 2.0D && health == d.health, detail);
                    } else if (d.name.endsWith("bed")) {
                        result(d.name + " bounces (0.66), no fall damage", onTop && up > 0.4D && up < 3.0D && health == d.health, detail);
                    } else if (d.name.endsWith("stone")) {
                        result(d.name + " does not bounce (control)", onTop && up < 0.3D, detail);
                    } else {
                        log("INFO " + d.name + " " + detail);
                    }
                    d.e.setDead();
                }
                drops.clear();
            }
        });
        // B: advancement triggers
        final int ax = bx + 10;
        final int az = bz + 30;
        server(10, new Runnable() {
            @Override
            public void run() {
                Data112.revokeAll(p());
                tp(ax + 0.5D, dy, az + 0.5D, 0.0F, 30.0F);
                // tame_animal: bones to a wolf through EntityPlayer.interactWith
                net.minecraft.entity.passive.EntityWolf wolf = new net.minecraft.entity.passive.EntityWolf(w());
                wolf.setLocationAndAngles(ax + 2.5D, dy, az + 0.5D, 0.0F, 0.0F);
                w().spawnEntityInWorld(wolf);
                hand(new ItemStack(Items.bone, 64));
                int tries = 0;
                while (!wolf.isTamed() && tries < 60) {
                    p().interactWith(wolf);
                    tries++;
                }
                log("INFO wolf tamed=" + wolf.isTamed() + " after " + tries);
                // brewed_potion: a potion taken out of a brewing stand slot
                net.minecraft.inventory.ContainerBrewingStand brew = new net.minecraft.inventory.ContainerBrewingStand(p().inventory, new net.minecraft.tileentity.TileEntityBrewingStand());
                ((net.minecraft.inventory.Slot) brew.inventorySlots.get(0)).onPickupFromSlot(p(), new ItemStack(Items.potionitem, 1, 8193));
                // enchanted_item: the enchanting table container
                net.minecraft.inventory.ContainerEnchantment ench = new net.minecraft.inventory.ContainerEnchantment(p().inventory, w(), ax, dy, az);
                ench.putStackInSlot(0, new ItemStack(Items.diamond_sword));
                p().experienceLevel = 30;
                boolean enchanted = ench.enchantItem(p(), 0);
                log("INFO enchantItem=" + enchanted + " levels=" + ench.enchantLevels[0] + " sword=" + ench.getSlot(0).getStack());
                // item_durability_changed: a diamond hoe with no durability left breaks
                cmd("gamemode 0");
                ItemStack hoe = new ItemStack(Items.diamond_hoe);
                hoe.setItemDamage(hoe.getMaxDamage());
                hand(hoe);
                hoe.damageItem(1, p());
                log("INFO hoe size=" + hoe.stackSize);
                cmd("gamemode 1");
                fly(true);
                hand(null);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                done("trigger tame_animal", "minecraft:husbandry/tame_an_animal");
                done("trigger brewed_potion", "minecraft:nether/brew_potion");
                done("trigger enchanted_item", "minecraft:story/enchant_item");
                done("trigger item_durability_changed (broken diamond hoe)", "minecraft:husbandry/break_diamond_hoe");
                // cured_zombie_villager: golden apple to a weak zombie villager, conversion shortened to one tick
                net.minecraft.entity.monster.EntityZombie z = new net.minecraft.entity.monster.EntityZombie(w());
                z.setLocationAndAngles(ax - 2.5D, dy, az + 0.5D, 0.0F, 0.0F);
                z.setVillager(true);
                z.setCurrentItemOrArmor(4, new ItemStack(Items.leather_helmet));
                w().spawnEntityInWorld(z);
                z.addPotionEffect(new PotionEffect(Potion.weakness.id, 1200, 0));
                hand(new ItemStack(Items.golden_apple, 1, 0));
                p().interactWith(z);
                log("INFO zombie converting=" + z.isConverting());
                ReflectionHelper.setPrivateValue(net.minecraft.entity.monster.EntityZombie.class, z, Integer.valueOf(1), "conversionTime", "field_82234_d");
                hand(null);
                // construct_beacon: level 1 pyramid
                for (int x = -1; x <= 1; x++) {
                    for (int zz = -1; zz <= 1; zz++) {
                        w().setBlock(ax + 6 + x, dy - 1, az + zz, Blocks.iron_block, 0, 2);
                    }
                }
                w().setBlock(ax + 6, dy, az, Blocks.beacon, 0, 3);
                // enter_block: a water source at the feet
                w().setBlock(ax, dy, az + 3, Blocks.water, 0, 2);
                tp(ax + 0.5D, dy, az + 3.5D, 0.0F, 30.0F);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                done("trigger cured_zombie_villager", "minecraft:story/cure_zombie_villager");
                result("trigger enter_block (water)", Data112.hasCriterion(p(), "minecraft:recipes/transportation/boat", "entered_water"), "");
                w().setBlockToAir(ax, dy, az + 3);
                // enter_block: the end gateway, its teleport held back
                p().timeUntilPortal = 2000;
                w().setBlock(ax, dy, az - 3, ru.codex.offhand1710.End112.gateway, 0, 2);
                tp(ax + 0.5D, dy, az - 2.5D, 0.0F, 30.0F);
            }
        });
        server(160, new Runnable() {
            @Override
            public void run() {
                done("trigger enter_block (end gateway)", "minecraft:end/enter_end_gateway");
                w().setBlockToAir(ax, dy, az - 3);
                done("trigger construct_beacon level 1", "minecraft:nether/create_beacon");
                result("construct_beacon level 1 is not the full beacon", !Data112.isDone(p(), "minecraft:nether/create_full_beacon"), "");
                for (int layer = 1; layer <= 3; layer++) {
                    for (int x = -1 - layer; x <= 1 + layer; x++) {
                        for (int zz = -1 - layer; zz <= 1 + layer; zz++) {
                            w().setBlock(ax + 6 + x, dy - 1 - layer, az + zz, Blocks.iron_block, 0, 2);
                        }
                    }
                }
            }
        });
        server(200, new Runnable() {
            @Override
            public void run() {
                done("trigger construct_beacon level 4", "minecraft:nether/create_full_beacon");
                TileEntity beacon = w().getTileEntity(ax + 6, dy, az);
                log("INFO beacon levels=" + (beacon instanceof net.minecraft.tileentity.TileEntityBeacon ? ((net.minecraft.tileentity.TileEntityBeacon) beacon).getLevels() : -1));
                // nether_travel: into the nether here, out of it 1000 blocks further (8000 in the overworld)
                tp(ax + 0.5D, dy + 1.0D, az + 0.5D, 0.0F, 30.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(p(), -1, new Keep(MinecraftServer.getServer().worldServerForDimension(-1)));
                log("INFO in dimension " + p().dimension + " at " + (int) p().posX + "," + (int) p().posY + "," + (int) p().posZ);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                fly(true);
                p().playerNetServerHandler.setPlayerLocation(p().posX + 1000.0D, 120.0D, p().posZ, 0.0F, 0.0F);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(p(), 0, new Keep(MinecraftServer.getServer().worldServerForDimension(0)));
                log("INFO back in dimension " + p().dimension + " at " + (int) p().posX + "," + (int) p().posY + "," + (int) p().posZ);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                done("trigger nether_travel (7000 blocks)", "minecraft:nether/fast_travel");
                fly(true);
                tp(bx + 0.5D, dy + 2.0D, bz + 0.5D, 0.0F, 30.0F);
            }
        });
        finish();
    }

    // ---- functional scenarios ----

    private void more() {
        server(40, new Runnable() {
            @Override
            public void run() {
                bx = (int) p().posX;
                bz = (int) p().posZ;
                cmd("difficulty 0");
                cmd("gamerule doDaylightCycle false");
                cmd("time set 6000");
                w().setBlock(bx, 3, bz, Content119.slime, 0, 3);
                fly(false);
                tp(bx + 0.5D, 12.0D, bz + 0.5D, 0.0F, 30.0F);
                maxMotion = 0.0D;
                sample = true;
            }
        });
        server(70, new Runnable() {
            @Override
            public void run() {
                sample = false;
                result("player bounces on slime", maxMotion > 0.3D, "maxUp=" + maxMotion + " minDown=" + minMotion + " lastY=" + lastY + " flying=" + flyingNow);
                // piston under a slime block with an item and a player on top
                w().setBlock(bx + 4, 3, bz, Blocks.piston, 1, 2);
                w().setBlock(bx + 4, 4, bz, Content119.slime, 0, 2);
                flying = new EntityItem(w(), bx + 4.5D, 5.3D, bz + 0.5D, new ItemStack(Items.apple)); // box bottom is y - 0.125: must start above the block
                flying.delayBeforeCanPickup = 32767;
                flying.motionX = 0.0D;
                flying.motionZ = 0.0D;
                w().spawnEntityInWorld(flying);
                fly(false);
                tp(bx + 4.5D, 5.0D, bz + 0.5D, 0.0F, 30.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                itemStart = flying.posY + (flying.onGround ? 1000.0D : 0.0D);
                maxItemY = 0.0D;
                maxMotion = 0.0D;
                sample = true;
                sampleItem = true;
                w().setBlock(bx + 3, 3, bz, Blocks.redstone_block, 0, 3);
            }
        });
        server(50, new Runnable() {
            @Override
            public void run() {
                sample = false;
                sampleItem = false;
                result("slime launches item", maxItemY > 7.0D, "maxY=" + maxItemY + " startY=" + itemStart + " dead=" + flying.isDead);
                result("slime launches player", maxMotion > 0.5D, "up=" + maxMotion);
                fly(true);
                tp(bx + 20.5D, 10.0D, bz + 0.5D, 0.0F, 0.0F);
                // lingering potion of slowness
                target = new EntityPig(w());
                target.setLocationAndAngles(bx + 10.5D, 4.0D, bz + 0.5D, 0.0F, 0.0F);
                target.func_110163_bv();
                w().spawnEntityInWorld(target);
                Brew112.ThrownLingering shot = (Brew112.ThrownLingering) EntityList.createEntityByName("mc112bridge.LingeringPotion", w());
                shot.setPotionDamage(16394);
                shot.setPosition(bx + 12.0D, 7.0D, bz + 0.5D);
                shot.motionY = -0.5D;
                w().spawnEntityInWorld(shot);
            }
        });
        server(50, new Runnable() {
            @Override
            public void run() {
                Brew112.Cloud c = cloudNear(bx + 10.5D, 4.0D, bz + 0.5D, 4.0D);
                result("lingering makes cloud", c != null, c == null ? "" : "r=" + c.radius());
                result("cloud applies effect", target.isPotionActive(Potion.moveSlowdown), String.valueOf(target.getActivePotionEffects()));
                // tipped and spectral arrows
                target.clearActivePotions();
                EntityArrow tipped = (EntityArrow) Brew112.newTipped(w(), target, 1.0F, 8202);
                tipped.shootingEntity = p();
                tipped.setPosition(target.posX, target.posY + 3.0D, target.posZ);
                tipped.motionX = 0.0D;
                tipped.motionY = -1.5D;
                tipped.motionZ = 0.0D;
                w().spawnEntityInWorld(tipped);
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("tipped arrow effect", target.isPotionActive(Potion.moveSlowdown), String.valueOf(target.getActivePotionEffects()));
                target.clearActivePotions();
                target.hurtResistantTime = 0;
                EntityArrow a = Content120.newSpectral(w(), target, 1.0F);
                a.shootingEntity = p();
                a.setPosition(target.posX, target.posY + 3.0D, target.posZ);
                a.motionX = 0.0D;
                a.motionY = -1.5D;
                a.motionZ = 0.0D;
                w().spawnEntityInWorld(a);
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("spectral arrow glowing", target.isPotionActive(Content120.glowing) && Maps112.isGlowing(target), "");
                Brew112.DragonFireball f = (Brew112.DragonFireball) EntityList.createEntityByName("mc112bridge.DragonFireball", w());
                f.setPosition(bx + 20.5D, 7.0D, bz + 10.5D);
                f.accelerationY = -0.1D;
                f.motionY = -0.5D;
                w().spawnEntityInWorld(f);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                Brew112.Cloud c = cloudNear(bx + 20.5D, 4.0D, bz + 10.5D, 5.0D);
                result("dragon fireball makes breath cloud", c != null && c.isBreath(), "");
                tp(bx + 20.5D, 4.0D, bz + 10.5D, 0.0F, 0.0F);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                ItemStack bottle = new ItemStack(Items.glass_bottle, 1);
                ItemStack got = Items.glass_bottle.onItemRightClick(bottle, w(), p());
                boolean ok = got.getItem() == Brew112.dragonBreath;
                if (!ok) {
                    for (ItemStack s : p().inventory.mainInventory) {
                        ok |= s != null && s.getItem() == Brew112.dragonBreath;
                    }
                }
                result("bottle collects breath (through hook)", ok, String.valueOf(got));
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("give " + p().getCommandSenderName() + " mc112bridge:chain_command_block");
                cmd("give " + p().getCommandSenderName() + " mc112bridge:repeating_command_block");
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                boolean chain = false;
                boolean rep = false;
                for (ItemStack s : p().inventory.mainInventory) {
                    chain |= s != null && Item.getItemFromBlock(Content122.chain) == s.getItem();
                    rep |= s != null && Item.getItemFromBlock(Content122.repeating) == s.getItem();
                }
                result("/give chain and repeating", chain && rep, "");
                w().setBlock(bx + 40, 4, bz, Blocks.command_block, 0, 3);
                tp(bx + 40.5D, 6.0D, bz + 2.5D, 180.0F, 40.0F);
                fly(true);
            }
        });
        client(30, openCmd());
        client(5, clickId(9201));
        client(5, closeGui());
        server(15, new Runnable() {
            @Override
            public void run() {
                result("mode button makes chain block", w().getBlock(bx + 40, 4, bz) == Content122.chain, String.valueOf(w().getBlock(bx + 40, 4, bz)));
            }
        });
        client(20, openCmd());
        client(5, clickId(9201));
        client(5, closeGui());
        client(20, openCmd());
        client(5, clickId(9203));
        client(5, closeGui());
        client(20, openCmd());
        client(5, clickId(9202));
        client(5, shoot("cmd_gui"));
        client(5, closeGui());
        server(15, new Runnable() {
            @Override
            public void run() {
                TileEntity t = w().getTileEntity(bx + 40, 4, bz);
                result("repeating, auto, conditional from buttons", w().getBlock(bx + 40, 4, bz) == Content122.repeating && t instanceof Content122.ModeTile && ((Content122.ModeTile) t).auto && (w().getBlockMetadata(bx + 40, 4, bz) & 8) != 0, "");
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                KeyBinding.onTick(DataClient.keyCode());
                FMLCommonHandler.instance().bus().post(new InputEvent.KeyInputEvent());
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                result("key L opens advancements", mc.currentScreen instanceof DataClient.Screen, String.valueOf(mc.currentScreen));
                mc.displayGuiScreen(null);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("recipe take " + p().getCommandSenderName() + " *");
                cmd("recipe give " + p().getCommandSenderName() + " minecraft:crafting_table");
                java.util.Arrays.fill(p().inventory.mainInventory, null);
                p().inventory.mainInventory[20] = new ItemStack(Blocks.planks, 8, 0);
                cmd("gamemode 0");
                fly(true);
            }
        });
        client(30, new Runnable() {
            @Override
            public void run() {
                if (DataClient.isBookOpen()) {
                    log("note book was open");
                }
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                click(button(9300));
                result("button R opens panel", DataClient.isBookOpen(), "");
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                GuiButton recipe = button(9400);
                log("BOOKSTATE knownOnClient=" + DataClient.knownCount() + " (server: crafting_table only)");
                result("panel shows known recipe", recipe != null, "");
                if (recipe != null) {
                    // The button shows the first known recipe in 1.12.2 order; advancements unlock more than the crafting
                    // table (planks in the inventory give the stick, plate, button, ...), so the expected grid is read from it.
                    try {
                        java.lang.reflect.Method m = recipe.getClass().getDeclaredMethod("recipe", Minecraft.class);
                        m.setAccessible(true);
                        ru.codex.offhand1710.Data112.Recipe r = (ru.codex.offhand1710.Data112.Recipe) m.invoke(recipe, mc);
                        clickedRecipe = r.id;
                        clickedCount = 0;
                        for (List<ItemStack> s : r.slots) {
                            if (s != null && !s.isEmpty()) {
                                clickedCount++;
                            }
                        }
                    } catch (Exception e) {
                        log("FAIL reading recipe of button " + e);
                    }
                    click(recipe);
                }
            }
        });
        client(10, shoot("book_filled"));
        server(5, new Runnable() {
            @Override
            public void run() {
                int filled = 0;
                StringBuilder grid = new StringBuilder();
                for (int i = 1; i <= 4; i++) {
                    if (p().inventoryContainer.getSlot(i).getHasStack()) {
                        filled++;
                    }
                    grid.append(p().inventoryContainer.getSlot(i).getStack()).append(' ');
                }
                ItemStack left = p().inventory.mainInventory[20];
                int remaining = left == null ? 0 : left.stackSize;
                result("click on recipe fills grid", clickedCount > 0 && filled == clickedCount && remaining == 8 - clickedCount,
                        "recipe=" + clickedRecipe + " ingredients=" + clickedCount + " grid=" + grid + "planksLeft=" + remaining);
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(null);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 4; i++) {
                    p().inventoryContainer.getSlot(i).putStack(null);
                }
                cmd("gamemode 1");
                fly(true);
                // sponge placed by the real item path
                int x = bx + 50;
                for (int a = -2; a <= 2; a++) {
                    for (int b = -2; b <= 2; b++) {
                        w().setBlock(x + a, 4, bz + b, Blocks.stone, 0, 2);
                        w().setBlock(x + a, 5, bz + b, a == 0 && b == 0 ? Blocks.air : Blocks.water, 0, 2);
                    }
                }
                ItemStack sponge = new ItemStack(Blocks.sponge);
                p().theItemInWorldManager.activateBlockOrUseItem(p(), w(), sponge, x, 4, bz, 1, 0.5F, 1.0F, 0.5F);
                result("sponge absorbs and becomes wet", w().getBlock(x, 5, bz) == Content120.wetSponge && w().getBlock(x + 1, 5, bz) != Blocks.water, String.valueOf(w().getBlock(x, 5, bz)));
                fly(false);
                tp(bx + 60.5D, 5.0D, bz + 0.5D, 0.0F, 0.0F);
                p().addPotionEffect(new PotionEffect(Content120.levitation.id, 60, 0));
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                levY = mc.thePlayer.posY;
            }
        });
        client(40, new Runnable() {
            @Override
            public void run() {
                result("levitation lifts the player (client)", mc.thePlayer.posY > levY + 1.0D, "from " + levY + " to " + mc.thePlayer.posY);
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                int ok = 0;
                int bad = 0;
                try {
                    java.io.InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("mc112bridge", "sounds.json")).getInputStream();
                    com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(in)).getAsJsonObject();
                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : json.entrySet()) {
                        boolean found = mc.getSoundHandler().getSound(new ResourceLocation("mc112bridge", e.getKey())) != null;
                        for (com.google.gson.JsonElement s : e.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                            String name = s.isJsonPrimitive() ? s.getAsString() : s.getAsJsonObject().get("name").getAsString();
                            try {
                                Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("mc112bridge", "sounds/" + name + ".ogg"));
                            } catch (IOException x) {
                                found = false;
                                log("NOFILE " + name);
                            }
                        }
                        if (found) {
                            ok++;
                        } else {
                            bad++;
                        }
                    }
                } catch (IOException e) {
                    log("FAIL sounds " + e);
                }
                result("all mod sounds registered with files", bad == 0, "ok=" + ok + " bad=" + bad);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                lootAndLuck();
                mechanics();
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                int x = bx + 70;
                result("vanilla piston pushes stones", w().getBlock(x + 2, 4, bz) == Blocks.stone && w().getBlock(x + 3, 4, bz) == Blocks.stone && w().getBlock(x + 1, 4, bz) == Blocks.piston_head, "");
                result("vanilla sticky pulls", w().getBlock(x + 11, 4, bz) == Blocks.stone && w().getBlock(x + 12, 4, bz) == Blocks.air, "");
                result("redstone lamp via repeater", w().getBlock(x + 24, 4, bz) == Blocks.lit_redstone_lamp, String.valueOf(w().getBlock(x + 24, 4, bz)));
                w().setBlock(x + 9, 4, bz, Blocks.air, 0, 3);
                walkStart = p().posZ;
                cmd("gamemode 1");
                fly(false);
                tp(bx + 100.5D, 4.0D, bz + 0.5D, 0.0F, 0.0F);
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                walkStart = mc.thePlayer.posZ;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            }
        });
        client(40, new Runnable() {
            @Override
            public void run() {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                result("walking with forward key", mc.thePlayer.posZ > walkStart + 2.0D, "moved " + (mc.thePlayer.posZ - walkStart));
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                int x = bx + 105;
                ItemStack stone = new ItemStack(Blocks.stone);
                p().theItemInWorldManager.activateBlockOrUseItem(p(), w(), stone, x, 3, bz, 1, 0.5F, 1.0F, 0.5F);
                boolean placed = w().getBlock(x, 4, bz) == Blocks.stone;
                p().theItemInWorldManager.tryHarvestBlock(x, 4, bz);
                result("place and break a block", placed && w().isAirBlock(x, 4, bz), "");
                int id = w().getUniqueDataId("map");
                ItemStack map = new ItemStack(Items.filled_map, 1, id);
                net.minecraft.world.storage.MapData data = new net.minecraft.world.storage.MapData("map_" + id);
                w().setItemData(data.mapName, data);
                data.scale = 2;
                data.dimension = 0;
                data.markDirty();
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Marker112Type", 9);
                tag.setInteger("Marker112X", 0);
                tag.setInteger("Marker112Z", 0);
                map.setTagCompound(tag);
                p().inventory.mainInventory[8] = map;
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                log("SAVED");
                mc.thePlayer.sendChatMessage("/save-all");
            }
        });
        finish();
    }

    private double walkStart;
    private String clickedRecipe;
    private int clickedCount;

    // ---- extra: depth strider, tipped arrow from a bow, dragon fireballs, glow on other mobs ----

    private int ex;
    private int ez;
    private double landDist;
    private double water0;
    private double water3;
    private int fireballsSeen;
    private Brew112.Cloud breathFound;
    private net.minecraft.entity.boss.EntityDragon dragon;

    private void walkFor(final String what, final double dx, final double dz) {
        server(5, new Runnable() {
            @Override
            public void run() {
                // offsets from ex/ez: those are known only when the first step has run
                double x = ex + dx;
                double z = ez + dz;
                fly(false);
                tp(x, 4.0D, z, 0.0F, 0.0F);
                int bxx = net.minecraft.util.MathHelper.floor_double(x);
                int bzz = net.minecraft.util.MathHelper.floor_double(z);
                log("WALKSETUP " + what + " target=" + x + "," + z + " server block=" + Block.blockRegistry.getNameForObject(w().getBlock(bxx, 4, bzz))
                        + " ex=" + ex + " ez=" + ez);
            }
        });
        client(30, new Runnable() {
            @Override
            public void run() {
                int px = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posX);
                int py = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.boundingBox.minY);
                int pz = net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posZ);
                log("WALKSTART " + what + " player=" + mc.thePlayer.posX + "," + mc.thePlayer.posY + "," + mc.thePlayer.posZ + " client block=" + Block.blockRegistry.getNameForObject(mc.theWorld.getBlock(px, py, pz))
                        + " inWater=" + mc.thePlayer.isInWater() + " flying=" + mc.thePlayer.capabilities.isFlying);
                walkStart = mc.thePlayer.posZ;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            }
        });
        client(60, new Runnable() {
            @Override
            public void run() {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                double d = mc.thePlayer.posZ - walkStart;
                log("WALK " + what + " 60 ticks: " + d + " blocks, inWater=" + mc.thePlayer.isInWater() + " onGround=" + mc.thePlayer.onGround
                        + " boots=" + mc.thePlayer.getEquipmentInSlot(1));
                if ("land".equals(what)) {
                    landDist = d;
                } else if ("water".equals(what)) {
                    water0 = d;
                } else {
                    water3 = d;
                }
            }
        });
    }

    private void extra() {
        server(40, new Runnable() {
            @Override
            public void run() {
                ex = (int) p().posX + 30;
                ez = (int) p().posZ + 30;
                cmd("difficulty 0");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                // a water channel 2 deep along +Z, walled, all sources so nothing flows
                for (int z = ez - 1; z <= ez + 41; z++) {
                    for (int y = 4; y <= 6; y++) {
                        w().setBlock(ex - 1, y, z, Blocks.stone, 0, 2);
                        w().setBlock(ex + 1, y, z, Blocks.stone, 0, 2);
                        boolean end = z == ez - 1 || z == ez + 41;
                        w().setBlock(ex, y, z, end ? Blocks.stone : (y <= 5 ? Blocks.water : Blocks.air), 0, 2);
                    }
                }
                p().inventory.armorInventory[0] = null;
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        walkFor("land", 4.5D, 0.5D);
        walkFor("water", 0.5D, 0.5D);
        server(5, new Runnable() {
            @Override
            public void run() {
                ItemStack boots = new ItemStack(Items.diamond_boots);
                boots.addEnchantment(Content119.depthStrider, 3);
                p().inventory.armorInventory[0] = boots;
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        walkFor("water+DS3", 0.5D, 0.5D);
        server(5, new Runnable() {
            @Override
            public void run() {
                result("depth strider III speeds up walking in water", water3 > water0 * 1.6D, "water=" + water0 + " ds3=" + water3 + " land=" + landDist);
                result("depth strider III in water close to land speed (1.12.2: equal)", Math.abs(water3 - landDist) < landDist * 0.2D, "ds3=" + water3 + " land=" + landDist);
                p().inventory.armorInventory[0] = null;
                // tipped arrow shot from a real bow
                cmd("clear");
                cmd("gamemode 0");
                p().addPotionEffect(new PotionEffect(Potion.resistance.id, 6000, 4));
                p().addPotionEffect(new PotionEffect(Potion.regeneration.id, 6000, 4));
                p().inventory.currentItem = 0;
                p().inventory.mainInventory[0] = new ItemStack(Items.bow);
                p().inventory.mainInventory[5] = new ItemStack(Brew112.tipped, 16, 8202);
                p().inventoryContainer.detectAndSendChanges();
                fly(false);
                tp(ex + 10.5D, 4.0D, ez + 0.5D, 0.0F, 12.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                target = new EntityPig(w());
                target.setLocationAndAngles(ex + 10.5D, 4.0D, ez + 5.5D, 180.0F, 0.0F);
                target.func_110163_bv();
                w().spawnEntityInWorld(target);
            }
        });
        server(3, new Runnable() {
            @Override
            public void run() {
                ItemStack bow = p().inventory.mainInventory[0];
                p().rotationYaw = 0.0F;
                p().rotationPitch = 12.0F;
                bow.getItem().onPlayerStoppedUsing(bow, w(), p(), bow.getMaxItemUseDuration() - 20);
                int arrows = 0;
                String kind = "";
                for (Object o : w().loadedEntityList) {
                    if (o instanceof EntityArrow) {
                        arrows++;
                        kind += o.getClass().getSimpleName() + " ";
                    }
                }
                log("BOW arrows=" + arrows + " " + kind + " ammoLeft=" + p().inventory.mainInventory[5]);
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("tipped arrow from a bow gives its effect", target.isPotionActive(Potion.moveSlowdown),
                        "effects=" + target.getActivePotionEffects() + " pigAt=" + (int) target.posX + "," + (int) target.posZ + " hp=" + target.getHealth());
                ItemStack left = p().inventory.mainInventory[5];
                result("bow uses up a tipped arrow in survival", left != null && left.stackSize == 15, String.valueOf(left));
                target.setDead();
                // the dragon shoots fireballs at a survival player
                tp(ex + 20.5D, 4.0D, ez + 0.5D, 0.0F, -30.0F);
                dragon = new net.minecraft.entity.boss.EntityDragon(w());
                dragon.setLocationAndAngles(ex + 20.5D, 30.0D, ez + 20.5D, 0.0F, 0.0F);
                w().spawnEntityInWorld(dragon);
                fireballsSeen = 0;
                breathFound = null;
            }
        });
        for (int i = 0; i < 90; i++) {
            server(20, new Runnable() {
                @Override
                public void run() {
                    if (breathFound != null) {
                        return;
                    }
                    // the overworld dragon roams; the shot needs a player within 80 blocks, so it is brought back now and then
                    double dist = dragon.getDistanceToEntity(p());
                    if (dist > 48.0D) {
                        log("DRAGON far " + (int) dist + ", brought back");
                        dragon.setPosition(p().posX + 20.0D, 30.0D, p().posZ + 20.0D);
                    }
                    for (Object o : w().loadedEntityList) {
                        if (o instanceof Brew112.DragonFireball && ((Brew112.DragonFireball) o).ticksExisted < 21) {
                            fireballsSeen++;
                            Brew112.DragonFireball f = (Brew112.DragonFireball) o;
                            log("FIREBALL at " + (int) f.posX + "," + (int) f.posY + "," + (int) f.posZ + " shooter=" + (f.shootingEntity == null ? null : f.shootingEntity.getClass().getSimpleName()));
                        }
                        if (o instanceof Brew112.Cloud && ((Brew112.Cloud) o).isBreath()) {
                            breathFound = (Brew112.Cloud) o;
                            log("BREATH at " + (int) breathFound.posX + "," + (int) breathFound.posY + "," + (int) breathFound.posZ + " r=" + breathFound.radius()
                                    + " player=" + (int) p().posX + "," + (int) p().posY + "," + (int) p().posZ);
                        }
                    }
                }
            });
        }
        server(5, new Runnable() {
            @Override
            public void run() {
                // a fireball can live less than the 20-tick poll; the breath cloud keeps its owner
                Object owner = breathFound == null ? null : ReflectionHelper.getPrivateValue(Brew112.Cloud.class, breathFound, "owner");
                result("dragon shoots fireballs at a survival player", fireballsSeen > 0 || owner instanceof net.minecraft.entity.boss.EntityDragon,
                        "seen=" + fireballsSeen + " cloudOwner=" + (owner == null ? null : owner.getClass().getSimpleName()));
                result("dragon fireball leaves a breath cloud", breathFound != null, breathFound == null ? "" : "y=" + breathFound.posY);
                if (breathFound != null) {
                    result("breath cloud lands near the ground, not in the air", breathFound.posY < 12.0D, "y=" + breathFound.posY);
                }
                dragon.setDead();
                for (Object o : w().loadedEntityList) {
                    if (o instanceof Brew112.DragonFireball || o instanceof Brew112.Cloud) {
                        ((net.minecraft.entity.Entity) o).setDead();
                    }
                }
                // glowing on mobs other than the pig
                cmd("gamemode 1");
                p().clearActivePotions();
                int gx = ex + 60;
                net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(w());
                cow.setLocationAndAngles(gx + 0.5D, 4.0D, ez + 6.5D, 90.0F, 0.0F);
                cow.func_110163_bv();
                w().spawnEntityInWorld(cow);
                EntityVillager vil = new EntityVillager(w(), 0);
                vil.setLocationAndAngles(gx + 3.5D, 4.0D, ez + 6.5D, 180.0F, 0.0F);
                w().spawnEntityInWorld(vil);
                net.minecraft.entity.passive.EntityChicken ch = new net.minecraft.entity.passive.EntityChicken(w());
                ch.setLocationAndAngles(gx - 2.5D, 4.0D, ez + 6.5D, 180.0F, 0.0F);
                ch.func_110163_bv();
                w().spawnEntityInWorld(ch);
                for (net.minecraft.entity.EntityLivingBase e : new net.minecraft.entity.EntityLivingBase[] {cow, vil, ch}) {
                    e.addPotionEffect(new PotionEffect(Content120.glowing.id, 4000, 0));
                }
                fly(true);
                tp(gx + 0.5D, 4.5D, ez + 0.5D, 0.0F, 10.0F);
            }
        });
        client(60, new Runnable() {
            @Override
            public void run() {
                shot("glow_mobs");
                log("GLOW mobs drawn=" + GlowOutline.lastDrawn);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                int gx = ex + 60;
                int glowing = 0;
                for (Object o : w().loadedEntityList) {
                    if (o instanceof net.minecraft.entity.EntityLivingBase && !(o instanceof EntityPlayerMP) && Maps112.isGlowing((net.minecraft.entity.Entity) o)) {
                        glowing++;
                    }
                }
                result("glowing flag on cow, villager, chicken", glowing >= 3, "flagged=" + glowing);
                for (int x = gx - 4; x <= gx + 5; x++) {
                    for (int y = 4; y <= 7; y++) {
                        w().setBlock(x, y, ez + 3, Blocks.stone, 0, 2);
                    }
                }
            }
        });
        client(40, new Runnable() {
            @Override
            public void run() {
                shot("glow_mobs_wall");
                result("outline drawn for three non-pig mobs", GlowOutline.lastDrawn >= 3, "drawn=" + GlowOutline.lastDrawn);
            }
        });
        finish();
    }

    private void mechanics() {
        int x = bx + 70;
        // plain piston, line of three stones
        w().setBlock(x, 4, bz, Blocks.piston, 5, 2);
        for (int i = 1; i <= 2; i++) {
            w().setBlock(x + i, 4, bz, Blocks.stone, 0, 2);
        }
        w().setBlock(x - 1, 4, bz, Blocks.redstone_block, 0, 3);
        // sticky piston pulling back
        w().setBlock(x + 10, 4, bz, Blocks.sticky_piston, 5 | 8, 2);
        w().setBlock(x + 11, 4, bz, Blocks.piston_head, 5 | 8, 2);
        w().setBlock(x + 12, 4, bz, Blocks.stone, 0, 2);
        w().setBlock(x + 9, 4, bz, Blocks.redstone_block, 0, 2);
        w().setBlock(x + 9, 4, bz, Blocks.air, 0, 3);
        // redstone through a repeater into a lamp
        w().setBlock(x + 20, 4, bz, Blocks.redstone_block, 0, 3);
        w().setBlock(x + 21, 4, bz, Blocks.redstone_wire, 0, 3);
        w().setBlock(x + 22, 4, bz, Blocks.redstone_wire, 0, 3);
        w().setBlock(x + 23, 4, bz, Blocks.unpowered_repeater, 1, 3);
        w().setBlock(x + 24, 4, bz, Blocks.redstone_lamp, 0, 3);
    }

    private void lootAndLuck() {
        Random r = new Random(5L);
        for (String cat : new String[] {ChestGenHooks.DUNGEON_CHEST, ChestGenHooks.MINESHAFT_CORRIDOR, ChestGenHooks.PYRAMID_DESERT_CHEST, ChestGenHooks.STRONGHOLD_LIBRARY, ChestGenHooks.VILLAGE_BLACKSMITH}) {
            int empty = 0;
            int enchanted = 0;
            for (int i = 0; i < 100; i++) {
                IInventory inv = new InventoryBasic("t", false, 27);
                WeightedRandomChestContent.generateChestContents(r, ChestGenHooks.getItems(cat, r), inv, ChestGenHooks.getCount(cat, r));
                int n = 0;
                for (int s = 0; s < 27; s++) {
                    ItemStack st = inv.getStackInSlot(s);
                    if (st != null) {
                        n++;
                        enchanted += st.isItemEnchanted() ? 1 : 0;
                    }
                }
                empty += n == 0 ? 1 : 0;
            }
            result("chest loot " + cat, empty < 5, "empty=" + empty + " enchanted=" + enchanted);
        }
        EntityPlayerMP pl = p();
        int plain = Maps112.fishingLuck(pl);
        pl.addPotionEffect(new PotionEffect(Content120.luck.id, 400, 3));
        result("luck adds to fishing", Maps112.fishingLuck(pl) == plain + 4, "");
        try {
            EntityFishHook hook = new EntityFishHook(w(), pl);
            Method roll = ReflectionHelper.findMethod(EntityFishHook.class, hook, new String[] {"func_146033_f"});
            int treasure0 = fish(roll, hook, 3000);
            pl.removePotionEffect(Content120.luck.id);
            int treasure1 = fish(roll, hook, 3000);
            result("luck improves real fishing rolls", treasure0 > treasure1, "with=" + treasure0 + " without=" + treasure1);
            hook.setDead();
        } catch (Exception e) {
            log("FAIL fishing " + e);
        }
    }

    private int fish(Method roll, EntityFishHook hook, int n) throws Exception {
        int treasure = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = (ItemStack) roll.invoke(hook);
            if (s.getItem() == Items.enchanted_book || s.getItem() == Items.name_tag || s.getItem() == Items.saddle || s.getItem() == Item.getItemFromBlock(Blocks.waterlily) || s.getItem() == Items.bow || s.getItem() == Items.fishing_rod) {
                treasure++;
            }
        }
        return treasure;
    }


    // ---- stage gui: real GUI actions of 0.30.0 (recipe book, advancements, glow after hand, toast sound) ----

    private boolean fakeOn;
    private int fx;
    private int fy;
    private int leftFrames;
    private int rightFrames;
    private long frames;
    private boolean measuring;
    private int glErrors;
    private long frameMark;
    private int hookMark;
    private int itemsBefore;
    private boolean cursorKept;
    private int nAll;
    private GuiButton planksButton;
    private String expectId;
    private int glowDrawn;
    private Data112.Advancement progressAdv;
    private int progressN;
    private net.minecraft.entity.passive.EntityCow glowCow;

    /** Steps that may throw: the exception is logged as a failed step by run(). */
    private abstract class X implements Runnable {
        @Override
        public final void run() {
            try {
                go();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        abstract void go() throws Exception;
    }

    private static Field lwjgl(String name) throws Exception {
        Field f = Mouse.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /** Sets what Mouse.getX/getY/isButtonDown return for the frame about to be drawn (the OS state overwrites it after the frame). */
    private void poke(boolean left, boolean right) {
        try {
            lwjgl("x").setInt(null, fx);
            lwjgl("y").setInt(null, fy);
            ByteBuffer b = (ByteBuffer) lwjgl("buttons").get(null);
            b.put(0, (byte) (left ? 1 : 0));
            b.put(1, (byte) (right ? 1 : 0));
        } catch (Exception e) {
            if (!pokeFailed) {
                pokeFailed = true;
                log("FAIL mouse poke " + e);
            }
        }
    }

    private boolean pokeFailed;

    /** Puts the polled mouse over the scaled gui point. */
    private void mouseTo(int sx, int sy) {
        int f = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        fx = sx * f + f / 2;
        fy = mc.displayHeight - sy * f - f / 2;
        fakeOn = true;
    }

    private Method screenMethod(String[] names, Class<?>... types) {
        return ReflectionHelper.findMethod(GuiScreen.class, mc.currentScreen, names, types);
    }

    /** A click as the screen gets it from the input loop: pressed and released at one point. */
    private void clickAt(int x, int y, int button) throws Exception {
        screenMethod(new String[] {"mouseClicked", "func_73864_a"}, int.class, int.class, int.class).invoke(mc.currentScreen, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(button));
        screenMethod(new String[] {"mouseMovedOrUp", "func_146286_b"}, int.class, int.class, int.class).invoke(mc.currentScreen, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(button));
    }

    private void key(char c, int code) throws Exception {
        screenMethod(new String[] {"keyTyped", "func_73869_a"}, char.class, int.class).invoke(mc.currentScreen, Character.valueOf(c), Integer.valueOf(code));
    }

    private void type(String text) throws Exception {
        for (char c : text.toCharArray()) {
            key(c, c == ' ' ? Keyboard.KEY_SPACE : Keyboard.getKeyIndex(String.valueOf(Character.toUpperCase(c))));
        }
    }

    private void erase(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            key('\b', Keyboard.KEY_BACK);
        }
    }

    private static Object dc(String name) throws Exception {
        Field f = DataClient.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private GuiTextField searchField() throws Exception {
        Object ev = dc("events");
        Field f = ev.getClass().getDeclaredField("search");
        f.setAccessible(true);
        return (GuiTextField) f.get(ev);
    }

    @SuppressWarnings("unchecked")
    private List<GuiButton> recipeButtons() {
        List<GuiButton> all = ReflectionHelper.getPrivateValue(GuiScreen.class, mc.currentScreen, "buttonList", "field_146292_n");
        List<GuiButton> out = new java.util.ArrayList<GuiButton>();
        for (GuiButton b : all) {
            if (b.id >= 9400 && b.id < 9420) {
                out.add(b);
            }
        }
        return out;
    }

    private Object groupOf(GuiButton b) throws Exception {
        Field g = b.getClass().getDeclaredField("group");
        g.setAccessible(true);
        return g.get(b);
    }

    @SuppressWarnings("unchecked")
    private List<Data112.Recipe> recipesOf(GuiButton b) throws Exception {
        Object grp = groupOf(b);
        Field r = grp.getClass().getDeclaredField("recipes");
        r.setAccessible(true);
        return (List<Data112.Recipe>) r.get(grp);
    }

    private boolean canCraft(Data112.Recipe r) throws Exception {
        Method m = DataClient.class.getDeclaredMethod("canCraft", Minecraft.class, Data112.Recipe.class);
        m.setAccessible(true);
        return ((Boolean) m.invoke(null, mc, r)).booleanValue();
    }

    private String ghostId() throws Exception {
        Object g = dc("ghost");
        return g == null ? null : ((Data112.Recipe) g).id;
    }

    private int bookPage() throws Exception {
        return ((Integer) dc("bookPage")).intValue();
    }

    private int itemEntities() {
        int n = 0;
        for (Object o : w().loadedEntityList) {
            if (o instanceof EntityItem) {
                n++;
            }
        }
        return n;
    }

    private GuiButton buttonFor(String recipeId) throws Exception {
        for (GuiButton b : recipeButtons()) {
            for (Data112.Recipe r : recipesOf(b)) {
                if (r.id.equals(recipeId)) {
                    return b;
                }
            }
        }
        return null;
    }

    private int panelX() {
        return (mc.currentScreen.width - 147) / 2 - 86;
    }

    private int panelY() {
        return (mc.currentScreen.height - 166) / 2;
    }

    private void gui() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("difficulty 0");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                cmd("gamemode 0");
                for (int i = 0; i < p().inventory.mainInventory.length; i++) {
                    p().inventory.mainInventory[i] = null;
                }
                cmd("advancement revoke " + p().getCommandSenderName() + " everything");
                cmd("recipe give " + p().getCommandSenderName() + " *");
            }
        });
        // ---- 1. held-item click ----
        client(60, new X() {
            @Override
            void go() throws Exception {
                result("recipe sync: client knows every recipe after the first /recipe give *", DataClient.knownCount() >= 400, "client knows " + DataClient.knownCount() + " of " + Data112.RECIPES.size());
            }
        });
        server(2, new Runnable() {
            @Override
            public void run() {
                cmd("recipe give " + p().getCommandSenderName() + " *");
            }
        });
        client(40, new X() {
            @Override
            void go() throws Exception {
                log("INFO knownRecipes=" + DataClient.knownCount() + " recipes=" + Data112.RECIPES.size() + " screen=" + mc.displayWidth + "x" + mc.displayHeight);
                mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                GuiButton b = button(9300);
                click(b);
                result("recipe book opens with button 9300", DataClient.isBookOpen() && button(9302) != null && !recipeButtons().isEmpty(), "open=" + DataClient.isBookOpen() + " slots=" + recipeButtons().size());
            }
        });
        server(2, new Runnable() {
            @Override
            public void run() {
                p().inventory.setItemStack(new ItemStack(Items.stick, 7));
                itemsBefore = itemEntities();
            }
        });
        client(2, new X() {
            @Override
            void go() throws Exception {
                mc.thePlayer.inventory.setItemStack(new ItemStack(Items.stick, 7));
                clickAt(panelX() + 4, panelY() + 160, 0);
            }
        });
        client(10, new X() {
            @Override
            void go() throws Exception {
                cursorKept = mc.thePlayer.inventory.getItemStack() != null && mc.thePlayer.inventory.getItemStack().stackSize == 7;
            }
        });
        server(2, new Runnable() {
            @Override
            public void run() {
                result("click on the book panel keeps the stack on the cursor, no item entity", cursorKept && p().inventory.getItemStack() != null && itemEntities() == itemsBefore,
                        "clientCursor=" + cursorKept + " serverCursor=" + p().inventory.getItemStack() + " entities=" + itemEntities() + "/" + itemsBefore);
            }
        });
        client(2, new X() {
            @Override
            void go() throws Exception {
                clickAt(mc.currentScreen.width - 3, mc.currentScreen.height / 2, 0);
            }
        });
        client(10, new X() {
            @Override
            void go() throws Exception {
                cursorKept = mc.thePlayer.inventory.getItemStack() == null;
            }
        });
        server(6, new Runnable() {
            @Override
            public void run() {
                boolean sticks = false;
                for (Object o : w().loadedEntityList) {
                    if (o instanceof EntityItem && ((EntityItem) o).getEntityItem().getItem() == Items.stick && ((EntityItem) o).getEntityItem().stackSize == 7) {
                        sticks = true;
                        ((EntityItem) o).setDead();
                    }
                }
                result("click really outside the window drops the stack", sticks, "dropped7sticks=" + sticks + " clientCursorEmpty=" + cursorKept + " serverCursor=" + p().inventory.getItemStack());
            }
        });
        // ---- 2. search field ----
        client(5, new X() {
            @Override
            void go() throws Exception {
                nAll = recipeButtons().size();
                GuiButton next = button(9302);
                click(next);
                boolean page2 = bookPage() >= 1 && button(9301) != null;
                result("book: one click on the next-page arrow advances exactly one page", bookPage() == 1, "bookPage(0-based) after one click from page 0 = " + bookPage());
                log("INFO page click: next at " + next.xPosition + "," + next.yPosition + " size " + next.width + "x" + next.height + " bookPage=" + bookPage() + " back=" + (button(9301) != null) + " slotsNow=" + recipeButtons().size());
                clickAt(panelX() + 25 + 4, panelY() + 14 + 3, 0);
                boolean focus = searchField().isFocused();
                type("stick");
                int n = recipeButtons().size();
                boolean names = n > 0;
                for (GuiButton b : recipeButtons()) {
                    boolean any = false;
                    for (Data112.Recipe r : recipesOf(b)) {
                        any |= net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(r.result.getDisplayName()).toLowerCase().contains("stick");
                    }
                    names &= any;
                }
                result("search: field gets the focus from a click", focus, "");
                result("search 'stick' shrinks the list to matching names and returns to page 1", page2 && n < nAll && names && bookPage() == 0 && button(9301) == null,
                        "page2first=" + page2 + " slots " + nAll + "->" + n + " allMatch=" + names + " pageNow=" + bookPage() + " text=" + searchField().getText());
            }
        });
        client(6, new X() {
            @Override
            void go() throws Exception {
                shot("gui_book_search");
            }
        });
        client(3, new X() {
            @Override
            void go() throws Exception {
                GuiScreen before = mc.currentScreen;
                key('e', mc.gameSettings.keyBindInventory.getKeyCode());
                String text = searchField().getText();
                result("search: the inventory key types its letter and keeps the screen open", mc.currentScreen == before && "sticke".equals(text), "text=" + text + " screenSame=" + (mc.currentScreen == before));
                erase(6);
                result("search: clearing restores the full list", searchField().getText().length() == 0 && recipeButtons().size() == nAll && bookPage() == 0, "text='" + searchField().getText() + "' slots=" + recipeButtons().size() + "/" + nAll);
            }
        });
        // ---- 3. alternatives window ----
        client(3, new X() {
            @Override
            void go() throws Exception {
                type("plank");
                planksButton = null;
                for (GuiButton b : recipeButtons()) {
                    if (recipesOf(b).size() > 1) {
                        planksButton = b;
                        break;
                    }
                }
                log("INFO planks button=" + (planksButton == null ? null : planksButton.xPosition + "," + planksButton.yPosition + " recipes=" + recipesOf(planksButton).size()));
                expectId = recipesOf(planksButton).get(2).id;
                mouseTo(planksButton.xPosition + 12, planksButton.yPosition + 12);
                rightFrames = 1;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                result("alternatives: right click on a slot with several recipes opens the window", dc("altGroup") == groupOf(planksButton), "altGroup=" + (dc("altGroup") != null));
            }
        });
        client(4, new X() {
            @Override
            void go() throws Exception {
                shot("gui_alt");
            }
        });
        client(2, new X() {
            @Override
            void go() throws Exception {
                int n = recipesOf(planksButton).size();
                int cols = Math.min(4, n);
                int rows = (n + 3) / 4;
                int bw = cols * 25 + 8;
                int bh = rows * 25 + 8;
                int bx = Math.max(2, Math.min(mc.currentScreen.width - bw - 2, planksButton.xPosition + 12 - bw / 2));
                int by = Math.max(2, Math.min(mc.currentScreen.height - bh - 2, planksButton.yPosition + 27));
                int cx = bx + 4 + 25 * (2 % 4) + 12;
                int cy = by + 4 + 25 * (2 / 4) + 12;
                mouseTo(cx, cy);
                clickAt(cx, cy, 0);
                leftFrames = 1;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                result("alternatives: a click on one of its cells picks that recipe and closes it", dc("altGroup") == null && expectId.equals(ghostId()), "altOpen=" + (dc("altGroup") != null) + " ghost=" + ghostId() + " expected=" + expectId);
                mouseTo(planksButton.xPosition + 12, planksButton.yPosition + 12);
                rightFrames = 1;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                boolean open = dc("altGroup") != null;
                mouseTo(mc.currentScreen.width - 6, 6);
                clickAt(mc.currentScreen.width - 6, 6, 0);
                leftFrames = 1;
                cursorKept = open;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                result("alternatives: a left click elsewhere closes it and picks nothing", cursorKept && dc("altGroup") == null && expectId.equals(ghostId()), "wasOpen=" + cursorKept + " stillOpen=" + (dc("altGroup") != null) + " ghost=" + ghostId());
                mouseTo(planksButton.xPosition + 12, planksButton.yPosition + 12);
                rightFrames = 1;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                cursorKept = dc("altGroup") != null;
                mouseTo(mc.currentScreen.width - 6, 6);
                rightFrames = 1;
            }
        });
        client(5, new X() {
            @Override
            void go() throws Exception {
                result("alternatives: a right click elsewhere closes it", cursorKept && dc("altGroup") == null, "wasOpen=" + cursorKept + " stillOpen=" + (dc("altGroup") != null));
            }
        });
        // ---- 4. ghost recipe ----
        client(3, new X() {
            @Override
            void go() throws Exception {
                erase(5);
                type("crafting");
                GuiButton b = buttonFor("minecraft:crafting_table");
                Data112.Recipe r = Data112.RECIPES.get("minecraft:crafting_table");
                boolean craftable = canCraft(r);
                click(b);
                boolean ghost = "minecraft:crafting_table".equals(ghostId());
                result("ghost: click on a recipe with missing ingredients sets the ghost", b != null && !craftable && ghost, "button=" + (b != null) + " craftableBefore=" + craftable + " ghost=" + ghostId());
            }
        });
        server(8, new Runnable() {
            @Override
            public void run() {
                boolean empty = true;
                for (int i = 1; i <= 4; i++) {
                    empty &= !p().openContainer.getSlot(i).getHasStack();
                }
                result("ghost: no fill happens while ingredients are missing", empty && p().inventory.getItemStack() == null, "gridEmpty=" + empty);
            }
        });
        client(2, new X() {
            @Override
            void go() throws Exception {
                shot("gui_ghost");
            }
        });
        server(3, new Runnable() {
            @Override
            public void run() {
                p().inventory.addItemStackToInventory(new ItemStack(Blocks.planks, 4, 0));
                p().inventoryContainer.detectAndSendChanges();
            }
        });
        client(12, new X() {
            @Override
            void go() throws Exception {
                Data112.Recipe r = Data112.RECIPES.get("minecraft:crafting_table");
                boolean craftable = canCraft(r);
                click(buttonFor("minecraft:crafting_table"));
                result("ghost: with the ingredients given the recipe is craftable", craftable, "canCraft=" + craftable);
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                int filled = 0;
                for (int i = 1; i <= 4; i++) {
                    ItemStack s = p().openContainer.getSlot(i).getStack();
                    if (s != null && s.getItem() == Item.getItemFromBlock(Blocks.planks) && s.stackSize == 1) {
                        filled++;
                    }
                }
                int left = 0;
                for (ItemStack s : p().inventory.mainInventory) {
                    left += s != null && s.getItem() == Item.getItemFromBlock(Blocks.planks) ? s.stackSize : 0;
                }
                result("ghost: click fills the grid from the inventory", filled == 4 && left == 0, "gridPlanks=" + filled + " inventoryPlanks=" + left);
            }
        });
        client(8, new X() {
            @Override
            void go() throws Exception {
                result("ghost: cleared once the recipe is made craftable", ghostId() == null, "ghost=" + ghostId());
                mc.displayGuiScreen(null);
                fakeOn = false;
            }
        });
        // ---- 6. glow after the hand (before the advancement toasts) ----
        server(10, new Runnable() {
            @Override
            public void run() {
                int gx = (int) p().posX;
                int gz = (int) p().posZ;
                hold(new ItemStack(Items.iron_sword));
                p().clearActivePotions();
                tp(gx + 0.5D, p().posY, gz + 0.5D, 0.0F, 12.0F);
                net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(w());
                cow.setLocationAndAngles(gx + 0.5D - 1.7D, p().posY, gz + 2.4D, 180.0F, 0.0F);
                cow.func_110163_bv();
                w().spawnEntityInWorld(cow);
                glowCow = cow;
                cow.addPotionEffect(new PotionEffect(Content120.glowing.id, 4000, 0));
                view(0);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                // cows wander: put it back behind the hand and keep it still for the shot
                glowCow.setLocationAndAngles(p().posX - 1.7D, p().posY, p().posZ + 2.4D, 180.0F, 0.0F);
                glowCow.motionX = 0.0D;
                glowCow.motionZ = 0.0D;
            }
        });
        client(4, new X() {
            @Override
            void go() throws Exception {
                glowDrawn = GlowOutline.lastDrawn;
                shot("glow_hand");
                log("GLOW hand drawn=" + glowDrawn + " held=" + mc.thePlayer.getHeldItem() + " view=" + mc.gameSettings.thirdPersonView);
                GL11.glGetError();
                glErrors = 0;
                frameMark = frames;
                hookMark = GlowOutline.hookCalls();
                measuring = true;
            }
        });
        client(60, new X() {
            @Override
            void go() throws Exception {
                measuring = false;
                long n = frames - frameMark;
                int h = GlowOutline.hookCalls() - hookMark;
                boolean held = mc.thePlayer.getHeldItem() != null && mc.gameSettings.thirdPersonView == 0;
                result("glow after hand: outline drawn with an item in the first-person hand", held && glowDrawn >= 1, "GLOW drawn=" + glowDrawn + " held=" + mc.thePlayer.getHeldItem());
                result("glow after hand: hook fires once per frame", n >= 20 && h == n, "frames=" + n + " hookCalls=" + h);
                result("glow after hand: no GL error in those frames", glErrors == 0, "glErrors=" + glErrors);
            }
        });
        // ---- 5. advancement progress ----
        server(5, new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                try {
                    for (String id : new String[] {"minecraft:adventure/root", "minecraft:adventure/sleep_in_bed"}) {
                        Data112.Advancement a = Data112.ADVANCEMENTS.get(id);
                        for (String c : ((java.util.Map<String, Object>) fieldOf(Data112.Advancement.class, a, "criteria")).keySet()) {
                            Data112.grant(p(), a, c);
                        }
                    }
                    progressAdv = Data112.ADVANCEMENTS.get("minecraft:adventure/adventuring_time");
                    int n = 0;
                    for (String c : ((java.util.Map<String, Object>) fieldOf(Data112.Advancement.class, progressAdv, "criteria")).keySet()) {
                        if (n++ < 3) {
                            Data112.grant(p(), progressAdv, c);
                        }
                    }
                    Data112.sync(p());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        client(20, new X() {
            @Override
            @SuppressWarnings("unchecked")
            void go() throws Exception {
                java.util.Set<String> crit = (java.util.Set<String>) dc("CRIT");
                java.util.Set<String> done = (java.util.Set<String>) dc("DONE");
                int n = progressAdv.completed(crit);
                int m = progressAdv.total();
                result("progress: partially granted advancement computes n/m", n >= 3 && n < m && m > 3 && !done.contains(progressAdv.id), "n=" + n + " m=" + m + " done=" + done.contains(progressAdv.id) + " (3 granted by the test, more if the player's own biome counted)");
                progressN = n;
                Field last = DataClient.class.getDeclaredField("lastTab");
                last.setAccessible(true);
                last.set(null, "minecraft:adventure/root");
                DataClient.openAdvancements();
            }
        });
        client(8, new X() {
            @Override
            @SuppressWarnings("unchecked")
            void go() throws Exception {
                boolean screen = mc.currentScreen instanceof DataClient.Screen;
                Object scr = mc.currentScreen;
                Object sel = fieldOf(scr.getClass(), scr, "selected");
                List<Object> all = (List<Object>) fieldOf(sel.getClass(), sel, "all");
                Object widget = null;
                for (Object wd : all) {
                    if (fieldOf(wd.getClass(), wd, "a") == progressAdv) {
                        widget = wd;
                    }
                }
                String progress = widget == null ? null : (String) fieldOf(widget.getClass(), widget, "progress");
                float fraction = widget == null ? -1.0F : ((Float) fieldOf(widget.getClass(), widget, "fraction")).floatValue();
                result("progress: the L screen draws and the widget shows n/m", screen && widget != null && (progressN + "/" + progressAdv.total()).equals(progress) && Math.abs(fraction - (float) progressN / progressAdv.total()) < 0.001F,
                        "screen=" + screen + " widget=" + (widget != null) + " text=" + progress + " fraction=" + fraction + " keyL=" + (DataClient.keyCode() == Keyboard.KEY_L));
                if (widget != null) {
                    int wx = ((Integer) fieldOf(widget.getClass(), widget, "x")).intValue();
                    int wy = ((Integer) fieldOf(widget.getClass(), widget, "y")).intValue();
                    Field sx = sel.getClass().getDeclaredField("scrollX");
                    Field sy = sel.getClass().getDeclaredField("scrollY");
                    sx.setAccessible(true);
                    sy.setAccessible(true);
                    sx.setInt(sel, 117 - wx - 13);
                    sy.setInt(sel, 56 - wy - 13);
                    int left = (mc.currentScreen.width - 252) / 2;
                    int top = (mc.currentScreen.height - 140) / 2;
                    mouseTo(left + 9 + 117, top + 18 + 56);
                }
            }
        });
        client(40, new X() {
            @Override
            void go() throws Exception {
                shot("gui_progress");
                log("INFO progress screen still open=" + (mc.currentScreen instanceof DataClient.Screen));
                mc.displayGuiScreen(null);
                fakeOn = false;
            }
        });
        // ---- 7. challenge toast sound ----
        client(5, new X() {
            @Override
            void go() throws Exception {
                ResourceLocation rl = new ResourceLocation("mc112bridge", "ui.toast.challenge_complete");
                result("toast sound: ui.toast.challenge_complete is registered in the sound manager", mc.getSoundHandler().getSound(rl) != null, "");
                mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(rl, 1.0F));
            }
        });
        server(5, new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                try {
                    for (String c : ((java.util.Map<String, Object>) fieldOf(Data112.Advancement.class, progressAdv, "criteria")).keySet()) {
                        Data112.grant(p(), progressAdv, c);
                    }
                    Data112.sync(p());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        client(60, new X() {
            @Override
            @SuppressWarnings("unchecked")
            void go() throws Exception {
                result("toast sound: completing a challenge syncs it done (toast plays the sound)", ((java.util.Set<String>) dc("DONE")).contains(progressAdv.id), "");
            }
        });
        finish();
    }

    private static Object fieldOf(Class<?> c, Object o, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

}
