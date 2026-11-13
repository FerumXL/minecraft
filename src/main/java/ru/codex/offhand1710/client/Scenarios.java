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
        if ("more".equals(STAGE) || "more2".equals(STAGE) || "scene".equals(STAGE) || "bench".equals(STAGE) || "extra".equals(STAGE) || "gui".equals(STAGE) || "terrain".equals(STAGE)) {
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
        run(false);
    }

    @SubscribeEvent
    public void onServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && started) {
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
    private void terrain() {
        final String gen = System.getenv("MC112_GEN");
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
