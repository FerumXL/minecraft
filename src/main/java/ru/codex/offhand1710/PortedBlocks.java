package ru.codex.offhand1710;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Stones and simple blocks of 1.8-1.12.2: granite, diorite, andesite and their polished forms (generated in
 * the overworld), prismarine, sea lantern, prismarine shard and crystals, bone block, nether wart block, red
 * nether brick, iron trapdoor, and doors, fences and fence gates of spruce, birch, jungle, acacia and dark oak.
 * Properties, recipes and textures follow the 1.12.2 game files.
 */
public final class PortedBlocks {
    public static Block granite;
    public static Block polishedGranite;
    public static Block diorite;
    public static Block polishedDiorite;
    public static Block andesite;
    public static Block polishedAndesite;
    public static Block prismarine;
    public static Block prismarineBricks;
    public static Block darkPrismarine;
    public static Block seaLantern;
    public static Item prismarineShard;
    public static Item prismarineCrystals;
    public static Block boneBlock;
    public static Block netherWartBlock;
    public static Block redNetherBrick;
    public static Block ironTrapdoor;

    public static Block redSandstone;
    public static Block chiseledRedSandstone;
    public static Block smoothRedSandstone;
    public static Block redSandstoneStairs;
    public static BlockPortedSlab redSandstoneSlab;
    public static Block purpurBlock;
    public static Block purpurPillar;
    public static Block purpurStairs;
    public static BlockPortedSlab purpurSlab;
    public static Block endBricks;
    public static Item poppedChorusFruit;
    public static Block grassPath;
    public static Item muttonRaw;
    public static Item muttonCooked;

    private static final String[] WOODS = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
    private static final int[] PLANKS_META = {1, 2, 3, 4, 5};
    private static final String[] PLANKS_TEXTURE = {
            "planks_spruce", "planks_birch", "planks_jungle", "planks_acacia", "planks_big_oak"
    };
    /** 1.12.2 BlockPlanks.EnumType map colors. */
    private static final MapColor[] WOOD_COLORS = {
            MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor
    };

    public static final Block[] DOORS = new Block[WOODS.length];
    public static final Item[] DOOR_ITEMS = new Item[WOODS.length];
    public static final Block[] FENCES = new Block[WOODS.length];
    public static final Block[] FENCE_GATES = new Block[WOODS.length];

    private PortedBlocks() {
    }

    public static void register() {
        granite = block(stone("granite", "stone_granite", MapColor.dirtColor), "granite");
        polishedGranite = block(stone("polished_granite", "stone_granite_smooth", MapColor.dirtColor), "polished_granite");
        diorite = block(stone("diorite", "stone_diorite", MapColor.quartzColor), "diorite");
        polishedDiorite = block(stone("polished_diorite", "stone_diorite_smooth", MapColor.quartzColor), "polished_diorite");
        andesite = block(stone("andesite", "stone_andesite", MapColor.stoneColor), "andesite");
        polishedAndesite = block(stone("polished_andesite", "stone_andesite_smooth", MapColor.stoneColor), "polished_andesite");

        prismarine = block(stone("prismarine", "prismarine_rough", MapColor.cyanColor), "prismarine");
        prismarineBricks = block(stone("prismarine_bricks", "prismarine_bricks", MapColor.diamondColor), "prismarine_bricks");
        darkPrismarine = block(stone("dark_prismarine", "prismarine_dark", MapColor.diamondColor), "dark_prismarine");
        seaLantern = block(new BlockSeaLantern(), "sea_lantern");
        prismarineShard = item("prismarine_shard");
        prismarineCrystals = item("prismarine_crystals");

        boneBlock = block(new BlockBone(), "bone_block");
        netherWartBlock = block(new BlockSimple(Material.grass, MapColor.redColor, "nether_wart_block",
                "nether_wart_block", CreativeTabs.tabBlock, 1.0F, -1.0F, Block.soundTypeWood), "nether_wart_block");
        redNetherBrick = block(new BlockSimple(Material.rock, MapColor.netherrackColor, "red_nether_brick",
                "red_nether_brick", CreativeTabs.tabBlock, 2.0F, 10.0F, Block.soundTypePiston).tool("pickaxe"),
                "red_nether_brick");
        ironTrapdoor = block(new BlockIronTrapdoor(), "iron_trapdoor");

        for (int i = 0; i < WOODS.length; i++) {
            String wood = WOODS[i];
            BlockWoodDoor door = new BlockWoodDoor(wood, WOOD_COLORS[i]);
            GameRegistry.registerBlock(door, (Class<? extends ItemBlock>) null, "door_" + wood);
            DOORS[i] = door;
            DOOR_ITEMS[i] = new ItemWoodDoor(door, wood);
            GameRegistry.registerItem(DOOR_ITEMS[i], wood + "_door");
            door.setItem(DOOR_ITEMS[i]);

            FENCES[i] = block(new BlockWoodFence(wood, PLANKS_TEXTURE[i], WOOD_COLORS[i]), wood + "_fence");
            FENCE_GATES[i] = block(new BlockWoodFenceGate(wood, PLANKS_META[i], WOOD_COLORS[i]), wood + "_fence_gate");
        }

        registerSandstoneAndPurpur();

        GameRegistry.registerWorldGenerator(new StoneGenerator(), 0);
        addRecipes();
        addSandstoneAndPurpurRecipes();
    }

    /** Red sandstone (1.8), purpur and end stone bricks (1.9), grass path (1.9), mutton (1.8). */
    private static void registerSandstoneAndPurpur() {
        // 1.12.2 red sandstone: rock, hardness 0.8, adobe map color; side / top / bottom per variant.
        redSandstone = block(new BlockSidedTextures(Material.rock, MapColor.adobeColor, "red_sandstone",
                "red_sandstone_normal", "red_sandstone_top", "red_sandstone_bottom", CreativeTabs.tabBlock, 0.8F, -1.0F,
                Block.soundTypePiston).tool("pickaxe"), "red_sandstone");
        chiseledRedSandstone = block(new BlockSidedTextures(Material.rock, MapColor.adobeColor, "chiseled_red_sandstone",
                "red_sandstone_carved", "red_sandstone_top", "red_sandstone_top", CreativeTabs.tabBlock, 0.8F, -1.0F,
                Block.soundTypePiston).tool("pickaxe"), "chiseled_red_sandstone");
        smoothRedSandstone = block(new BlockSidedTextures(Material.rock, MapColor.adobeColor, "smooth_red_sandstone",
                "red_sandstone_smooth", "red_sandstone_top", "red_sandstone_top", CreativeTabs.tabBlock, 0.8F, -1.0F,
                Block.soundTypePiston).tool("pickaxe"), "smooth_red_sandstone");
        redSandstoneStairs = block(new BlockPortedStairs(redSandstone, "red_sandstone_stairs"), "red_sandstone_stairs");
        redSandstoneSlab = slab(MapColor.adobeColor, "red_sandstone_slab", "red_sandstone_normal", "red_sandstone_top",
                "red_sandstone_bottom");

        // 1.12.2 purpur: rock, hardness 1.5, resistance 10, magenta; slab hardness 2.
        purpurBlock = block(new BlockSimple(Material.rock, MapColor.magentaColor, "purpur_block", "purpur_block",
                CreativeTabs.tabBlock, 1.5F, 10.0F, Block.soundTypePiston).tool("pickaxe"), "purpur_block");
        purpurPillar = block(new BlockPortedPillar(Material.rock, MapColor.magentaColor, "purpur_pillar",
                "purpur_pillar", "purpur_pillar_top", 1.5F, 10.0F, Block.soundTypePiston), "purpur_pillar");
        purpurPillar.setHarvestLevel("pickaxe", 0);
        purpurStairs = block(new BlockPortedStairs(purpurBlock, "purpur_stairs"), "purpur_stairs");
        purpurSlab = slab(MapColor.magentaColor, "purpur_slab", "purpur_block", "purpur_block", "purpur_block");
        endBricks = block(new BlockSimple(Material.rock, MapColor.sandColor, "end_bricks", "end_bricks",
                CreativeTabs.tabBlock, 0.8F, -1.0F, Block.soundTypePiston).tool("pickaxe"), "end_bricks");
        poppedChorusFruit = item("chorus_fruit_popped");

        grassPath = block(new BlockGrassPath(), "grass_path");

        muttonRaw = new net.minecraft.item.ItemFood(2, 0.3F, true)
                .setUnlocalizedName(MC112Bridge.MOD_ID + ".mutton_raw")
                .setTextureName(MC112Bridge.MOD_ID + ":mutton_raw");
        GameRegistry.registerItem(muttonRaw, "mutton_raw");
        muttonCooked = new net.minecraft.item.ItemFood(6, 0.8F, true)
                .setUnlocalizedName(MC112Bridge.MOD_ID + ".mutton_cooked")
                .setTextureName(MC112Bridge.MOD_ID + ":mutton_cooked");
        GameRegistry.registerItem(muttonCooked, "mutton_cooked");
    }

    /** Single and double slab pair: hardness 2, resistance 10 (stone_slab2 and purpur_slab of 1.12.2). */
    private static BlockPortedSlab slab(MapColor color, String name, String side, String top, String bottom) {
        BlockPortedSlab single = new BlockPortedSlab(false, Material.rock, color, name, side, top, bottom, 2.0F, 10.0F,
                Block.soundTypePiston);
        BlockPortedSlab doubleSlab = new BlockPortedSlab(true, Material.rock, color, name, side, top, bottom, 2.0F,
                10.0F, Block.soundTypePiston);
        doubleSlab.setSingle(single);
        single.setHarvestLevel("pickaxe", 0);
        doubleSlab.setHarvestLevel("pickaxe", 0);
        GameRegistry.registerBlock(single, ItemPortedSlab.class, name, single, doubleSlab, Boolean.FALSE);
        GameRegistry.registerBlock(doubleSlab, ItemPortedSlab.class, "double_" + name, single, doubleSlab, Boolean.TRUE);
        return single;
    }

    private static void addSandstoneAndPurpurRecipes() {
        GameRegistry.addRecipe(new ItemStack(redSandstone), "##", "##", '#', new ItemStack(Blocks.sand, 1, 1));
        GameRegistry.addRecipe(new ItemStack(chiseledRedSandstone), "#", "#", '#', redSandstoneSlab);
        GameRegistry.addRecipe(new ItemStack(smoothRedSandstone, 4), "##", "##", '#', redSandstone);
        for (Block variant : new Block[] {redSandstone, chiseledRedSandstone, smoothRedSandstone}) {
            GameRegistry.addRecipe(new ItemStack(redSandstoneSlab, 6), "###", '#', variant);
            GameRegistry.addRecipe(new ItemStack(redSandstoneStairs, 4), "#  ", "## ", "###", '#', variant);
        }

        GameRegistry.addRecipe(new ItemStack(purpurBlock, 4), "FF", "FF", 'F', poppedChorusFruit);
        GameRegistry.addRecipe(new ItemStack(purpurPillar), "#", "#", '#', purpurSlab);
        GameRegistry.addRecipe(new ItemStack(purpurSlab, 6), "###", '#', purpurBlock);
        GameRegistry.addRecipe(new ItemStack(purpurStairs, 4), "#  ", "## ", "###", '#', purpurBlock);
        GameRegistry.addRecipe(new ItemStack(endBricks, 4), "##", "##", '#', Blocks.end_stone);

        GameRegistry.addSmelting(muttonRaw, new ItemStack(muttonCooked), 0.35F);
    }

    /** Stone and prismarine of 1.12.2: rock, hardness 1.5, resistance 10, stone sounds, needs a pickaxe. */
    private static Block stone(String name, String texture, MapColor color) {
        return new BlockSimple(Material.rock, color, name, texture, CreativeTabs.tabBlock, 1.5F, 10.0F,
                Block.soundTypePiston).tool("pickaxe");
    }

    private static Block block(Block block, String name) {
        GameRegistry.registerBlock(block, name);
        return block;
    }

    private static Item item(String name) {
        Item item = new Item()
                .setUnlocalizedName(MC112Bridge.MOD_ID + "." + name)
                .setTextureName(MC112Bridge.MOD_ID + ":" + name)
                .setCreativeTab(CreativeTabs.tabMaterials);
        GameRegistry.registerItem(item, name);
        return item;
    }

    /** Recipes of the 1.12.2 recipe files. */
    private static void addRecipes() {
        GameRegistry.addRecipe(new ItemStack(diorite, 2), "CQ", "QC", 'C', Blocks.cobblestone, 'Q', Items.quartz);
        GameRegistry.addShapelessRecipe(new ItemStack(granite), diorite, Items.quartz);
        GameRegistry.addShapelessRecipe(new ItemStack(andesite, 2), diorite, Blocks.cobblestone);
        GameRegistry.addRecipe(new ItemStack(polishedGranite, 4), "SS", "SS", 'S', granite);
        GameRegistry.addRecipe(new ItemStack(polishedDiorite, 4), "SS", "SS", 'S', diorite);
        GameRegistry.addRecipe(new ItemStack(polishedAndesite, 4), "SS", "SS", 'S', andesite);

        GameRegistry.addRecipe(new ItemStack(prismarine), "SS", "SS", 'S', prismarineShard);
        GameRegistry.addRecipe(new ItemStack(prismarineBricks), "SSS", "SSS", "SSS", 'S', prismarineShard);
        GameRegistry.addRecipe(new ItemStack(darkPrismarine), "SSS", "SIS", "SSS", 'S', prismarineShard,
                'I', new ItemStack(Items.dye, 1, 0));
        GameRegistry.addRecipe(new ItemStack(seaLantern), "SCS", "CCC", "SCS", 'S', prismarineShard,
                'C', prismarineCrystals);

        ItemStack boneMeal = new ItemStack(Items.dye, 1, 15);
        GameRegistry.addRecipe(new ItemStack(boneBlock), "XXX", "XXX", "XXX", 'X', boneMeal);
        GameRegistry.addShapelessRecipe(new ItemStack(Items.dye, 9, 15), boneBlock);
        GameRegistry.addRecipe(new ItemStack(netherWartBlock), "###", "###", "###", '#', Items.nether_wart);
        GameRegistry.addRecipe(new ItemStack(redNetherBrick), "NW", "WN", 'N', Items.netherbrick, 'W', Items.nether_wart);
        GameRegistry.addRecipe(new ItemStack(ironTrapdoor), "##", "##", '#', Items.iron_ingot);

        for (int i = 0; i < WOODS.length; i++) {
            ItemStack planks = new ItemStack(Blocks.planks, 1, PLANKS_META[i]);
            GameRegistry.addRecipe(new ItemStack(DOOR_ITEMS[i], 3), "##", "##", "##", '#', planks);
            GameRegistry.addRecipe(new ItemStack(FENCES[i], 3), "W#W", "W#W", '#', Items.stick, 'W', planks);
            GameRegistry.addRecipe(new ItemStack(FENCE_GATES[i]), "#W#", "#W#", '#', Items.stick, 'W', planks);
        }
    }
}
