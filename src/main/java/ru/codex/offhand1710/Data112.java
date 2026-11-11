package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

/** Advancements, the recipe book and functions of 1.12: data read from JSON kept in the mod, own triggers and storage. */
public final class Data112 {
    public static final Map<String, Advancement> ADVANCEMENTS = new LinkedHashMap<String, Advancement>();
    public static final Map<String, Recipe> RECIPES = new LinkedHashMap<String, Recipe>();
    private static final Map<String, String[]> ALIASES = new HashMap<String, String[]>();
    private static final Map<String, String> ENTITIES = new HashMap<String, String>();
    private static final Map<String, String> BIOMES = new HashMap<String, String>();
    private static final Map<String, List<Criterion>> BY_TRIGGER = new HashMap<String, List<Criterion>>();
    private static final String TAG = "mc112data";
    private static final Set<EntityPlayerMP> DIRTY = Collections.newSetFromMap(new WeakHashMap<EntityPlayerMP, Boolean>());

    private Data112() {
    }

    public static final class Advancement {
        public String id;
        public String parent;
        public String title;
        public String description;
        public ItemStack icon;
        public String frame = "task";
        public String background;
        public boolean hidden;
        public boolean toast = true;
        public boolean announce = true;
        public boolean shown;
        final Map<String, Criterion> criteria = new LinkedHashMap<String, Criterion>();
        List<List<String>> requirements = new ArrayList<List<String>>();
        final List<String> rewardRecipes = new ArrayList<String>();
        int rewardXp;
        String rewardFunction;

        /** Number of requirement groups: the n of the progress n/m. */
        public int total() {
            return requirements.size();
        }

        /** Requirement groups with at least one criterion done. */
        public int completed(java.util.Set<String> crit) {
            int n = 0;
            for (List<String> any : requirements) {
                for (String c : any) {
                    if (crit.contains(id + "|" + c)) {
                        n++;
                        break;
                    }
                }
            }
            return n;
        }
    }

    static final class Criterion {
        Advancement owner;
        String name;
        String trigger;
        JsonObject conditions;
    }

    public static final class Recipe {
        public String id;
        public String group;
        public boolean shaped;
        public int width;
        public int height;
        public List<List<ItemStack>> slots = new ArrayList<List<ItemStack>>();
        public ItemStack result;
    }

    private static String full(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String text(JsonElement e) {
        if (e == null) {
            return "";
        }
        if (e.isJsonPrimitive()) {
            return e.getAsString();
        }
        JsonObject o = e.getAsJsonObject();
        return o.has("translate") ? o.get("translate").getAsString() : (o.has("text") ? o.get("text").getAsString() : "");
    }

    private static String[] lines(String resource) {
        InputStream in = Data112.class.getResourceAsStream("/assets/mc112bridge/" + resource);
        if (in == null) {
            throw new IllegalStateException("Missing " + resource);
        }
        try {
            List<String> out = new ArrayList<String>();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.length() > 0) {
                    out.add(line);
                }
            }
            r.close();
            return out.toArray(new String[0]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonObject json(String resource) {
        InputStream in = Data112.class.getResourceAsStream("/assets/mc112bridge/" + resource);
        if (in == null) {
            return null;
        }
        return new JsonParser().parse(new InputStreamReader(in, Charset.forName("UTF-8"))).getAsJsonObject();
    }

    public static ItemStack item(String name, int data) {
        String[] alias = ALIASES.get(name + ":" + data);
        if (alias == null) {
            alias = ALIASES.get(name + ":0");
        }
        String registry = alias == null ? name : alias[0];
        int meta = alias == null ? data : Integer.parseInt(alias[1]);
        Object item = Item.itemRegistry.getObject(registry);
        return item == null ? null : new ItemStack((Item) item, 1, meta);
    }

    private static List<ItemStack> options(JsonElement e) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        JsonArray array;
        if (e.isJsonArray()) {
            array = e.getAsJsonArray();
        } else {
            array = new JsonArray();
            array.add(e);
        }
        for (JsonElement x : array) {
            JsonObject o = x.getAsJsonObject();
            int data = o.has("data") ? o.get("data").getAsInt() : 0;
            ItemStack s = item(o.get("item").getAsString(), data == 32767 ? 0 : data);
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    public static void load() {
        ADVANCEMENTS.clear();
        RECIPES.clear();
        BY_TRIGGER.clear();
        for (String l : lines("data/aliases.txt")) {
            String[] p = l.split("=");
            ALIASES.put(p[0], p[1].split(" "));
        }
        for (String l : lines("data/entities.txt")) {
            String[] p = l.split("=");
            ENTITIES.put(p[0], p[1]);
        }
        for (String l : lines("data/biomes.txt")) {
            String[] p = l.split("=");
            BIOMES.put(p[0], p[1]);
        }
        for (String name : lines("data/recipes.txt")) {
            loadRecipe(name);
        }
        for (String name : lines("data/advancements.txt")) {
            loadAdvancement(name);
        }
    }

    private static void loadRecipe(String name) {
        JsonObject o = json("data/recipes/" + name + ".json");
        if (o == null) {
            return;
        }
        JsonObject res = o.getAsJsonObject("result");
        ItemStack result = item(res.get("item").getAsString(), res.has("data") ? res.get("data").getAsInt() : 0);
        if (result == null) {
            return;
        }
        Recipe r = new Recipe();
        r.id = "minecraft:" + name;
        r.group = o.has("group") ? o.get("group").getAsString() : name;
        result = result.copy();
        result.stackSize = res.has("count") ? res.get("count").getAsInt() : 1;
        r.result = result;
        if ("crafting_shaped".equals(o.get("type").getAsString())) {
            r.shaped = true;
            JsonArray pattern = o.getAsJsonArray("pattern");
            JsonObject key = o.getAsJsonObject("key");
            r.height = pattern.size();
            r.width = pattern.get(0).getAsString().length();
            for (JsonElement row : pattern) {
                for (char c : row.getAsString().toCharArray()) {
                    r.slots.add(c == ' ' ? new ArrayList<ItemStack>() : options(key.get(String.valueOf(c))));
                }
            }
        } else {
            for (JsonElement ing : o.getAsJsonArray("ingredients")) {
                r.slots.add(options(ing));
            }
        }
        RECIPES.put(r.id, r);
    }

    private static void loadAdvancement(String name) {
        JsonObject o = json("data/advancements/" + name + ".json");
        if (o == null) {
            return;
        }
        Advancement a = new Advancement();
        a.id = "minecraft:" + name;
        a.parent = o.has("parent") ? full(o.get("parent").getAsString()) : null;
        if (o.has("display")) {
            JsonObject d = o.getAsJsonObject("display");
            a.shown = true;
            a.title = text(d.get("title"));
            a.description = text(d.get("description"));
            JsonObject icon = d.getAsJsonObject("icon");
            a.icon = item(icon.get("item").getAsString(), icon.has("data") ? icon.get("data").getAsInt() : 0);
            a.frame = d.has("frame") ? d.get("frame").getAsString() : "task";
            a.background = d.has("background") ? d.get("background").getAsString() : null;
            a.hidden = d.has("hidden") && d.get("hidden").getAsBoolean();
            a.toast = !d.has("show_toast") || d.get("show_toast").getAsBoolean();
            a.announce = !d.has("announce_to_chat") || d.get("announce_to_chat").getAsBoolean();
        }
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("criteria").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            Criterion cr = new Criterion();
            cr.owner = a;
            cr.name = e.getKey();
            cr.trigger = c.get("trigger").getAsString().replace("minecraft:", "");
            cr.conditions = c.has("conditions") ? c.getAsJsonObject("conditions") : new JsonObject();
            a.criteria.put(cr.name, cr);
            List<Criterion> list = BY_TRIGGER.get(cr.trigger);
            if (list == null) {
                list = new ArrayList<Criterion>();
                BY_TRIGGER.put(cr.trigger, list);
            }
            list.add(cr);
        }
        if (o.has("requirements")) {
            for (JsonElement group : o.getAsJsonArray("requirements")) {
                List<String> any = new ArrayList<String>();
                for (JsonElement x : group.getAsJsonArray()) {
                    any.add(x.getAsString());
                }
                a.requirements.add(any);
            }
        } else {
            for (String c : a.criteria.keySet()) {
                List<String> one = new ArrayList<String>();
                one.add(c);
                a.requirements.add(one);
            }
        }
        if (o.has("rewards")) {
            JsonObject r = o.getAsJsonObject("rewards");
            if (r.has("recipes")) {
                for (JsonElement x : r.getAsJsonArray("recipes")) {
                    a.rewardRecipes.add(full(x.getAsString()));
                }
            }
            a.rewardXp = r.has("experience") ? r.get("experience").getAsInt() : 0;
            a.rewardFunction = r.has("function") ? r.get("function").getAsString() : null;
        }
        ADVANCEMENTS.put(a.id, a);
    }

    public static void register() {
        load();
        Events events = new Events();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    // ---- player storage ----

    private static NBTTagCompound store(EntityPlayer p) {
        NBTTagCompound persisted = p.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!p.getEntityData().hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            p.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        if (!persisted.hasKey(TAG)) {
            persisted.setTag(TAG, new NBTTagCompound());
        }
        return persisted.getCompoundTag(TAG);
    }

    private static Set<String> set(EntityPlayer p, String key) {
        Set<String> out = new HashSet<String>();
        NBTTagList list = store(p).getTagList(key, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            out.add(list.getStringTagAt(i));
        }
        return out;
    }

    private static void write(EntityPlayer p, String key, Set<String> values) {
        NBTTagList list = new NBTTagList();
        for (String v : values) {
            list.appendTag(new NBTTagString(v));
        }
        store(p).setTag(key, list);
    }

    public static boolean isDone(EntityPlayer p, String id) {
        return set(p, "done").contains(id);
    }

    public static boolean knowsRecipe(EntityPlayer p, String id) {
        return set(p, "recipes").contains(id);
    }

    private static boolean complete(Advancement a, Set<String> crit) {
        if (a.requirements.isEmpty()) {
            return false;
        }
        for (List<String> any : a.requirements) {
            boolean ok = false;
            for (String c : any) {
                ok |= crit.contains(a.id + "|" + c);
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Grants one criterion; returns true if something changed. */
    public static boolean grant(EntityPlayerMP p, Advancement a, String criterion) {
        Set<String> done = set(p, "done");
        if (done.contains(a.id)) {
            return false;
        }
        Set<String> crit = set(p, "crit");
        if (!crit.add(a.id + "|" + criterion)) {
            return false;
        }
        write(p, "crit", crit);
        if (complete(a, crit)) {
            done.add(a.id);
            write(p, "done", done);
            finish(p, a);
        }
        return true;
    }

    private static void finish(EntityPlayerMP p, Advancement a) {
        if (!a.rewardRecipes.isEmpty()) {
            Set<String> known = set(p, "recipes");
            known.addAll(a.rewardRecipes);
            write(p, "recipes", known);
        }
        if (a.rewardXp > 0) {
            p.addExperience(a.rewardXp);
        }
        if (a.rewardFunction != null) {
            runFunction(p, a.rewardFunction);
        }
        if (a.shown && a.toast) {
            MC112Bridge.NETWORK.channel.sendTo(new Toast(a.id), p);
        }
        if (a.shown && a.announce && !a.hidden) {
            IChatComponent title = new ChatComponentTranslation(a.title);
            MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentTranslation("chat.type.advancement." + a.frame, p.func_145748_c_(), title));
        }
        sync(p);
        fire(p, "recipe_unlocked", null);
    }

    /**
     * The whole state goes to the client at most once a tick: granting every advancement asked for a sync some
     * thousand times in one tick.
     */
    public static void sync(EntityPlayerMP p) {
        DIRTY.add(p);
    }

    private static void flush(EntityPlayerMP p) {
        if (!DIRTY.remove(p)) {
            return;
        }
        MC112Bridge.NETWORK.channel.sendTo(new Sync(join(set(p, "done")), join(set(p, "crit")), join(set(p, "recipes"))), p);
    }

    private static String join(Set<String> s) {
        StringBuilder b = new StringBuilder();
        for (String v : s) {
            b.append(v).append(';');
        }
        return b.toString();
    }

    public static void revokeAll(EntityPlayerMP p) {
        write(p, "done", new HashSet<String>());
        write(p, "crit", new HashSet<String>());
        sync(p);
    }

    // ---- triggers ----

    interface Check {
        boolean matches(JsonObject conditions);
    }

    private static void fire(EntityPlayerMP p, String trigger, Check check) {
        List<Criterion> list = BY_TRIGGER.get(trigger);
        if (list == null) {
            return;
        }
        boolean changed = false;
        for (Criterion c : new ArrayList<Criterion>(list)) {
            if (isDone(p, c.owner.id)) {
                continue;
            }
            if ("recipe_unlocked".equals(trigger)) {
                if (knowsRecipe(p, full(c.conditions.get("recipe").getAsString()))) {
                    changed |= grant(p, c.owner, c.name);
                }
            } else if (check == null || check.matches(c.conditions)) {
                changed |= grant(p, c.owner, c.name);
            }
        }
        if (changed) {
            sync(p);
        }
    }

    private static boolean hasItem(EntityPlayer p, ItemStack want, int data) {
        for (ItemStack s : p.inventory.mainInventory) {
            if (s != null && s.getItem() == want.getItem() && (data < 0 || s.getItemDamage() == want.getItemDamage())) {
                return true;
            }
        }
        for (ItemStack s : p.inventory.armorInventory) {
            if (s != null && s.getItem() == want.getItem()) {
                return true;
            }
        }
        return false;
    }

    private static boolean inventory(EntityPlayer p, JsonObject c) {
        if (!c.has("items")) {
            return false;
        }
        for (JsonElement e : c.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            if (!o.has("item")) {
                return false;
            }
            int data = o.has("data") ? o.get("data").getAsInt() : -1;
            ItemStack want = item(o.get("item").getAsString(), Math.max(data, 0));
            if (want == null || !hasItem(p, want, data)) {
                return false;
            }
        }
        return true;
    }

    private static boolean itemCond(JsonObject c, ItemStack used) {
        if (!c.has("item")) {
            return true;
        }
        JsonObject o = c.getAsJsonObject("item");
        if (!o.has("item")) {
            return true;
        }
        ItemStack want = item(o.get("item").getAsString(), 0);
        return want != null && used != null && want.getItem() == used.getItem();
    }

    private static boolean entityCond(JsonObject c, String field, Entity e) {
        if (!c.has(field)) {
            return true;
        }
        JsonObject o = c.getAsJsonObject(field);
        if (!o.has("type")) {
            return true;
        }
        String name = ENTITIES.get(o.get("type").getAsString());
        String actual = EntityList.getEntityString(e);
        if (actual == null) {
            return e instanceof EntityPlayer && "minecraft:player".equals(o.get("type").getAsString());
        }
        return name != null && (name.equals(actual) || actual.endsWith("." + name));
    }

    private static boolean range(JsonObject parent, String axis, double value) {
        if (!parent.has(axis)) {
            return true;
        }
        JsonElement e = parent.get(axis);
        if (e.isJsonPrimitive()) {
            return value == e.getAsDouble();
        }
        JsonObject o = e.getAsJsonObject();
        return (!o.has("min") || value >= o.get("min").getAsDouble()) && (!o.has("max") || value <= o.get("max").getAsDouble());
    }

    private static boolean location(EntityPlayerMP p, JsonObject c) {
        if (c.has("dimension") && dimension(c.get("dimension").getAsString()) != p.dimension) {
            return false;
        }
        if (c.has("biome")) {
            String wanted = BIOMES.get(c.get("biome").getAsString());
            BiomeGenBase biome = p.worldObj.getBiomeGenForCoords(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posZ));
            if (wanted == null || !wanted.equals(biome.biomeName)) {
                return false;
            }
        }
        if (c.has("position")) {
            JsonObject pos = c.getAsJsonObject("position");
            if (!range(pos, "x", p.posX) || !range(pos, "y", p.posY) || !range(pos, "z", p.posZ)) {
                return false;
            }
        }
        if (c.has("feature")) {
            String f = c.get("feature").getAsString();
            int x = MathHelper.floor_double(p.posX);
            int y = MathHelper.floor_double(p.posY);
            int z = MathHelper.floor_double(p.posZ);
            if ("Monument".equals(f)) {
                return MonumentGenerator.isInsideMonument(p.worldObj, x, y, z);
            }
            if ("Fortress".equals(f)) {
                return p.dimension == -1 && p.worldObj.getBlock(x, y - 1, z) == net.minecraft.init.Blocks.nether_brick;
            }
            if ("EndCity".equals(f)) {
                return p.dimension == 1 && p.worldObj.getBlock(x, y - 1, z) == End112PurpurLookup.purpur();
            }
            return false;
        }
        return true;
    }

    private static final class End112PurpurLookup {
        static Block purpur() {
            return PortedBlocks.purpurBlock;
        }

        private End112PurpurLookup() {
        }
    }

    private static int dimension(String name) {
        return "nether".equals(name) ? -1 : ("the_end".equals(name) ? 1 : 0);
    }

    public static final class Events {
        private final Map<String, Integer> lastDim = new HashMap<String, Integer>();

        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote || !(event.player instanceof EntityPlayerMP)) {
                return;
            }
            final EntityPlayerMP p = (EntityPlayerMP) event.player;
            fire(p, "tick", null);
            if (p.isPotionActive(Content120.levitation)) {
                fire(p, "levitation", null);
            }
            if (!p.getActivePotionEffects().isEmpty()) {
                fire(p, "effects_changed", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return !c.has("effects");
                    }
                });
            }
            if (p.ticksExisted % 10 == 0) {
                fire(p, "inventory_changed", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return inventory(p, c);
                    }
                });
            }
            if (p.ticksExisted % 20 == 0) {
                fire(p, "location", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return location(p, c);
                    }
                });
                Integer dim = lastDim.get(p.getCommandSenderName());
                if (dim != null && dim.intValue() != p.dimension) {
                    final int from = dim.intValue();
                    fire(p, "changed_dimension", new Check() {
                        @Override
                        public boolean matches(JsonObject c) {
                            return (!c.has("from") || dimension(c.get("from").getAsString()) == from) && (!c.has("to") || dimension(c.get("to").getAsString()) == p.dimension);
                        }
                    });
                }
                lastDim.put(p.getCommandSenderName(), Integer.valueOf(p.dimension));
            }
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                sync((EntityPlayerMP) event.player);
            }
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                sync((EntityPlayerMP) event.player);
            }
        }

        @SubscribeEvent
        public void onDeath(final LivingDeathEvent event) {
            Entity killer = event.source.getEntity();
            if (killer instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) killer, "player_killed_entity", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return entityCond(c, "entity", event.entityLiving);
                    }
                });
            }
            if (event.entityLiving instanceof EntityPlayerMP && killer != null) {
                final Entity k = killer;
                fire((EntityPlayerMP) event.entityLiving, "entity_killed_player", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return entityCond(c, "entity", k);
                    }
                });
            }
        }

        @SubscribeEvent
        public void onHurt(final LivingHurtEvent event) {
            if (event.source.getEntity() instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.source.getEntity(), "player_hurt_entity", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return entityCond(c, "entity", event.entityLiving);
                    }
                });
            }
            if (event.entityLiving instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.entityLiving, "entity_hurt_player", null);
            }
        }

        /** Vanilla 1.7.10 has no breeding event: a new baby credits the closest player within 16 blocks. */
        @SubscribeEvent
        public void onJoin(final EntityJoinWorldEvent event) {
            if (event.world.isRemote) {
                return;
            }
            Entity e = event.entity;
            boolean baby = e instanceof net.minecraft.entity.passive.EntityAnimal && ((net.minecraft.entity.passive.EntityAnimal) e).isChild() && e.ticksExisted == 0;
            boolean summoned = e instanceof net.minecraft.entity.boss.EntityWither || e instanceof net.minecraft.entity.monster.EntityIronGolem || e instanceof net.minecraft.entity.monster.EntitySnowman;
            if (!baby && !summoned) {
                return;
            }
            EntityPlayer near = event.world.getClosestPlayerToEntity(e, 16.0D);
            if (near instanceof EntityPlayerMP) {
                final Entity target = e;
                fire((EntityPlayerMP) near, baby ? "bred_animals" : "summoned_entity", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return entityCond(c, "child", target) && entityCond(c, "entity", target);
                    }
                });
            }
        }

        @SubscribeEvent
        public void onSleep(PlayerSleepInBedEvent event) {
            if (event.entityPlayer instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.entityPlayer, "slept_in_bed", null);
            }
        }

        @SubscribeEvent
        public void onConsume(final PlayerUseItemEvent.Finish event) {
            if (event.entityPlayer instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.entityPlayer, "consume_item", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        return itemCond(c, event.item);
                    }
                });
            }
        }

        @SubscribeEvent
        public void onPlace(final BlockEvent.PlaceEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.player, "placed_block", new Check() {
                    @Override
                    public boolean matches(JsonObject c) {
                        if (!c.has("block")) {
                            return true;
                        }
                        ItemStack want = item(c.get("block").getAsString(), 0);
                        return want != null && Block.getBlockFromItem(want.getItem()) == event.block;
                    }
                });
            }
        }

        @SubscribeEvent
        public void onTrade(EntityInteractEvent event) {
            if (event.entityPlayer instanceof EntityPlayerMP && event.target instanceof net.minecraft.entity.passive.EntityVillager) {
                lastTrader = (EntityPlayerMP) event.entityPlayer;
            }
        }

        @SubscribeEvent
        public void onCraft(PlayerEvent.ItemCraftedEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                fire((EntityPlayerMP) event.player, "recipe_unlocked", null);
            }
        }

        @SubscribeEvent
        public void onWorld(WorldEvent.Load event) {
            if (!event.world.getGameRules().hasRule("gameLoopFunction")) {
                event.world.getGameRules().addGameRule("gameLoopFunction", "-");
            }
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (MinecraftServer.getServer() == null) {
                return;
            }
            if (event.phase == TickEvent.Phase.END) {
                if (!DIRTY.isEmpty()) {
                    for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                        flush((EntityPlayerMP) o);
                    }
                    // the ones left behind have logged out or respawned as a new entity; a new one syncs itself
                    DIRTY.clear();
                }
                return;
            }
            WorldServer world = MinecraftServer.getServer().worldServers[0];
            String loop = world.getGameRules().getGameRuleStringValue("gameLoopFunction");
            if (loop != null && loop.length() > 0 && !"-".equals(loop)) {
                runFunction(MinecraftServer.getServer(), loop);
            }
        }

        private EntityPlayerMP lastTrader;
    }

    public static void trigger(EntityPlayerMP p, String name) {
        fire(p, name, null);
    }

    // ---- functions ----

    private static final Map<String, List<String>> FUNCTIONS = new HashMap<String, List<String>>();
    private static final int MAX_LINES = 65536;
    private static int running;

    private static File functionFile(String id) {
        String[] p = id.split(":", 2);
        String ns = p.length == 2 ? p[0] : "minecraft";
        String path = p.length == 2 ? p[1] : p[0];
        File base = new File(MinecraftServer.getServer().worldServers[0].getSaveHandler().getWorldDirectory(), "data/functions");
        return new File(new File(base, ns), path + ".mcfunction");
    }

    public static void reloadFunctions() {
        FUNCTIONS.clear();
    }

    /** FunctionManager.execute: every line runs as the sender; nested calls are counted together. */
    public static int runFunction(ICommandSender sender, String id) {
        List<String> body = FUNCTIONS.get(id);
        if (body == null) {
            File f = functionFile(id);
            if (!f.isFile()) {
                return -1;
            }
            body = new ArrayList<String>();
            try {
                for (Object o : Files.readLines(f, Charset.forName("UTF-8"))) {
                    String line = ((String) o).trim();
                    if (line.length() > 0 && !line.startsWith("#")) {
                        body.add(line.startsWith("/") ? line.substring(1) : line);
                    }
                }
            } catch (IOException e) {
                return -1;
            }
            FUNCTIONS.put(id, body);
        }
        int count = 0;
        for (String line : body) {
            if (running >= MAX_LINES) {
                break;
            }
            running++;
            try {
                MinecraftServer.getServer().getCommandManager().executeCommand(sender, line);
            } finally {
                running--;
            }
            count++;
        }
        return count;
    }

    public static final class CommandFunction extends CommandBase {
        @Override
        public String getCommandName() {
            return "function";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/function <name> [if <selector>|unless <selector>]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 && args.length != 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            if (args.length == 3) {
                boolean found = PlayerSelector.matchOnePlayer(sender, args[2]) != null;
                if (found != "if".equals(args[1])) {
                    sender.addChatMessage(new ChatComponentText("Skipped function " + args[0]));
                    return;
                }
            }
            int n = runFunction(sender, args[0]);
            if (n < 0) {
                throw new WrongUsageException("Unknown function " + args[0]);
            }
            sender.addChatMessage(new ChatComponentText("Executed " + n + " commands from function " + args[0]));
        }
    }

    public static final class CommandReload extends CommandBase {
        @Override
        public String getCommandName() {
            return "reload";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/reload";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            reloadFunctions();
            load();
            sender.addChatMessage(new ChatComponentText("Reloaded " + ADVANCEMENTS.size() + " advancements, " + RECIPES.size() + " recipes"));
        }
    }

    public static final class CommandRecipe extends CommandBase {
        @Override
        public String getCommandName() {
            return "recipe";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/recipe <give|take> <player> <name|*>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            EntityPlayerMP p = getPlayer(sender, args[1]);
            Set<String> known = set(p, "recipes");
            Set<String> names = new HashSet<String>();
            if ("*".equals(args[2])) {
                names.addAll(RECIPES.keySet());
            } else {
                names.add(args[2].contains(":") ? args[2] : "minecraft:" + args[2]);
            }
            if ("give".equals(args[0])) {
                known.addAll(names);
            } else {
                known.removeAll(names);
            }
            write(p, "recipes", known);
            sync(p);
            sender.addChatMessage(new ChatComponentText("Recipes changed: " + names.size()));
        }
    }

    public static final class CommandAdvancement extends CommandBase {
        @Override
        public String getCommandName() {
            return "advancement";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/advancement <grant|revoke> <player> <only <id>|everything>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            EntityPlayerMP p = getPlayer(sender, args[1]);
            boolean grant = "grant".equals(args[0]);
            if ("everything".equals(args[2])) {
                if (grant) {
                    for (Advancement a : ADVANCEMENTS.values()) {
                        for (String c : a.criteria.keySet()) {
                            grant(p, a, c);
                        }
                    }
                } else {
                    revokeAll(p);
                }
            } else if ("only".equals(args[2]) && args.length >= 4) {
                Advancement a = ADVANCEMENTS.get(args[3].contains(":") ? args[3] : "minecraft:" + args[3]);
                if (a == null) {
                    throw new WrongUsageException("Unknown advancement " + args[3]);
                }
                if (grant) {
                    for (String c : a.criteria.keySet()) {
                        grant(p, a, c);
                    }
                } else {
                    Set<String> done = set(p, "done");
                    done.remove(a.id);
                    write(p, "done", done);
                    Set<String> crit = set(p, "crit");
                    for (String c : a.criteria.keySet()) {
                        crit.remove(a.id + "|" + c);
                    }
                    write(p, "crit", crit);
                }
            } else {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            sync(p);
            sender.addChatMessage(new ChatComponentText("Advancements updated"));
        }
    }

    // ---- recipe book ----

    private static boolean matches(ItemStack have, List<ItemStack> options) {
        for (ItemStack o : options) {
            if (o.getItem() == have.getItem() && (o.getItemDamage() == have.getItemDamage() || o.getItemDamage() == 32767)) {
                return true;
            }
        }
        return false;
    }

    /** Moves the ingredients of a known recipe from the inventory into the open crafting grid. */
    public static boolean fill(EntityPlayerMP p, String id) {
        Recipe r = RECIPES.get(id);
        Container c = p.openContainer;
        int size = c instanceof ContainerPlayer ? 2 : (c instanceof ContainerWorkbench ? 3 : 0);
        if (r == null || size == 0 || !knowsRecipe(p, id) || (r.shaped && (r.width > size || r.height > size)) || r.slots.size() > size * size) {
            return false;
        }
        int first = 1;
        for (int i = 0; i < size * size; i++) {
            Slot slot = (Slot) c.inventorySlots.get(first + i);
            if (slot.getHasStack()) {
                ItemStack old = slot.getStack();
                slot.putStack(null);
                if (!p.inventory.addItemStackToInventory(old)) {
                    p.dropPlayerItemWithRandomChoice(old, false);
                }
            }
        }
        // Check first that everything is there.
        ItemStack[] copy = new ItemStack[p.inventory.mainInventory.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = p.inventory.mainInventory[i] == null ? null : p.inventory.mainInventory[i].copy();
        }
        int[] from = new int[r.slots.size()];
        for (int i = 0; i < from.length; i++) {
            from[i] = -1;
            if (r.slots.get(i).isEmpty()) {
                continue;
            }
            for (int j = 0; j < copy.length; j++) {
                if (copy[j] != null && copy[j].stackSize > 0 && matches(copy[j], r.slots.get(i))) {
                    from[i] = j;
                    copy[j].stackSize--;
                    break;
                }
            }
            if (from[i] < 0) {
                c.detectAndSendChanges();
                return false;
            }
        }
        for (int i = 0; i < from.length; i++) {
            if (from[i] < 0) {
                continue;
            }
            int target = first + (r.shaped ? (i / r.width) * size + i % r.width : i);
            ItemStack have = p.inventory.decrStackSize(from[i], 1);
            ((Slot) c.inventorySlots.get(target)).putStack(have);
        }
        c.detectAndSendChanges();
        return true;
    }

    public static final class FillMessage implements IMessage {
        public String id = "";

        public FillMessage() {
        }

        public FillMessage(String id) {
            this.id = id;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            id = ByteBufUtils.readUTF8String(b);
        }

        @Override
        public void toBytes(ByteBuf b) {
            ByteBufUtils.writeUTF8String(b, id);
        }
    }

    public static final class FillHandler implements IMessageHandler<FillMessage, IMessage> {
        @Override
        public IMessage onMessage(FillMessage m, MessageContext context) {
            fill(context.getServerHandler().playerEntity, m.id);
            return null;
        }
    }

    public static final class Sync implements IMessage {
        public String done = "";
        public String crit = "";
        public String recipes = "";

        public Sync() {
        }

        Sync(String done, String crit, String recipes) {
            this.done = done;
            this.crit = crit;
            this.recipes = recipes;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            done = readLong(b);
            crit = readLong(b);
            recipes = readLong(b);
        }

        @Override
        public void toBytes(ByteBuf b) {
            writeLong(b, done);
            writeLong(b, crit);
            writeLong(b, recipes);
        }
    }

    /**
     * A string of any length: writeUTF8String stops at 32767 characters, and with every advancement granted the list
     * of criteria is longer than that. Written as deflated UTF-8 bytes behind the lengths before and after packing;
     * a custom payload of 1.7.10 may hold up to 2 MB.
     */
    static void writeLong(ByteBuf b, String s) {
        byte[] raw = s.getBytes(Charset.forName("UTF-8"));
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + 64);
        byte[] chunk = new byte[4096];
        while (!d.finished()) {
            int n = d.deflate(chunk);
            out.write(chunk, 0, n);
        }
        d.end();
        byte[] packed = out.toByteArray();
        b.writeInt(raw.length);
        b.writeInt(packed.length);
        b.writeBytes(packed);
    }

    static String readLong(ByteBuf b) {
        int size = b.readInt();
        byte[] packed = new byte[b.readInt()];
        b.readBytes(packed);
        if (size < 0 || size > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("Bad length " + size);
        }
        byte[] raw = new byte[size];
        Inflater i = new Inflater();
        i.setInput(packed);
        try {
            int at = 0;
            while (at < size && !i.finished()) {
                int n = i.inflate(raw, at, size - at);
                if (n == 0 && (i.needsInput() || i.needsDictionary())) {
                    break;
                }
                at += n;
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException(e);
        } finally {
            i.end();
        }
        return new String(raw, Charset.forName("UTF-8"));
    }

    public static final class Toast implements IMessage {
        public String id = "";

        public Toast() {
        }

        Toast(String id) {
            this.id = id;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            id = ByteBufUtils.readUTF8String(b);
        }

        @Override
        public void toBytes(ByteBuf b) {
            ByteBufUtils.writeUTF8String(b, id);
        }
    }

}
