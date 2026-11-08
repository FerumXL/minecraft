package ru.codex.offhand1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = MC112Bridge.MOD_ID, name = "Minecraft 1.12.2 Content for 1.7.10", version = "0.8.0", acceptedMinecraftVersions = "[1.7.10]")
public final class MC112Bridge {
    public static final String MOD_ID = "mc112bridge";
    public static final String[] COLOR_NAMES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final int[] LEGACY_DYE_DAMAGE = {
            15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0
    };

    public static Item ironNugget;
    public static Block[] concrete = new Block[16];
    public static Block[] concretePowder = new Block[16];
    public static Block[] glazedTerracotta = new Block[16];
    public static Block magmaBlock;
    public static final OffhandNetwork NETWORK = new OffhandNetwork();
    @SidedProxy(clientSide = "ru.codex.offhand1710.client.ClientProxy",
            serverSide = "ru.codex.offhand1710.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        NETWORK.register();
        OffhandEvents events = new OffhandEvents();
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(events);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new PortedEvents());
        proxy.preInitialize();
    }

    @EventHandler
    public void initialize(FMLInitializationEvent event) {
        ironNugget = new Item()
                .setUnlocalizedName(MOD_ID + ".iron_nugget")
                .setTextureName(MOD_ID + ":iron_nugget")
                .setCreativeTab(CreativeTabs.tabMaterials);
        GameRegistry.registerItem(ironNugget, "iron_nugget");
        OreDictionary.registerOre("nuggetIron", ironNugget);

        // Match the vanilla 1.12.2 conversion recipes without replacing vanilla items.
        GameRegistry.addShapelessRecipe(new net.minecraft.item.ItemStack(ironNugget, 9),
                Items.iron_ingot);
        GameRegistry.addShapedRecipe(new net.minecraft.item.ItemStack(Items.iron_ingot),
                "nnn", "nnn", "nnn", 'n', ironNugget);

        addNuggetSmelting();
        registerConcreteBlocks();
        registerConcreteRecipes();
        registerGlazedTerracotta();
        registerMagmaBlock();
        PortedBlocks.register();
    }

    private void registerConcreteBlocks() {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String color = COLOR_NAMES[i];
            concrete[i] = new BlockConcrete(color, i);
            GameRegistry.registerBlock(concrete[i], "concrete_" + color);

            concretePowder[i] = new BlockConcretePowder(concrete[i], color, i);
            GameRegistry.registerBlock(concretePowder[i], "concrete_powder_" + color);
        }
    }

    private void registerConcreteRecipes() {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            GameRegistry.addShapelessRecipe(new ItemStack(concretePowder[i], 8),
                    Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand,
                    Blocks.gravel, Blocks.gravel, Blocks.gravel, Blocks.gravel,
                    new ItemStack(Items.dye, 1, LEGACY_DYE_DAMAGE[i]));
        }
    }

    private void registerGlazedTerracotta() {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String color = COLOR_NAMES[i];
            glazedTerracotta[i] = new BlockGlazedTerracotta(color, i);
            GameRegistry.registerBlock(glazedTerracotta[i], "glazed_terracotta_" + color);
            GameRegistry.addSmelting(new ItemStack(Blocks.stained_hardened_clay, 1, i),
                    new ItemStack(glazedTerracotta[i]), 0.1F);
        }
    }

    private void registerMagmaBlock() {
        magmaBlock = new BlockMagma();
        GameRegistry.registerBlock(magmaBlock, "magma_block");
        GameRegistry.addShapedRecipe(new ItemStack(magmaBlock),
                "mm", "mm", 'm', Items.magma_cream);
    }

    /**
     * 1.12.2 furnace recipes: iron tools, iron and chainmail armor and iron horse armor give an iron nugget;
     * the golden ones give a gold nugget (0.1 experience each).
     */
    private void addNuggetSmelting() {
        ItemStack iron = new ItemStack(ironNugget);
        Item[] ironItems = {
                Items.iron_pickaxe, Items.iron_shovel, Items.iron_axe, Items.iron_hoe, Items.iron_sword,
                Items.iron_helmet, Items.iron_chestplate, Items.iron_leggings, Items.iron_boots,
                Items.iron_horse_armor,
                Items.chainmail_helmet, Items.chainmail_chestplate, Items.chainmail_leggings, Items.chainmail_boots
        };
        for (Item item : ironItems) {
            GameRegistry.addSmelting(item, iron, 0.1F);
        }
        ItemStack gold = new ItemStack(Items.gold_nugget);
        Item[] goldItems = {
                Items.golden_pickaxe, Items.golden_shovel, Items.golden_axe, Items.golden_hoe, Items.golden_sword,
                Items.golden_helmet, Items.golden_chestplate, Items.golden_leggings, Items.golden_boots,
                Items.golden_horse_armor
        };
        for (Item item : goldItems) {
            GameRegistry.addSmelting(item, gold, 0.1F);
        }
    }
}
