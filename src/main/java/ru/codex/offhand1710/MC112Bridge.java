package ru.codex.offhand1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Mod(modid = MC112Bridge.MOD_ID, name = "Minecraft 1.12.2 Content for 1.7.10", version = "0.2.1", acceptedMinecraftVersions = "[1.7.10]")
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

    @EventHandler
    public void initialize(FMLInitializationEvent event) {
        ironNugget = new Item()
                .setUnlocalizedName(MOD_ID + ".iron_nugget")
                .setTextureName(MOD_ID + ":iron_nugget")
                .setCreativeTab(CreativeTabs.tabMaterials);
        GameRegistry.registerItem(ironNugget, "iron_nugget");

        // Match the vanilla 1.12.2 conversion recipes without replacing vanilla items.
        GameRegistry.addShapelessRecipe(new net.minecraft.item.ItemStack(ironNugget, 9),
                Items.iron_ingot);
        GameRegistry.addShapedRecipe(new net.minecraft.item.ItemStack(Items.iron_ingot),
                "nnn", "nnn", "nnn", 'n', ironNugget);

        addIronScrapSmelting();
        registerConcreteBlocks();
        registerConcreteRecipes();
    }

    private void registerConcreteBlocks() {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String color = COLOR_NAMES[i];
            concrete[i] = new BlockConcrete(color, false);
            GameRegistry.registerBlock(concrete[i], "concrete_" + color);

            concretePowder[i] = new BlockConcretePowder(concrete[i],
                    MOD_ID + ".concrete_powder_" + color,
                    MOD_ID + ":concrete_powder_" + color);
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

    private void addIronScrapSmelting() {
        net.minecraft.item.ItemStack nugget = new net.minecraft.item.ItemStack(ironNugget);
        GameRegistry.addSmelting(Items.iron_sword, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_pickaxe, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_shovel, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_axe, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_hoe, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_helmet, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_chestplate, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_leggings, nugget, 0.1F);
        GameRegistry.addSmelting(Items.iron_boots, nugget, 0.1F);
    }
}

