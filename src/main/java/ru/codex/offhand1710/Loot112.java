package ru.codex.offhand1710;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

/** Fills the vanilla chest categories from the loot tables of 1.12.2 kept as data in the mod. */
public final class Loot112 {
    private static final Map<String, String> ALIAS = new HashMap<String, String>();

    static {
        ALIAS.put("minecraft:beetroot_seeds", "mc112bridge:beetroot_seeds");
        ALIAS.put("minecraft:beetroot", "mc112bridge:beetroot");
        ALIAS.put("minecraft:iron_nugget", "mc112bridge:iron_nugget");
        ALIAS.put("minecraft:mutton", "mc112bridge:mutton_raw");
        ALIAS.put("minecraft:rabbit", "mc112bridge:rabbit_raw");
        ALIAS.put("minecraft:prismarine_crystals", "mc112bridge:prismarine_crystals");
        ALIAS.put("minecraft:prismarine_shard", "mc112bridge:prismarine_shard");
    }

    private Loot112() {
    }

    /** One entry of a pool; picks its count and enchantments when placed. */
    private static final class Entry extends WeightedRandomChestContent {
        private final boolean randomEnchant;
        private final int levelsMin;
        private final int levelsMax;
        private final boolean book;
        /** The 1.12.2 table this entry was flattened from; with it the chest is filled by pools. */
        Table table;

        Entry(ItemStack stack, int min, int max, int weight, boolean randomEnchant, int levelsMin, int levelsMax) {
            super(stack, min, max, weight);
            this.randomEnchant = randomEnchant;
            this.levelsMin = levelsMin;
            this.levelsMax = levelsMax;
            this.book = stack.getItem() == Items.book || stack.getItem() == Items.enchanted_book;
        }

        @Override
        protected ItemStack[] generateChestContent(Random random, IInventory inventory) {
            int count = theMinimumChanceToGenerateItem + (theMaximumChanceToGenerateItem > theMinimumChanceToGenerateItem ? random.nextInt(theMaximumChanceToGenerateItem - theMinimumChanceToGenerateItem + 1) : 0);
            if (book && (randomEnchant || levelsMax > 0)) {
                return new ItemStack[] {Items.enchanted_book.func_92114_b(random).theItemId};
            }
            ItemStack stack = theItemId.copy();
            stack.stackSize = Math.max(1, Math.min(count, stack.getMaxStackSize()));
            if (levelsMax > 0) {
                EnchantmentHelper.addRandomEnchantment(random, stack, levelsMin + (levelsMax > levelsMin ? random.nextInt(levelsMax - levelsMin + 1) : 0));
            } else if (randomEnchant) {
                EnchantmentHelper.addRandomEnchantment(random, stack, 1 + random.nextInt(20));
            }
            return new ItemStack[] {stack};
        }
    }

    private static int low(JsonElement e) {
        return e.isJsonObject() ? e.getAsJsonObject().get("min").getAsInt() : e.getAsInt();
    }

    private static int high(JsonElement e) {
        return e.isJsonObject() ? e.getAsJsonObject().get("max").getAsInt() : e.getAsInt();
    }

    private static Item item(String name) {
        String real = ALIAS.containsKey(name) ? ALIAS.get(name) : name;
        return (Item) Item.itemRegistry.getObject(real);
    }

    private static void fill(String category, String table) {
        InputStream in = Loot112.class.getResourceAsStream("/assets/mc112bridge/loot/" + table + ".json");
        if (in == null) {
            throw new IllegalStateException("Missing loot table " + table);
        }
        JsonObject root = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
        ChestGenHooks info = ChestGenHooks.getInfo(category);
        for (WeightedRandomChestContent old : info.getItems(new Random(0L))) {
            info.removeItem(old.theItemId);
        }
        int rollsMin = 0;
        int rollsMax = 0;
        JsonArray pools = root.getAsJsonArray("pools");
        for (JsonElement p : pools) {
            JsonObject pool = p.getAsJsonObject();
            int lo = low(pool.get("rolls"));
            int hi = high(pool.get("rolls"));
            rollsMin += lo;
            rollsMax += hi;
            int total = 0;
            for (JsonElement e : pool.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                total += entry.has("weight") ? entry.get("weight").getAsInt() : 1;
            }
            // One flat list: an entry's weight is its share of the pool times the pool's average rolls.
            for (JsonElement e : pool.getAsJsonArray("entries")) {
                JsonObject entry = e.getAsJsonObject();
                if (!"item".equals(entry.get("type").getAsString())) {
                    continue;
                }
                Item item = item(entry.get("name").getAsString());
                if (item == null) {
                    continue;
                }
                int data = 0;
                int min = 1;
                int max = 1;
                boolean random = false;
                int levelsMin = 0;
                int levelsMax = 0;
                if (entry.has("functions")) {
                    for (JsonElement f : entry.getAsJsonArray("functions")) {
                        JsonObject fn = f.getAsJsonObject();
                        String name = fn.get("function").getAsString();
                        if ("minecraft:set_count".equals(name) || "set_count".equals(name)) {
                            min = low(fn.get("count"));
                            max = high(fn.get("count"));
                        } else if ("minecraft:set_data".equals(name) || "set_data".equals(name)) {
                            data = low(fn.get("data"));
                        } else if ("minecraft:enchant_randomly".equals(name) || "enchant_randomly".equals(name)) {
                            random = true;
                        } else if ("minecraft:enchant_with_levels".equals(name) || "enchant_with_levels".equals(name)) {
                            levelsMin = low(fn.get("levels"));
                            levelsMax = high(fn.get("levels"));
                        }
                    }
                }
                int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                int scaled = Math.max(1, Math.round(1000.0F * (float) weight / (float) total * ((float) (lo + hi) / 2.0F)));
                info.addItem(new Entry(new ItemStack(item, 1, data), Math.max(1, min), Math.max(1, max), scaled, random, levelsMin, levelsMax));
            }
        }
        info.setMin(rollsMin);
        info.setMax(rollsMax);
        Table model = parse(table, root);
        TABLES.put(category, model);
        for (WeightedRandomChestContent c : info.getItems(new Random(0L))) {
            if (c instanceof Entry) {
                ((Entry) c).table = model;
            }
        }
    }

    // ---- LootTable of 1.12.2 for chests ---------------------------------------------------------------------

    public static final boolean POOLS_OFF = System.getProperty("mc112.lootPools17") != null;
    static final Map<String, Table> TABLES = new HashMap<String, Table>();

    static final class Range {
        final int min;
        final int max;

        Range(JsonElement e) {
            min = low(e);
            max = high(e);
        }

        /** RandomValueRange.generateInt. */
        int roll(Random rand) {
            return min >= max ? min : rand.nextInt(max - min + 1) + min;
        }
    }

    static final class PoolEntry {
        Item item;
        int weight = 1;
        Range count;
        Range data;
        Range levels;
        boolean randomly;
    }

    static final class Pool {
        Range rolls;
        final java.util.List<PoolEntry> entries = new java.util.ArrayList<PoolEntry>();
    }

    public static final class Table {
        final String name;
        final java.util.List<Pool> pools = new java.util.ArrayList<Pool>();

        Table(String name) {
            this.name = name;
        }
    }

    public static Table table(String category) {
        return TABLES.get(category);
    }

    private static Table parse(String name, JsonObject root) {
        Table t = new Table(name);
        for (JsonElement p : root.getAsJsonArray("pools")) {
            JsonObject o = p.getAsJsonObject();
            Pool pool = new Pool();
            pool.rolls = new Range(o.get("rolls"));
            for (JsonElement e : o.getAsJsonArray("entries")) {
                JsonObject eo = e.getAsJsonObject();
                PoolEntry pe = new PoolEntry();
                pe.weight = eo.has("weight") ? eo.get("weight").getAsInt() : 1;
                // an item 1.7.10 cannot have keeps its share of the pool and rolls as empty
                if ("item".equals(eo.get("type").getAsString())) {
                    pe.item = item(eo.get("name").getAsString());
                }
                if (eo.has("functions")) {
                    for (JsonElement f : eo.getAsJsonArray("functions")) {
                        JsonObject fn = f.getAsJsonObject();
                        String n = fn.get("function").getAsString().replace("minecraft:", "");
                        if ("set_count".equals(n)) {
                            pe.count = new Range(fn.get("count"));
                        } else if ("set_data".equals(n)) {
                            pe.data = new Range(fn.get("data"));
                        } else if ("enchant_with_levels".equals(n)) {
                            pe.levels = new Range(fn.get("levels"));
                        } else if ("enchant_randomly".equals(n)) {
                            pe.randomly = true;
                        }
                    }
                }
                pool.entries.add(pe);
            }
            t.pools.add(pool);
        }
        return t;
    }

    /** LootTable.generateLootForPools: each pool rolls its count of times, one weighted entry per roll. */
    public static java.util.List<ItemStack> generate(Table table, Random rand) {
        java.util.List<ItemStack> out = new java.util.ArrayList<ItemStack>();
        for (Pool pool : table.pools) {
            int rolls = pool.rolls.roll(rand);
            int total = 0;
            for (PoolEntry e : pool.entries) {
                total += e.weight;
            }
            for (int r = 0; r < rolls && total > 0; r++) {
                int j = rand.nextInt(total);
                for (PoolEntry e : pool.entries) {
                    j -= e.weight;
                    if (j < 0) {
                        add(e, rand, out);
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** LootEntryItem.addLoot with its functions in order, split into full stacks. */
    private static void add(PoolEntry e, Random rand, java.util.List<ItemStack> out) {
        if (e.item == null) {
            return;
        }
        ItemStack stack = new ItemStack(e.item, 1, 0);
        if (e.count != null) {
            stack.stackSize = e.count.roll(rand);
        }
        if (e.data != null) {
            stack.setItemDamage(e.data.roll(rand));
        }
        if (e.levels != null) {
            stack = EnchantmentHelper.addRandomEnchantment(rand, stack, e.levels.roll(rand));
        }
        if (e.randomly) {
            stack = enchantRandomly(stack, rand);
        }
        int left = stack.stackSize;
        while (left > 0) {
            ItemStack part = stack.copy();
            part.stackSize = Math.min(stack.getMaxStackSize(), left);
            left -= part.stackSize;
            out.add(part);
        }
    }

    /** EnchantRandomly without a list: any enchantment the item takes (a book takes all), at a random level. */
    private static ItemStack enchantRandomly(ItemStack stack, Random rand) {
        boolean book = stack.getItem() == Items.book;
        java.util.List<net.minecraft.enchantment.Enchantment> fits = new java.util.ArrayList<net.minecraft.enchantment.Enchantment>();
        for (net.minecraft.enchantment.Enchantment en : net.minecraft.enchantment.Enchantment.enchantmentsList) {
            if (en != null && (book || en.canApply(stack))) {
                fits.add(en);
            }
        }
        if (fits.isEmpty()) {
            return stack;
        }
        net.minecraft.enchantment.Enchantment en = fits.get(rand.nextInt(fits.size()));
        int level = between(rand, en.getMinLevel(), en.getMaxLevel());
        if (book) {
            ItemStack b = new ItemStack(Items.enchanted_book, stack.stackSize);
            Items.enchanted_book.addEnchantment(b, new net.minecraft.enchantment.EnchantmentData(en, level));
            return b;
        }
        stack.addEnchantment(en, level);
        return stack;
    }

    /** LootTable.fillInventory: shuffled empty slots, stacks split at random until the slots run out. */
    public static void fillInventory(Table table, Random rand, IInventory inv) {
        java.util.List<ItemStack> stacks = generate(table, rand);
        java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (inv.getStackInSlot(i) == null) {
                slots.add(i);
            }
        }
        java.util.Collections.shuffle(slots, rand);
        shuffleItems(stacks, slots.size(), rand);
        for (ItemStack s : stacks) {
            if (slots.isEmpty()) {
                return;
            }
            inv.setInventorySlotContents(slots.remove(slots.size() - 1), s);
        }
    }

    private static int between(Random rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }

    private static void shuffleItems(java.util.List<ItemStack> stacks, int free, Random rand) {
        java.util.List<ItemStack> big = new java.util.ArrayList<ItemStack>();
        for (java.util.Iterator<ItemStack> it = stacks.iterator(); it.hasNext();) {
            ItemStack s = it.next();
            if (s.stackSize <= 0) {
                it.remove();
            } else if (s.stackSize > 1) {
                big.add(s);
                it.remove();
            }
        }
        free -= stacks.size();
        while (free > 0 && !big.isEmpty()) {
            ItemStack a = big.remove(between(rand, 0, big.size() - 1));
            ItemStack b = a.splitStack(between(rand, 1, a.stackSize / 2));
            if (a.stackSize > 1 && rand.nextBoolean()) {
                big.add(a);
            } else {
                stacks.add(a);
            }
            if (b.stackSize > 1 && rand.nextBoolean()) {
                big.add(b);
            } else {
                stacks.add(b);
            }
        }
        stacks.addAll(big);
        java.util.Collections.shuffle(stacks, rand);
    }

    /**
     * Called by the coremod at the start of WeightedRandomChestContent.generateChestContents and
     * generateDispenserContents: a list built from a 1.12.2 table fills the container by its pools.
     */
    public static boolean fillPools(Random rand, WeightedRandomChestContent[] items, IInventory inv) {
        if (POOLS_OFF || items == null || inv == null) {
            return false;
        }
        for (WeightedRandomChestContent c : items) {
            if (c instanceof Entry && ((Entry) c).table != null) {
                fillInventory(((Entry) c).table, rand, inv);
                return true;
            }
        }
        return false;
    }

    static void register() {
        fill(ChestGenHooks.DUNGEON_CHEST, "simple_dungeon");
        fill(ChestGenHooks.MINESHAFT_CORRIDOR, "abandoned_mineshaft");
        fill(ChestGenHooks.PYRAMID_DESERT_CHEST, "desert_pyramid");
        fill(ChestGenHooks.PYRAMID_JUNGLE_CHEST, "jungle_temple");
        fill(ChestGenHooks.PYRAMID_JUNGLE_DISPENSER, "jungle_temple_dispenser");
        fill(ChestGenHooks.STRONGHOLD_CORRIDOR, "stronghold_corridor");
        fill(ChestGenHooks.STRONGHOLD_CROSSING, "stronghold_crossing");
        fill(ChestGenHooks.STRONGHOLD_LIBRARY, "stronghold_library");
        fill(ChestGenHooks.VILLAGE_BLACKSMITH, "village_blacksmith");
        fill(ChestGenHooks.BONUS_CHEST, "spawn_bonus_chest");
        fill(NETHER_BRIDGE, "nether_bridge");
    }

    /** Own category: 1.7.10 fills fortress chests from a fixed array, the coremod points them here. */
    public static final String NETHER_BRIDGE = "mc112NetherBridge";

    /** Called by StructureNetherBridgePieces$Corridor/Corridor2 in place of their chest list. */
    public static WeightedRandomChestContent[] netherBridge() {
        return ChestGenHooks.getItems(NETHER_BRIDGE, new Random());
    }

    /** The bound of "2 + rand.nextInt(bound)" there: 3 gives the 2..4 rolls of nether_bridge.json. */
    public static int netherBridgeSpan() {
        ChestGenHooks info = ChestGenHooks.getInfo(NETHER_BRIDGE);
        return Math.max(1, info.getMax() - info.getMin() + 1);
    }
}
