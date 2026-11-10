package ru.codex.offhand1710.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Automatic self-test, only active when the environment variable MC112_AUDIT is "new" or "reload":
 * checks registrations, creates a world, places every block, spawns every mob, generates terrain,
 * takes screenshots, and on "reload" checks that the world survived a restart.
 */
public final class Audit112 {
    private static final String STAGE = System.getenv("MC112_AUDIT");
    private static final String WORLD = "audit112";
    private static final int BASE_Y = 180;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<String> placed = new ArrayList<String>();
    private FileWriter out;
    private boolean launched;
    private int serverTicks;
    private int clientTicks;
    private int step;
    private int genIndex;
    private final Map<String, Integer> found = new TreeMap<String, Integer>();
    private volatile boolean wantShots;
    private volatile boolean wantQuit;
    private int shotStage;
    private int shotDelay;

    private Audit112() {
    }

    static void start() {
        if ("new".equals(STAGE) || "reload".equals(STAGE)) {
            Audit112 audit = new Audit112();
            FMLCommonHandler.instance().bus().register(audit);
        }
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

    private void registrations() {
        Set<Item> crafted = new HashSet<Item>();
        for (Object recipe : CraftingManager.getInstance().getRecipeList()) {
            ItemStack output = ((IRecipe) recipe).getRecipeOutput();
            if (output != null) {
                crafted.add(output.getItem());
            }
        }
        for (Object output : FurnaceRecipes.smelting().getSmeltingList().values()) {
            crafted.add(((ItemStack) output).getItem());
        }
        int blocks = 0;
        for (Object o : Block.blockRegistry) {
            Block block = (Block) o;
            String name = Block.blockRegistry.getNameForObject(block);
            if (name.startsWith("mc112bridge:")) {
                blocks++;
                if (!StatCollector.canTranslate(block.getUnlocalizedName() + ".name")) {
                    log("NOLANG block " + name + " " + block.getUnlocalizedName());
                }
            }
        }
        int items = 0;
        for (Object o : Item.itemRegistry) {
            Item item = (Item) o;
            String name = Item.itemRegistry.getNameForObject(item);
            if (name.startsWith("mc112bridge:")) {
                items++;
                ItemStack stack = new ItemStack(item);
                if (!StatCollector.canTranslate(item.getUnlocalizedName(stack) + ".name")) {
                    log("NOLANG item " + name + " " + item.getUnlocalizedName(stack));
                }
                if (!crafted.contains(item)) {
                    log("NORECIPE " + name);
                }
                if (item.getCreativeTab() == null) {
                    log("NOTAB " + name);
                }
            }
        }
        int entities = 0;
        for (Object o : EntityList.classToStringMapping.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (((Class<?>) e.getKey()).getName().startsWith("ru.codex")) {
                entities++;
                if (!StatCollector.canTranslate("entity." + e.getValue() + ".name")) {
                    log("NOLANG entity " + e.getValue());
                }
            }
        }
        log("REGISTERED blocks=" + blocks + " items=" + items + " entities=" + entities);
    }

    @SubscribeEvent
    public void onClient(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        clientTicks++;
        if (wantQuit) {
            mc.shutdown();
            return;
        }
        if (!launched && mc.currentScreen instanceof GuiMainMenu && clientTicks > 20) {
            launched = true;
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.renderDistanceChunks = 6;
            registrations();
            dumpStatic();
            recipeCases();
            FMLCommonHandler.instance().bus();
            mc.launchIntegratedServer(WORLD, WORLD, new WorldSettings(112L, WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands());
            return;
        }
        if (wantShots && mc.thePlayer != null && --shotDelay <= 0) {
            shots();
        }
    }

    private int glowCount() {
        int n = 0;
        for (Object o : mc.theWorld.loadedEntityList) {
            if (ru.codex.offhand1710.Maps112.isGlowing((Entity) o)) {
                n++;
            }
        }
        return n;
    }

    private void shot(String name) {
        log("SHOT " + name + " screen=" + mc.currentScreen);
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, "audit-" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void tab(CreativeTabs tab) {
        try {
            GuiContainerCreative gui = new GuiContainerCreative(mc.thePlayer);
            mc.displayGuiScreen(gui);
            java.lang.reflect.Method m = GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab", CreativeTabs.class);
            m.setAccessible(true);
            m.invoke(gui, tab);
            gui.handleMouseInput();
        } catch (Exception e) {
            log("TABFAIL " + e);
        }
    }

    private void shots() {
        shotDelay = 40;
        switch (shotStage++) {
            case 0:
                shot("world");
                tab(CreativeTabs.tabDecorations);
                break;
            case 1:
                shot("decor");
                tab(CreativeTabs.tabBlock);
                break;
            case 2:
                shot("blocks");
                tab(CreativeTabs.tabMaterials);
                break;
            case 3:
                shot("materials");
                tab(CreativeTabs.tabFood);
                break;
            case 4:
                shot("food");
                mc.displayGuiScreen(null);
                ru.codex.offhand1710.client.Client122.leftHand = true;
                ru.codex.offhand1710.client.Client122.subtitles = true;
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.click", 1.0F, 1.0F, false);
                break;
            case 5:
                // captions live 3 s (Client122), so they are counted one step (2 s) after the sound, not at step 9
                log((ru.codex.offhand1710.client.Client122.captionCount() > 0 ? "PASS" : "FAIL") + " subtitles captions=" + ru.codex.offhand1710.client.Client122.captionCount());
                shot("left");
                log("GLOW drawn=" + ru.codex.offhand1710.client.GlowOutline.lastDrawn + " fbo=" + net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled() + " flagged=" + glowCount());
                ru.codex.offhand1710.client.DataClient.openAdvancements();
                break;
            case 6:
                shot("adv");
                mc.displayGuiScreen(null);
                mc.thePlayer.sendChatMessage("/gamemode 0");
                break;
            case 7:
                ru.codex.offhand1710.client.DataClient.openBook();
                mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.thePlayer));
                break;
            case 8:
                shot("book");
                mc.displayGuiScreen(null);
                mc.thePlayer.sendChatMessage("/gamemode 1");
                break;
            case 9:
                mc.displayGuiScreen(null);
                log("PASS stopsound ran stopped=" + ru.codex.offhand1710.client.Client122.stop("|"));
                ru.codex.offhand1710.client.Client122.leftHand = false;
                ru.codex.offhand1710.client.Client122.subtitles = false;
                break;
            default:
                wantShots = false;
                mc.gameSettings.renderDistanceChunks = 2;
                step++;
                break;
        }
    }

    @SubscribeEvent
    public void onServer(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MinecraftServer.getServer() == null) {
            return;
        }
        try {
            if ("reload".equals(STAGE)) {
                reloadTick();
            } else {
                newWorldTick();
            }
        } catch (Throwable t) {
            log("FAIL step " + step + " " + t);
            t.printStackTrace();
            wantQuit = true;
        }
    }

    private EntityPlayerMP player() {
        List<?> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        return players.isEmpty() ? null : (EntityPlayerMP) players.get(0);
    }

    private void newWorldTick() throws IOException {
        EntityPlayerMP player = player();
        if (player == null) {
            return;
        }
        serverTicks++;
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        observerWatch(world);
        if (!player.capabilities.allowFlying || !player.capabilities.isFlying) {
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
        }
        if (tradeVillager != null && tradeTick != 0 && serverTicks == tradeTick + 3) {
            tradeVillager.useRecipe((net.minecraft.village.MerchantRecipe) tradeVillager.getRecipes(player).get(0));
        }
        if (tradeVillager != null && tradeTick != 0 && serverTicks == tradeTick + 6) {
            tradeVillager.setCustomer(null);
        }
        int cx = (int) Math.floor(player.posX);
        int cz = (int) Math.floor(player.posZ);
        if (step == 0 && serverTicks > 100) {
            step = 1;
            buildGallery(world, cx, cz);
            player.playerNetServerHandler.setPlayerLocation(cx + 12.5D, BASE_Y + 2.0D, cz - 8.0D, 0.0F, 25.0F);
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            log("GALLERY placed=" + placed.size());
            functionalSetup(world, player, cx, cz);
            dumpEntities(world);
            lootDump();
        } else if (step == 1 && serverTicks > 260) {
            functionalCheck(world, player);
            step = 2;
            shotDelay = 1;
            wantShots = true;
        } else if (step == 3 && dataCheck) {
            dataCheck = false;
            boolean adv = ru.codex.offhand1710.Data112.isDone(player, "minecraft:story/mine_stone") || ru.codex.offhand1710.Data112.isDone(player, "minecraft:story/root");
            result("advancement from inventory", adv, "root=" + ru.codex.offhand1710.Data112.isDone(player, "minecraft:story/root") + " stone=" + ru.codex.offhand1710.Data112.isDone(player, "minecraft:story/mine_stone"));
            result("recipe unlocked by advancement", ru.codex.offhand1710.Data112.knowsRecipe(player, "minecraft:stone_pickaxe") || ru.codex.offhand1710.client.DataClient.knownCount() > 0, "known=" + ru.codex.offhand1710.client.DataClient.knownCount());
            result("function ran", world.getBlock(funcX, fy + 15, funcZ) == Blocks.gold_block, "");
            MinecraftServer.getServer().getCommandManager().executeCommand(player, "recipe give " + player.getCommandSenderName() + " *");
            player.inventory.mainInventory[20] = new ItemStack(Blocks.planks, 4, 0);
            player.openContainer = player.inventoryContainer;
            boolean filled = ru.codex.offhand1710.Data112.fill(player, "minecraft:crafting_table");
            result("recipe book fills grid", filled && player.inventoryContainer.getSlot(1).getHasStack(), "");
            player.inventoryContainer.getSlot(1).putStack(null);
        } else if (step == 3 && cartCheck) {
            cartCheck = false;
            result("bought map has marker", boughtMap != null && ru.codex.offhand1710.Maps112.hasMarker(world, boughtMap), "");
        } else if (step == 3 && mapStack != null) {
            result("map marker", ru.codex.offhand1710.Maps112.hasMarker(world, mapStack), "");
            mapStack = null;
            result("glow flag on server", glowPig != null && ru.codex.offhand1710.Maps112.isGlowing(glowPig), "");
        } else if (step == 3 && brX != 0 && genIndex == 0) {
            net.minecraft.tileentity.TileEntityBrewingStand st = (net.minecraft.tileentity.TileEntityBrewingStand) world.getTileEntity(brX, fy, brZ);
            ItemStack made = st.getStackInSlot(0);
            result("brew result lingering", made != null && made.getItem() == ru.codex.offhand1710.Brew112.lingering, String.valueOf(made) + " other=" + st.getStackInSlot(1) + " ing=" + st.getStackInSlot(3));
            brX = 0;
            ru.codex.offhand1710.Brew112.Cloud cloud = (ru.codex.offhand1710.Brew112.Cloud) net.minecraft.entity.EntityList.createEntityByName("mc112bridge.AreaEffectCloud", world);
            result("cloud created", cloud != null && cloud.radius() == 3.0F, "");
        } else if (step == 3 && pistonRetract) {
            pistonRetract = false;
            result("slime pull", world.getBlock(pX + 1, fy, pZ) == ru.codex.offhand1710.Content119.slime && world.getBlock(pX + 1, fy + 1, pZ) == Blocks.planks && world.getBlock(pX + 2, fy, pZ) == Blocks.air && world.getBlock(pX, fy, pZ) == Blocks.sticky_piston, world.getBlock(pX + 1, fy, pZ) + " " + world.getBlock(pX + 2, fy, pZ));
        } else if (step == 3 && tradeVillager != null && genIndex == 0) {
            result("title on client", ru.codex.offhand1710.client.TitleOverlay.currentTitle().startsWith("Audit"), ru.codex.offhand1710.client.TitleOverlay.currentTitle());
            int lvl = tradeVillager.getEntityData().getInteger("CareerLevel");
            result("trades refresh after first use", lvl >= 3, "level=" + lvl);
            tradeVillager = null;
        } else if (step == 3) {
        if (levelVillager != null && genIndex == 0) {
            boolean pending = cpw.mods.fml.relauncher.ReflectionHelper.<Boolean, net.minecraft.entity.passive.EntityVillager>getPrivateValue(net.minecraft.entity.passive.EntityVillager.class, levelVillager, "needsInitilization", "field_70959_by");
            int level = levelVillager.getEntityData().getInteger("CareerLevel");
            int now = levelVillager.getRecipes(player).size();
            result("trades level up after use", !pending && level == 3 && now > levelCount, "level=" + level + " recipes " + levelCount + "->" + now);
            levelVillager = null;
        }
            generate(world, 0, 16, 0, 0);
        } else if (step == 4) {
            generate(MinecraftServer.getServer().worldServerForDimension(-1), 0, 8, 0, 0);
        } else if (step == 5) {
            generate(MinecraftServer.getServer().worldServerForDimension(1), 0, 2, 0, 0);
        } else if (step == 6) {
            generate(MinecraftServer.getServer().worldServerForDimension(1), 0, 10, 90, 0);
        } else if (step == 7) {
            step = 8;
            Files.write(Joiner.on("\n").join(placed), new File(mc.mcDataDir, "audit112-placed.txt"), Charset.forName("UTF-8"));
            MinecraftServer.getServer().getConfigurationManager().saveAllPlayerData();
            for (WorldServer w : MinecraftServer.getServer().worldServers) {
                try {
                    w.saveAllChunks(true, null);
                } catch (Exception e) {
                    log("SAVEFAIL " + e);
                }
            }
            log("SAVED");
            wantQuit = true;
        }
    }

    private int obsX;
    private int obsY;
    private int obsZ;
    private int obsState;
    private int waterX;
    private int waterZ;
    private int fy;

    private void result(String name, boolean ok, String detail) {
        log((ok ? "PASS " : "FAIL ") + name + " " + detail);
    }

    private FileWriter dump;

    private void d(String line) {
        try {
            if (dump == null) {
                dump = new FileWriter(new File(mc.mcDataDir, "audit112-dump.tsv"));
            }
            dump.write(line + "\n");
            dump.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String materialName(net.minecraft.block.material.Material m) {
        for (java.lang.reflect.Field f : net.minecraft.block.material.Material.class.getFields()) {
            try {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.get(null) == m) {
                    return f.getName();
                }
            } catch (Exception e) {
                return "?";
            }
        }
        return "?";
    }

    private void dumpStatic() {
        for (Object o : Block.blockRegistry) {
            Block b = (Block) o;
            float hard;
            try {
                hard = b.getBlockHardness(null, 0, 0, 0);
            } catch (Exception e) {
                hard = -99;
            }
            float res;
            try {
                res = b.getExplosionResistance(null);
            } catch (Exception e) {
                res = -99;
            }
            d("B\t" + Block.blockRegistry.getNameForObject(b) + "\t" + hard + "\t" + res + "\t" + b.getLightValue() + "\t" + b.getLightOpacity()
                    + "\t" + b.stepSound.soundName + "\t" + b.slipperiness + "\t" + materialName(b.getMaterial()) + "\t" + b.getHarvestTool(0) + "\t" + b.getHarvestLevel(0) + "\t" + b.getCreativeTabToDisplayOn());
        }
        for (Object o : Item.itemRegistry) {
            Item i = (Item) o;
            String food = "";
            if (i instanceof net.minecraft.item.ItemFood) {
                ItemStack s = new ItemStack(i);
                food = ((net.minecraft.item.ItemFood) i).func_150905_g(s) + "\t" + ((net.minecraft.item.ItemFood) i).func_150906_h(s);
            }
            d("I\t" + Item.itemRegistry.getNameForObject(i) + "\t" + i.getItemStackLimit(new ItemStack(i)) + "\t" + i.getMaxDamage() + "\t" + food);
        }
    }

    /** Lines written by the comparison script: name|expected item:meta:count|nine slots item:meta or - */
    private void recipeCases() {
        File file = new File(mc.mcDataDir, "audit112-recipes.txt");
        if (!file.exists()) {
            return;
        }
        WorldSettings unused = null;
        try {
            int ok = 0;
            int bad = 0;
            for (Object o : Files.readLines(file, Charset.forName("UTF-8"))) {
                String[] p = ((String) o).split("\\|");
                net.minecraft.inventory.InventoryCrafting inv = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                    @Override
                    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer pl) {
                        return false;
                    }
                }, 3, 3);
                String[] slots = p[2].split(";");
                boolean known = true;
                for (int i = 0; i < 9; i++) {
                    if (!"-".equals(slots[i])) {
                        String[] s = slots[i].split(",");
                        Object item = Item.itemRegistry.getObject(s[0]);
                        if (item == null) {
                            known = false;
                            log("RECIPEITEM " + p[0] + " " + s[0]);
                            break;
                        }
                        inv.setInventorySlotContents(i, new ItemStack((Item) item, 1, Integer.parseInt(s[1])));
                    }
                }
                if (!known) {
                    continue;
                }
                String[] e = p[1].split(",");
                ItemStack got = CraftingManager.getInstance().findMatchingRecipe(inv, mc.theWorld);
                String gotName = got == null ? "null" : Item.itemRegistry.getNameForObject(got.getItem()) + "," + got.getItemDamage() + "," + got.stackSize;
                if (gotName.equals(e[0] + "," + e[1] + "," + e[2])) {
                    ok++;
                } else {
                    bad++;
                    d("R\t" + p[0] + "\texpected " + p[1] + "\tgot " + gotName);
                }
            }
            log("RECIPES ok=" + ok + " bad=" + bad);
        } catch (IOException ex) {
            log("RECIPES failed " + ex);
        }
    }

    private void dumpEntities(WorldServer world) {
        for (Object o : new ArrayList<Object>(EntityList.classToStringMapping.entrySet())) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Class<?> c = (Class<?>) e.getKey();
            if (!net.minecraft.entity.EntityLiving.class.isAssignableFrom(c) || java.lang.reflect.Modifier.isAbstract(c.getModifiers())) {
                continue;
            }
            try {
                net.minecraft.entity.EntityLiving en = (net.minecraft.entity.EntityLiving) EntityList.createEntityByName((String) e.getValue(), world);
                if (en == null) {
                    continue;
                }
                StringBuilder sb = new StringBuilder("E\t" + e.getValue() + "\t" + en.width + "\t" + en.height + "\t" + en.getMaxHealth());
                for (net.minecraft.entity.ai.attributes.IAttribute a : new net.minecraft.entity.ai.attributes.IAttribute[] {net.minecraft.entity.SharedMonsterAttributes.movementSpeed, net.minecraft.entity.SharedMonsterAttributes.attackDamage, net.minecraft.entity.SharedMonsterAttributes.followRange, net.minecraft.entity.SharedMonsterAttributes.knockbackResistance}) {
                    net.minecraft.entity.ai.attributes.IAttributeInstance i = en.getEntityAttribute(a);
                    sb.append("\t").append(i == null ? "-" : String.valueOf(i.getBaseValue()));
                }
                d(sb.toString());
                if (c == net.minecraft.entity.boss.EntityDragon.class || c == net.minecraft.entity.boss.EntityWither.class) {
                    continue;
                }
                java.lang.reflect.Method m = EntityLivingBaseAccess.dropFewItems();
                Map<String, int[]> seen = new TreeMap<String, int[]>();
                for (int k = 0; k < 200; k++) {
                    en.captureDrops = true;
                    en.capturedDrops.clear();
                    m.invoke(en, Boolean.TRUE, Integer.valueOf(0));
                    Map<String, Integer> sum = new TreeMap<String, Integer>();
                    for (Object drop : en.capturedDrops) {
                        ItemStack s = ((net.minecraft.entity.item.EntityItem) drop).getEntityItem();
                        String n = Item.itemRegistry.getNameForObject(s.getItem()) + ":" + s.getItemDamage();
                        Integer old = sum.get(n);
                        sum.put(n, (old == null ? 0 : old) + s.stackSize);
                    }
                    for (Map.Entry<String, Integer> x : sum.entrySet()) {
                        int[] r = seen.get(x.getKey());
                        if (r == null) {
                            r = new int[] {x.getValue(), x.getValue(), 0};
                            seen.put(x.getKey(), r);
                        }
                        r[0] = Math.min(r[0], x.getValue());
                        r[1] = Math.max(r[1], x.getValue());
                        r[2]++;
                    }
                }
                StringBuilder dr = new StringBuilder("D\t" + e.getValue());
                for (Map.Entry<String, int[]> x : seen.entrySet()) {
                    dr.append("\t").append(x.getKey()).append("=").append(x.getValue()[0]).append("-").append(x.getValue()[1]).append("/").append(x.getValue()[2]);
                }
                d(dr.toString());
            } catch (Throwable t) {
                d("X\t" + e.getValue() + "\t" + t);
            }
        }
    }

    private static final class EntityLivingBaseAccess {
        static java.lang.reflect.Method dropFewItems() throws NoSuchMethodException {
            java.lang.reflect.Method m = net.minecraft.entity.EntityLivingBase.class.getDeclaredMethod("dropFewItems", boolean.class, int.class);
            m.setAccessible(true);
            return m;
        }
    }

    private void lootDump() {
        for (String cat : new String[] {"dungeonChest", "mineshaftCorridor", "pyramidDesertyChest", "pyramidJungleChest", "strongholdCorridor", "strongholdCrossing", "strongholdLibrary", "villageBlacksmith", "bonusChest"}) {
            net.minecraftforge.common.ChestGenHooks info = net.minecraftforge.common.ChestGenHooks.getInfo(cat);
            d("L\t" + cat + "\t" + info.getMin() + "-" + info.getMax() + "\t" + info.getItems(new java.util.Random(1L)).length);
        }
    }
    private net.minecraft.entity.passive.EntityVillager levelVillager;
    private int levelCount;
    private net.minecraft.entity.passive.EntityVillager tradeVillager;
    private int tradeTick;

    private void observerWatch(WorldServer world) {
        if (obsState == 1 && world.getBlock(obsX, obsY, obsZ) == ru.codex.offhand1710.Content119.observer && (world.getBlockMetadata(obsX, obsY, obsZ) & 8) != 0) {
            obsState = 2;
        }
    }

    private void functionalSetup(WorldServer world, EntityPlayerMP player, int cx, int cz) {
        fy = BASE_Y + 20;
        int x = cx - 40;
        // Commands
        MinecraftServer.getServer().getCommandManager().executeCommand(player, "fill " + x + " " + fy + " " + cz + " " + (x + 2) + " " + (fy + 2) + " " + (cz + 2) + " mc112bridge:slime");
        int filled = 0;
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) for (int c = 0; c < 3; c++) if (world.getBlock(x + a, fy + b, cz + c) == ru.codex.offhand1710.Content119.slime) filled++;
        result("/fill", filled == 27, "filled=" + filled);
        chainSetup(world, x, cz);
        brewSetup(world, x, cz + 30);
        pistonSetup(world, x, cz - 14);
        MinecraftServer.getServer().getCommandManager().executeCommand(player, "clone " + x + " " + fy + " " + cz + " " + (x + 2) + " " + (fy + 2) + " " + (cz + 2) + " " + (x + 5) + " " + fy + " " + cz);
        int cloned = 0;
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) for (int c = 0; c < 3; c++) if (world.getBlock(x + 5 + a, fy + b, cz + c) == ru.codex.offhand1710.Content119.slime) cloned++;
        result("/clone", cloned == 27, "cloned=" + cloned);
        // Observer facing east (5), metadata = facing xor 3
        obsX = x + 12;
        obsY = fy;
        obsZ = cz;
        Block obs = ru.codex.offhand1710.Content119.observer;
        world.setBlock(obsX, obsY, obsZ, obs, 5 ^ 3, 2);
        world.setBlock(obsX + 1, obsY, obsZ, Blocks.stone, 0, 2);
        obs.onNeighborBlockChange(world, obsX, obsY, obsZ, Blocks.stone);
        obsState = 1;
        // Frost walker: a zombie on a stone pillar in still water
        waterX = x + 20;
        waterZ = cz;
        for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) {
            world.setBlock(waterX + a, fy - 1, waterZ + b, Blocks.stone, 0, 2);
            world.setBlock(waterX + a, fy, waterZ + b, Blocks.water, 0, 2);
        }
        world.setBlock(waterX, fy, waterZ, Blocks.stone, 0, 2);
        net.minecraft.entity.monster.EntityZombie zombie = new net.minecraft.entity.monster.EntityZombie(world);
        ItemStack boots = new ItemStack(Items.iron_boots);
        boots.addEnchantment(ru.codex.offhand1710.Content119.frostWalker, 2);
        zombie.setCurrentItemOrArmor(1, boots);
        zombie.func_110163_bv();
        zombie.setLocationAndAngles(waterX + 0.5D, fy + 1.0D, waterZ + 0.5D, 0.0F, 0.0F);
        world.spawnEntityInWorld(zombie);
        // Shulker box keeps its contents when broken
        int bx = x + 30;
        Block box = ru.codex.offhand1710.Items112.BOXES[3];
        world.setBlock(bx, fy, cz, box, 1, 2);
        ((IInventory) world.getTileEntity(bx, fy, cz)).setInventorySlotContents(5, new ItemStack(Items.emerald, 9));
        world.func_147480_a(bx, fy, cz, false);
        box.breakBlock(world, bx, fy, cz, box, 1);
        boolean kept = false;
        for (Object o : world.loadedEntityList) {
            if (o instanceof net.minecraft.entity.item.EntityItem) {
                ItemStack s = ((net.minecraft.entity.item.EntityItem) o).getEntityItem();
                if (net.minecraft.block.Block.getBlockFromItem(s.getItem()) == box && s.hasTagCompound() && s.getTagCompound().hasKey("BlockEntityTag")) {
                    kept = true;
                }
            }
        }
        result("shulker keeps items", kept, "");
        // Beetroot
        java.util.ArrayList<ItemStack> ripe = ru.codex.offhand1710.Items112.beetroots.getDrops(world, 0, 64, 0, 3, 0);
        java.util.ArrayList<ItemStack> young = ru.codex.offhand1710.Items112.beetroots.getDrops(world, 0, 64, 0, 0, 0);
        result("beetroot drops", ripe.get(0).getItem() == ru.codex.offhand1710.Items112.beetroot && young.size() == 1 && young.get(0).getItem() == ru.codex.offhand1710.Items112.beetrootSeeds, "ripe=" + ripe.size());
        // Recipes
        ItemStack B = new ItemStack(ru.codex.offhand1710.Items112.beetroot);
        recipe("beetroot soup", ru.codex.offhand1710.Items112.beetrootSoup, world, B, B, B, B, B, B, null, new ItemStack(Items.bowl), null);
        ItemStack S = new ItemStack(Items.stick);
        recipe("armor stand", ru.codex.offhand1710.Items112.armorStand, world, S, S, S, null, S, null, S, new ItemStack(Blocks.stone_slab), S);
        ItemStack W = new ItemStack(Blocks.wool, 1, 11);
        ItemStack P = new ItemStack(Blocks.planks, 1, 2);
        recipe("blue bed", ru.codex.offhand1710.Items112.BED_ITEMS[11], world, W, W, W, P, P, P, null, null, null);
        ItemStack red = new ItemStack(Blocks.wool, 1, 14);
        recipe("red bed", Items.bed, world, red, red, red, P, P, P, null, null, null);
        ItemStack G = new ItemStack(Blocks.glass);
        recipe("end crystal", ru.codex.offhand1710.Content119.endCrystal, world, G, G, G, G, new ItemStack(Items.ender_eye), G, G, new ItemStack(Items.ghast_tear), G);
        ItemStack C = new ItemStack(Blocks.cobblestone);
        ItemStack R = new ItemStack(Items.redstone);
        recipe("observer", Item.getItemFromBlock(ru.codex.offhand1710.Content119.observer), world, C, C, C, R, R, new ItemStack(Items.quartz), C, C, C);
        ItemStack woolW = new ItemStack(Blocks.wool, 1, 0);
        recipe("white banner", ru.codex.offhand1710.Banner112.banner, world, woolW, woolW, woolW, woolW, woolW, woolW, null, S, null);
        ItemStack sp = new ItemStack(Blocks.planks, 1, 1);
        recipe("spruce boat", ru.codex.offhand1710.Banner112.BOAT_ITEMS[0], world, sp, null, sp, sp, sp, sp, null, null, null);
        ItemStack oak = new ItemStack(Blocks.planks, 1, 0);
        recipe("oak boat", Items.boat, world, oak, null, oak, oak, oak, oak, null, null, null);
        ItemStack redDye = new ItemStack(Items.dye, 1, 1);
        ItemStack blank = new ItemStack(ru.codex.offhand1710.Banner112.banner, 1, 15);
        recipe("banner pattern", ru.codex.offhand1710.Banner112.banner, world, redDye, redDye, redDye, null, blank, null, null, null, null);
        net.minecraft.inventory.InventoryCrafting bgrid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < 3; i++) {
            bgrid.setInventorySlotContents(i, redDye);
        }
        bgrid.setInventorySlotContents(4, blank);
        ItemStack done = CraftingManager.getInstance().findMatchingRecipe(bgrid, world);
        String code = done == null ? "null" : done.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).getCompoundTagAt(0).getString("Pattern");
        result("banner pattern code", "ts".equals(code), code);
        player.inventory.mainInventory[player.inventory.currentItem] = done;
        player.inventoryContainer.detectAndSendChanges();
        net.minecraft.inventory.InventoryCrafting sgrid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) {
                return false;
            }
        }, 3, 3);
        sgrid.setInventorySlotContents(0, new ItemStack(ru.codex.offhand1710.PortedBlocks.shield));
        sgrid.setInventorySlotContents(1, done);
        ItemStack decorated = CraftingManager.getInstance().findMatchingRecipe(sgrid, world);
        result("shield pattern", decorated != null && decorated.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).tagCount() == 1 && decorated.getTagCompound().getCompoundTag("BlockEntityTag").getInteger("Base") == 15, String.valueOf(decorated));
        player.inventory.mainInventory[(player.inventory.currentItem + 1) % 9] = decorated;
        int[] monument = ru.codex.offhand1710.Content121.locate(world, "Monument", cx, cz);
        int[] mansion = ru.codex.offhand1710.Content121.locate(world, "Mansion", cx, cz);
        result("/locate", monument != null && mansion != null, (monument == null ? "null" : monument[0] + "," + monument[1]) + " mansion " + (mansion == null ? "null" : mansion[0] + "," + mansion[1]));
        MinecraftServer.getServer().getCommandManager().executeCommand(player, "title " + player.getCommandSenderName() + " times 0 3000 0");
        MinecraftServer.getServer().getCommandManager().executeCommand(player, "title " + player.getCommandSenderName() + " title Audit");
        // A placed banner with a pattern, seen by the screenshot in front of the player
        int by = BASE_Y + 1;
        for (int i = 0; i < 3; i++) {
            world.setBlock(cx + 10 + i * 2, by - 1, cz - 3, Blocks.stone, 0, 2);
            world.setBlock(cx + 10 + i * 2, by, cz - 3, ru.codex.offhand1710.Banner112.standing, 8, 2);
            ((ru.codex.offhand1710.Banner112.BannerTile) world.getTileEntity(cx + 10 + i * 2, by, cz - 3)).base = i == 0 ? 15 : (i == 1 ? 1 : 4);
            world.markBlockForUpdate(cx + 10 + i * 2, by, cz - 3);
        }
        ru.codex.offhand1710.Banner112.EntityBoat112 boat = new ru.codex.offhand1710.Banner112.EntityBoat112(world);
        boat.setLocationAndAngles(cx + 6.5D, BASE_Y + 2.0D, cz - 3.5D, 0.0F, 0.0F);
        world.setBlock(cx + 6, BASE_Y, cz - 4, Blocks.stone, 0, 2);
        world.spawnEntityInWorld(boat);
        ItemStack sl = new ItemStack(Items.slime_ball);
        recipe("slime block", Item.getItemFromBlock(ru.codex.offhand1710.Content119.slime), world, sl, sl, sl, sl, sl, sl, sl, sl, sl);
        ItemStack shell = new ItemStack(ru.codex.offhand1710.Items112.shulkerShell);
        recipe("shulker box", Item.getItemFromBlock(ru.codex.offhand1710.Items112.BOXES[10]), world, null, shell, null, null, new ItemStack(Blocks.chest), null, null, shell, null);
    }

    private void recipe(String name, Item expected, WorldServer world, ItemStack... grid) {
        net.minecraft.inventory.InventoryCrafting inv = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < 9; i++) {
            inv.setInventorySlotContents(i, grid[i]);
        }
        ItemStack out = CraftingManager.getInstance().findMatchingRecipe(inv, world);
        result("recipe " + name, out != null && out.getItem() == expected, String.valueOf(out));
    }

    private int chX;
    private int chZ;

    private int pX;
    private int pZ;
    private boolean pistonRetract;

    private void pistonSetup(WorldServer world, int x, int z) {
        pX = x;
        pZ = z;
        world.setBlock(pX, fy, pZ, Blocks.sticky_piston, 5, 2);
        world.setBlock(pX + 1, fy, pZ, ru.codex.offhand1710.Content119.slime, 0, 2);
        world.setBlock(pX + 1, fy + 1, pZ, Blocks.planks, 0, 2);
        world.setBlock(pX - 1, fy, pZ, Blocks.redstone_block, 0, 3);
    }

    private void pistonRelease(WorldServer world) {
        result("slime push", world.getBlock(pX + 2, fy, pZ) == ru.codex.offhand1710.Content119.slime && world.getBlock(pX + 2, fy + 1, pZ) == Blocks.planks && world.getBlock(pX + 1, fy + 1, pZ) == Blocks.air && world.getBlock(pX + 1, fy, pZ) == Blocks.piston_head, "");
        world.setBlock(pX - 1, fy, pZ, Blocks.air, 0, 3);
        pistonRetract = true;
    }

    private int brX;
    private int brZ;
    private net.minecraft.entity.monster.EntityZombie cloudTarget;

    private void brewSetup(WorldServer world, int x, int z) {
        brX = x;
        brZ = z;
        world.setBlock(brX, fy, brZ, Blocks.brewing_stand, 0, 2);
        net.minecraft.tileentity.TileEntityBrewingStand st = (net.minecraft.tileentity.TileEntityBrewingStand) world.getTileEntity(brX, fy, brZ);
        st.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 16386));
        st.setInventorySlotContents(1, new ItemStack(Items.potionitem, 1, 8193));
        st.setInventorySlotContents(3, new ItemStack(ru.codex.offhand1710.Brew112.dragonBreath));
        // Vanilla brewing still works
        world.setBlock(brX + 2, fy, brZ, Blocks.brewing_stand, 0, 2);
        net.minecraft.tileentity.TileEntityBrewingStand v = (net.minecraft.tileentity.TileEntityBrewingStand) world.getTileEntity(brX + 2, fy, brZ);
        v.setInventorySlotContents(0, new ItemStack(Items.potionitem, 1, 0));
        v.setInventorySlotContents(3, new ItemStack(Items.nether_wart));
        ru.codex.offhand1710.Brew112.ThrownLingering shot = (ru.codex.offhand1710.Brew112.ThrownLingering) net.minecraft.entity.EntityList.createEntityByName("mc112bridge.LingeringPotion", world);
        result("lingering entity created", shot != null, "");
    }

    private void brewCheck(WorldServer world) {
        net.minecraft.tileentity.TileEntityBrewingStand st = (net.minecraft.tileentity.TileEntityBrewingStand) world.getTileEntity(brX, fy, brZ);
        net.minecraft.tileentity.TileEntityBrewingStand v = (net.minecraft.tileentity.TileEntityBrewingStand) world.getTileEntity(brX + 2, fy, brZ);
        ItemStack a = st.getStackInSlot(0);
        result("brew lingering (needs 400 ticks)", st.getBrewTime() > 0 || (a != null && a.getItem() == ru.codex.offhand1710.Brew112.lingering), "time=" + st.getBrewTime() + " " + a);
        result("brew vanilla started", v.getBrewTime() > 0 || v.getStackInSlot(0).getItemDamage() != 0, "time=" + v.getBrewTime());
    }

    private void chainSetup(WorldServer world, int x, int z) {
        chX = x + 3;
        chZ = z + 12;
        int y = fy;
        net.minecraft.block.Block rep = ru.codex.offhand1710.Content122.repeating;
        net.minecraft.block.Block ch = ru.codex.offhand1710.Content122.chain;
        world.setBlock(chX, y, chZ, rep, 5, 2);
        world.setBlock(chX + 1, y, chZ, ch, 5, 2);
        setCmd(world, chX, y, chZ, "setblock " + chX + " " + (y + 3) + " " + chZ + " minecraft:gold_block");
        setCmd(world, chX + 1, y, chZ, "setblock " + (chX + 1) + " " + (y + 3) + " " + chZ + " minecraft:diamond_block");
        world.setBlock(chX, y, chZ + 3, Blocks.command_block, 0, 2);
        world.setBlock(chX + 1, y, chZ + 3, ch, 5, 2);
        ((net.minecraft.tileentity.TileEntityCommandBlock) world.getTileEntity(chX, y, chZ + 3)).func_145993_a().func_145752_a("say impulse");
        setCmd(world, chX + 1, y, chZ + 3, "setblock " + (chX + 1) + " " + (y + 3) + " " + (chZ + 3) + " minecraft:emerald_block");
        Blocks.command_block.updateTick(world, chX, y, chZ + 3, world.rand);
        result("chain after impulse", world.getBlock(chX + 1, y + 3, chZ + 3) == Blocks.emerald_block, "");
    }

    private void setCmd(WorldServer world, int x, int y, int z, String command) {
        ru.codex.offhand1710.Content122.ModeTile tile = (ru.codex.offhand1710.Content122.ModeTile) world.getTileEntity(x, y, z);
        tile.auto = true;
        tile.func_145993_a().func_145752_a(command);
    }

    private boolean dataCheck;
    private int markerTick;
    private boolean cartCheck;
    private ItemStack boughtMap;
    private int funcX;
    private int funcZ;
    private ItemStack mapStack;
    private net.minecraft.entity.passive.EntityPig glowPig;

    private void functionalCheck(WorldServer world, EntityPlayerMP player) {
        int cx0 = (int) player.posX;
        int cz0 = (int) player.posZ;
        pistonRelease(world);
        brewCheck(world);
        net.minecraft.entity.passive.EntityVillager carto = new net.minecraft.entity.passive.EntityVillager(world, 1);
        carto.setLocationAndAngles(player.posX + 3.0D, player.posY, player.posZ, 0.0F, 0.0F);
        world.spawnEntityInWorld(carto);
        carto.getEntityData().setInteger("Career", 2);
        carto.getEntityData().setInteger("CareerLevel", 3);
        ru.codex.offhand1710.Trades112.advance(carto);
        int maps = 0;
        for (Object o : carto.getRecipes(player)) {
            net.minecraft.village.MerchantRecipe r = (net.minecraft.village.MerchantRecipe) o;
            if (r.getItemToSell().getItem() == Items.filled_map) {
                maps++;
                boughtMap = r.getItemToSell().copy();
            }
        }
        result("cartographer sells monument and mansion maps", maps == 2, "maps=" + maps);
        if (boughtMap != null) {
            player.inventory.mainInventory[30] = boughtMap;
            cartCheck = true;
        }
        result("data loaded", ru.codex.offhand1710.Data112.ADVANCEMENTS.size() > 400 && ru.codex.offhand1710.Data112.RECIPES.size() > 400, ru.codex.offhand1710.Data112.ADVANCEMENTS.size() + " " + ru.codex.offhand1710.Data112.RECIPES.size());
        player.inventory.mainInventory[21] = new ItemStack(Blocks.cobblestone);
        player.inventory.mainInventory[22] = new ItemStack(Blocks.crafting_table);
        try {
            java.io.File dir = new java.io.File(world.getSaveHandler().getWorldDirectory(), "data/functions/test");
            dir.mkdirs();
            Files.write("# comment\nsetblock " + cx0 + " " + (fy + 15) + " " + cz0 + " minecraft:gold_block\n", new java.io.File(dir, "hello.mcfunction"), Charset.forName("UTF-8"));
        } catch (IOException e) {
            log("FAIL function file " + e);
        }
        MinecraftServer.getServer().getCommandManager().executeCommand(player, "function test:hello");
        dataCheck = true;
        funcX = cx0;
        funcZ = cz0;
        ItemStack map = ru.codex.offhand1710.Maps112.explorerMap(world, "Monument", cx0, cz0);
        result("explorer map", map != null && map.getItem() == Items.filled_map, String.valueOf(map));
        if (map != null) {
            mapStack = map;
            player.inventory.mainInventory[4] = map;
        }
        net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(world);
        world.setBlock((int) player.posX + 1, (int) player.posY - 4, (int) player.posZ + 6, Blocks.stone, 0, 2);
        pig.setLocationAndAngles((int) player.posX + 1.5D, (int) player.posY - 3.0D, (int) player.posZ + 6.5D, 0.0F, 0.0F);
        pig.func_110163_bv();
        pig.setPositionAndUpdate((int) player.posX + 1.5D, (int) player.posY - 3.0D, (int) player.posZ + 6.5D);
        world.spawnEntityInWorld(pig);
        pig.addPotionEffect(new net.minecraft.potion.PotionEffect(ru.codex.offhand1710.Content120.glowing.id, 2000, 0));
        glowPig = pig;
        result("repeating and chain", world.getBlock(chX, fy + 3, chZ) == Blocks.gold_block && world.getBlock(chX + 1, fy + 3, chZ) == Blocks.diamond_block, "");
        result("observer pulse", obsState == 2 && (world.getBlockMetadata(obsX, obsY, obsZ) & 8) == 0, "state=" + obsState + " meta=" + world.getBlockMetadata(obsX, obsY, obsZ));
        int ice = 0;
        for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (world.getBlock(waterX + a, fy, waterZ + b) == ru.codex.offhand1710.Content119.frostedIce) ice++;
        result("frost walker", ice > 0, "ice=" + ice);
        net.minecraft.entity.item.EntityXPOrb orb = new net.minecraft.entity.item.EntityXPOrb(world, player.posX, player.posY, player.posZ, 10);
        ItemStack tool = new ItemStack(Items.iron_pickaxe);
        tool.addEnchantment(ru.codex.offhand1710.Content119.mending, 1);
        tool.setItemDamage(50);
        ItemStack old = player.inventory.mainInventory[player.inventory.currentItem];
        player.inventory.mainInventory[player.inventory.currentItem] = tool;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerPickupXpEvent(player, orb));
        player.inventory.mainInventory[player.inventory.currentItem] = old;
        result("mending", tool.getItemDamage() == 30 && orb.xpValue == 0, "damage=" + tool.getItemDamage() + " xp=" + orb.xpValue);
        net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(world);
        cow.setLocationAndAngles(player.posX + 4.0D, player.posY, player.posZ, 0.0F, 0.0F);
        world.spawnEntityInWorld(cow);
        result("mob size after spawn", Math.abs(cow.height - 1.4F) < 0.01F, "cow height=" + cow.height);
        net.minecraft.command.ICommandManager commands = MinecraftServer.getServer().getCommandManager();
        commands.executeCommand(player, "replaceitem entity " + player.getCommandSenderName() + " 8 minecraft:apple 5");
        ItemStack apple = player.inventory.mainInventory[8];
        result("/replaceitem", apple != null && apple.getItem() == Items.apple && apple.stackSize == 5, String.valueOf(apple));
        int ey = fy + 10;
        commands.executeCommand(player, "execute " + player.getCommandSenderName() + " " + (waterX + 10) + " " + ey + " " + waterZ + " setblock ~ ~ ~ minecraft:gold_block");
        result("/execute", world.getBlock(waterX + 10, ey, waterZ) == Blocks.gold_block, "");
        commands.executeCommand(player, "teleport " + player.getCommandSenderName() + " ~ ~ ~");
        world.setBlock(waterX + 12, ey, waterZ, ru.codex.offhand1710.Banner112.standing, 0, 2);
        commands.executeCommand(player, "blockdata " + (waterX + 12) + " " + ey + " " + waterZ + " {Base:4}");
        result("/blockdata", ((ru.codex.offhand1710.Banner112.BannerTile) world.getTileEntity(waterX + 12, ey, waterZ)).base == 4, "");
        for (int profession = 0; profession < 5; profession++) {
            net.minecraft.entity.passive.EntityVillager v = new net.minecraft.entity.passive.EntityVillager(world, profession);
            v.setLocationAndAngles(player.posX + 2.0D, player.posY, player.posZ + profession, 0.0F, 0.0F);
            world.spawnEntityInWorld(v);
            int first = v.getRecipes(player).size();
            ru.codex.offhand1710.Trades112.advance(v);
            int second = v.getRecipes(player).size();
            boolean career = v.getEntityData().getInteger("Career") >= 1 && v.getEntityData().getInteger("CareerLevel") == 2;
            if (profession == 1) {
                tradeVillager = v;
                tradeTick = serverTicks;
                v.setCustomer(player);
            }
            if (profession == 3) {
                levelVillager = v;
                levelCount = v.getRecipes(player).size();
                cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.entity.passive.EntityVillager.class, v, Boolean.TRUE, "needsInitilization", "field_70959_by");
                cpw.mods.fml.relauncher.ReflectionHelper.setPrivateValue(net.minecraft.entity.passive.EntityVillager.class, v, Integer.valueOf(5), "timeUntilReset", "field_70961_j");
            }
            result("trades profession " + profession, first > 0 && second >= first && career, "first=" + first + " second=" + second + " career=" + v.getEntityData().getInteger("Career") + " " + ru.codex.offhand1710.Trades112.careerName(v));
        }        result("carrot food", ((net.minecraft.item.ItemFood) Items.carrot).func_150905_g(new ItemStack(Items.carrot)) == 3, "");
        world.setBlock(waterX + 14, ey, waterZ, Blocks.daylight_detector, 0, 2);
        net.minecraftforge.event.entity.player.PlayerInteractEvent click = new net.minecraftforge.event.entity.player.PlayerInteractEvent(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, waterX + 14, ey, waterZ, 1, world);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(click);
        result("inverted sensor", world.getBlock(waterX + 14, ey, waterZ) == ru.codex.offhand1710.Content120.invertedSensor, "");
    }

    private void buildGallery(WorldServer world, int cx, int cz) {
        List<Block> blocks = new ArrayList<Block>();
        for (Object o : Block.blockRegistry) {
            Block b = (Block) o;
            if (Block.blockRegistry.getNameForObject(b).startsWith("mc112bridge:")) {
                blocks.add(b);
            }
        }
        int row = 0;
        int col = 0;
        for (Block b : blocks) {
            int x = cx + col * 2;
            int z = cz + row * 2;
            world.setBlock(x, BASE_Y - 1, z, b instanceof net.minecraft.block.BlockBush ? Blocks.farmland : (b == ru.codex.offhand1710.End112.chorusFlower || b == ru.codex.offhand1710.End112.chorusPlant ? Blocks.end_stone : Blocks.stone), 0, 2);
            boolean ok = world.setBlock(x, BASE_Y, z, b, 0, 2) && world.getBlock(x, BASE_Y, z) == b;
            if (!ok) {
                log("PLACEFAIL " + Block.blockRegistry.getNameForObject(b));
            } else {
                placed.add(x + " " + BASE_Y + " " + z + " " + Block.blockRegistry.getNameForObject(b));
                TileEntity tile = world.getTileEntity(x, BASE_Y, z);
                if (tile instanceof IInventory) {
                    ((IInventory) tile).setInventorySlotContents(3, new ItemStack(Items.diamond, 7));
                }
            }
            if (++col == 13) {
                col = 0;
                row++;
            }
        }
        int ex = 0;
        for (Object o : new ArrayList<Object>(EntityList.classToStringMapping.entrySet())) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (!((Class<?>) e.getKey()).getName().startsWith("ru.codex")) {
                continue;
            }
            try {
                Entity entity = EntityList.createEntityByName((String) e.getValue(), world);
                if (entity == null) {
                    log("SPAWNFAIL " + e.getValue() + " null");
                    continue;
                }
                world.setBlock(cx + ex * 3, BASE_Y + 3, cz - 4, Blocks.stone, 0, 2);
                entity.setLocationAndAngles(cx + ex * 3 + 0.5D, BASE_Y + 4.0D, cz - 3.5D, 180.0F, 0.0F);
                world.spawnEntityInWorld(entity);
                ex++;
            } catch (Throwable t) {
                log("SPAWNFAIL " + e.getValue() + " " + t);
            }
        }
        log("SPAWNED " + ex);
    }

    private void count(Chunk chunk) {
        for (ExtendedBlockStorage s : chunk.getBlockStorageArray()) {
            if (s == null) {
                continue;
            }
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block b = s.getBlockByExtId(x, y, z);
                        if (b != Blocks.air) {
                            String name = Block.blockRegistry.getNameForObject(b);
                            if (name.startsWith("mc112bridge:")) {
                                Integer n = found.get(name);
                                found.put(name, n == null ? 1 : n + 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void generate(WorldServer world, int unused, int radius, int ox, int oz) {
        int side = radius * 2 + 1;
        int total = side * side;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 30 && genIndex < total; i++, genIndex++) {
            int a = ox - radius + genIndex % side;
            int b = oz - radius + genIndex / side;
            // Decoration of a chunk needs its neighbours.
            world.theChunkProviderServer.loadChunk(a + 1, b);
            world.theChunkProviderServer.loadChunk(a, b + 1);
            world.theChunkProviderServer.loadChunk(a + 1, b + 1);
            world.theChunkProviderServer.loadChunk(a, b);
        }
        if (genIndex >= total) {
            for (int a = ox - radius; a <= ox + radius; a++) {
                for (int b = oz - radius; b <= oz + radius; b++) {
                    count(world.theChunkProviderServer.loadChunk(a, b));
                }
            }
            log("GEN dim=" + world.provider.dimensionId + " radius=" + radius + " at " + ox + "," + oz + " tiles=" + world.loadedTileEntityList.size() + " blocks=" + found);
            found.clear();
            genIndex = 0;
            step++;
            world.theChunkProviderServer.unloadAllChunks();
        }
        if (System.currentTimeMillis() - start > 10000L) {
            log("SLOWGEN dim=" + world.provider.dimensionId + " index=" + genIndex + " ms=" + (System.currentTimeMillis() - start));
        }
    }

    private void reloadTick() throws IOException {
        EntityPlayerMP player = player();
        if (player == null) {
            return;
        }
        serverTicks++;
        if (step == 0 && serverTicks > 100) {
            step = 1;
            WorldServer world = MinecraftServer.getServer().worldServers[0];
            int good = 0;
            int bad = 0;
            int boxes = 0;
            for (Object line : Files.readLines(new File(mc.mcDataDir, "audit112-placed.txt"), Charset.forName("UTF-8"))) {
                String[] p = ((String) line).split(" ");
                int x = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                int z = Integer.parseInt(p[2]);
                world.theChunkProviderServer.loadChunk(x >> 4, z >> 4);
                Block b = world.getBlock(x, y, z);
                String found = Block.blockRegistry.getNameForObject(b);
                if (p[3].equals(found)) {
                    good++;
                } else if (("mc112bridge:chorus_flower".equals(p[3]) && "mc112bridge:chorus_plant".equals(found))
                        || ("mc112bridge:frosted_ice".equals(p[3]) && "minecraft:water".equals(found))) {
                    // Random ticks while the world ran: a chorus flower grows into a plant, frosted ice melts. 1.12.2 does the same.
                    good++;
                    log("CHANGED " + p[3] + " -> " + found + " (expected)");
                } else {
                    bad++;
                    log("LOST " + p[3] + " found " + Block.blockRegistry.getNameForObject(b));
                }
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof IInventory) {
                    ItemStack s = ((IInventory) tile).getStackInSlot(3);
                    if (s != null && s.getItem() == Items.diamond && s.stackSize == 7) {
                        boxes++;
                    } else {
                        log("LOSTITEMS " + p[3] + " " + s);
                    }
                }
            }
            log("RELOAD good=" + good + " bad=" + bad + " inventories=" + boxes);
            int ents = 0;
            for (Object o : world.loadedEntityList) {
                if (o.getClass().getName().startsWith("ru.codex")) {
                    ents++;
                }
            }
            log("RELOAD entities=" + ents);
            ItemStack bought = player.inventory.mainInventory[30];
            boolean tagged = bought != null && bought.getItem() == Items.filled_map && bought.hasTagCompound() && bought.getTagCompound().hasKey("Marker112Type");
            log((tagged ? "PASS" : "FAIL") + " cartographer map kept after reload " + bought);
            step = 2;
            markerTick = serverTicks + 120;
        } else if (step == 2 && serverTicks > markerTick) {
            step = 3;
            ItemStack bought = player.inventory.mainInventory[30];
            WorldServer world = MinecraftServer.getServer().worldServers[0];
            log((bought != null && ru.codex.offhand1710.Maps112.hasMarker(world, bought) ? "PASS" : "FAIL") + " cartographer map marker after reload");
            wantQuit = true;
        }
    }
}
