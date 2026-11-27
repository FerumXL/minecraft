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
    private long launchedAt;
    private int reconnects;

    /**
     * The local login sometimes never finishes (the server runs, the client sits on the main menu; seen 2026-11-22).
     * After 40 s the client opens a new local connection to the same server, at most twice, and says so in the log.
     */
    private boolean retryConnect() {
        if (reconnects >= 2 || System.currentTimeMillis() - launchedAt < 40000L * (reconnects + 1)) {
            return false;
        }
        net.minecraft.server.integrated.IntegratedServer server = mc.getIntegratedServer();
        if (server == null || !server.isServerRunning()) {
            return false;
        }
        reconnects++;
        try {
            java.lang.reflect.Field f = ReflectionHelper.findField(net.minecraft.client.Minecraft.class, "field_71453_ak", "myNetworkManager");
            net.minecraft.network.NetworkManager old = (net.minecraft.network.NetworkManager) f.get(mc);
            log("INFO local login stuck after " + (System.currentTimeMillis() - launchedAt) / 1000L + " s: screen="
                + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName()) + " handler="
                + (old == null || old.getNetHandler() == null ? "none" : old.getNetHandler().getClass().getSimpleName())
                + " open=" + (old != null && old.isChannelOpen()) + "; reconnecting (" + reconnects + ")");
            if (old != null) {
                old.closeChannel(new net.minecraft.util.ChatComponentText("retry"));
            }
            java.net.SocketAddress address = server.func_147137_ag().addLocalEndpoint();
            net.minecraft.network.NetworkManager nm = net.minecraft.network.NetworkManager.provideLocalClient(address);
            nm.setNetHandler(new net.minecraft.client.network.NetHandlerLoginClient(nm, mc, null));
            nm.scheduleOutboundPacket(new net.minecraft.network.handshake.client.C00Handshake(5, address.toString(), 0, net.minecraft.network.EnumConnectionState.LOGIN));
            nm.scheduleOutboundPacket(new net.minecraft.network.login.client.C00PacketLoginStart(mc.getSession().func_148256_e()));
            f.set(mc, nm);
        } catch (Exception e) {
            log("INFO reconnect failed: " + e);
        }
        return true;
    }
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
        if ("more".equals(STAGE) || "more2".equals(STAGE) || "scene".equals(STAGE) || "bench".equals(STAGE) || "extra".equals(STAGE) || "gui".equals(STAGE) || "terrain".equals(STAGE) || "backlog1".equals(STAGE) || "villager".equals(STAGE) || "elytra".equals(STAGE) || "group2".equals(STAGE) || "registry".equals(STAGE) || "sounds".equals(STAGE) || "idle".equals(STAGE) || "selectors".equals(STAGE) || "boats".equals(STAGE) || "gamerules".equals(STAGE) || "names".equals(STAGE) || "commands2".equals(STAGE) || "border".equals(STAGE) || "crafting".equals(STAGE) || "specchunks".equals(STAGE) || "dragon".equals(STAGE) || "food".equals(STAGE) || "armor".equals(STAGE) || "brewing".equals(STAGE) || "visual".equals(STAGE) || "online".equals(STAGE)) {
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

    /** A player command from the client: it goes to the dedicated server as chat. */
    private void say(String c) {
        mc.thePlayer.sendChatMessage(c);
    }

    private int clientCount(net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            n += s != null && s.getItem() == item ? s.stackSize : 0;
        }
        return n;
    }

    /** Stage online: only what crosses the network is checked, from the client side. */
    private void online() {
        client(40, new Runnable() {
            @Override
            public void run() {
                result("the client plays on a dedicated server", !mc.isSingleplayer() && mc.theWorld != null && MinecraftServer.getServer() == null,
                    "singleplayer=" + mc.isSingleplayer());
                say("/gamemode 1");
                say("/clear");
                say("/replaceitem entity @p slot.weapon.offhand diamond 5");
                say("/give @p iron_ingot 7");
                say("/title @p title {\"text\":\"online\"}");
                say("/summon Chicken ~ ~1 ~2 {NoAI:1b}");
                say("/worldborder set 200");
            }
        });
        client(60, new Runnable() {
            @Override
            public void run() {
                ItemStack off = ru.codex.offhand1710.OffhandInventory.get(mc.thePlayer).getStack();
                result("offhand set by the server reaches the client (Sync)", off != null && off.getItem() == Items.diamond && off.stackSize == 5, "offhand=" + off);
                result("/give of 1.12.2 fills the client inventory", clientCount(Items.iron_ingot) == 7, "iron=" + clientCount(Items.iron_ingot));
                String title = "";
                try {
                    java.lang.reflect.Field f = TitleOverlay.class.getDeclaredField("title");
                    f.setAccessible(true);
                    title = String.valueOf(f.get(null));
                } catch (ReflectiveOperationException e) {
                    title = e.toString();
                }
                result("/title reaches the client (TitleMessage)", title.contains("online"), "title=" + title);
                int chickens = 0;
                for (Object o : mc.theWorld.loadedEntityList) {
                    chickens += o instanceof net.minecraft.entity.passive.EntityChicken && ((net.minecraft.entity.Entity) o).getDistanceToEntity(mc.thePlayer) < 6 ? 1 : 0;
                }
                result("a summoned mob is seen by the client", chickens >= 1, "chickens=" + chickens);
                // client -> server: the F key's swap request
                ru.codex.offhand1710.MC112Bridge.NETWORK.channel.sendToServer(new ru.codex.offhand1710.OffhandMessages.SwapRequest());
            }
        });
        client(40, new Runnable() {
            @Override
            public void run() {
                ItemStack main = mc.thePlayer.getHeldItem();
                result("the swap request is done by the server and synced back", main != null && main.getItem() == Items.diamond, "main=" + main
                    + " offhand=" + ru.codex.offhand1710.OffhandInventory.get(mc.thePlayer).getStack());
                say("/kill @e[type=Chicken]");
                say("/clear");
                say("/worldborder set 60000000");
            }
        });
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
        if (launched && !started && mc.theWorld == null && retryConnect()) {
            return;
        }
        if (!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched = true;
            launchedAt = System.currentTimeMillis();
            // watchdog: a world that does not open in 90 s gets every thread's stack in the log
            Thread dog = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(90000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!started) {
                        log("FAIL world did not open in 90 s; screen=" + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName())
                            + " world=" + (mc.theWorld != null) + " player=" + (mc.thePlayer != null)
                            + " server=" + (MinecraftServer.getServer() != null && MinecraftServer.getServer().isServerRunning()) + "; threads:");
                        for (java.util.Map.Entry<Thread, StackTraceElement[]> en : Thread.getAllStackTraces().entrySet()) {
                            StringBuilder sb = new StringBuilder("THREAD " + en.getKey().getName() + " " + en.getKey().getState());
                            for (StackTraceElement el : en.getValue()) {
                                sb.append(" | at ").append(el);
                            }
                            log(sb.toString());
                        }
                    }
                }
            }, "mc112-watchdog");
            dog.setDaemon(true);
            dog.start();
            mc.gameSettings.pauseOnLostFocus = false;
            String rd = System.getenv("MC112_BENCH_RD");
            mc.gameSettings.renderDistanceChunks = rd == null ? 8 : Integer.parseInt(rd);
            if ("online".equals(STAGE)) {
                // a dedicated server (MC112_SERVER, default localhost:25565) instead of an integrated one
                String addr = System.getenv("MC112_SERVER") == null ? "localhost:25565" : System.getenv("MC112_SERVER");
                // FML sets up its server list (and the latch the handshake waits on) only from the multiplayer screen
                cpw.mods.fml.client.FMLClientHandler.instance().setupServerList();
                cpw.mods.fml.client.FMLClientHandler.instance().connectToServer(mc.currentScreen, new net.minecraft.client.multiplayer.ServerData("mc112", addr));
                return;
            }
            if ("terrain".equals(STAGE) || "idle".equals(STAGE)) {
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
        if (mc.thePlayer == null || !"online".equals(STAGE) && (MinecraftServer.getServer() == null || MinecraftServer.getServer().getConfigurationManager().playerEntityList.isEmpty())) {
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
        } else if ("registry".equals(STAGE)) {
            registry();
        } else if ("sounds".equals(STAGE)) {
            sounds();
        } else if ("idle".equals(STAGE)) {
            idle();
        } else if ("selectors".equals(STAGE)) {
            selectors();
        } else if ("boats".equals(STAGE)) {
            boatsStage();
        } else if ("gamerules".equals(STAGE)) {
            gamerules();
        } else if ("names".equals(STAGE)) {
            names();
        } else if ("commands2".equals(STAGE)) {
            commands2();
        } else if ("border".equals(STAGE)) {
            border();
        } else if ("crafting".equals(STAGE)) {
            crafting();
        } else if ("specchunks".equals(STAGE)) {
            spectatorChunks();
        } else if ("dragon".equals(STAGE)) {
            dragonStage();
        } else if ("food".equals(STAGE)) {
            foodStage();
        } else if ("armor".equals(STAGE)) {
            armorStage();
        } else if ("brewing".equals(STAGE)) {
            brewingStage();
        } else if ("visual".equals(STAGE)) {
            visual();
        } else if ("online".equals(STAGE)) {
            online();
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
                    final int[] box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
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
                                                if (n.contains("brick_concrete") || n.contains("deco_steel") || n.contains("fence_metal") || n.contains("yellow_barrel") || n.contains("brick_light")) {
                                                    int wx = cx * 16 + bx, wz = cz * 16 + bz;
                                                    box[0] = Math.min(box[0], wx); box[1] = Math.min(box[1], by); box[2] = Math.min(box[2], wz);
                                                    box[3] = Math.max(box[3], wx); box[4] = Math.max(box[4], by); box[5] = Math.max(box[5], wz);
                                                }
                                                Integer k = names.get(n);
                                                names.put(n, k == null ? 1 : k + 1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    log("TERRAIN hbmStarts blocked=" + ru.codex.offhand1710.HbmVillageGuard.blocked + " dungeonsBlocked=" + ru.codex.offhand1710.HbmVillageGuard.dungeonsBlocked + " " + x0 + "," + z0 + " " + hbmStarts(x0 * 16, z0 * 16, x1 * 16 + 15, z1 * 16 + 15));
                    log("TERRAIN hbmBuilt " + java.util.Arrays.toString(box));
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
        // the spectator menu (GuiSpectator / SpectatorMenu of 1.12.2)
        client(5, new Runnable() {
            @Override
            public void run() {
                SpectatorMenu.open = false;
                SpectatorMenu.press(0);
                boolean first = SpectatorMenu.open && SpectatorMenu.selected == 0 && SpectatorMenu.category == null;
                SpectatorMenu.press(0);
                boolean picked = "".equals(SpectatorMenu.category);
                SpectatorMenu.press(8);
                SpectatorMenu.press(8);
                boolean closed = !SpectatorMenu.open;
                SpectatorMenu.press(1);
                result("spectator menu: a number selects, the same number picks (players), slot 9 closes", first && picked && closed && SpectatorMenu.open && SpectatorMenu.selected == 1,
                    "first=" + first + " picked=" + picked + " closed=" + closed);
            }
        });
        client(3, new Runnable() {
            @Override
            public void run() {
                shot("g2-spectator-menu");
                SpectatorMenu.open = false;
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
                for (Object o : w().getEntitiesWithinAABB(net.minecraft.entity.passive.EntityHorse.class, trap[0].boundingBox.expand(24.0D, 200.0D, 24.0D))) {
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
        // structure block: save, detect size from corners, load rotated by 90 degrees, load on redstone, the window
        final ru.codex.offhand1710.StructureBlock112.Tile[] sb = new ru.codex.offhand1710.StructureBlock112.Tile[2];
        server(5, new Runnable() {
            @Override
            public void run() {
                net.minecraft.block.Block sbk = ru.codex.offhand1710.StructureBlock112.structureBlock;
                int ax = bx - 9, az = bz - 9;
                for (int x = ax - 1; x <= ax + 12; x++) {
                    for (int z = az - 1; z <= az + 5; z++) {
                        for (int y = dy; y < dy + 4; y++) {
                            w().setBlockToAir(x, y, z);
                        }
                    }
                }
                w().setBlock(ax, dy, az, sbk, 0, 3);
                sb[0] = (ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax, dy, az);
                sb[0].mode = 0;
                sb[0].name = "test:house";
                sb[0].ignoreEntities = false;
                // corners at both ends of the 3x2x3 box (one outside each corner), as in 1.12.2
                w().setBlock(ax, dy - 1, az - 1, sbk, 2, 3);
                ((ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax, dy - 1, az - 1)).mode = 2;
                ((ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax, dy - 1, az - 1)).name = "test:house";
                w().setBlock(ax + 4, dy + 2, az + 3, sbk, 2, 3);
                ((ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax + 4, dy + 2, az + 3)).mode = 2;
                ((ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax + 4, dy + 2, az + 3)).name = "test:house";
                boolean detected = sb[0].detectSize();
                result("DETECT finds the box between the corner blocks", detected && sb[0].posX == 1 && sb[0].posY == 0 && sb[0].posZ == 0 && sb[0].sizeX == 3 && sb[0].sizeY == 2 && sb[0].sizeZ == 3,
                    "pos=" + sb[0].posX + "," + sb[0].posY + "," + sb[0].posZ + " size=" + sb[0].sizeX + "," + sb[0].sizeY + "," + sb[0].sizeZ);
                w().setBlock(ax + 1, dy, az, Blocks.oak_stairs, 0, 3);
                w().setBlock(ax + 3, dy, az + 2, Blocks.chest, 0, 3);
                ((net.minecraft.tileentity.TileEntityChest) w().getTileEntity(ax + 3, dy, az + 2)).setInventorySlotContents(0, new ItemStack(Items.diamond, 7));
                w().setBlock(ax + 2, dy + 1, az + 1, Blocks.glass, 0, 3);
                w().setBlock(ax + 2, dy, az + 1, ru.codex.offhand1710.StructureBlock112.structureVoid, 0, 3);
                net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(w());
                pig.setLocationAndAngles(ax + 2.5D, dy, az + 1.5D, 0.0F, 0.0F);
                pig.setCustomNameTag("StructPig");
                w().spawnEntityInWorld(pig);
                boolean saved = sb[0].save(true);
                java.io.File f = new java.io.File(w().getChunkSaveLocation(), "structures/house.nbt");
                NBTTagCompound t = null;
                try {
                    t = net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.FileInputStream(f));
                } catch (Exception ignored) {
                }
                result("SAVE writes structures/house.nbt (namespace dropped as in 1.12) with size, palette, blocks, entities and no void cell", saved && t != null
                    && t.getTagList("size", 3).tagCount() == 3 && t.getTagList("blocks", 10).tagCount() == 17 && t.getTagList("entities", 10).tagCount() == 1,
                    "saved=" + saved + " exists=" + f.exists() + (t == null ? "" : " blocks=" + t.getTagList("blocks", 10).tagCount() + " entities=" + t.getTagList("entities", 10).tagCount()));
                if (t != null) {
                    net.minecraft.nbt.NBTTagList es = t.getTagList("entities", 10);
                    for (int i = 0; i < es.tagCount(); i++) {
                        log("INFO saved entity " + es.getCompoundTagAt(i).getCompoundTag("nbt").getString("id") + " " + es.getCompoundTagAt(i).getTagList("pos", 6));
                    }
                }
                pig.setDead();
                // load at a second block 8 east, turned clockwise 90
                w().setBlock(ax + 8, dy, az, sbk, 1, 3);
                sb[1] = (ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(ax + 8, dy, az);
                sb[1].mode = 1;
                sb[1].name = "test:house";
                sb[1].posX = 3;
                sb[1].posY = 0;
                sb[1].posZ = 0;
                sb[1].rotation = 1;
                sb[1].ignoreEntities = false;
                boolean loaded = sb[1].load(true);
                int lx = ax + 8 + 3, lz = az;
                // rotation 90: (x, y, z) -> (-z, y, x)
                boolean chest = w().getBlock(lx - 2, dy, lz + 2) == Blocks.chest && w().getTileEntity(lx - 2, dy, lz + 2) instanceof net.minecraft.tileentity.TileEntityChest
                    && ((net.minecraft.tileentity.TileEntityChest) w().getTileEntity(lx - 2, dy, lz + 2)).getStackInSlot(0) != null
                    && ((net.minecraft.tileentity.TileEntityChest) w().getTileEntity(lx - 2, dy, lz + 2)).getStackInSlot(0).stackSize == 7;
                boolean glass = w().getBlock(lx - 1, dy + 1, lz + 1) == Blocks.glass;
                boolean stairs = w().getBlock(lx, dy, lz) == Blocks.oak_stairs && w().getBlockMetadata(lx, dy, lz) != 0;
                boolean pigBack = !w().getEntitiesWithinAABB(net.minecraft.entity.passive.EntityPig.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(lx - 4, dy, lz - 1, lx + 1, dy + 3, lz + 4)).isEmpty();
                result("LOAD rotated 90: chest with its 7 diamonds, glass and stairs moved, stairs turned, the pig comes back",
                    loaded && chest && glass && stairs && pigBack, "loaded=" + loaded + " chest=" + chest + " glass=" + glass + " stairs=" + stairs
                    + " stairsMeta=" + w().getBlockMetadata(lx, dy, lz) + " pig=" + pigBack);
                // redstone: clear the copy, then a redstone block next to the load block loads it again
                w().setBlockToAir(lx - 1, dy + 1, lz + 1);
                w().setBlock(ax + 8, dy + 1, az, Blocks.redstone_block, 0, 3);
                boolean again = w().getBlock(lx - 1, dy + 1, lz + 1) == Blocks.glass;
                NBTTagCompound tag = new NBTTagCompound();
                sb[1].writeToNBT(tag);
                result("a redstone pulse loads again; tile NBT has the 1.12 keys", again && "LOAD".equals(tag.getString("mode")) && "CLOCKWISE_90".equals(tag.getString("rotation"))
                    && tag.getBoolean("powered") && "test:house".equals(tag.getString("name")), "again=" + again + " mode=" + tag.getString("mode") + " rotation=" + tag.getString("rotation"));
                tp(ax + 4.5D, dy + 1.0D, az + 7.5D, 180.0F, 30.0F);
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                net.minecraft.tileentity.TileEntity te = mc.theWorld.getTileEntity(sb[0].xCoord, sb[0].yCoord, sb[0].zCoord);
                shot("g2-structure-box");
                if (te instanceof ru.codex.offhand1710.StructureBlock112.Tile) {
                    mc.displayGuiScreen(new StructureGui((ru.codex.offhand1710.StructureBlock112.Tile) te));
                }
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                result("the structure block window opens in save mode", mc.currentScreen instanceof StructureGui, String.valueOf(mc.currentScreen));
                shot("g2-structure-gui");
                mc.displayGuiScreen(null);
            }
        });
        // a real 1.12.2 template (endcity/ship.nbt from the installed 1.12.2 jar), loaded plain and turned + mirrored:
        // every block must come out with the Name and Properties the template has
        server(5, new Runnable() {
            @Override
            public void run() {
                java.io.File jar = new java.io.File(System.getenv("APPDATA"), ".minecraft/versions/1.12.2/1.12.2.jar");
                NBTTagCompound tpl = null;
                try {
                    java.util.zip.ZipFile z = new java.util.zip.ZipFile(jar);
                    java.io.InputStream in = z.getInputStream(z.getEntry("assets/minecraft/structures/endcity/ship.nbt"));
                    java.io.File out = new java.io.File(w().getChunkSaveLocation(), "structures/ship112.nbt");
                    out.getParentFile().mkdirs();
                    java.nio.file.Files.copy(in, out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    in.close();
                    z.close();
                    tpl = net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.FileInputStream(out));
                } catch (Exception ex) {
                    log("FAIL ship112 " + ex);
                    return;
                }
                int[][] cases = {{0, 0}, {1, 1}};
                for (int c = 0; c < 2; c++) {
                    int bxs = bx - 40 + c * 80, bzs = bz + 40;
                    w().setBlock(bxs, dy + 30, bzs, ru.codex.offhand1710.StructureBlock112.structureBlock, 1, 3);
                    ru.codex.offhand1710.StructureBlock112.Tile st = (ru.codex.offhand1710.StructureBlock112.Tile) w().getTileEntity(bxs, dy + 30, bzs);
                    st.mode = 1;
                    st.name = "ship112";
                    st.posX = 0;
                    st.posY = 1;
                    st.posZ = 0;
                    st.rotation = cases[c][0];
                    st.mirror = cases[c][1];
                    boolean loaded = st.load(true);
                    NBTTagList pal = tpl.getTagList("palette", 10);
                    NBTTagList blocks = tpl.getTagList("blocks", 10);
                    int checked = 0, bad = 0;
                    StringBuilder ex = new StringBuilder();
                    for (int i = 0; i < blocks.tagCount(); i++) {
                        NBTTagCompound e = blocks.getCompoundTagAt(i);
                        NBTTagList pl = (NBTTagList) e.getTagList("pos", 3).copy();
                        int[] rel = {((net.minecraft.nbt.NBTTagInt) pl.removeTag(0)).func_150287_d(), ((net.minecraft.nbt.NBTTagInt) pl.removeTag(0)).func_150287_d(), ((net.minecraft.nbt.NBTTagInt) pl.removeTag(0)).func_150287_d()};
                        int[] wpos = st.transformPublic(rel[0], rel[1], rel[2]);
                        int x = bxs + wpos[0], y = dy + 31 + wpos[1], z = bzs + wpos[2];
                        NBTTagCompound p = pal.getCompoundTagAt(e.getInteger("state"));
                        String name = p.getString("Name");
                        java.util.Map<String, String> want = ru.codex.offhand1710.States112.transform(
                            p.hasKey("Properties") ? ru.codex.offhand1710.States112.fromTag(p.getCompoundTag("Properties")) : new java.util.TreeMap<String, String>(), cases[c][1], cases[c][0]);
                        net.minecraft.block.Block b = w().getBlock(x, y, z);
                        int meta = w().getBlockMetadata(x, y, z);
                        String got = ru.codex.offhand1710.States112.name112(b, meta);
                        java.util.Map<String, String> gp = got == null ? null : ru.codex.offhand1710.States112.props(got, meta);
                        checked++;
                        // properties outside the metadata (stair shape, fence sides...) are not stored by 1.12.2 either
                        boolean ok = name.equals(got);
                        if (ok && gp != null) {
                            for (java.util.Map.Entry<String, String> en : want.entrySet()) {
                                if (gp.containsKey(en.getKey()) && !gp.get(en.getKey()).equals(en.getValue())
                                    && ru.codex.offhand1710.States112.stored(name, en.getKey())) {
                                    ok = false;
                                }
                            }
                        }
                        if (!ok) {
                            bad++;
                            if (ex.length() < 300) {
                                ex.append(' ').append(name).append(want).append("->").append(got).append(gp);
                            }
                        }
                    }
                    result("1.12.2 template endcity/ship.nbt " + (c == 0 ? "as is" : "turned 90 and mirrored") + ": every block with its 1.12.2 name and properties",
                        loaded && bad == 0 && st.unknownStates == 0 && checked > 500, "loaded=" + loaded + " checked=" + checked + " wrong=" + bad + " unknown=" + st.unknownStates + ex);
                }
                // what this mod writes: 1.12.2 names and properties (oak stairs of the saved house)
                try {
                    NBTTagCompound saved = net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.FileInputStream(new java.io.File(w().getChunkSaveLocation(), "structures/house.nbt")));
                    NBTTagList pal = saved.getTagList("palette", 10);
                    String stairs = null;
                    for (int i = 0; i < pal.tagCount(); i++) {
                        if ("minecraft:oak_stairs".equals(pal.getCompoundTagAt(i).getString("Name"))) {
                            stairs = pal.getCompoundTagAt(i).toString();
                        }
                    }
                    result("saved palette has 1.12.2 names and Properties", stairs != null && stairs.contains("facing:") && stairs.contains("half:"), String.valueOf(stairs));
                } catch (Exception ex) {
                    result("saved palette has 1.12.2 names and Properties", false, ex.toString());
                }
            }
        });
        // chase speeds of the old-AI mobs against EntityAIAttackMelee 1.0 x movementSpeed of 1.12.2
        final String[] names = {"spider", "cave spider", "angry pigman", "silverfish"};
        final double[] want = {walkSpeed(0.3D), walkSpeed(0.3D), walkSpeed(0.28D), walkSpeed(0.25D)};
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("time set 18000");
                cmd("gamemode 0");
                cmd("difficulty 2");
                fly(false);
                for (int x = bx - 2; x <= bx + 2; x++) {
                    for (int z = bz - 12; z <= bz + 12; z++) {
                        // sandstone: a silverfish would hide in stone
                        w().setBlock(x, dy - 1, z, Blocks.sandstone, 0, 2);
                        for (int y = dy; y < dy + 4; y++) {
                            w().setBlockToAir(x, y, z);
                        }
                    }
                }
                // a glass niche keeps the player from being knocked off the platform
                for (int y = dy; y < dy + 3; y++) {
                    w().setBlock(bx - 1, y, bz - 12, Blocks.glass, 0, 2);
                    w().setBlock(bx + 1, y, bz - 12, Blocks.glass, 0, 2);
                    w().setBlock(bx, y, bz - 13, Blocks.glass, 0, 2);
                }
                tp(bx + 0.5D, dy, bz - 11.5D, 0.0F, 0.0F);
                p().addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.resistance.id, 2000, 10));
                p().addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.regeneration.id, 2000, 5));
            }
        });
        for (int k = 0; k < 4; k++) {
            final int i = k;
            server(5, new Runnable() {
                @Override
                public void run() {
                    endSpeeds();
                    tp(bx + 0.5D, dy, bz - 11.5D, 0.0F, 0.0F);
                    p().setHealth(p().getMaxHealth());
                    net.minecraft.entity.EntityLiving e = i == 0 ? new net.minecraft.entity.monster.EntitySpider(w()) : i == 1 ? new net.minecraft.entity.monster.EntityCaveSpider(w())
                        : i == 2 ? new net.minecraft.entity.monster.EntityPigZombie(w()) : new net.minecraft.entity.monster.EntitySilverfish(w());
                    lane(e, names[i], bx + 0.5D, dy, bz + 11.5D + 6.0D, 0.0D);
                    e.setLocationAndAngles(bx + 0.5D, dy, bz + 2.5D, 180.0F, 0.0F);
                    if (i == 2) {
                        e.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(p()), 0.0F);
                    }
                    log("INFO speed " + names[i] + " movementSpeed=" + e.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).getAttributeValue());
                }
            });
            server(80, new Runnable() {
                @Override
                public void run() {
                    Speed s = speed(names[i]);
                    List<Double> mv = new java.util.ArrayList<Double>();
                    for (int j = 1; j < s.d.size(); j++) {
                        if (s.d.get(j) > 0.02D && s.d.get(j) < 1.0D) {
                            mv.add(s.d.get(j));
                        }
                    }
                    java.util.Collections.sort(mv);
                    double med = mv.isEmpty() ? 0.0D : mv.get(mv.size() / 2);
                    int near = 0;
                    for (double v : mv) {
                        near += Math.abs(v - want[i]) < 0.008D ? 1 : 0;
                    }
                    // steady walking ticks at the 1.12.2 speed; leaps and turns at the target make the rest
                    result("chase speed of the " + names[i] + " as 1.12.2", mv.size() > 30 && near * 10 >= mv.size() * 3,
                        String.format("median=%.4f p90=%.4f want=%.4f movingTicks=%d dist=%.1f near=" + near, med, mv.isEmpty() ? 0.0D : mv.get((int) (mv.size() * 0.9D)), want[i], mv.size(), s.e.getDistanceToEntity(p())));
                    s.e.setDead();
                }
            });
        }
        // end gateway exit (TileEntityEndGateway of 1.12.2): out to the islands ~1024 blocks away and exactly back
        final double[] gwr = new double[4];
        server(5, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                p().travelToDimension(1);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
                ru.codex.offhand1710.End112.buildGateway(end, 96, 75, 0);
                net.minecraft.block.Block g = end.getBlock(96, 75, 0);
                g.onEntityCollidedWithBlock(end, 96, 75, 0, p());
                ru.codex.offhand1710.End112.GatewayTile tile = (ru.codex.offhand1710.End112.GatewayTile) end.getTileEntity(96, 75, 0);
                int[] ex = tile.exit;
                net.minecraft.tileentity.TileEntity back = ex == null ? null : end.getTileEntity(ex[0], ex[1], ex[2]);
                double r = Math.sqrt(p().posX * p().posX + p().posZ * p().posZ);
                boolean standing = end.getBlock(net.minecraft.util.MathHelper.floor_double(p().posX), net.minecraft.util.MathHelper.floor_double(p().posY) - 1, net.minecraft.util.MathHelper.floor_double(p().posZ)).isNormalCube();
                boolean link = back instanceof ru.codex.offhand1710.End112.GatewayTile && ((ru.codex.offhand1710.End112.GatewayTile) back).exact
                    && java.util.Arrays.equals(((ru.codex.offhand1710.End112.GatewayTile) back).exit, new int[] {96, 75, 0});
                result("end gateway: out ~1024 blocks along its direction onto a solid block; a return gateway 10 above the island top leads back exactly",
                    ex != null && r > 900.0D && r < 1400.0D && Math.abs(p().posZ) < 40.0D && standing && link && tile.cooldown > 0,
                    String.format("pos=%.1f,%.1f,%.1f r=%.0f standing=%s exit=%s link=%s", p().posX, p().posY, p().posZ, r, standing, java.util.Arrays.toString(ex), link));
                gwr[0] = ex == null ? 0 : ex[0];
                gwr[1] = ex == null ? 0 : ex[1];
                gwr[2] = ex == null ? 0 : ex[2];
            }
        });
        server(45, new Runnable() {
            @Override
            public void run() {
                WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
                int x = (int) gwr[0], y = (int) gwr[1], z = (int) gwr[2];
                end.getBlock(x, y, z).onEntityCollidedWithBlock(end, x, y, z, p());
                result("end gateway: the return gateway brings the player back to the first one", Math.abs(p().posX - 96.5D) < 1.0D && Math.abs(p().posZ - 0.5D) < 1.0D,
                    String.format("pos=%.1f,%.1f,%.1f", p().posX, p().posY, p().posZ));
                // leaving the End the way the exit portal does it (credits, then respawn), not travelToDimension(0)
                MinecraftServer.getServer().getConfigurationManager().respawnPlayer(p(), 0, true);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                tp(bx + 0.5D, dy, bz + 0.5D, 0.0F, 0.0F);
            }
        });
        // blaze (AIFireballAttack of 1.12.2): finds a player 23 blocks away and hovers there shooting, no horizontal approach
        final net.minecraft.entity.monster.EntityBlaze[] blaze = new net.minecraft.entity.monster.EntityBlaze[1];
        server(5, new Runnable() {
            @Override
            public void run() {
                endSpeeds();
                cmd("gamemode 0");
                cmd("time set 18000");
                // night: no other monster may pick a fight with the blaze and become its target
                cmd("gamerule doMobSpawning false");
                for (Object o : w().getEntitiesWithinAABB(net.minecraft.entity.monster.EntityMob.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(bx - 64, dy - 32, bz - 64, bx + 64, dy + 32, bz + 64))) {
                    ((net.minecraft.entity.Entity) o).setDead();
                }
                tp(bx + 0.5D, dy, bz - 11.5D, 0.0F, 0.0F);
                p().setHealth(p().getMaxHealth());
                p().addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.fireResistance.id, 2000, 0));
                blaze[0] = new net.minecraft.entity.monster.EntityBlaze(w());
                lane(blaze[0], "blaze", bx + 0.5D, dy + 3, bz + 24.5D, 0.0D);
                blaze[0].setLocationAndAngles(bx + 0.5D, dy, bz + 11.5D, 180.0F, 0.0F);
            }
        });
        server(100, new Runnable() {
            @Override
            public void run() {
                Speed s = speed("blaze");
                double moved = 0.0D;
                for (int j = 1; j < s.d.size(); j++) {
                    moved += s.d.get(j) < 1.0D ? s.d.get(j) : 0.0D;
                }
                net.minecraft.entity.Entity t = blaze[0].getEntityToAttack();
                result("blaze targets a player 23 blocks away (followRange 48, 1.7.10: 16) and hovers without closing in", t == p() && moved < 1.5D,
                    String.format("target=%s horizontalPath=%.2f dist=%.1f", t == null ? "none" : t.getCommandSenderName(), moved, blaze[0].getDistanceToEntity(p()))
                    + " vulnerable=" + w().getClosestVulnerablePlayerToEntity(blaze[0], 48.0D) + " seen=" + blaze[0].canEntityBeSeen(p()) + " dim=" + p().dimension + " blazeDim=" + blaze[0].dimension + " disableDamage=" + p().capabilities.disableDamage + " sameWorld=" + (p().worldObj == blaze[0].worldObj) + " inList=" + w().playerEntities.contains(p()) + " listSize=" + w().playerEntities.size() + " alive=" + p().isEntityAlive() + " invisible=" + p().isInvisible() + " pWorldIsW=" + (p().worldObj == w()));
                blaze[0].setDead();
                endSpeeds();
                cmd("gamemode 1");
                cmd("time set 6000");
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

    /** Resolves every item of a 1.12.2 registry dump (MC112_ITEMS: "item	name	meta	unlocalized	tab") through the mod's 1.12 names. */
    private void registry() {
        server(20, new Runnable() {
            @Override
            public void run() {
                int total = 0;
                int missing = 0;
                int renamed = 0;
                try {
                    java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(System.getenv("MC112_ITEMS")), "UTF-8"));
                    for (String l; (l = r.readLine()) != null;) {
                        String[] f = l.split("	");
                        if (!"item".equals(f[0])) {
                            continue;
                        }
                        total++;
                        ItemStack s = ru.codex.offhand1710.Data112.item(f[1], Integer.parseInt(f[2]));
                        if (s == null) {
                            missing++;
                            log("REG MISSING " + f[1] + " " + f[2] + " " + f[3] + " " + f[4]);
                            continue;
                        }
                        String ours;
                        try {
                            ours = s.getUnlocalizedName();
                        } catch (Throwable e) {
                            ours = "!" + e;
                        }
                        String a = f[3].substring(f[3].lastIndexOf('.') + 1);
                        String b = ours.substring(ours.lastIndexOf('.') + 1);
                        if (!a.equalsIgnoreCase(b)) {
                            renamed++;
                            log("REG NAME " + f[1] + " " + f[2] + " " + f[3] + " -> " + ours + " " + Item.itemRegistry.getNameForObject(s.getItem()) + ":" + s.getItemDamage());
                        }
                    }
                    r.close();
                } catch (IOException e) {
                    log("FAIL registry " + e);
                }
                log("REG total=" + total + " missing=" + missing + " renamed=" + renamed);
                // 1.12 fortress chests: real Corridor pieces with their chest forced on
                int chests = 0;
                int obsidian = 0;
                int most = 0;
                int fewest = 99;
                java.util.Random rand = new java.util.Random(112L);
                try {
                    java.lang.reflect.Field made = null;
                    for (java.lang.reflect.Field f : net.minecraft.world.gen.structure.StructureNetherBridgePieces.Corridor.class.getDeclaredFields()) {
                        if (f.getType() == boolean.class) {
                            made = f;
                        }
                    }
                    made.setAccessible(true);
                    for (int i = 0; i < 200; i++) {
                        int x = 3000 + (i % 20) * 8;
                        int z = 3000 + (i / 20) * 8;
                        net.minecraft.world.gen.structure.StructureNetherBridgePieces.Corridor c = net.minecraft.world.gen.structure.StructureNetherBridgePieces.Corridor
                            .createValidComponent(new java.util.ArrayList(), rand, x, 100, z, 0, 0);
                        made.setBoolean(c, true);
                        net.minecraft.world.gen.structure.StructureBoundingBox box = c.getBoundingBox();
                        c.addComponentParts(w(), rand, new net.minecraft.world.gen.structure.StructureBoundingBox(box.minX - 1, box.minY - 1, box.minZ - 1, box.maxX + 1, box.maxY + 1, box.maxZ + 1));
                        for (int bx = box.minX; bx <= box.maxX; bx++) {
                            for (int by = box.minY; by <= box.maxY; by++) {
                                for (int bz = box.minZ; bz <= box.maxZ; bz++) {
                                    net.minecraft.tileentity.TileEntity te = w().getTileEntity(bx, by, bz);
                                    if (te instanceof net.minecraft.tileentity.TileEntityChest) {
                                        net.minecraft.tileentity.TileEntityChest chest = (net.minecraft.tileentity.TileEntityChest) te;
                                        int stacks = 0;
                                        for (int s = 0; s < chest.getSizeInventory(); s++) {
                                            ItemStack st = chest.getStackInSlot(s);
                                            if (st != null) {
                                                stacks++;
                                                if (st.getItem() == Item.getItemFromBlock(net.minecraft.init.Blocks.obsidian)) {
                                                    obsidian++;
                                                }
                                                chest.setInventorySlotContents(s, null);
                                            }
                                        }
                                        chests++;
                                        most = Math.max(most, stacks);
                                        fewest = Math.min(fewest, stacks);
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    log("FAIL fortress chest " + e);
                }
                int rollsLo = 99;
                int rollsHi = 0;
                for (int i = 0; i < 300; i++) {
                    int n = ru.codex.offhand1710.Loot112.generate(ru.codex.offhand1710.Loot112.table(ru.codex.offhand1710.Loot112.NETHER_BRIDGE), rand).size();
                    rollsLo = Math.min(rollsLo, n);
                    rollsHi = Math.max(rollsHi, n);
                }
                result("fortress chests 1.12", chests >= 150 && obsidian > 0 && fewest >= 1 && rollsLo == 2 && rollsHi == 4,
                    "chests=" + chests + " obsidianStacks=" + obsidian + " stacks=" + fewest + ".." + most + " rolls=" + rollsLo + ".." + rollsHi);
                // chests by the pools of 1.12.2, through the same call the structures make
                {
                    java.util.Random lr = new java.util.Random(1712L);
                    int badFirst = 0;
                    int badThird = 0;
                    int badTools = 0;
                    int logMeta = 0;
                    java.util.Set<Item> first = new java.util.HashSet<Item>(java.util.Arrays.asList(Items.saddle, Items.golden_apple, Items.record_13, Items.record_cat,
                        Items.name_tag, Items.golden_horse_armor, Items.iron_horse_armor, Items.diamond_horse_armor, Items.enchanted_book, Items.book));
                    java.util.Set<Item> third = new java.util.HashSet<Item>(java.util.Arrays.asList(Items.bone, Items.gunpowder, Items.rotten_flesh, Items.string));
                    for (int i = 0; i < 300; i++) {
                        net.minecraft.inventory.InventoryBasic inv = new net.minecraft.inventory.InventoryBasic("t", false, 27);
                        net.minecraft.util.WeightedRandomChestContent.generateChestContents(lr, net.minecraftforge.common.ChestGenHooks.getItems(net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST, lr),
                            inv, net.minecraftforge.common.ChestGenHooks.getCount(net.minecraftforge.common.ChestGenHooks.DUNGEON_CHEST, lr));
                        int f = 0;
                        int q = 0;
                        for (int s = 0; s < 27; s++) {
                            ItemStack st = inv.getStackInSlot(s);
                            if (st != null && first.contains(st.getItem())) {
                                f += st.stackSize;
                            }
                            if (st != null && third.contains(st.getItem())) {
                                q += st.stackSize;
                            }
                        }
                        badFirst += f < 1 || f > 3 ? 1 : 0;
                        badThird += q < 3 || q > 24 ? 1 : 0;
                        inv = new net.minecraft.inventory.InventoryBasic("t", false, 27);
                        net.minecraft.util.WeightedRandomChestContent.generateChestContents(lr, net.minecraftforge.common.ChestGenHooks.getItems(net.minecraftforge.common.ChestGenHooks.BONUS_CHEST, lr),
                            inv, net.minecraftforge.common.ChestGenHooks.getCount(net.minecraftforge.common.ChestGenHooks.BONUS_CHEST, lr));
                        int axes = 0;
                        int picks = 0;
                        for (int s = 0; s < 27; s++) {
                            ItemStack st = inv.getStackInSlot(s);
                            if (st == null) {
                                continue;
                            }
                            axes += st.getItem() == Items.stone_axe || st.getItem() == Items.wooden_axe ? 1 : 0;
                            picks += st.getItem() == Items.stone_pickaxe || st.getItem() == Items.wooden_pickaxe ? 1 : 0;
                            if (st.getItem() == Item.getItemFromBlock(Blocks.log) && st.getItemDamage() > 0) {
                                logMeta++;
                            }
                        }
                        badTools += axes != 1 || picks != 1 ? 1 : 0;
                    }
                    result("dungeon chests by 1.12 pools", badFirst == 0 && badThird == 0, "300 chests: first pool outside 1..3 in " + badFirst + ", third pool outside 3..24 in " + badThird);
                    result("bonus chests by 1.12 pools", badTools == 0 && logMeta > 0, "300 chests: not one axe and one pickaxe in " + badTools + ", logs with data>0 " + logMeta);
                }
                result("registry names", missing <= 1, "missing=" + missing + " (air has no item in 1.7.10)");
                for (Object k : Item.itemRegistry.getKeys()) {
                    Item it = (Item) Item.itemRegistry.getObject(k);
                    java.util.List<ItemStack> subs = new java.util.ArrayList<ItemStack>();
                    try {
                        it.getSubItems(it, null, subs);
                    } catch (Throwable e) {
                        subs.clear();
                    }
                    if (subs.isEmpty()) {
                        subs.add(new ItemStack(it));
                    }
                    for (ItemStack s : subs) {
                        String un;
                        try {
                            un = s.getUnlocalizedName();
                        } catch (Throwable e) {
                            un = "?";
                        }
                        log("REG MOD " + k + " " + s.getItemDamage() + " " + un);
                    }
                }
            }
        });
        finish();
    }

    /** Hears what Sounds112 played or renamed last; the squid's names come straight from its own methods. */
    public static final class SoundEar {
        String entity = "";
        String client = "";

        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
        public void onEntity(net.minecraftforge.event.entity.PlaySoundAtEntityEvent event) {
            entity = String.valueOf(event.name);
        }

        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
        public void onClient(net.minecraftforge.client.event.sound.PlaySoundEvent17 event) {
            client = String.valueOf((event.result != null ? event.result : event.sound).getPositionedSoundLocation());
        }
    }

    private final SoundEar ear = new SoundEar();
    private int sx;
    private int sy;
    private int sz;

    private void heard(String label, String want) {
        String got = ru.codex.offhand1710.Sounds112.last;
        result("sound " + label, want.equals(got), "got=" + got);
        ru.codex.offhand1710.Sounds112.last = "";
    }

    private void inHand(ItemStack s) {
        p().inventory.setInventorySlotContents(p().inventory.currentItem, s);
    }

    private void look(double x, double y, double z) {
        EntityPlayerMP pl = p();
        pl.setPositionAndUpdate(x, y, z);
        pl.prevPosX = pl.posX;
        pl.prevPosY = pl.posY;
        pl.prevPosZ = pl.posZ;
        pl.rotationPitch = 90.0F;
        pl.prevRotationPitch = 90.0F;
        pl.rotationYaw = 0.0F;
        pl.prevRotationYaw = 0.0F;
    }

    /** The 1.12.2 sounds that 1.7.10 lacks, each made the way the game makes it (Sounds112, asm.SoundPatches). */
    private void sounds() {
        server(40, new Runnable() {
            @Override
            public void run() {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ear);
                cmd("gamemode 0");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                WorldServer w = w();
                sx = (int) Math.floor(p().posX) + 20;
                sy = 30;
                sz = (int) Math.floor(p().posZ);
                for (int dx = -12; dx <= 12; dx++) {
                    for (int dz = -12; dz <= 12; dz++) {
                        w.setBlock(sx + dx, sy - 1, sz + dz, Blocks.stone);
                        for (int dy = 0; dy < 6; dy++) {
                            w.setBlock(sx + dx, sy + dy, sz + dz, Blocks.air);
                        }
                    }
                }
                look(sx + 0.5D, sy, sz + 0.5D);
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                WorldServer w = w();
                EntityPlayerMP pl = p();
                ru.codex.offhand1710.Sounds112.last = "";
                net.minecraft.entity.monster.EntityWitch witch = new net.minecraft.entity.monster.EntityWitch(w);
                witch.setPosition(sx + 3.5D, sy, sz + 3.5D);
                w.spawnEntityInWorld(witch);
                witch.playLivingSound();
                heard("witch ambient", "entity.witch.ambient");
                net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(w);
                cow.setPosition(sx - 3.5D, sy, sz + 3.5D);
                w.spawnEntityInWorld(cow);
                witch.attackEntityWithRangedAttack(cow, 1.0F);
                heard("witch throw", "entity.witch.throw");
                witch.setFire(30);
                for (int i = 0; i < 400 && !"entity.witch.drink".equals(ru.codex.offhand1710.Sounds112.last); i++) {
                    witch.onLivingUpdate();
                }
                heard("witch drink", "entity.witch.drink");
                net.minecraft.entity.monster.EntitySkeleton wither = new net.minecraft.entity.monster.EntitySkeleton(w);
                wither.setSkeletonType(1);
                wither.setPosition(sx + 3.5D, sy, sz - 3.5D);
                w.spawnEntityInWorld(wither);
                wither.playLivingSound();
                heard("wither skeleton", "entity.wither_skeleton.ambient");
                net.minecraft.entity.monster.EntityZombie zv = new net.minecraft.entity.monster.EntityZombie(w);
                zv.setVillager(true);
                zv.setPosition(sx - 3.5D, sy, sz - 3.5D);
                w.spawnEntityInWorld(zv);
                zv.playLivingSound();
                heard("zombie villager", "entity.zombie_villager.ambient");
                net.minecraft.entity.monster.EntitySnowman snow = new net.minecraft.entity.monster.EntitySnowman(w);
                snow.setPosition(sx + 5.5D, sy, sz + 0.5D);
                w.spawnEntityInWorld(snow);
                ear.entity = "";
                snow.attackEntityFrom(net.minecraft.util.DamageSource.generic, 1.0F);
                result("sound snowman hurt", "mc112bridge:entity.snowman.hurt".equals(ear.entity), "got=" + ear.entity);
                net.minecraft.entity.passive.EntitySquid squid = new net.minecraft.entity.passive.EntitySquid(w);
                squid.setPosition(sx + 5.5D, sy, sz + 5.5D);
                w.spawnEntityInWorld(squid);
                ear.entity = "";
                squid.playLivingSound();
                result("sound squid ambient", "mc112bridge:entity.squid.ambient".equals(ear.entity), "got=" + ear.entity);
                net.minecraft.enchantment.Enchantment.thorns.func_151367_b(pl, witch, 10);
                heard("thorns", "enchant.thorns.hit");
                pl.hurtResistantTime = 0;
                pl.attackEntityFrom(net.minecraft.util.DamageSource.drown, 1.0F);
                heard("player drown", "entity.player.hurt_drown");
                pl.hurtResistantTime = 0;
                pl.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
                heard("player burning", "entity.player.hurt_on_fire");
                pl.setHealth(pl.getMaxHealth());
                inHand(new ItemStack(Items.bucket));
                pl.interactWith(cow);
                heard("cow milk", "entity.cow.milk");
                inHand(new ItemStack(Items.ender_eye));
                result("sound eye launch name", "entity.endereye.launch".equals(ru.codex.offhand1710.Sounds112.rename(pl, "random.bow")), "");
                net.minecraft.entity.passive.EntityHorse horse = new net.minecraft.entity.passive.EntityHorse(w);
                horse.setPosition(sx - 5.5D, sy, sz + 0.5D);
                w.spawnEntityInWorld(horse);
                horse.playSound("eating", 1.0F, 1.0F);
                heard("horse eats", "entity.horse.eat");
                witch.setDead();
                wither.setDead();
                zv.setDead();
                snow.setDead();
                // parrot: a zombie near it is repeated (one time in 50 per tick)
                net.minecraft.entity.monster.EntityZombie near = new net.minecraft.entity.monster.EntityZombie(w);
                near.setPosition(sx - 5.5D, sy, sz - 5.5D);
                w.spawnEntityInWorld(near);
                ru.codex.offhand1710.MoreMobs.Parrot parrot = new ru.codex.offhand1710.MoreMobs.Parrot(w);
                parrot.setPosition(sx - 5.5D, sy, sz - 3.5D);
                w.spawnEntityInWorld(parrot);
                ru.codex.offhand1710.Sounds112.last = "";
                int tries = 0;
                while (tries < 2000 && !ru.codex.offhand1710.Sounds112.parrotMimic(parrot)) {
                    tries++;
                }
                heard("parrot repeats a zombie", "entity.parrot.imitate.zombie");
                log("INFO parrot mimic after " + tries + " ticks (1.12: one in 50)");
                horse.setDead();
                near.setDead();
                parrot.setDead();
                witch.setDead();
                wither.setDead();
                zv.setDead();
                snow.setDead();
                squid.setDead();
                cow.setDead();
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                WorldServer w = w();
                EntityPlayerMP pl = p();
                // buckets: a water source in the floor, the player above it looking down
                w.setBlock(sx + 2, sy - 1, sz, Blocks.water);
                look(sx + 2.5D, sy, sz + 0.5D);
                ItemStack empty = new ItemStack(Items.bucket);
                inHand(empty);
                Items.bucket.onItemRightClick(empty, w, pl);
                heard("bucket fill", "item.bucket.fill");
                look(sx - 6.5D, sy, sz - 6.5D);
                Items.water_bucket.onItemRightClick(new ItemStack(Items.water_bucket), w, pl);
                heard("bucket empty", "item.bucket.empty");
                look(sx - 8.5D, sy, sz - 8.5D);
                Items.lava_bucket.onItemRightClick(new ItemStack(Items.lava_bucket), w, pl);
                heard("bucket empty lava", "item.bucket.empty_lava");
                w.setBlock(sx - 6, sy, sz - 6, Blocks.air);
                w.setBlock(sx - 8, sy, sz - 8, Blocks.air);
                w.setBlock(sx + 4, sy - 1, sz, Blocks.water);
                look(sx + 4.5D, sy, sz + 0.5D);
                ItemStack bottle = new ItemStack(Items.glass_bottle);
                inHand(bottle);
                Items.glass_bottle.onItemRightClick(bottle, w, pl);
                heard("bottle fill", "item.bottle.fill");
                Item.getItemFromBlock(Blocks.waterlily).onItemRightClick(new ItemStack(Blocks.waterlily), w, pl);
                heard("lily pad placed", w.getBlock(sx + 4, sy, sz) == Blocks.waterlily ? "block.waterlily.place" : "(no pad)");
                w.setBlock(sx + 4, sy, sz, Blocks.air);
                w.setBlock(sx + 2, sy - 1, sz, Blocks.stone);
                w.setBlock(sx + 4, sy - 1, sz, Blocks.stone);
                look(sx + 0.5D, sy, sz + 0.5D);
                // armour put on with a right click
                pl.inventory.armorInventory[2] = null;
                Items.diamond_chestplate.onItemRightClick(new ItemStack(Items.diamond_chestplate), w, pl);
                heard("equip diamond", "item.armor.equip_diamond");
                pl.inventory.armorInventory[2] = null;
                ru.codex.offhand1710.Elytra112.elytra.onItemRightClick(new ItemStack(ru.codex.offhand1710.Elytra112.elytra), w, pl);
                heard("equip elytra", "item.armor.equip_elytra");
                pl.inventory.armorInventory[2] = null;
                pl.inventory.armorInventory[0] = null;
                Items.leather_boots.onItemRightClick(new ItemStack(Items.leather_boots), w, pl);
                heard("equip leather", "item.armor.equip_leather");
                pl.inventory.armorInventory[0] = null;
                // cauldron filled with a bucket
                w.setBlock(sx - 2, sy, sz, Blocks.cauldron, 0, 3);
                inHand(new ItemStack(Items.water_bucket));
                Blocks.cauldron.onBlockActivated(w, sx - 2, sy, sz, pl, 1, 0.5F, 0.5F, 0.5F);
                heard("cauldron bucket", "item.bucket.empty");
            }
        });
        server(2, new Runnable() {
            @Override
            public void run() {
                WorldServer w = w();
                EntityPlayerMP pl = p();
                inHand(new ItemStack(Items.glass_bottle));
                Blocks.cauldron.onBlockActivated(w, sx - 2, sy, sz, pl, 1, 0.5F, 0.5F, 0.5F);
                heard("cauldron bottle", "item.bottle.fill");
                // item frame on a wall: placed, item in, turned, item out, broken
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = 0; dy < 4; dy++) {
                        w.setBlock(sx + dx, sy + dy, sz + 3, Blocks.stone);
                        w.setBlock(sx + dx, sy + dy, sz + 7, Blocks.stone);
                    }
                }
                ItemStack frameItem = new ItemStack(Items.item_frame);
                Items.item_frame.onItemUse(frameItem, pl, w, sx, sy + 1, sz + 3, 2, 0.5F, 0.5F, 0.0F);
                heard("item frame place", "entity.itemframe.place");
                java.util.List frames = w.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItemFrame.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(sx - 1, sy, sz + 1, sx + 2, sy + 3, sz + 4));
                if (frames.isEmpty()) {
                    log("FAIL sound item frame missing");
                } else {
                    net.minecraft.entity.item.EntityItemFrame frame = (net.minecraft.entity.item.EntityItemFrame) frames.get(0);
                    inHand(new ItemStack(Items.diamond));
                    frame.interactFirst(pl);
                    heard("item frame add", "entity.itemframe.add_item");
                    frame.interactFirst(pl);
                    heard("item frame rotate", "entity.itemframe.rotate_item");
                    frame.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(pl), 1.0F);
                    heard("item frame remove", "entity.itemframe.remove_item");
                    frame.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(pl), 1.0F);
                    heard("item frame break", "entity.itemframe.break");
                }
                Items.painting.onItemUse(new ItemStack(Items.painting), pl, w, sx, sy + 1, sz + 7, 2, 0.5F, 0.5F, 0.0F);
                heard("painting place", "entity.painting.place");
                // a lead from a leashed cow tied to a fence: a new knot, then broken
                w.setBlock(sx + 6, sy, sz - 6, Blocks.fence);
                net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(w);
                cow.setPosition(sx + 5.5D, sy, sz - 4.5D);
                w.spawnEntityInWorld(cow);
                cow.setLeashedToEntity(pl, true);
                look(sx + 5.5D, sy, sz - 5.0D);
                net.minecraft.item.ItemLead.func_150909_a(pl, w, sx + 6, sy, sz - 6);
                heard("leash knot place", "entity.leashknot.place");
                java.util.List knots = w.getEntitiesWithinAABB(net.minecraft.entity.EntityLeashKnot.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(sx + 5, sy - 1, sz - 7, sx + 8, sy + 2, sz - 5));
                if (!knots.isEmpty()) {
                    ((net.minecraft.entity.Entity) knots.get(0)).attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(pl), 1.0F);
                    heard("leash knot break", "entity.leashknot.break");
                } else {
                    log("FAIL sound leash knot missing");
                }
                cow.setDead();
                look(sx + 0.5D, sy, sz + 0.5D);
                // brewing finished
                w.setBlock(sx - 4, sy, sz - 3, Blocks.brewing_stand);
                net.minecraft.tileentity.TileEntityBrewingStand stand = (net.minecraft.tileentity.TileEntityBrewingStand) w.getTileEntity(sx - 4, sy, sz - 3);
                stand.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 0));
                stand.setInventorySlotContents(3, new ItemStack(Items.nether_wart));
                try {
                    for (java.lang.reflect.Method m : net.minecraft.tileentity.TileEntityBrewingStand.class.getDeclaredMethods()) {
                        if (("brewPotions".equals(m.getName()) || "func_145940_l".equals(m.getName())) && m.getParameterTypes().length == 0) {
                            m.setAccessible(true);
                            m.invoke(stand);
                        }
                    }
                } catch (Exception e) {
                    log("FAIL sound brewing " + e);
                }
                heard("brewing", "block.brewing_stand.brew");
                // enchanting table
                w.setBlock(sx - 4, sy, sz + 2, Blocks.enchanting_table);
                net.minecraft.inventory.ContainerEnchantment table = new net.minecraft.inventory.ContainerEnchantment(pl.inventory, w, sx - 4, sy, sz + 2);
                table.putStackInSlot(0, new ItemStack(Items.iron_sword));
                pl.experienceLevel = 30;
                boolean enchanted = table.enchantItem(pl, 0);
                heard("enchanting table", enchanted ? "block.enchantment_table.use" : "(not enchanted)");
                table.putStackInSlot(0, null);
                // eyes of ender: one into a lone frame, then the last of twelve opens a portal
                w.setBlock(sx + 9, sy, sz + 9, Blocks.end_portal_frame, 0, 3);
                Items.ender_eye.onItemUse(new ItemStack(Items.ender_eye), pl, w, sx + 9, sy, sz + 9, 1, 0.5F, 0.5F, 0.5F);
                heard("portal frame fill", "block.end_portal_frame.fill");
                int cx = sx - 8;
                int cz = sz + 8;
                for (int i = -1; i <= 1; i++) {
                    w.setBlock(cx + i, sy, cz - 2, Blocks.end_portal_frame, i == 0 ? 0 : 4, 3);
                    w.setBlock(cx + i, sy, cz + 2, Blocks.end_portal_frame, 2 | 4, 3);
                    w.setBlock(cx - 2, sy, cz + i, Blocks.end_portal_frame, 3 | 4, 3);
                    w.setBlock(cx + 2, sy, cz + i, Blocks.end_portal_frame, 1 | 4, 3);
                }
                Items.ender_eye.onItemUse(new ItemStack(Items.ender_eye), pl, w, cx, sy, cz - 2, 1, 0.5F, 0.5F, 0.5F);
                heard("end portal opens", "block.end_portal.spawn");
                result("end portal built", w.getBlock(cx, sy, cz) == Blocks.end_portal, "");
                net.minecraft.entity.item.EntityEnderEye eye = new net.minecraft.entity.item.EntityEnderEye(w, sx + 0.5D, sy + 2.0D, sz + 0.5D);
                w.spawnEntityInWorld(eye);
                for (int i = 0; i < 90 && !eye.isDead; i++) {
                    eye.onUpdate();
                }
                heard("eye of ender breaks", eye.isDead ? "entity.endereye.death" : "(alive)");
                // fishing rod: cast, then reeled in
                ItemStack rod = new ItemStack(Items.fishing_rod);
                inHand(rod);
                Items.fishing_rod.onItemRightClick(rod, w, pl);
                heard("bobber throw", "entity.bobber.throw");
                Items.fishing_rod.onItemRightClick(rod, w, pl);
                heard("bobber retrieve", "entity.bobber.retrieve");
                // chorus flower on end stone: grows or dies
                w.setBlock(sx + 9, sy - 1, sz - 9, Blocks.end_stone);
                w.setBlock(sx + 9, sy, sz - 9, ru.codex.offhand1710.End112.chorusFlower, 0, 3);
                java.util.Random rand = new java.util.Random(5L);
                for (int i = 0; i < 40 && !ru.codex.offhand1710.Sounds112.last.startsWith("block.chorus_flower"); i++) {
                    ru.codex.offhand1710.End112.chorusFlower.updateTick(w, sx + 9, sy, sz - 9, rand);
                }
                String chorus = ru.codex.offhand1710.Sounds112.last;
                result("sound chorus flower", chorus.equals("block.chorus_flower.grow") || chorus.equals("block.chorus_flower.death"), "got=" + chorus);
                ru.codex.offhand1710.Sounds112.last = "";
                // blocks for the client renames
                w.setBlock(sx - 1, sy, sz - 1, Blocks.chest);
                w.setBlock(sx - 1, sy, sz - 3, Blocks.ender_chest);
                w.setBlock(sx + 1, sy, sz - 1, Blocks.wooden_door, 0, 3);
                w.setBlock(sx + 1, sy, sz - 3, Blocks.iron_door, 0, 3);
                w.setBlock(sx + 3, sy, sz - 1, Blocks.trapdoor, 0, 3);
                w.setBlock(sx + 3, sy, sz - 3, ru.codex.offhand1710.PortedBlocks.ironTrapdoor, 0, 3);
                w.setBlock(sx + 5, sy, sz - 1, Blocks.fence_gate, 0, 3);
                w.setBlock(sx - 3, sy, sz - 1, Blocks.gold_block);
                w.setBlock(sx - 3, sy + 1, sz - 1, Blocks.noteblock);
                w.setBlock(sx - 5, sy, sz - 1, ru.codex.offhand1710.PortedBlocks.boneBlock);
                w.setBlock(sx - 5, sy + 1, sz - 1, Blocks.noteblock);
                w.setBlock(sx - 7, sy + 1, sz - 1, Blocks.noteblock);
                w.setBlock(sx - 7, sy, sz - 3, Blocks.farmland);
                w.setBlock(sx - 9, sy + 1, sz - 1, Blocks.lit_furnace, 2, 3);
            }
        });
        client(20, new Runnable() {
            @Override
            public void run() {
                net.minecraft.world.World cw = mc.theWorld;
                String[][] cases = {
                    {"chest", "random.chestopen", "-1", "0", "-1", "block.chest.open"},
                    {"ender chest", "random.chestclosed", "-1", "0", "-3", "block.enderchest.close"},
                    {"wooden door", "random.door_open", "1", "0", "-1", "block.wooden_door.open"},
                    {"iron door", "random.door_close", "1", "0", "-3", "block.iron_door.close"},
                    {"trapdoor", "random.door_open", "3", "0", "-1", "block.wooden_trapdoor.open"},
                    {"iron trapdoor", "random.door_open", "3", "0", "-3", "block.iron_trapdoor.open"},
                    {"fence gate", "random.door_close", "5", "0", "-1", "block.fence_gate.close"},
                    {"note on gold", "note.harp", "-3", "1", "-1", "block.note.bell"},
                    {"note on bone", "note.bd", "-5", "1", "-1", "block.note.xylophone"},
                    {"note harp", "note.harp", "-7", "1", "-1", "block.note.harp"},
                    {"hoe", "step.gravel", "-7", "0", "-3", "item.hoe.till"},
                };
                for (String[] c : cases) {
                    float x = sx + Integer.parseInt(c[2]) + 0.5F;
                    float y = sy + Integer.parseInt(c[3]) + 0.5F;
                    float z = sz + Integer.parseInt(c[4]) + 0.5F;
                    String got = ru.codex.offhand1710.client.Sounds112Client.rename(cw, c[1], x, y, z, 1.0F);
                    result("sound " + c[0], c[5].equals(got), "got=" + got);
                }
                result("sound walking on farmland kept", ru.codex.offhand1710.client.Sounds112Client.rename(cw, "step.gravel", sx - 6.7F, sy + 1.0F, sz - 2.2F, 0.15F) == null, "");
                ru.codex.offhand1710.Sounds112.last = "";
                java.util.Random rand = new java.util.Random(1L);
                for (int i = 0; i < 100 && ru.codex.offhand1710.Sounds112.last.length() == 0; i++) {
                    Blocks.lit_furnace.randomDisplayTick(cw, sx - 9, sy + 1, sz - 1, rand);
                }
                heard("furnace crackle", "block.furnace.fire_crackle");
                // end to end through the sound system, when there is one
                ear.client = "";
                cw.playSound(sx - 0.5D, sy + 0.5D, sz - 0.5D, "random.chestopen", 0.5F, 1.0F, false);
                log("INFO client sound path heard=" + ear.client + " (empty: no sound system in this run)");
                if (ear.client.length() > 0) {
                    result("sound chest through the sound system", "mc112bridge:block.chest.open".equals(ear.client), "got=" + ear.client);
                }
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.Sounds112.toastSounds = 0;
                cmd("advancement grant @p only minecraft:story/mine_stone");
            }
        });
        client(160, new Runnable() {
            @Override
            public void run() {
                result("sound toast in and out", ru.codex.offhand1710.Sounds112.toastSounds >= 2, "played=" + ru.codex.offhand1710.Sounds112.toastSounds);
            }
        });
        finish();
    }

    private final java.util.List<Integer> idleFps = new java.util.ArrayList<Integer>();
    private final java.util.List<Double> idleTick = new java.util.ArrayList<Double>();

    private static int debugFps() {
        for (String n : new String[] {"debugFPS", "field_71470_ab"}) {
            try {
                java.lang.reflect.Field f = net.minecraft.client.Minecraft.class.getDeclaredField(n);
                f.setAccessible(true);
                return f.getInt(null);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    /** An existing save left alone: after 30 s, one minute of FPS and server tick times (MinecraftServer.tickTimeArray). */
    private void idle() {
        client(600, new Runnable() {
            @Override
            public void run() {
                log("INFO idle start at " + (int) mc.thePlayer.posX + "," + (int) mc.thePlayer.posY + "," + (int) mc.thePlayer.posZ
                    + " renderDistance=" + mc.gameSettings.renderDistanceChunks + " entities=" + mc.theWorld.loadedEntityList.size());
            }
        });
        for (int i = 0; i < 60; i++) {
            client(20, new Runnable() {
                @Override
                public void run() {
                    idleFps.add(debugFps());
                    long[] t = MinecraftServer.getServer().tickTimeArray;
                    long sum = 0L;
                    for (long v : t) {
                        sum += v;
                    }
                    idleTick.add(sum / (double) t.length / 1.0E6D);
                }
            });
        }
        client(1, new Runnable() {
            @Override
            public void run() {
                int fmin = Integer.MAX_VALUE;
                long fsum = 0L;
                for (int f : idleFps) {
                    fmin = Math.min(fmin, f);
                    fsum += f;
                }
                double tmax = 0.0D;
                double tsum = 0.0D;
                for (double d : idleTick) {
                    tmax = Math.max(tmax, d);
                    tsum += d;
                }
                double tavg = tsum / idleTick.size();
                log("INFO idle fps avg=" + (fsum / idleFps.size()) + " min=" + fmin + " tick ms avg=" + String.format("%.2f", tavg) + " max=" + String.format("%.2f", tmax)
                    + " tps=" + String.format("%.1f", Math.min(20.0D, 1000.0D / Math.max(50.0D, tmax))));
                result("idle server keeps 20 TPS", tmax < 50.0D, "max mean tick " + String.format("%.2f", tmax) + " ms");
            }
        });
        finish();
    }

    private int diamonds;

    private int sel(String token) {
        return ru.codex.offhand1710.Selector112.select(p(), token, net.minecraft.entity.Entity.class).size();
    }

    /** 1.12.2 selectors in vanilla commands (Selector112 through PlayerSelector and the CommandHandler loop). */
    private void selectors() {
        final net.minecraft.entity.Entity[] mobs = new net.minecraft.entity.Entity[8];
        server(40, new Runnable() {
            @Override
            public void run() {
                WorldServer w = w();
                cmd("gamemode 1");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                sx = (int) Math.floor(p().posX) + 40;
                sy = 30;
                sz = (int) Math.floor(p().posZ);
                for (int dx = -12; dx <= 12; dx++) {
                    for (int dz = -12; dz <= 12; dz++) {
                        w.setBlock(sx + dx, sy - 1, sz + dz, Blocks.stone);
                        for (int dy = 0; dy < 4; dy++) {
                            w.setBlock(sx + dx, sy + dy, sz + dz, Blocks.air);
                        }
                    }
                }
                for (Object o : w.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class, net.minecraft.util.AxisAlignedBB.getBoundingBox(sx - 60, 0, sz - 60, sx + 60, 255, sz + 60))) {
                    ((net.minecraft.entity.Entity) o).setDead();
                }
                look(sx + 0.5D, sy, sz + 0.5D);
                double[][] at = {{3, 0}, {6, 0}, {9, 0}, {-3, 3}, {-3, 6}, {0, -8}};
                for (int i = 0; i < 6; i++) {
                    net.minecraft.entity.EntityLiving e = i < 3 ? new net.minecraft.entity.monster.EntityZombie(w) : i < 5 ? new net.minecraft.entity.passive.EntityPig(w)
                        : new net.minecraft.entity.monster.EntitySkeleton(w);
                    if (i == 5) {
                        ((net.minecraft.entity.monster.EntitySkeleton) e).setSkeletonType(1);
                    }
                    e.setLocationAndAngles(sx + 0.5D + at[i][0], sy, sz + 0.5D + at[i][1], i * 40.0F, 0.0F);
                    e.func_110163_bv();
                    w.spawnEntityInWorld(e);
                    mobs[i] = e;
                }
                ((net.minecraft.entity.EntityLiving) mobs[1]).setCustomNameTag("Bob");
                ((net.minecraft.entity.EntityLiving) mobs[0]).setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
            }
        });
        server(5, new Runnable() {
            @Override
            public void run() {
                result("@e[type=zombie] finds three", sel("@e[type=zombie]") == 3, "n=" + sel("@e[type=zombie]"));
                result("@e[type=!zombie,r=20] skips zombies", sel("@e[type=!zombie,r=20]") == 4, "n=" + sel("@e[type=!zombie,r=20]") + " (2 pigs, wither skeleton, player)");
                result("@e[type=wither_skeleton] is the variant", sel("@e[type=wither_skeleton]") == 1 && sel("@e[type=skeleton]") == 0, "");
                result("@e[name=Bob] and name=!Bob", sel("@e[name=Bob]") == 1 && sel("@e[type=zombie,name=!Bob]") == 2, "");
                result("@e[c=1] nearest, c=-1 farthest", ru.codex.offhand1710.Selector112.select(p(), "@e[type=zombie,c=1]", net.minecraft.entity.Entity.class).get(0) == mobs[0]
                    && ru.codex.offhand1710.Selector112.select(p(), "@e[type=zombie,c=-1]", net.minecraft.entity.Entity.class).get(0) == mobs[2], "");
                result("@e[dx,dy,dz] box", sel("@e[x=" + (sx + 5) + ",y=" + sy + ",z=" + sz + ",dx=2,dy=1,dz=0]") == 1, "n=" + sel("@e[x=" + (sx + 5) + ",y=" + sy + ",z=" + sz + ",dx=2,dy=1,dz=0]"));
                result("@e[rym,ry] by yaw", sel("@e[type=zombie,rym=30,ry=50]") == 1, "n=" + sel("@e[type=zombie,rym=30,ry=50]"));
                result("@a[m=1] creative player", sel("@a[m=1]") == 1 && sel("@a[m=0]") == 0, "");
                result("@s is the sender", sel("@s") == 1 && sel("@s[type=player]") == 1 && sel("@s[type=zombie]") == 0, "");
                result("@r[type=pig] picks one pig", sel("@r[type=pig]") == 1, "");
                boolean threw = false;
                try {
                    sel("@e[foo=1]");
                } catch (net.minecraft.command.CommandException e) {
                    threw = "commands.generic.selector_argument".equals(e.getMessage());
                }
                result("unknown argument is an error", threw, "");
                result("PlayerSelector knows @e", net.minecraft.command.PlayerSelector.matchesMultiplePlayers("@e[type=zombie]") && net.minecraft.command.PlayerSelector.hasArguments("@s"), "");
                result("getPlayer(@s) is the player", net.minecraft.command.CommandBase.getPlayer(p(), "@s") == p(), "");
                net.minecraft.util.IChatComponent names = net.minecraft.command.PlayerSelector.func_150869_b(p(), "@e[type=zombie,name=Bob]");
                result("/say names entities", names != null && "Bob".equals(names.getUnformattedText()), "got=" + (names == null ? null : names.getUnformattedText()));
                // real commands through CommandHandler
                cmd("scoreboard objectives add t112 dummy");
                cmd("scoreboard players set @e[type=zombie] t112 5");
                result("/scoreboard players set @e sets every zombie (by UUID)", sel("@e[type=zombie,score_t112_min=5,score_t112=5]") == 3, "n=" + sel("@e[type=zombie,score_t112_min=5]"));
                cmd("scoreboard players tag @e[type=zombie,c=1] add boss");
                result("/scoreboard players tag adds a tag", sel("@e[tag=boss]") == 1 && sel("@e[type=zombie,tag=!boss]") == 2 && sel("@e[type=pig,tag=]") == 2, "");
                cmd("enchant @e[tag=boss] sharpness 2");
                ItemStack sword = ((net.minecraft.entity.EntityLiving) mobs[0]).getHeldItem();
                result("/enchant @e enchants a zombie's sword", sword != null && net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(16, sword) == 2, "");
                ru.codex.offhand1710.Commands112.last = 0;
                cmd("testfor @e[type=pig]");
                result("/testfor @e counts entities", ru.codex.offhand1710.Commands112.last == 2, "found=" + ru.codex.offhand1710.Commands112.last);
                ru.codex.offhand1710.Commands112.last = 0;
                cmd("testfor @e[type=zombie,c=1] {CustomName:\"Bob\"}");
                result("/testfor with a data tag that does not match", ru.codex.offhand1710.Commands112.last == 0, "");
                int before = 0;
                for (ItemStack s : p().inventory.mainInventory) {
                    before += s != null && s.getItem() == Items.diamond ? s.stackSize : 0;
                }
                diamonds = before;
                cmd("give @e[type=player] diamond 3");
                cmd("kill @e[type=pig]");
                result("/kill @e[type=pig] through the command loop", ((net.minecraft.entity.EntityLivingBase) mobs[3]).getHealth() <= 0.0F
                    && ((net.minecraft.entity.EntityLivingBase) mobs[4]).getHealth() <= 0.0F && ((net.minecraft.entity.EntityLivingBase) mobs[0]).getHealth() > 0.0F, "");
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                int after = 0;
                for (ItemStack s : p().inventory.mainInventory) {
                    after += s != null && s.getItem() == Items.diamond ? s.stackSize : 0;
                }
                StringBuilder near = new StringBuilder();
                for (Object o : w().getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, p().boundingBox.expand(8, 8, 8))) {
                    net.minecraft.entity.item.EntityItem ei = (net.minecraft.entity.item.EntityItem) o;
                    near.append(ei.getEntityItem()).append(" delay=").append(ei.delayBeforeCanPickup).append(" d=").append(String.format("%.1f", ei.getDistanceToEntity(p()))).append("; ");
                }
                result("/give @e[type=player] (1.7.10 player command)", after - diamonds == 3, "diamonds +" + (after - diamonds) + " near: " + near
                    + " offhand=" + ru.codex.offhand1710.OffhandInventory.get(p()).getStack() + " pos=" + (int) p().posX + "," + (int) p().posY + "," + (int) p().posZ
                    + " dead=" + p().isDead + " inList=" + w().playerEntities.contains(p()));
                for (net.minecraft.entity.Entity e : mobs) {
                    if (e != null) {
                        e.setDead();
                    }
                }
                cmd("scoreboard objectives remove t112");
            }
        });
        finish();
    }

    private final net.minecraft.entity.item.EntityBoat[] boats = new net.minecraft.entity.item.EntityBoat[4];
    private final double[] boatMark = new double[8];
    private net.minecraft.entity.passive.EntityPig boatPig;

    private net.minecraft.entity.item.EntityBoat boatAt(double x, double y, double z, float yaw) {
        net.minecraft.entity.item.EntityBoat b = new net.minecraft.entity.item.EntityBoat(w(), x, y, z);
        b.rotationYaw = yaw;
        b.prevRotationYaw = yaw;
        w().spawnEntityInWorld(b);
        return b;
    }

    private static void drive(net.minecraft.entity.item.EntityBoat b, boolean forward, boolean left) {
        ru.codex.offhand1710.Boat112.state(b).input = new Boolean[] {forward, false, left, false};
    }

    /** Translation keys of the chat lines this client got (ClientChatReceivedEvent), for the game rule stage. */
    public static final class ChatKeys {
        final java.util.List<String> keys = java.util.Collections.synchronizedList(new java.util.ArrayList<String>());

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onChat(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
            collect(event.message);
        }

        private void collect(net.minecraft.util.IChatComponent c) {
            if (c instanceof net.minecraft.util.ChatComponentTranslation) {
                net.minecraft.util.ChatComponentTranslation t = (net.minecraft.util.ChatComponentTranslation) c;
                keys.add(t.getKey() + "#" + t.getFormatArgs().length);
                for (Object a : t.getFormatArgs()) {
                    if (a instanceof net.minecraft.util.IChatComponent) {
                        collect((net.minecraft.util.IChatComponent) a);
                    }
                }
            }
            for (Object s : c.getSiblings()) {
                collect((net.minecraft.util.IChatComponent) s);
            }
        }

        boolean seen(String prefix) {
            synchronized (keys) {
                for (String k : keys) {
                    if (k.startsWith(prefix)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private final ChatKeys chat = new ChatKeys();
    private final java.util.List<net.minecraft.entity.passive.EntityPig> crammed = new java.util.ArrayList<net.minecraft.entity.passive.EntityPig>();
    private final java.util.List<net.minecraft.entity.passive.EntityPig> loose = new java.util.ArrayList<net.minecraft.entity.passive.EntityPig>();
    private net.minecraft.entity.passive.EntityWolf pet;

    private int itemsNear(double x, double y, double z) {
        return w().getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
            net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 3, y - 3, z - 3, x + 3, y + 3, z + 3)).size();
    }

    private int cropsGrown(int x0) {
        int grown = 0;
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                grown += w().getBlockMetadata(x0 + dx, sy, sz + dz) > 0 ? 1 : 0;
            }
        }
        return grown;
    }

    private void crops(int x0) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                w().setBlock(x0 + dx, sy - 1, sz + dz, Blocks.farmland, 7, 2);
                w().setBlock(x0 + dx, sy, sz + dz, Blocks.wheat, 0, 2);
            }
        }
    }

    /** The game rules of 1.12.2 (Rules112): defaults, /gamerule, and what each rule does. */
    private void gamerules() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(chat);
        server(40, new Runnable() {
            @Override
            public void run() {
                net.minecraft.world.GameRules r = w().getGameRules();
                StringBuilder wrong = new StringBuilder();
                for (java.util.Map.Entry<String, String> e : ru.codex.offhand1710.Rules112.ADDED.entrySet()) {
                    if (!r.hasRule(e.getKey()) || !e.getValue().equals(r.getGameRuleStringValue(e.getKey()))) {
                        wrong.append(e.getKey()).append(' ');
                    }
                }
                result("the 15 rules of 1.12.2 with their defaults", wrong.length() == 0 && r.getRules().length >= 24, wrong + "of " + r.getRules().length);
                cmd("gamerule noSuchRule");
                cmd("gamerule doMobLoot yes");
                cmd("gamerule keepInventory false");
                cmd("gamerule randomTickSpeed");
                cmd("time set 1000");
                cmd("gamerule doMobSpawning false");
                sx = (int) Math.floor(p().posX) + 30;
                sy = 70;
                sz = (int) Math.floor(p().posZ);
                for (int dx = -2; dx <= 30; dx++) {
                    for (int dz = -2; dz <= 8; dz++) {
                        w().setBlock(sx + dx, sy - 2, sz + dz, Blocks.stone);
                        w().setBlock(sx + dx, sy - 1, sz + dz, Blocks.stone);
                        for (int dy = 0; dy < 12; dy++) {
                            w().setBlock(sx + dx, sy + dy, sz + dz, Blocks.air);
                        }
                    }
                }
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/gamerule of a missing rule says so", chat.seen("commands.gamerule.norule#1"), "");
                result("/gamerule refuses a boolean that is not true or false", chat.seen("commands.generic.boolean.invalid#1") && w().getGameRules().getGameRuleBooleanValue("doMobLoot"),
                    "doMobLoot=" + w().getGameRules().getGameRuleStringValue("doMobLoot"));
                result("/gamerule success names the rule and the value", chat.seen("commands.gamerule.success#2"), "");
                result("the command result reaches the player (sendCommandFeedback true)", chat.seen("commands.time.set"), "");
                chat.keys.clear();
                cmd("gamerule sendCommandFeedback false");
                cmd("time set 2000");
                cmd("gamerule doEntityDrops false");
                // randomTickSpeed: 0 freezes the crops, 2000 grows them at once
                cmd("gamerule randomTickSpeed 0");
                crops(sx);
            }
        });
        server(100, new Runnable() {
            @Override
            public void run() {
                result("sendCommandFeedback false hides the result", !chat.seen("commands.time.set") && w().getWorldTime() % 24000 >= 2000 && w().getWorldTime() % 24000 < 2300, "time=" + w().getWorldTime() % 24000);
                result("randomTickSpeed 0: no crop grows in 100 ticks", cropsGrown(sx) == 0, "grown=" + cropsGrown(sx));
                cmd("gamerule sendCommandFeedback true");
                cmd("gamerule randomTickSpeed 2000");
                // doEntityDrops false: a boat and a minecart broken by damage drop nothing
                net.minecraft.entity.item.EntityBoat boat = new net.minecraft.entity.item.EntityBoat(w(), sx + 10.5D, sy, sz + 2.5D);
                w().spawnEntityInWorld(boat);
                boat.attackEntityFrom(net.minecraft.util.DamageSource.generic, 100.0F);
                net.minecraft.entity.item.EntityMinecartEmpty cart = new net.minecraft.entity.item.EntityMinecartEmpty(w(), sx + 10.5D, sy, sz + 5.5D);
                w().spawnEntityInWorld(cart);
                cart.attackEntityFrom(net.minecraft.util.DamageSource.generic, 100.0F);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                // about 10 random ticks a crop in 20 ticks; each grows a stage with a chance near 1/3 (first run: 21, then 17)
                result("randomTickSpeed 2000: the crops grow within 20 ticks", cropsGrown(sx) >= 10, "grown=" + cropsGrown(sx) + "/25");
                cmd("gamerule randomTickSpeed 3");
                result("doEntityDrops false: a broken boat and minecart drop nothing", itemsNear(sx + 10.5D, sy, sz + 4) == 0, "items=" + itemsNear(sx + 10.5D, sy, sz + 4));
                cmd("gamerule doEntityDrops true");
                net.minecraft.entity.item.EntityBoat boat = new net.minecraft.entity.item.EntityBoat(w(), sx + 16.5D, sy, sz + 2.5D);
                w().spawnEntityInWorld(boat);
                boat.attackEntityFrom(net.minecraft.util.DamageSource.generic, 100.0F);
                // maxEntityCramming: 30 pigs in one block hurt each other, 30 more with the rule at 0 do not
                for (int i = 0; i < 30; i++) {
                    net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(w());
                    pig.setPosition(sx + 22.5D, sy, sz + 2.5D);
                    w().spawnEntityInWorld(pig);
                    crammed.add(pig);
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dz != 0) {
                            for (int dy = 0; dy < 3; dy++) {
                                w().setBlock(sx + 22 + dx, sy + dy, sz + 2 + dz, Blocks.glass);
                            }
                        }
                    }
                }
                // weather: rain in 5 ticks, but doWeatherCycle false holds the clock
                cmd("gamerule doWeatherCycle false");
                w().getWorldInfo().setRaining(false);
                w().getWorldInfo().setRainTime(5);
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                result("doEntityDrops true: a broken boat drops again", itemsNear(sx + 16.5D, sy, sz + 2.5D) > 0, "items=" + itemsNear(sx + 16.5D, sy, sz + 2.5D));
                int hurt = 0;
                for (net.minecraft.entity.passive.EntityPig pig : crammed) {
                    hurt += pig.isDead || pig.getHealth() < pig.getMaxHealth() ? 1 : 0;
                }
                result("maxEntityCramming 24: 30 pigs in one block get hurt", hurt >= 10, "hurt=" + hurt + "/30");
                for (net.minecraft.entity.passive.EntityPig pig : crammed) {
                    pig.setDead();
                }
                result("doWeatherCycle false: the rain clock stands still", !w().getWorldInfo().isRaining() && w().getWorldInfo().getRainTime() == 5,
                    "raining=" + w().getWorldInfo().isRaining() + " time=" + w().getWorldInfo().getRainTime());
                cmd("gamerule doWeatherCycle true");
                cmd("gamerule maxEntityCramming 0");
                for (int i = 0; i < 30; i++) {
                    net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(w());
                    pig.setPosition(sx + 22.5D, sy, sz + 2.5D);
                    w().spawnEntityInWorld(pig);
                    loose.add(pig);
                }
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                int hurt = 0;
                for (net.minecraft.entity.passive.EntityPig pig : loose) {
                    hurt += pig.isDead || pig.getHealth() < pig.getMaxHealth() ? 1 : 0;
                    pig.setDead();
                }
                result("maxEntityCramming 0: the same pigs are left alone", hurt == 0, "hurt=" + hurt + "/30");
                result("doWeatherCycle true: the clock runs out and it rains", w().getWorldInfo().isRaining(), "time=" + w().getWorldInfo().getRainTime());
                cmd("weather clear");
                cmd("gamerule maxEntityCramming 24");
                // spawnRadius: the spread of WorldProvider.getRandomizedSpawnPoint
                net.minecraft.util.ChunkCoordinates spawn = w().getSpawnPoint();
                cmd("gamerule spawnRadius 0");
                boolean exact = true;
                for (int i = 0; i < 50; i++) {
                    net.minecraft.util.ChunkCoordinates c = w().provider.getRandomizedSpawnPoint();
                    exact &= c.posX == spawn.posX && c.posZ == spawn.posZ;
                }
                result("spawnRadius 0: everyone spawns on the spawn block", exact, "");
                cmd("gamerule spawnRadius 3");
                int min = 99;
                int max = -99;
                for (int i = 0; i < 400; i++) {
                    net.minecraft.util.ChunkCoordinates c = w().provider.getRandomizedSpawnPoint();
                    min = Math.min(min, Math.min(c.posX - spawn.posX, c.posZ - spawn.posZ));
                    max = Math.max(max, Math.max(c.posX - spawn.posX, c.posZ - spawn.posZ));
                }
                result("spawnRadius 3: spread -3..3 with both ends (1.12.2 nextInt(2r+1)-r)", min == -3 && max == 3, min + ".." + max);
                cmd("gamerule spawnRadius 10");
                // the elytra limit of "moved too quickly"
                ru.codex.offhand1710.OffhandInventory inv = ru.codex.offhand1710.OffhandInventory.get(p());
                boolean was = inv.elytraFlying;
                double walk = ru.codex.offhand1710.Rules112.speedLimit(p().playerNetServerHandler);
                inv.elytraFlying = true;
                double glide = ru.codex.offhand1710.Rules112.speedLimit(p().playerNetServerHandler);
                cmd("gamerule disableElytraMovementCheck true");
                double off = ru.codex.offhand1710.Rules112.speedLimit(p().playerNetServerHandler);
                inv.elytraFlying = was;
                cmd("gamerule disableElytraMovementCheck false");
                result("moved too quickly: 100 walking, 300 gliding, none with disableElytraMovementCheck", walk == 100.0D && glide == 300.0D && off == Double.MAX_VALUE, walk + " " + glide + " " + off);
                // showDeathMessages: the owner of a tamed wolf hears how it died
                chat.keys.clear();
                pet = new net.minecraft.entity.passive.EntityWolf(w());
                pet.setPosition(sx + 5.5D, sy, sz + 5.5D);
                pet.setTamed(true);
                pet.func_152115_b(p().getUniqueID().toString());
                w().spawnEntityInWorld(pet);
                pet.attackEntityFrom(net.minecraft.util.DamageSource.outOfWorld, 1000.0F);
                cmd("gamerule reducedDebugInfo true");
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("showDeathMessages: the owner is told the pet died", chat.seen("death.attack.outOfWorld"), String.valueOf(chat.keys));
                cmd("gamerule showDeathMessages false");
                chat.keys.clear();
                pet = new net.minecraft.entity.passive.EntityWolf(w());
                pet.setPosition(sx + 5.5D, sy, sz + 5.5D);
                pet.setTamed(true);
                pet.func_152115_b(p().getUniqueID().toString());
                w().spawnEntityInWorld(pet);
                pet.attackEntityFrom(net.minecraft.util.DamageSource.outOfWorld, 1000.0F);
                result("reducedDebugInfo reaches the client", RulesClient.reduced, "");
                java.util.ArrayList<String> left = new java.util.ArrayList<String>(java.util.Arrays.asList("Minecraft 1.7.10 (60 fps)", "C: 1/2", "E: 1/2", "P: 0", "Overworld", null, "x: 1.0", "y: 64.0", "z: 1.0", "f: 0", "lc: 1 b: Plains"));
                net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                net.minecraftforge.client.event.RenderGameOverlayEvent parent = new net.minecraftforge.client.event.RenderGameOverlayEvent(0.0F, res, 0, 0);
                new RulesClient().onText(new net.minecraftforge.client.event.RenderGameOverlayEvent.Text(parent, left, new java.util.ArrayList<String>()));
                result("reducedDebugInfo: F3 keeps the frame and chunk lines, not the position", left.size() == 6 && left.get(4).equals("Overworld"), String.valueOf(left));
                cmd("gamerule reducedDebugInfo false");
                cmd("gamerule announceAdvancements false");
                cmd("advancement grant @p only minecraft:story/mine_stone");
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("showDeathMessages false: not a word", !chat.seen("death.attack"), String.valueOf(chat.keys));
                result("reducedDebugInfo false reaches the client", !RulesClient.reduced, "");
                result("announceAdvancements false: the advancement is not in chat", !chat.seen("chat.type.advancement"), String.valueOf(chat.keys));
                cmd("gamerule announceAdvancements true");
                cmd("gamerule showDeathMessages true");
                cmd("advancement grant @p only minecraft:story/upgrade_tools");
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("announceAdvancements true: the next one is", chat.seen("chat.type.advancement"), String.valueOf(chat.keys));
            }
        });
        finish();
    }

    private int countOf(String registry, int meta) {
        int n = 0;
        for (net.minecraft.item.ItemStack s : p().inventory.mainInventory) {
            if (s != null && registry.equals(net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem())) && (meta < 0 || s.getItemDamage() == meta)) {
                n += s.stackSize;
            }
        }
        return n;
    }

    private String blockAt(int x, int y, int z) {
        return net.minecraft.block.Block.blockRegistry.getNameForObject(w().getBlock(x, y, z)) + ":" + w().getBlockMetadata(x, y, z);
    }

    /** Item, block and entity ids of 1.12.2 in /give, /clear, /setblock, /testforblock, /fill, /summon (Names112). */
    private void names() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(chat);
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("gamerule doMobSpawning false");
                p().inventory.clearInventory(null, -1);
                sx = (int) Math.floor(p().posX) + 6;
                sy = (int) Math.floor(p().posY) + 3;
                sz = (int) Math.floor(p().posZ);
                String n = p().getCommandSenderName();
                cmd("give " + n + " minecraft:concrete 3 5");
                cmd("give " + n + " concrete");
                cmd("give " + n + " stone 2 1");
                cmd("give " + n + " minecraft:purpur_block");
                cmd("give " + n + " minecraft:nope");
                cmd("setblock " + sx + " " + sy + " " + sz + " minecraft:concrete color=lime");
                cmd("setblock " + (sx + 1) + " " + sy + " " + sz + " stone variant=granite");
                cmd("setblock " + (sx + 2) + " " + sy + " " + sz + " minecraft:purpur_block");
                cmd("setblock " + (sx + 3) + " " + sy + " " + sz + " minecraft:concrete 14");
                cmd("setblock " + (sx + 4) + " " + sy + " " + sz + " minecraft:log axis=x,variant=birch");
                cmd("setblock " + (sx + 5) + " " + sy + " " + sz + " stone variant=nonsense");
                cmd("fill " + sx + " " + (sy + 1) + " " + sz + " " + (sx + 2) + " " + (sy + 1) + " " + sz + " minecraft:concrete color=red");
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                result("/give minecraft:concrete 3 5 gives lime concrete", countOf("mc112bridge:concrete_lime", -1) == 3, "lime=" + countOf("mc112bridge:concrete_lime", -1));
                result("/give concrete (no data) gives white concrete", countOf("mc112bridge:concrete_white", -1) == 1, "");
                result("/give stone 2 1 gives granite (a block of its own here)", countOf("mc112bridge:granite", 0) == 2, java.util.Arrays.toString(p().inventory.mainInventory));
                result("/give minecraft:purpur_block (a block of this mod)", countOf("mc112bridge:purpur_block", -1) == 1, "");
                result("/give of an unknown item says so", chat.seen("commands.give.item.notFound#1"), "");
                result("/setblock minecraft:concrete color=lime", blockAt(sx, sy, sz).equals("mc112bridge:concrete_lime:0"), blockAt(sx, sy, sz));
                result("/setblock stone variant=granite", blockAt(sx + 1, sy, sz).equals("mc112bridge:granite:0"), blockAt(sx + 1, sy, sz));
                result("/setblock minecraft:purpur_block", blockAt(sx + 2, sy, sz).equals("mc112bridge:purpur_block:0"), blockAt(sx + 2, sy, sz));
                result("/setblock minecraft:concrete 14 is red concrete", blockAt(sx + 3, sy, sz).equals("mc112bridge:concrete_red:0"), blockAt(sx + 3, sy, sz));
                result("/setblock minecraft:log axis=x,variant=birch is metadata 6", blockAt(sx + 4, sy, sz).equals("minecraft:log:6"), blockAt(sx + 4, sy, sz));
                result("/setblock refuses a state the block does not have", chat.seen("commands.generic.blockstate.invalid#2") && w().isAirBlock(sx + 5, sy, sz), "");
                result("/fill with a 1.12.2 state", blockAt(sx + 1, sy + 1, sz).equals("mc112bridge:concrete_red:0") && blockAt(sx + 2, sy + 1, sz).equals("mc112bridge:concrete_red:0"), blockAt(sx + 1, sy + 1, sz));
                chat.keys.clear();
                cmd("testforblock " + (sx + 1) + " " + sy + " " + sz + " stone variant=granite");
                cmd("testforblock " + sx + " " + sy + " " + sz + " minecraft:concrete 5");
                cmd("testforblock " + (sx + 3) + " " + sy + " " + sz + " minecraft:concrete");
                String n = p().getCommandSenderName();
                cmd("clear " + n + " minecraft:concrete 5 0");
                cmd("clear " + n + " minecraft:concrete 5 2");
                cmd("summon pig " + sx + " " + (sy + 3) + " " + (sz + 3));
                cmd("summon minecraft:pig " + sx + " " + (sy + 3) + " " + (sz + 3));
                cmd("summon Pig " + sx + " " + (sy + 3) + " " + (sz + 3));
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                int ok = 0;
                synchronized (chat.keys) {
                    for (String k : chat.keys) {
                        ok += k.startsWith("commands.testforblock.success") ? 1 : 0;
                    }
                }
                result("/testforblock with a state, with data, and red concrete for no data finds red (any data)", ok == 3, String.valueOf(chat.keys));
                result("/clear maxCount 0 only counts (3 lime)", chat.seen("commands.clear.testing#2"), "");
                result("/clear maxCount 2 removes two", countOf("mc112bridge:concrete_lime", -1) == 1, "lime=" + countOf("mc112bridge:concrete_lime", -1));
                java.util.List<?> pigs = w().getEntitiesWithinAABB(net.minecraft.entity.passive.EntityPig.class,
                    net.minecraft.util.AxisAlignedBB.getBoundingBox(sx - 4, sy - 12, sz - 1, sx + 4, sy + 6, sz + 7));
                result("/summon pig, minecraft:pig and Pig", pigs.size() == 3, "pigs=" + pigs.size());
                for (Object o : pigs) {
                    ((net.minecraft.entity.Entity) o).setDead();
                }
            }
        });
        finish();
    }

    private int score(String objective) {
        net.minecraft.scoreboard.Scoreboard b = w().getScoreboard();
        net.minecraft.scoreboard.ScoreObjective o = b.getObjective(objective);
        return o == null ? Integer.MIN_VALUE : b.func_96529_a(p().getCommandSenderName(), o).getScorePoints();
    }

    private int seenCount(String prefix) {
        int n = 0;
        synchronized (chat.keys) {
            for (String k : chat.keys) {
                n += k.startsWith(prefix) ? 1 : 0;
            }
        }
        return n;
    }

    /** /fill replace, /clone filtered and move, /testforblock(s), /stats, /trigger, /time query (BlockCommands112, Stats112). */
    private void commands2() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(chat);
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("gamerule doMobSpawning false");
                sx = (int) Math.floor(p().posX) + 8;
                sy = (int) Math.floor(p().posY) + 2;
                sz = (int) Math.floor(p().posZ);
                String a = sx + " " + sy + " " + sz;
                String b = (sx + 4) + " " + sy + " " + (sz + 4);
                cmd("fill " + sx + " " + (sy - 1) + " " + sz + " " + (sx + 30) + " " + (sy + 8) + " " + (sz + 10) + " air");
                // a 5x5 floor of stone with a granite cross, then only the granite is replaced by red concrete
                cmd("fill " + a + " " + b + " stone");
                cmd("fill " + (sx + 2) + " " + sy + " " + sz + " " + (sx + 2) + " " + sy + " " + (sz + 4) + " stone variant=granite");
                cmd("fill " + a + " " + b + " minecraft:concrete 14 replace stone variant=granite");
                cmd("fill " + a + " " + b + " glass 0 replace minecraft:stone 0");
                cmd("setblock " + sx + " " + (sy + 1) + " " + sz + " minecraft:log axis=x,variant=birch");
                cmd("testforblock " + sx + " " + (sy + 1) + " " + sz + " minecraft:log axis=x");
                cmd("testforblock " + sx + " " + (sy + 1) + " " + sz + " minecraft:log axis=z");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/fill replace stone variant=granite only hits the granite", blockAt(sx + 2, sy, sz + 1).equals("mc112bridge:concrete_red:0")
                    && blockAt(sx, sy, sz).equals("minecraft:glass:0"), blockAt(sx + 2, sy, sz + 1) + " " + blockAt(sx, sy, sz));
                result("/fill success says how many", chat.seen("commands.fill.success#1"), "");
                result("/testforblock with only axis=x finds the log, axis=z does not", seenCount("commands.testforblock.success") == 1
                    && chat.seen("commands.testforblock.failed.data"), String.valueOf(chat.keys));
                chat.keys.clear();
                String a = sx + " " + sy + " " + sz;
                String b = (sx + 4) + " " + (sy + 1) + " " + (sz + 4);
                cmd("clone " + a + " " + b + " " + (sx + 10) + " " + sy + " " + sz);
                cmd("testforblocks " + a + " " + b + " " + (sx + 10) + " " + sy + " " + sz);
                cmd("clone " + a + " " + b + " " + (sx + 20) + " " + sy + " " + sz + " filtered normal minecraft:concrete 14");
                cmd("clone " + a + " " + b + " " + (sx + 2) + " " + sy + " " + sz);
                cmd("testforblocks " + a + " " + b + " " + (sx + 20) + " " + sy + " " + sz);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/clone copies, /testforblocks finds the copy equal", blockAt(sx + 12, sy, sz + 1).equals("mc112bridge:concrete_red:0")
                    && blockAt(sx + 10, sy + 1, sz).equals("minecraft:log:6") && chat.seen("commands.compare.success#1"), String.valueOf(chat.keys));
                result("/clone filtered takes only red concrete", blockAt(sx + 22, sy, sz + 1).equals("mc112bridge:concrete_red:0")
                    && w().isAirBlock(sx + 20, sy, sz), blockAt(sx + 20, sy, sz));
                result("/clone onto itself is refused", chat.seen("commands.clone.noOverlap"), "");
                result("/testforblocks of a different area fails", chat.seen("commands.compare.failed"), "");
                chat.keys.clear();
                String n = p().getCommandSenderName();
                cmd("clone " + (sx + 10) + " " + sy + " " + sz + " " + (sx + 14) + " " + (sy + 1) + " " + (sz + 4) + " " + (sx + 12) + " " + sy + " " + sz + " replace move");
                // /stats: success count, affected blocks, query result
                cmd("scoreboard objectives add s_ok dummy");
                cmd("scoreboard objectives add s_blocks dummy");
                cmd("scoreboard objectives add s_query dummy");
                cmd("scoreboard objectives add s_none dummy");
                cmd("scoreboard players set " + n + " s_ok 99");
                cmd("scoreboard players set " + n + " s_blocks 99");
                cmd("scoreboard players set " + n + " s_query 99");
                cmd("stats entity " + n + " set SuccessCount " + n + " s_ok");
                cmd("stats entity " + n + " set AffectedBlocks @p s_blocks");
                cmd("stats entity " + n + " set QueryResult " + n + " s_query");
                cmd("stats entity " + n + " set AffectedItems " + n + " s_none");
                cmd("fill " + sx + " " + (sy + 5) + " " + sz + " " + (sx + 2) + " " + (sy + 5) + " " + (sz + 1) + " stone");
                cmd("time set 6000");
                cmd("time query daytime");
                cmd("give " + n + " stone 5");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/clone move leaves the source empty", w().isAirBlock(sx + 10, sy, sz) && blockAt(sx + 14, sy, sz + 1).equals("mc112bridge:concrete_red:0"),
                    blockAt(sx + 10, sy, sz) + " " + blockAt(sx + 14, sy, sz + 1));
                result("/stats AffectedBlocks through a selector: 6 blocks filled", score("s_blocks") == 6, "s_blocks=" + score("s_blocks"));
                result("/stats QueryResult of time query daytime", score("s_query") == 6000, "s_query=" + score("s_query"));
                result("/time query daytime answers", chat.seen("commands.time.query#1"), "");
                result("/stats SuccessCount after the last command (give)", score("s_ok") == 1, "s_ok=" + score("s_ok"));
                net.minecraft.scoreboard.ScoreObjective none = w().getScoreboard().getObjective("s_none");
                result("/stats only writes a score that exists (as 1.12.2)", none != null && !w().getScoreboard().func_96510_d(p().getCommandSenderName()).containsKey(none), "");
                chat.keys.clear();
                String n = p().getCommandSenderName();
                cmd("scoreboard objectives add trig trigger");
                cmd("scoreboard players set " + n + " trig 0");
                cmd("trigger trig add 5");
                cmd("blah_no_such_command");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/stats SuccessCount 0 after an unknown command", score("s_ok") == 0, "s_ok=" + score("s_ok"));
                result("the trigger criterion exists", w().getScoreboard().getObjective("trig") != null
                    && w().getScoreboard().getObjective("trig").getCriteria() == ru.codex.offhand1710.Stats112.TRIGGER, "");
                result("/trigger before enable is refused", chat.seen("commands.trigger.disabled") && score("trig") == 0, "trig=" + score("trig"));
                String n = p().getCommandSenderName();
                cmd("scoreboard players enable " + n + " trig");
                cmd("trigger trig add 5");
                cmd("trigger trig set 50");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                result("/trigger after enable adds once, then locks again", score("trig") == 5 && chat.seen("commands.scoreboard.players.enable.success"),
                    "trig=" + score("trig") + " " + chat.keys);
                cmd("stats entity " + p().getCommandSenderName() + " clear SuccessCount");
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                result("/stats clear says so", chat.seen("commands.stats.cleared#1"), "");
            }
        });
        finish();
    }

    private double borderStart;
    private float borderHealth;
    private net.minecraft.entity.passive.EntityPig borderPig;

    /** The world border of 1.12.2 (Border112): command, timed change, damage, wall, no building outside, level.dat, Nether. */
    private void border() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(chat);
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                cmd("gamerule doMobSpawning false");
                cmd("worldborder get");
                sx = (int) Math.floor(p().posX);
                sy = (int) Math.floor(p().posY);
                sz = (int) Math.floor(p().posZ);
                cmd("worldborder center " + sx + " " + sz);
                cmd("worldborder set 40");
                look(sx + 0.5D, sy + 1, sz + 15.5D);
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.Border112 b = ru.codex.offhand1710.Border112.of(w());
                result("/worldborder get of a new world: 60000000", chat.seen("commands.worldborder.get.success#1"), String.valueOf(chat.keys));
                result("/worldborder center and set 40", Math.abs(b.centerX - (sx + 0.5D)) < 1.0E-6 && b.diameter() == 40.0D
                    && chat.seen("commands.worldborder.center.success#2") && chat.seen("commands.worldborder.set.success#2"), b.centerX + " " + b.diameter());
                result("the client got the border", ru.codex.offhand1710.Border112.client.diameter() == 40.0D
                    && Math.abs(ru.codex.offhand1710.Border112.client.centerX - b.centerX) < 1.0E-6, "client=" + ru.codex.offhand1710.Border112.client.diameter());
                result("the wall is blue when still", b.color() == 0x20A0FF, Integer.toHexString(b.color()));
                int far = ru.codex.offhand1710.Border112.clampX(sx + 1000, -29999872, 29999872, w());
                result("a portal trip lands 16 blocks inside the border (transferEntityToWorld)", far == (int) (b.centerX + 20.0D - 16.0D), "x=" + (far - sx));
                double[] box = b.box(w());
                net.minecraft.world.World nether = MinecraftServer.getServer().worldServerForDimension(-1);
                double[] nb = b.box(nether);
                result("the Nether border has the centre / 8 and the same size", Math.abs((nb[0] + nb[2]) / 2 - b.centerX / 8.0D) < 1.0E-6
                    && Math.abs(nb[2] - nb[0] - 40.0D) < 1.0E-6, nb[0] + ".." + nb[2]);
                // the wall: a pig 1 block inside pushed 5 blocks out stops at the line
                borderPig = new net.minecraft.entity.passive.EntityPig(w());
                borderPig.setPosition(box[2] - 1.5D, sy, sz + 0.5D);
                w().spawnEntityInWorld(borderPig);
                borderPig.moveEntity(5.0D, 0.0D, 0.0D);
                // the block the line crosses still counts as inside (WorldBorder.contains of 1.12.2), so the wall is at its far side
                result("an entity inside cannot cross the border", borderPig.boundingBox.maxX <= Math.ceil(box[2]) + 1.0E-6 && borderPig.boundingBox.maxX > box[2] - 1.0D, String.format("maxX=%.3f border=%.3f", borderPig.boundingBox.maxX, box[2]));
                // no using or digging a block outside
                net.minecraftforge.event.entity.player.PlayerInteractEvent use = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p(),
                    net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, (int) box[2] + 2, sy - 1, sz, 1, w());
                net.minecraftforge.event.entity.player.PlayerInteractEvent dig = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p(),
                    net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, (int) box[2] + 2, sy - 1, sz, 1, w());
                net.minecraftforge.event.entity.player.PlayerInteractEvent inside = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(p(),
                    net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, sx, sy - 1, sz, 1, w());
                result("no using or digging blocks outside, inside is fine", use.isCanceled() && dig.isCanceled() && !inside.isCanceled(), "");
                // level.dat keeps it under the keys of 1.12.2
                net.minecraft.nbt.NBTTagCompound n = new net.minecraft.nbt.NBTTagCompound();
                ru.codex.offhand1710.Border112.write(w().getWorldInfo(), n);
                result("level.dat: BorderCenterX, BorderSize and the rest", n.getDouble("BorderSize") == 40.0D && n.getDouble("BorderCenterX") == b.centerX
                    && n.getDouble("BorderSafeZone") == 5.0D && n.getDouble("BorderDamagePerBlock") == 0.2D && n.getDouble("BorderWarningTime") == 15.0D, String.valueOf(n));
                // damage: a survival player 10 blocks out, past the buffer of 5, loses 1 per hurt
                cmd("gamemode 0");
                p().setHealth(20.0F);
                p().setPositionAndUpdate(box[2] + 10.5D, sy, sz + 0.5D);
                chat.keys.clear();
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                borderHealth = p().getHealth();
                result("outside past the buffer the player is hurt", borderHealth < 20.0F, "health=" + borderHealth);
                double[] box = ru.codex.offhand1710.Border112.of(w()).box(w());
                p().setPositionAndUpdate(box[2] - 2.5D, sy, sz + 0.5D);
                p().setHealth(20.0F);
                cmd("gamemode 1");
                cmd("worldborder damage buffer 2");
                cmd("worldborder damage amount 0.5");
                cmd("worldborder warning distance 8");
                cmd("worldborder warning time 20");
                borderStart = ru.codex.offhand1710.Border112.of(w()).diameter();
                cmd("worldborder set 20 4");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.Border112 b = ru.codex.offhand1710.Border112.of(w());
                result("damage, buffer and warning set", b.damageBuffer == 2.0D && b.damageAmount == 0.5D && b.warningDistance == 8 && b.warningTime == 20
                    && chat.seen("commands.worldborder.damage.amount.success#2") && chat.seen("commands.worldborder.warning.time.success#2"), String.valueOf(chat.keys));
                double now = b.diameter();
                result("set 20 over 4 s shrinks in real time and turns red", now < borderStart && now > 20.0D && b.color() == 0xFF3030
                    && chat.seen("commands.worldborder.setSlowly.shrink.success#3"), String.format("%.2f", now));
                result("the client shrinks it too", ru.codex.offhand1710.Border112.client.status() == 2, "");
                result("2.5 blocks from a shrinking border the screen goes red", Border112Client.tint() > 0.0F, String.format("tint=%.2f", Border112Client.tint()));
                double[] box = b.box(w());
                look(box[2] + 6.0D, sy + 1.5D, sz + 0.5D);
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("border");
            }
        });
        server(100, new Runnable() {
            @Override
            public void run() {
                ru.codex.offhand1710.Border112 b = ru.codex.offhand1710.Border112.of(w());
                result("after 4 s the border stands at 20", b.diameter() == 20.0D && b.status() == 0, String.format("%.2f", b.diameter()));
                cmd("worldborder add 10");
                result("/worldborder add 10 makes 30", b.diameter() == 30.0D, String.format("%.2f", b.diameter()));
                cmd("worldborder set 60000000");
                cmd("worldborder center 0 0");
                cmd("worldborder damage buffer 5");
                cmd("worldborder damage amount 0.2");
                cmd("worldborder warning distance 5");
                cmd("worldborder warning time 15");
                borderPig.setDead();
            }
        });
        finish();
    }

    private net.minecraft.item.ItemStack craftIn2x2(net.minecraft.item.ItemStack... stacks) {
        net.minecraft.inventory.ContainerPlayer c = (net.minecraft.inventory.ContainerPlayer) p().inventoryContainer;
        for (int i = 0; i < 4; i++) {
            c.craftMatrix.setInventorySlotContents(i, i < stacks.length && stacks[i] != null ? stacks[i].copy() : null);
        }
        net.minecraft.item.ItemStack out = c.craftResult.getStackInSlot(0);
        for (int i = 0; i < 4; i++) {
            c.craftMatrix.setInventorySlotContents(i, null);
        }
        return out;
    }

    private static String name(net.minecraft.item.ItemStack s) {
        return s == null ? "nothing" : net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem());
    }

    /** doLimitedCrafting of 1.12.2 (Crafting112): the grid gives only unlocked recipes, special recipes always. */
    private void crafting() {
        server(40, new Runnable() {
            @Override
            public void run() {
                String n = p().getCommandSenderName();
                cmd("gamemode 1");
                cmd("recipe take " + n + " *");
                net.minecraft.item.ItemStack planks = new net.minecraft.item.ItemStack(Blocks.planks);
                result("rule off: an unknown recipe crafts as before", "minecraft:crafting_table".equals(name(craftIn2x2(planks, planks, planks, planks))), "");
                cmd("gamerule doLimitedCrafting true");
                String got = name(craftIn2x2(planks, planks, planks, planks));
                result("rule on: the crafting table is not unlocked, no result", "nothing".equals(got), got);
                got = name(craftIn2x2(new net.minecraft.item.ItemStack(net.minecraft.init.Items.paper), new net.minecraft.item.ItemStack(net.minecraft.init.Items.gunpowder)));
                result("rule on: a special recipe (firework rocket) still crafts", "minecraft:fireworks".equals(got), got);
                cmd("recipe give " + n + " minecraft:crafting_table");
                got = name(craftIn2x2(planks, planks, planks, planks));
                result("rule on: after unlocking it crafts", "minecraft:crafting_table".equals(got), got);
                cmd("recipe take " + n + " minecraft:crafting_table");
            }
        });
        client(30, new Runnable() {
            @Override
            public void run() {
                result("the client knows the rule", RulesClient.limitedCrafting, "");
                net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                    @Override
                    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer pl) {
                        return false;
                    }
                }, 2, 2);
                for (int i = 0; i < 4; i++) {
                    grid.setInventorySlotContents(i, new net.minecraft.item.ItemStack(Blocks.planks));
                }
                net.minecraft.item.ItemStack out = net.minecraft.item.crafting.CraftingManager.getInstance().findMatchingRecipe(grid, mc.theWorld);
                result("the client grid hides a locked recipe too", out == null, name(out));
            }
        });
        server(10, new Runnable() {
            @Override
            public void run() {
                cmd("gamerule doLimitedCrafting false");
            }
        });
        finish();
    }

    private int specCx;
    private int specCz;

    /** spectatorsGenerateChunks of 1.12.2 (ChunkStream): a spectator far out generates nothing, a player there does. */
    private String specTrace = "";

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onChunkLoad(net.minecraftforge.event.world.ChunkEvent.Load event) {
        if (specTrace.isEmpty() && "specchunks".equals(STAGE) && !event.world.isRemote && event.getChunk().xPosition == specCx && event.getChunk().zPosition == specCz) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
                sb.append(" | ").append(el);
            }
            specTrace = sb.toString();
        }
    }

    private void spectatorChunks() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamerule spectatorsGenerateChunks false");
                cmd("gamemode 3");
                double x = p().posX + 5000.0D;
                double z = p().posZ + 5000.0D;
                specCx = (int) Math.floor(x) >> 4;
                specCz = (int) Math.floor(z) >> 4;
                result("the target chunk does not exist yet", !w().theChunkProviderServer.chunkExists(specCx, specCz), "");
                p().setPositionAndUpdate(x, 120.0D, z);
            }
        });
        server(80, new Runnable() {
            @Override
            public void run() {
                result("a spectator does not generate the chunk under it", !w().theChunkProviderServer.chunkExists(specCx, specCz)
                    && ru.codex.offhand1710.ChunkStream.heldForSpectators(specCx, specCz), "skipped=" + ru.codex.offhand1710.ChunkStream.spectatorSkipped
                    + " spectator=" + ru.codex.offhand1710.Spectator112.is(p()) + " rule=" + w().getGameRules().getGameRuleStringValue("spectatorsGenerateChunks")
                    + " reasons=" + java.util.Arrays.toString(ru.codex.offhand1710.ChunkStream.REASONS));
                log("TRACE" + specTrace);
                cmd("gamemode 1");
            }
        });
        server(60, new Runnable() {
            @Override
            public void run() {
                result("as a player it gets generated", w().theChunkProviderServer.chunkExists(specCx, specCz), "");
                cmd("gamerule spectatorsGenerateChunks true");
                cmd("gamemode 3");
                double x = p().posX + 3000.0D;
                specCx = (int) Math.floor(x) >> 4;
                specCz = (int) Math.floor(p().posZ) >> 4;
                p().setPositionAndUpdate(x, 120.0D, p().posZ);
            }
        });
        server(80, new Runnable() {
            @Override
            public void run() {
                result("with the rule on (default) a spectator generates as before", w().theChunkProviderServer.chunkExists(specCx, specCz), "");
                cmd("gamemode 1");
            }
        });
        finish();
    }

    private net.minecraft.entity.boss.EntityDragon fightDragon;
    private double[] dragonStart;
    private int fireballsBefore;
    private int breathsBefore;

    private net.minecraft.world.WorldServer end() {
        return MinecraftServer.getServer().worldServerForDimension(1);
    }

    private net.minecraft.entity.boss.EntityDragon findDragon() {
        for (Object o : end().loadedEntityList) {
            if (o instanceof net.minecraft.entity.boss.EntityDragon && !((net.minecraft.entity.Entity) o).isDead) {
                return (net.minecraft.entity.boss.EntityDragon) o;
            }
        }
        return null;
    }

    private String phases() {
        StringBuilder b = new StringBuilder(ru.codex.offhand1710.Dragon112.debug(fightDragon) + " | ");
        for (int i : ru.codex.offhand1710.Dragon112.state(fightDragon).history) {
            b.append(ru.codex.offhand1710.Dragon112.NAMES[i]).append(' ');
        }
        return b.toString();
    }

    private boolean saw(int phase) {
        return ru.codex.offhand1710.Dragon112.state(fightDragon).history.contains(phase);
    }

    /** The ender fightDragon of 1.12.2 (Dragon112): phases, landing, breath, strafing, damage, death. */
    private void dragonStage() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                p().travelToDimension(1);
            }
        });
        server(200, new Runnable() {
            @Override
            public void run() {
                fightDragon = findDragon();
                result("the End has a fightDragon", fightDragon != null, "dimension=" + p().dimension);
                if (fightDragon == null) {
                    return;
                }
                result("a new fight starts circling (HoldingPattern)", ru.codex.offhand1710.Dragon112.phase(fightDragon) == ru.codex.offhand1710.Dragon112.HOLDING
                    || saw(ru.codex.offhand1710.Dragon112.HOLDING), phases());
                dragonStart = new double[] {fightDragon.posX, fightDragon.posY, fightDragon.posZ};
                p().setPositionAndUpdate(0.5D, 140.0D, 0.5D);
                p().capabilities.isFlying = true;
            }
        });
        server(200, new Runnable() {
            @Override
            public void run() {
                if (fightDragon == null) {
                    return;
                }
                double moved = Math.sqrt(sq(fightDragon.posX - dragonStart[0]) + sq(fightDragon.posZ - dragonStart[2]));
                double r = Math.sqrt(fightDragon.posX * fightDragon.posX + fightDragon.posZ * fightDragon.posZ);
                result("it flies around the pillars (moved, within 100 of the centre, 60-200 high)", moved > 20.0D && r < 100.0D && fightDragon.posY > 60.0D && fightDragon.posY < 200.0D,
                    String.format("moved=%.0f r=%.0f y=%.0f %s", moved, r, fightDragon.posY, phases()));
                // a survival player waits by the portal: landing, scanning, the roar and the breath follow
                cmd("gamemode 0");
                p().capabilities.isFlying = false;
                p().setHealth(20.0F);
                int top = end().getHeightValue(0, 0);
                p().setPositionAndUpdate(15.5D, end().getHeightValue(15, 0) + 1, 0.5D);
                // the wings of a landing dragon kill a player on the portal (as in 1.12.2): resistance V keeps it alive
                p().addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.resistance.id, 6000, 4));
                p().addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.regeneration.id, 6000, 4));
                // the wings knock the player out of the 20 blocks the sitting dragon looks in: it stays put
                p().getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0D);
                breathsBefore = ru.codex.offhand1710.Dragon112.breaths;
                ru.codex.offhand1710.Dragon112.setPhase(fightDragon, ru.codex.offhand1710.Dragon112.state(fightDragon), ru.codex.offhand1710.Dragon112.LANDING_APPROACH);
            }
        });
        server(900, new Runnable() {
            @Override
            public void run() {
                if (fightDragon == null) {
                    return;
                }
                p().setHealth(20.0F);
                result("it lands on the portal: LandingApproach, Landing, SittingScanning", saw(ru.codex.offhand1710.Dragon112.LANDING)
                    && saw(ru.codex.offhand1710.Dragon112.SITTING_SCANNING), String.format("%s pos=%.0f,%.0f,%.0f", phases(), fightDragon.posX, fightDragon.posY, fightDragon.posZ));
                result("a player near the sitting dragon: SittingAttacking, then SittingFlaming with a breath cloud",
                    saw(ru.codex.offhand1710.Dragon112.SITTING_ATTACKING) && saw(ru.codex.offhand1710.Dragon112.SITTING_FLAMING)
                    && ru.codex.offhand1710.Dragon112.breaths > breathsBefore, "breaths=" + (ru.codex.offhand1710.Dragon112.breaths - breathsBefore));
                p().getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.knockbackResistance).setBaseValue(0.0D);
                // damage: a sitting dragon shrugs arrows off
                ru.codex.offhand1710.Dragon112.setPhase(fightDragon, ru.codex.offhand1710.Dragon112.state(fightDragon), ru.codex.offhand1710.Dragon112.SITTING_SCANNING);
                float before = fightDragon.getHealth();
                net.minecraft.entity.projectile.EntityArrow arrow = new net.minecraft.entity.projectile.EntityArrow(end(), p(), 1.0F);
                fightDragon.attackEntityFromPart(fightDragon.dragonPartBody, net.minecraft.util.DamageSource.causeArrowDamage(arrow, p()), 8.0F);
                result("arrows do not hurt a sitting dragon, they catch fire", fightDragon.getHealth() == before && arrow.isBurning(), "health " + before + " -> " + fightDragon.getHealth());
                cmd("gamemode 1");
                p().setPositionAndUpdate(0.5D, 140.0D, 0.5D);
                ru.codex.offhand1710.Dragon112.setPhase(fightDragon, ru.codex.offhand1710.Dragon112.state(fightDragon), ru.codex.offhand1710.Dragon112.TAKEOFF);
            }
        });
        server(200, new Runnable() {
            @Override
            public void run() {
                if (fightDragon == null) {
                    return;
                }
                result("it takes off again", saw(ru.codex.offhand1710.Dragon112.TAKEOFF) && ru.codex.offhand1710.Dragon112.phase(fightDragon) != ru.codex.offhand1710.Dragon112.SITTING_SCANNING, phases());
                float before = fightDragon.getHealth();
                fightDragon.attackEntityFromPart(fightDragon.dragonPartBody, net.minecraft.util.DamageSource.causePlayerDamage(p()), 8.0F);
                float body = before - fightDragon.getHealth();
                fightDragon.hurtResistantTime = 0;
                before = fightDragon.getHealth();
                fightDragon.attackEntityFromPart(fightDragon.dragonPartHead, net.minecraft.util.DamageSource.causePlayerDamage(p()), 8.0F);
                float head = before - fightDragon.getHealth();
                result("a body hit of 8 takes 8/4 + 1 = 3, the head takes 8", Math.abs(body - 3.0F) < 0.01F && Math.abs(head - 8.0F) < 0.01F, "body=" + body + " head=" + head);
                // strafing: a survival player in the open gets a fireball
                cmd("gamemode 0");
                p().capabilities.isFlying = false;
                p().setPositionAndUpdate(fightDragon.posX + 30.0D, end().getHeightValue((int) fightDragon.posX + 30, (int) fightDragon.posZ) + 1, fightDragon.posZ);
                fireballsBefore = ru.codex.offhand1710.Dragon112.fireballs;
                ru.codex.offhand1710.Dragon112.strafeAt(fightDragon, p());
            }
        });
        // the path to the player may take a long way round the pillars
        server(900, new Runnable() {
            @Override
            public void run() {
                if (fightDragon == null) {
                    return;
                }
                result("StrafePlayer shoots a fightDragon fireball, then back to HoldingPattern", ru.codex.offhand1710.Dragon112.fireballs > fireballsBefore
                    && saw(ru.codex.offhand1710.Dragon112.STRAFE), "fireballs=" + (ru.codex.offhand1710.Dragon112.fireballs - fireballsBefore) + " " + phases());
                cmd("gamemode 1");
                p().setPositionAndUpdate(20.5D, 140.0D, 20.5D);
                // death: down to 1 health, the last hit sends it to the portal, 12000 experience the first time
                int top = end().getHeightValue(0, 0);
                fightDragon.setPosition(0.0D, top + 6, 0.0D);
                fightDragon.setHealth(1.0F);
                fightDragon.hurtResistantTime = 0;
                // only a flying dragon goes to Dying: a sitting one just dies, as in 1.12.2 (getIsStationary)
                ru.codex.offhand1710.Dragon112.setPhase(fightDragon, ru.codex.offhand1710.Dragon112.state(fightDragon), ru.codex.offhand1710.Dragon112.HOLDING);
                fightDragon.attackEntityFromPart(fightDragon.dragonPartHead, net.minecraft.util.DamageSource.causePlayerDamage(p()), 20.0F);
                result("the killing blow turns it to Dying instead", ru.codex.offhand1710.Dragon112.phase(fightDragon) == ru.codex.offhand1710.Dragon112.DYING, phases());
            }
        });
        server(320, new Runnable() {
            @Override
            public void run() {
                int xp = ru.codex.offhand1710.Dragon112.xpDropped;
                for (Object o : end().loadedEntityList) {
                    if (o instanceof net.minecraft.entity.item.EntityXPOrb) {
                        ((net.minecraft.entity.Entity) o).setDead();
                    }
                }
                int top = end().getHeightValue(0, 0);
                boolean egg = false;
                boolean portal = false;
                for (int y = 0; y < 128; y++) {
                    egg |= end().getBlock(0, y, 0) == net.minecraft.init.Blocks.dragon_egg;
                    portal |= end().getBlock(2, y, 0) == net.minecraft.init.Blocks.end_portal;
                }
                result("the first fightDragon dies with 12000 experience, the egg and the exit portal", fightDragon != null && fightDragon.isDead && xp == 12000 && egg && portal,
                    "xp=" + xp + " egg=" + egg + " portal=" + portal + " dead=" + (fightDragon != null && fightDragon.isDead) + " " + phases());
                cmd("gamemode 1");
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                // the respawn: four crystals on the sides of the portal (the orbs removed a tick ago: a crystal needs room)
                int surface = ru.codex.offhand1710.End112.portalSurface(end());
                int[][] rim = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
                for (int[] r : rim) {
                    new net.minecraft.item.ItemStack(ru.codex.offhand1710.Content119.endCrystal).tryPlaceItemIntoWorld(p(), end(), r[0], surface, r[1], 1, 0.5F, 1.0F, 0.5F);
                }
                result("four crystals around the portal start the respawn and put the portal out", ru.codex.offhand1710.Fight112.stage != ru.codex.offhand1710.Fight112.NONE
                    && end().getBlock(2, surface, 0) != net.minecraft.init.Blocks.end_portal, "stage=" + ru.codex.offhand1710.Fight112.stage);
                p().setPositionAndUpdate(0.5D, surface + 20, 30.5D);
                look(0.5D, surface + 10, 0.5D);
            }
        });
        server(250, new Runnable() {
            @Override
            public void run() {
                boolean beams = true;
                int seen = 0;
                for (Object o : end().loadedEntityList) {
                    if (o instanceof ru.codex.offhand1710.End112.Crystal) {
                        ru.codex.offhand1710.End112.Crystal c = (ru.codex.offhand1710.End112.Crystal) o;
                        if (Math.abs(c.posX) < 5 && Math.abs(c.posZ) < 5) {
                            beams &= c.beam() != null;
                            seen++;
                        }
                    }
                }
                result("the ritual crystals beam while the pillars come back", ru.codex.offhand1710.Fight112.stage == ru.codex.offhand1710.Fight112.PILLARS && beams && seen == 4,
                    "stage=" + ru.codex.offhand1710.Fight112.stage + " t=" + ru.codex.offhand1710.Fight112.ticks + " crystals=" + seen);
            }
        });
        client(5, new Runnable() {
            @Override
            public void run() {
                shot("respawn");
            }
        });
        server(700, new Runnable() {
            @Override
            public void run() {
                net.minecraft.entity.boss.EntityDragon d = ru.codex.offhand1710.Fight112.summoned;
                int ritual = 0;
                int pillars = 0;
                boolean locked = false;
                for (Object o : end().loadedEntityList) {
                    if (o instanceof ru.codex.offhand1710.End112.Crystal && !((net.minecraft.entity.Entity) o).isDead) {
                        ru.codex.offhand1710.End112.Crystal c = (ru.codex.offhand1710.End112.Crystal) o;
                        if (Math.abs(c.posX) < 5 && Math.abs(c.posZ) < 5) {
                            ritual++;
                        } else {
                            pillars++;
                            locked |= c.invulnerable;
                        }
                    }
                }
                result("a new dragon circles, the ritual crystals are gone, ten pillar crystals breakable again",
                    d != null && !d.isDead && ru.codex.offhand1710.Fight112.stage == ru.codex.offhand1710.Fight112.NONE && ritual == 0 && pillars >= 10 && !locked,
                    "dragon=" + (d != null && !d.isDead) + " stage=" + ru.codex.offhand1710.Fight112.stage + " ritual=" + ritual + " pillars=" + pillars + " locked=" + locked);
            }
        });
        finish();
    }

    private static double sq(double v) {
        return v * v;
    }

    private float foodHealth;

    private float exhaustion() {
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        p().getFoodStats().writeNBT(tag);
        return tag.getFloat("foodExhaustionLevel");
    }

    /** FoodStats of 1.12.2 (Food112): fast healing from saturation, slow healing from 18 food. */
    private void foodStage() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                cmd("difficulty 2");
                p().getFoodStats().addStats(20, 1.0F);
                p().getFoodStats().setFoodSaturationLevel(20.0F);
                p().setHealth(10.0F);
                foodHealth = p().getHealth();
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                float healed = p().getHealth() - foodHealth;
                // every 10 ticks min(saturation, 6) / 6 = 1 health: about 4 in 40 ticks (1.7.10: none before 80 ticks)
                result("full food and saturation heals 1 every 10 ticks", healed >= 3.0F && healed <= 5.0F, "healed=" + healed + " sat=" + p().getFoodStats().getSaturationLevel());
                // 18 food, no saturation and no exhaustion left over from the fast healing
                net.minecraft.nbt.NBTTagCompound food = new net.minecraft.nbt.NBTTagCompound();
                food.setInteger("foodLevel", 18);
                food.setInteger("foodTickTimer", 0);
                food.setFloat("foodSaturationLevel", 0.0F);
                food.setFloat("foodExhaustionLevel", 0.0F);
                p().getFoodStats().readNBT(food);
                p().setHealth(10.0F);
                foodHealth = p().getHealth();
            }
        });
        server(170, new Runnable() {
            @Override
            public void run() {
                float healed = p().getHealth() - foodHealth;
                result("18 food without saturation heals 1 every 80 ticks", healed >= 1.0F && healed <= 3.0F, "healed=" + healed);
                // exhaustion of 1.9: a jump 0.05 (0.2 before), walking a metre nothing (0.01 before)
                float e0 = exhaustion();
                p().onGround = true;
                p().jump();
                float jump = exhaustion() - e0;
                e0 = exhaustion();
                p().onGround = true;
                p().addMovementStat(1.0D, 0.0D, 0.0D);
                float walk = exhaustion() - e0;
                result("a jump costs 0.05, walking costs nothing", Math.abs(jump - 0.05F) < 0.001F && Math.abs(walk) < 0.0001F, "jump=" + jump + " walk=" + walk);
                cmd("gamemode 1");
                p().setHealth(20.0F);
            }
        });
        finish();
    }

    /** CombatRules of 1.12.2 (Armor112): armor toughness, big hits go through armor. */
    private void armorStage() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 0");
                cmd("difficulty 2");
                // a zombie in full diamond: armor 20, toughness 8; a hit of 20 leaves 20 * (1 - 15/25) = 8 (1.7.10: 4)
                net.minecraft.entity.monster.EntityZombie z = new net.minecraft.entity.monster.EntityZombie(w());
                z.setPosition(p().posX + 4, p().posY, p().posZ);
                net.minecraft.item.Item[] diamond = {net.minecraft.init.Items.diamond_boots, net.minecraft.init.Items.diamond_leggings,
                    net.minecraft.init.Items.diamond_chestplate, net.minecraft.init.Items.diamond_helmet};
                for (int i = 0; i < 4; i++) {
                    z.setCurrentItemOrArmor(i + 1, new net.minecraft.item.ItemStack(diamond[i]));
                }
                w().spawnEntityInWorld(z);
                z.setHealth(20.0F);
                z.attackEntityFrom(net.minecraft.util.DamageSource.cactus, 20.0F);
                result("a zombie in diamond takes 8 of a hit of 20", Math.abs(20.0F - z.getHealth() - 8.0F) < 0.01F, "took=" + (20.0F - z.getHealth()));
                z.setDead();
                // the player in full diamond: the same 8
                for (int i = 0; i < 4; i++) {
                    p().inventory.armorInventory[i] = new net.minecraft.item.ItemStack(diamond[i]);
                }
                p().setHealth(20.0F);
                p().hurtResistantTime = 0;
                p().attackEntityFrom(net.minecraft.util.DamageSource.cactus, 20.0F);
                result("a player in diamond takes 8 of 20 and the armor wears by 5", Math.abs(20.0F - p().getHealth() - 8.0F) < 0.01F
                    && p().inventory.armorInventory[2].getItemDamage() == 5, "took=" + (20.0F - p().getHealth()) + " wear=" + p().inventory.armorInventory[2].getItemDamage());
                // leather: armor 7, no toughness; 10 * (1 - 2/25) = 9.2
                net.minecraft.item.Item[] leather = {net.minecraft.init.Items.leather_boots, net.minecraft.init.Items.leather_leggings,
                    net.minecraft.init.Items.leather_chestplate, net.minecraft.init.Items.leather_helmet};
                for (int i = 0; i < 4; i++) {
                    p().inventory.armorInventory[i] = new net.minecraft.item.ItemStack(leather[i]);
                }
                p().setHealth(20.0F);
                p().hurtResistantTime = 0;
                p().attackEntityFrom(net.minecraft.util.DamageSource.cactus, 10.0F);
                result("a player in leather takes 9.2 of 10", Math.abs(20.0F - p().getHealth() - 9.2F) < 0.01F, "took=" + (20.0F - p().getHealth()));
                // protection of 1.12.2: protection IV = 4, feather falling IV against a fall = 12, no random part
                net.minecraft.item.ItemStack chest = new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_chestplate);
                chest.addEnchantment(net.minecraft.enchantment.Enchantment.protection, 4);
                net.minecraft.item.ItemStack boots = new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_boots);
                boots.addEnchantment(net.minecraft.enchantment.Enchantment.featherFalling, 4);
                net.minecraft.item.ItemStack[] set = {boots, null, chest, null};
                int general = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentModifierDamage(set, net.minecraft.util.DamageSource.generic);
                int fall = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentModifierDamage(set, net.minecraft.util.DamageSource.fall);
                result("protection IV counts 4, with feather falling IV a fall counts 16", general == 4 && fall == 16, "generic=" + general + " fall=" + fall);
                for (int i = 0; i < 4; i++) {
                    p().inventory.armorInventory[i] = null;
                }
                p().setHealth(20.0F);
                cmd("gamemode 1");
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                java.util.List<String> tip = new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_helmet).getTooltip(mc.thePlayer, false);
                String all = String.valueOf(tip);
                result("the diamond helmet tooltip shows armor 3 and toughness 2", all.contains("3") && all.contains(net.minecraft.util.StatCollector.translateToLocal("attribute.name.generic.armorToughness")), all);
            }
        });
        finish();
    }

    private int[] brewAt;

    private net.minecraft.tileentity.TileEntityBrewingStand stand() {
        return (net.minecraft.tileentity.TileEntityBrewingStand) w().getTileEntity(brewAt[0], brewAt[1], brewAt[2]);
    }

    /** The brewing stand of 1.9 (BrewFuel112): blaze powder fuel for 20 brews, the fifth slot, the window. */
    private void brewingStage() {
        server(40, new Runnable() {
            @Override
            public void run() {
                cmd("gamemode 1");
                brewAt = new int[] {(int) Math.floor(p().posX) + 2, (int) Math.floor(p().posY), (int) Math.floor(p().posZ)};
                w().setBlock(brewAt[0], brewAt[1], brewAt[2], net.minecraft.init.Blocks.brewing_stand);
                net.minecraft.tileentity.TileEntityBrewingStand s = stand();
                result("the stand has five slots", s.getSizeInventory() == 5, "size=" + s.getSizeInventory());
                for (int i = 0; i < 3; i++) {
                    s.setInventorySlotContents(i, new ItemStack(Items.potionitem, 1, 0));
                }
                s.setInventorySlotContents(3, new ItemStack(Items.nether_wart));
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                net.minecraft.tileentity.TileEntityBrewingStand s = stand();
                result("without fuel it does not brew", s.getBrewTime() == 0, "brewTime=" + s.getBrewTime());
                result("blaze powder only in the fuel slot, hoppers reach it from the side",
                    s.isItemValidForSlot(4, new ItemStack(Items.blaze_powder)) && !s.isItemValidForSlot(4, new ItemStack(Items.potionitem))
                    && java.util.Arrays.toString(s.getAccessibleSlotsFromSide(2)).equals("[0, 1, 2, 4]"), java.util.Arrays.toString(s.getAccessibleSlotsFromSide(2)));
                // the window: 5 + 36 slots, the fifth at (17, 17) taking blaze powder only (shift-click tries the
                // ingredient slot first, as in 1.12.2: blaze powder is an ingredient too)
                net.minecraft.inventory.ContainerBrewingStand c = new net.minecraft.inventory.ContainerBrewingStand(p().inventory, s);
                net.minecraft.inventory.Slot fuelSlot = (net.minecraft.inventory.Slot) c.inventorySlots.get(4);
                result("the window has 5 + 36 slots, the fuel slot at 17,17 takes blaze powder only", c.inventorySlots.size() == 41
                    && fuelSlot.xDisplayPosition == 17 && fuelSlot.yDisplayPosition == 17 && fuelSlot.isItemValid(new ItemStack(Items.blaze_powder))
                    && !fuelSlot.isItemValid(new ItemStack(Items.nether_wart)), "slots=" + c.inventorySlots.size());
                s.setInventorySlotContents(4, new ItemStack(Items.blaze_powder, 2));
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                net.minecraft.tileentity.TileEntityBrewingStand s = stand();
                int fuel = ru.codex.offhand1710.BrewFuel112.fuel(s);
                result("one powder gives 20 brews: the brew started with 19 left and one powder taken", s.getBrewTime() > 0 && fuel == 19
                    && s.getStackInSlot(4) != null && s.getStackInSlot(4).stackSize == 1, "brewTime=" + s.getBrewTime() + " fuel=" + fuel + " left=" + s.getStackInSlot(4));
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                s.writeToNBT(tag);
                net.minecraft.tileentity.TileEntityBrewingStand copy = new net.minecraft.tileentity.TileEntityBrewingStand();
                copy.readFromNBT(tag);
                result("Fuel is saved as in 1.12.2", tag.getByte("Fuel") == 19 && ru.codex.offhand1710.BrewFuel112.fuel(copy) == 19
                    && copy.getStackInSlot(4) != null, "tag=" + tag.getByte("Fuel"));
                look(brewAt[0] + 0.5D, brewAt[1] + 0.5D, brewAt[2] + 0.5D);
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                net.minecraft.tileentity.TileEntity t = mc.theWorld.getTileEntity(brewAt[0], brewAt[1], brewAt[2]);
                if (t instanceof net.minecraft.tileentity.TileEntityBrewingStand) {
                    ru.codex.offhand1710.BrewFuel112.setFuel((net.minecraft.tileentity.TileEntityBrewingStand) t, 19);
                    mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiBrewingStand(mc.thePlayer.inventory, (net.minecraft.tileentity.TileEntityBrewingStand) t));
                }
            }
        });
        client(10, new Runnable() {
            @Override
            public void run() {
                shot("brewing");
                mc.displayGuiScreen(null);
            }
        });
        finish();
    }

    /**
     * The screenshot bench: plays the scene file (MC112_SCENE) that HarnessScene plays in vanilla 1.12.2, with the same
     * picture settings, so the two sets of screenshots can be compared pixel by pixel.
     */
    private void visual() {
        java.util.List<String> lines;
        try {
            lines = java.nio.file.Files.readAllLines(new File(System.getenv("MC112_SCENE")).toPath(), java.nio.charset.Charset.forName("UTF-8"));
        } catch (Exception e) {
            log("FAIL no scene file: " + e);
            finish();
            return;
        }
        client(5, new Runnable() {
            @Override
            public void run() {
                mc.gameSettings.fovSetting = 70.0F;
                mc.gameSettings.fancyGraphics = true;
                mc.gameSettings.ambientOcclusion = 2;
                mc.gameSettings.clouds = false;
                mc.gameSettings.particleSetting = 0;
                mc.gameSettings.guiScale = 0;
                mc.gameSettings.gammaSetting = 0.0F;
                mc.gameSettings.viewBobbing = true;
                mc.gameSettings.renderDistanceChunks = 8;
            }
        });
        int pending = 0;
        int shots = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int sp = line.indexOf(' ');
            String op = sp < 0 ? line : line.substring(0, sp);
            final String arg = sp < 0 ? "" : line.substring(sp + 1);
            if ("wait".equals(op)) {
                pending += Integer.parseInt(arg);
                continue;
            }
            int delay = 1 + pending;
            pending = 0;
            if ("cmd".equals(op)) {
                server(delay, new Runnable() {
                    @Override
                    public void run() {
                        cmd(arg);
                    }
                });
            } else if ("shot".equals(op)) {
                shots++;
                client(delay, new Runnable() {
                    @Override
                    public void run() {
                        shot(arg);
                    }
                });
            } else if ("hud".equals(op)) {
                client(delay, new Runnable() {
                    @Override
                    public void run() {
                        mc.gameSettings.hideGUI = "off".equals(arg);
                    }
                });
            } else if ("view".equals(op)) {
                client(delay, new Runnable() {
                    @Override
                    public void run() {
                        mc.gameSettings.thirdPersonView = Integer.parseInt(arg);
                    }
                });
            } else if ("gui".equals(op)) {
                client(delay, new Runnable() {
                    @Override
                    public void run() {
                        if (!"close".equals(arg)) {
                            // as the inventory key does: the "taking inventory" hint goes away
                            mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C16PacketClientStatus(
                                net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                        }
                        mc.displayGuiScreen("close".equals(arg) ? null : new net.minecraft.client.gui.inventory.GuiInventory(mc.thePlayer));
                    }
                });
            }
        }
        final int total = shots;
        client(20 + pending, new Runnable() {
            @Override
            public void run() {
                log("INFO visual bench: " + total + " shots");
            }
        });
        finish();
    }

    /** The boat of 1.12.2 (Boat112): speeds by the numbers of EntityBoat, turning, shore, ice, mobs, paddles. */
    private void boatsStage() {
        server(40, new Runnable() {
            @Override
            public void run() {
                WorldServer w = w();
                cmd("gamemode 1");
                cmd("gamerule doMobSpawning false");
                sx = (int) Math.floor(p().posX) + 60;
                sy = 40;
                sz = (int) Math.floor(p().posZ);
                for (int dx = -20; dx <= 20; dx++) {
                    for (int dz = -10; dz <= 70; dz++) {
                        w.setBlock(sx + dx, sy - 3, sz + dz, Blocks.stone);
                        boolean pool = dx >= -6 && dx <= 6 && dz <= 40;
                        Block lane = dx >= 11 && dx <= 13 ? Blocks.packed_ice : dx >= 15 && dx <= 17 ? Blocks.stone : null;
                        w.setBlock(sx + dx, sy - 2, sz + dz, pool ? Blocks.water : lane != null ? lane : Blocks.stone);
                        boolean rim = !pool && dx >= -7 && dx <= 7 && dz <= 41;
                        w.setBlock(sx + dx, sy - 1, sz + dz, pool ? Blocks.water : rim ? Blocks.stone : Blocks.air);
                        for (int dy = 0; dy < 4; dy++) {
                            w.setBlock(sx + dx, sy + dy, sz + dz, Blocks.air);
                        }
                    }
                }
                // the ice lane goes on: a boat on packed ice gets fast
                for (int dx = 11; dx <= 13; dx++) {
                    for (int dz = 71; dz <= 260; dz++) {
                        w.setBlock(sx + dx, sy - 2, sz + dz, Blocks.packed_ice);
                        for (int dy = -1; dy < 3; dy++) {
                            w.setBlock(sx + dx, sy + dy, sz + dz, Blocks.air);
                        }
                    }
                }
                // a wall across the pool at its far end
                for (int dx = -6; dx <= 6; dx++) {
                    w.setBlock(sx + dx, sy - 1, sz + 41, Blocks.stone);
                    w.setBlock(sx + dx, sy, sz + 41, Blocks.stone);
                }
                look(sx + 0.5D, sy + 4, sz - 6.5D);
                // heading +z: the yaw of 1.7.10 is the heading + 90
                boats[0] = boatAt(sx + 0.5D, sy, sz + 0.5D, 90.0F);
                boats[1] = boatAt(sx - 3.5D, sy, sz + 20.5D, 90.0F);
                boats[2] = boatAt(sx + 12.5D, sy - 1, sz + 0.5D, 90.0F);
                boats[3] = boatAt(sx + 16.5D, sy - 1, sz + 0.5D, 90.0F);
                drive(boats[0], true, false);
                drive(boats[1], false, true);
                drive(boats[2], true, false);
                drive(boats[3], true, false);
                ru.codex.offhand1710.Boat112.lastSound = "";
            }
        });
        server(30, new Runnable() {
            @Override
            public void run() {
                boatMark[0] = boats[0].posZ;
                boatMark[5] = MinecraftServer.getServer().getTickCounter();
                boatMark[6] = ru.codex.offhand1710.Boat112.state(boats[3]).ticks[3] + ru.codex.offhand1710.Boat112.state(boats[3]).ticks[4];
                boatMark[1] = boats[1].rotationYaw;
                boatMark[2] = boats[2].posZ;
                boatMark[3] = boats[3].posZ;
                boatMark[4] = boats[0].posX;
            }
        });
        server(20, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    int[] t = ru.codex.offhand1710.Boat112.state(boats[i]).ticks;
                    log("INFO boat " + i + " ticks in water/under/flowing/land/air " + t[0] + "/" + t[1] + "/" + t[2] + "/" + t[3] + "/" + t[4] + " y=" + boats[i].posY);
                }
                double ticks = MinecraftServer.getServer().getTickCounter() - boatMark[5];
                int[] t3 = ru.codex.offhand1710.Boat112.state(boats[3]).ticks;
                log("INFO boat window: server ticks " + ticks + ", boat updates " + (t3[3] + t3[4] - boatMark[6]));
                double water = ru.codex.offhand1710.Boat112.state(boats[0]).maxSpeed;
                result("boat in water: steady speed 0.04/(1-0.9) = 0.4 blocks/tick", water > 0.38D && water <= 0.401D,
                    String.format("%.3f, sideways %.3f, status y=%.2f", water, boats[0].posX - boatMark[4], boats[0].posY));
                double turn = -ru.codex.offhand1710.Boat112.state(boats[1]).maxTurn;
                result("boat turning: -1 per tick kept at 0.9 = -10 degrees/tick", turn < -9.5D && turn >= -10.01D, String.format("%.2f", turn));
                double stone = ru.codex.offhand1710.Boat112.state(boats[3]).maxSpeed;
                result("boat on stone: 0.04/(1-0.6) = 0.1 blocks/tick", stone > 0.095D && stone <= 0.1001D, String.format("%.3f", stone));
                double ice = ru.codex.offhand1710.Boat112.state(boats[2]).maxSpeed;
                result("boat on packed ice: glide 0.98 makes it fast (2 blocks/tick at most)", ice > 1.2D && ice <= 2.0D, String.format("%.3f", ice));
                result("paddle sound in water", "entity.boat.paddle_water".equals(ru.codex.offhand1710.Boat112.lastSound) || "entity.boat.paddle_land".equals(ru.codex.offhand1710.Boat112.lastSound),
                    "last=" + ru.codex.offhand1710.Boat112.lastSound);
                result("paddles reach the client watcher", ru.codex.offhand1710.Boat112.paddling(boats[0], 0) && ru.codex.offhand1710.Boat112.paddling(boats[0], 1), "");
                ru.codex.offhand1710.Boat112.state(boats[1]).input = null;
                ru.codex.offhand1710.Boat112.state(boats[2]).input = null;
                ru.codex.offhand1710.Boat112.state(boats[3]).input = null;
                // an empty boat picks up a mob that touches it
                boatPig = new net.minecraft.entity.passive.EntityPig(w());
                boatPig.setPosition(boats[1].posX + 0.6D, boats[1].posY, boats[1].posZ);
                w().spawnEntityInWorld(boatPig);
            }
        });
        server(40, new Runnable() {
            @Override
            public void run() {
                result("boat at full speed into a wall does not break (1.7.10 broke it)", !boats[0].isDead && boats[0].posZ < sz + 41, String.format("z=%.1f", boats[0].posZ - sz));
                result("empty boat picks up a mob", boatPig.ridingEntity == boats[1], "riding=" + boatPig.ridingEntity);
                result("the rider sits in the middle", boatPig.ridingEntity == boats[1] && Math.abs(boatPig.posX - boats[1].posX) < 0.01D && Math.abs(boatPig.posZ - boats[1].posZ) < 0.01D, "");
                // under water for 3 seconds: the rider is thrown off
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 0; dy < 3; dy++) {
                            w().setBlock(net.minecraft.util.MathHelper.floor_double(boats[1].posX) + dx, sy + dy, net.minecraft.util.MathHelper.floor_double(boats[1].posZ) + dz, Blocks.water);
                        }
                    }
                }
                w().setBlock(net.minecraft.util.MathHelper.floor_double(boats[1].posX), sy + 3, net.minecraft.util.MathHelper.floor_double(boats[1].posZ), Blocks.stone);
            }
        });
        server(100, new Runnable() {
            @Override
            public void run() {
                result("under water 60 ticks throws the rider off", boatPig.ridingEntity == null, "riding=" + boatPig.ridingEntity);
                for (net.minecraft.entity.item.EntityBoat b : boats) {
                    b.setDead();
                }
                boatPig.setDead();
            }
        });
        finish();
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
                // no healing while measuring: the fast regeneration of 1.9 would give 2 back within the 20 ticks
                p.getFoodStats().setFoodLevel(10);
                p.getFoodStats().setFoodSaturationLevel(0.0F);
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
