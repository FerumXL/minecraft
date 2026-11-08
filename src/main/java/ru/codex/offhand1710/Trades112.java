package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.village.Village;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/** Careers and trade levels of 1.8+. The table is data read from 1.12.2 (assets/mc112bridge/trades.txt). */
public final class Trades112 {
    private static final String[][] CAREERS = {
            {"farmer", "fisherman", "shepherd", "fletcher"},
            {"librarian", "cartographer"},
            {"cleric"},
            {"armor", "weapon", "tool"},
            {"butcher", "leather"}
    };
    /** profession, career, level, trades */
    private static final List<List<List<List<Trade>>>> TABLE = new ArrayList<List<List<List<Trade>>>>();

    private Trades112() {
    }

    private static final class Trade {
        String kind;
        ItemStack first;
        int low;
        int high;
        ItemStack second;
        int low2;
        int high2;
    }


    private static ItemStack stack(String name, String meta) {
        Object item = Item.itemRegistry.getObject(name);
        if (item == null) {
            throw new IllegalStateException("Unknown item in trades: " + name);
        }
        return new ItemStack((Item) item, 1, Integer.parseInt(meta));
    }

    public static void register() {
        InputStream in = Trades112.class.getResourceAsStream("/assets/mc112bridge/trades.txt");
        if (in == null) {
            throw new IllegalStateException("Missing trades.txt");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.trim().split(" ");
                if (p.length < 4) {
                    continue;
                }
                int profession = Integer.parseInt(p[0]);
                int career = Integer.parseInt(p[1]);
                int level = Integer.parseInt(p[2]);
                Trade trade = new Trade();
                trade.kind = p[3];
                if ("BUY".equals(p[3]) || "SELL".equals(p[3]) || "ENCH".equals(p[3])) {
                    trade.first = stack(p[4], p[5]);
                    trade.low = Integer.parseInt(p[6]);
                    trade.high = Integer.parseInt(p[7]);
                } else if ("PROC".equals(p[3])) {
                    trade.first = stack(p[4], p[5]);
                    trade.low = Integer.parseInt(p[6]);
                    trade.high = Integer.parseInt(p[7]);
                    trade.second = stack(p[8], p[9]);
                    trade.low2 = Integer.parseInt(p[10]);
                    trade.high2 = Integer.parseInt(p[11]);
                }
                while (TABLE.size() <= profession) {
                    TABLE.add(new ArrayList<List<List<Trade>>>());
                }
                List<List<List<Trade>>> careers = TABLE.get(profession);
                while (careers.size() <= career) {
                    careers.add(new ArrayList<List<Trade>>());
                }
                List<List<Trade>> levels = careers.get(career);
                while (levels.size() <= level) {
                    levels.add(new ArrayList<Trade>());
                }
                levels.get(level).add(trade);
            }
            reader.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static int roll(Random random, int low, int high) {
        return low >= high ? low : low + random.nextInt(high - low + 1);
    }

    private static void add(Trade t, MerchantRecipeList list, Random random) {
        ItemStack emerald = new ItemStack(Items.emerald);
        if ("BUY".equals(t.kind)) {
            ItemStack buy = t.first.copy();
            buy.stackSize = roll(random, t.low, t.high);
            list.add(new MerchantRecipe(buy, Items.emerald));
        } else if ("SELL".equals(t.kind)) {
            int price = roll(random, t.low, t.high);
            ItemStack sold = t.first.copy();
            if (price < 0) {
                sold.stackSize = -price;
                list.add(new MerchantRecipe(emerald, sold));
            } else {
                list.add(new MerchantRecipe(new ItemStack(Items.emerald, price), sold));
            }
        } else if ("ENCH".equals(t.kind)) {
            ItemStack item = t.first.copy();
            EnchantmentHelper.addRandomEnchantment(random, item, 5 + random.nextInt(15));
            list.add(new MerchantRecipe(new ItemStack(Items.emerald, roll(random, t.low, t.high)), item));
        } else if ("PROC".equals(t.kind)) {
            ItemStack in = t.first.copy();
            in.stackSize = roll(random, t.low, t.high);
            ItemStack out = t.second.copy();
            out.stackSize = roll(random, t.low2, t.high2);
            list.add(new MerchantRecipe(in, emerald, out));
        } else if ("BOOK".equals(t.kind)) {
            Enchantment e = Enchantment.enchantmentsBookList[random.nextInt(Enchantment.enchantmentsBookList.length)];
            int level = roll(random, e.getMinLevel(), e.getMaxLevel());
            int price = 2 + random.nextInt(5 + level * 10) + 3 * level;
            if (Content119.isTreasure(e)) {
                price *= 2;
            }
            list.add(new MerchantRecipe(new ItemStack(Items.book), new ItemStack(Items.emerald, Math.min(price, 64)),
                    Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(e, level))));
        }
        // MAP (explorer maps of the cartographer) is not available: 1.7.10 has no decorated maps.
    }

    private static MerchantRecipeList list(EntityVillager villager) {
        return ReflectionHelper.getPrivateValue(EntityVillager.class, villager, "buyingList", "field_70963_i");
    }

    /** EntityVillager.populateBuyingList of 1.12.2. */
    public static void advance(EntityVillager villager) {
        int profession = villager.getProfession();
        if (profession < 0 || profession >= TABLE.size()) {
            return;
        }
        List<List<List<Trade>>> careers = TABLE.get(profession);
        NBTTagCompound data = villager.getEntityData();
        int career = data.getInteger("Career");
        int level = data.getInteger("CareerLevel");
        if (career != 0 && level != 0) {
            level++;
        } else {
            career = villager.worldObj.rand.nextInt(careers.size()) + 1;
            level = 1;
        }
        data.setInteger("Career", career);
        data.setInteger("CareerLevel", level);
        MerchantRecipeList list = list(villager);
        if (list == null) {
            list = new MerchantRecipeList();
            ReflectionHelper.setPrivateValue(EntityVillager.class, villager, list, "buyingList", "field_70963_i");
        }
        List<List<Trade>> levels = careers.get(Math.min(career - 1, careers.size() - 1));
        if (level - 1 < levels.size()) {
            for (Trade trade : levels.get(level - 1)) {
                add(trade, list, villager.worldObj.rand);
            }
        }
    }

    public static String careerName(EntityVillager villager) {
        int profession = villager.getProfession();
        int career = villager.getEntityData().getInteger("Career");
        if (profession >= 0 && profession < CAREERS.length && career >= 1 && career <= CAREERS[profession].length) {
            return StatCollector.translateToLocal("entity.Villager." + CAREERS[profession][career - 1]);
        }
        return StatCollector.translateToLocal("entity.Villager.name");
    }

    public static final class Events {
        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent event) {
            if (event.world.isRemote || !(event.entity instanceof EntityVillager)) {
                return;
            }
            EntityVillager villager = (EntityVillager) event.entity;
            if (villager.getProfession() < 0 || villager.getProfession() >= TABLE.size() || villager.getEntityData().hasKey("Career")) {
                return;
            }
            if (list(villager) == null) {
                advance(villager);
            } else {
                // A villager from an older world keeps its trades and continues from level 1.
                villager.getEntityData().setInteger("Career", villager.worldObj.rand.nextInt(TABLE.get(villager.getProfession()).size()) + 1);
                villager.getEntityData().setInteger("CareerLevel", 1);
            }
        }

        /** Runs just before EntityVillager.updateAITasks would add a random vanilla trade. */
        @SubscribeEvent
        public void onUpdate(LivingUpdateEvent event) {
            if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityVillager)) {
                return;
            }
            EntityVillager villager = (EntityVillager) event.entityLiving;
            boolean pending = ReflectionHelper.<Boolean, EntityVillager>getPrivateValue(EntityVillager.class, villager, "needsInitilization", "field_70959_by");
            int timer = ReflectionHelper.<Integer, EntityVillager>getPrivateValue(EntityVillager.class, villager, "timeUntilReset", "field_70961_j");
            if (!pending || timer != 1 || villager.isTrading() || villager.getProfession() >= TABLE.size()) {
                return;
            }
            ReflectionHelper.setPrivateValue(EntityVillager.class, villager, Boolean.FALSE, "needsInitilization", "field_70959_by");
            MerchantRecipeList list = list(villager);
            if (list != null) {
                for (Object o : list) {
                    MerchantRecipe recipe = (MerchantRecipe) o;
                    if (recipe.isRecipeDisabled()) {
                        recipe.func_82783_a(villager.worldObj.rand.nextInt(6) + villager.worldObj.rand.nextInt(6) + 2);
                    }
                }
            }
            advance(villager);
            String buyer = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, "lastBuyingPlayer", "field_82189_bL");
            Village village = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, "villageObj", "field_70954_d");
            if (village != null && buyer != null) {
                villager.worldObj.setEntityState(villager, (byte) 14);
                village.setReputationForPlayer(buyer, 1);
            }
        }

        /** The trading screen is titled with the career. */
        @SubscribeEvent
        public void onInteract(EntityInteractEvent event) {
            if (event.entity.worldObj.isRemote || !(event.target instanceof EntityVillager)) {
                return;
            }
            EntityVillager villager = (EntityVillager) event.target;
            EntityPlayer player = event.entityPlayer;
            ItemStack held = player.inventory.getCurrentItem();
            if ((held != null && held.getItem() == Items.spawn_egg) || !villager.isEntityAlive() || villager.isTrading() || villager.isChild()) {
                return;
            }
            villager.setCustomer(player);
            player.displayGUIMerchant(villager, villager.hasCustomNameTag() ? villager.getCustomNameTag() : careerName(villager));
            event.setCanceled(true);
        }
    }
}
