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
final class Loot112 {
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
    }
}
