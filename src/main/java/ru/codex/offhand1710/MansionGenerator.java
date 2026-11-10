package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

/**
 * Woodland mansions: the grid of WoodlandMansion and the original templates, placed by the layout in MansionLayout. As with
 * the monument, a mansion is rebuilt from the seed and each populated chunk draws the part inside it.
 */
public final class MansionGenerator implements IWorldGenerator {
    private static final int SEED = 10387319;
    private static final int REACH = 9;
    private static final List<BiomeGenBase> FOREST = Arrays.asList(BiomeGenBase.roofedForest);
    private static final String DIRECTIONS = "nesw";

    private static final class Entry {
        int x;
        int y;
        int z;
        String name;
        Block block;
        int meta;
        String kind;
        String face;
    }

    private static final class Template {
        int sizeX;
        int sizeY;
        int sizeZ;
        final List<Entry> entries = new ArrayList<Entry>();
    }

    private static final Map<String, Template> TEMPLATES = new HashMap<String, Template>();
    private static Map<String, Block> blocks;

    private static Map<String, Block> blocks() {
        if (blocks == null) {
            Map<String, Block> m = new HashMap<String, Block>();
            Object[][] pairs = {{"air", Blocks.air}, {"cobblestone", Blocks.cobblestone}, {"glass", Blocks.glass}, {"bookshelf", Blocks.bookshelf},
                    {"obsidian", Blocks.obsidian}, {"lapis_block", Blocks.lapis_block}, {"diamond_block", Blocks.diamond_block},
                    {"melon_block", Blocks.melon_block}, {"tnt", Blocks.tnt}, {"web", Blocks.web}, {"waterlily", Blocks.waterlily},
                    {"red_mushroom", Blocks.red_mushroom}, {"brown_mushroom", Blocks.brown_mushroom}, {"mob_spawner", Blocks.mob_spawner},
                    {"cobblestone_wall", Blocks.cobblestone_wall}, {"fence", Blocks.fence}, {"glass_pane", Blocks.glass_pane},
                    {"iron_bars", Blocks.iron_bars}, {"redstone_wire", Blocks.redstone_wire}, {"double_stone_slab", Blocks.double_stone_slab},
                    {"birch_fence", PortedBlocks.FENCES[1]}, {"dark_oak_fence", PortedBlocks.FENCES[4]}, {"carpet", Blocks.carpet},
                    {"wool", Blocks.wool}, {"planks", Blocks.planks}, {"log2", Blocks.log2}, {"leaves2", Blocks.leaves2},
                    {"sapling", Blocks.sapling}, {"dirt", Blocks.dirt}, {"andesite", PortedBlocks.polishedAndesite},
                    {"stone_slab", Blocks.stone_slab}, {"wooden_slab", Blocks.wooden_slab}, {"farmland", Blocks.farmland},
                    {"oak_stairs", Blocks.oak_stairs}, {"birch_stairs", Blocks.birch_stairs}, {"dark_oak_stairs", Blocks.dark_oak_stairs},
                    {"stone_stairs", Blocks.stone_stairs}, {"chest", Blocks.chest}, {"trapped_chest", Blocks.trapped_chest},
                    {"ladder", Blocks.ladder}, {"pumpkin", Blocks.pumpkin}, {"anvil", Blocks.anvil},
                    {"dark_oak_fence_gate", PortedBlocks.FENCE_GATES[4]}, {"dark_oak_door", PortedBlocks.DOORS[4]}, {"iron_door", Blocks.iron_door},
                    {"lever", Blocks.lever}, {"torch", Blocks.torch}, {"rail", Blocks.rail}, {"vine", Blocks.vine},
                    {"melon_stem", Blocks.melon_stem}, {"pumpkin_stem", Blocks.pumpkin_stem}, {"wheat", Blocks.wheat}, {"water", Blocks.water},
                    {"lava", Blocks.lava}, {"flower_pot", Blocks.flower_pot}, {"cauldron", Blocks.cauldron}, {"monster_egg", Blocks.monster_egg}};
            for (Object[] pair : pairs) {
                m.put((String) pair[0], (Block) pair[1]);
            }
            blocks = m;
        }
        return blocks;
    }

    private static Template template(String name) {
        Template template = TEMPLATES.get(name);
        if (template != null) {
            return template;
        }
        template = new Template();
        InputStream stream = MansionGenerator.class.getResourceAsStream("/assets/mc112bridge/structures/mansion/" + name + ".txt");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String[] size = reader.readLine().split(" ");
            template.sizeX = Integer.parseInt(size[0]);
            template.sizeY = Integer.parseInt(size[1]);
            template.sizeZ = Integer.parseInt(size[2]);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(" ");
                if (p.length < 7) {
                    continue;
                }
                Entry e = new Entry();
                e.x = Integer.parseInt(p[0]);
                e.y = Integer.parseInt(p[1]);
                e.z = Integer.parseInt(p[2]);
                e.name = p[3];
                e.block = blocks().get(p[3]);
                e.meta = Integer.parseInt(p[4]);
                e.kind = p[5];
                e.face = p[6];
                template.entries.add(e);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read mansion piece " + name, e);
        }
        TEMPLATES.put(name, template);
        return template;
    }

    private static int[] transform(int x, int z, MansionLayout.ary mirror, MansionLayout.atm rotation) {
        int mx = mirror == MansionLayout.ary.c ? -x : x;
        int mz = mirror == MansionLayout.ary.b ? -z : z;
        switch (rotation) {
            case b:
                return new int[] {-mz, mx};
            case c:
                return new int[] {-mx, -mz};
            case d:
                return new int[] {mz, -mx};
            default:
                return new int[] {mx, mz};
        }
    }

    private static int direction(String face, MansionLayout.ary mirror, MansionLayout.atm rotation) {
        int index = DIRECTIONS.indexOf(face.charAt(0));
        if (mirror == MansionLayout.ary.b && index % 2 == 0) {
            index = (index + 2) % 4;
        } else if (mirror == MansionLayout.ary.c && index % 2 == 1) {
            index = (index + 2) % 4;
        }
        return (index + rotation.ordinal()) % 4;
    }

    private static int meta(Entry e, MansionLayout.ary mirror, MansionLayout.atm rotation) {
        String kind = e.kind;
        if ("none".equals(kind)) {
            return e.meta;
        }
        if ("axis".equals(kind)) {
            if ("y".equals(e.face)) {
                return e.meta;
            }
            boolean x = "x".equals(e.face) == (rotation.ordinal() % 2 == 0);
            return e.meta + (x ? 4 : 8);
        }
        if ("doorup".equals(kind)) {
            return mirror == MansionLayout.ary.a ? e.meta : e.meta ^ 1;
        }
        if ("rail".equals(kind)) {
            return railMeta(e.face, mirror, rotation);
        }
        if ("vine".equals(kind)) {
            int bits = e.meta;
            for (int i = 0; i < rotation.ordinal(); i++) {
                bits = (bits << 1 & 15) | (bits >> 3); // south -> west -> north -> east
            }
            return bits;
        }
        int dir = direction(e.face, mirror, rotation);
        int[] table;
        if ("stair".equals(kind)) {
            table = new int[] {3, 0, 2, 1};
        } else if ("side".equals(kind)) {
            table = new int[] {2, 5, 3, 4};
        } else if ("door".equals(kind)) {
            table = new int[] {3, 0, 1, 2};
        } else if ("gate".equals(kind) || "pumpkin".equals(kind)) {
            table = new int[] {2, 3, 0, 1};
        } else if ("anvil".equals(kind)) {
            table = new int[] {0, 1, 2, 3};
        } else if ("wall".equals(kind)) {
            table = new int[] {4, 1, 3, 2};
        } else {
            return e.meta;
        }
        return e.meta + table[dir];
    }

    private static int railMeta(String shape, MansionLayout.ary mirror, MansionLayout.atm rotation) {
        if ("north_south".equals(shape) || "east_west".equals(shape)) {
            boolean ns = "north_south".equals(shape);
            return ns == (rotation.ordinal() % 2 == 0) ? 0 : 1;
        }
        String[] curves = {"south_east", "south_west", "north_west", "north_east"};
        int index = Arrays.asList(curves).indexOf(shape);
        if (mirror == MansionLayout.ary.b) {
            index = 3 - index;
        } else if (mirror == MansionLayout.ary.c) {
            index = (5 - index) % 4;
        }
        return 6 + (index + rotation.ordinal()) % 4;
    }

    private static final class Placed {
        final MansionLayout.i piece;
        final int minX;
        final int minZ;
        final int maxX;
        final int maxZ;

        Placed(MansionLayout.i piece) {
            this.piece = piece;
            Template t = template(piece.name);
            int[] far = transform(t.sizeX - 1, t.sizeZ - 1, piece.mirror, piece.rotation);
            minX = Math.min(piece.pos.x, piece.pos.x + far[0]);
            maxX = Math.max(piece.pos.x, piece.pos.x + far[0]);
            minZ = Math.min(piece.pos.z, piece.pos.z + far[1]);
            maxZ = Math.max(piece.pos.z, piece.pos.z + far[1]);
        }
    }

    private final Map<Long, List<Placed>> cache = new LinkedHashMap<Long, List<Placed>>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Placed>> eldest) {
            return size() > 4;
        }
    };
    private ChunkProviderGenerate probe;
    private long probeSeed;

    static boolean isGrid(World world, int chunkX, int chunkZ) {
        int regionX = (chunkX < 0 ? chunkX - 79 : chunkX) / 80;
        int regionZ = (chunkZ < 0 ? chunkZ - 79 : chunkZ) / 80;
        Random random = world.setRandomSeed(regionX, regionZ, SEED);
        int x = regionX * 80 + (random.nextInt(60) + random.nextInt(60)) / 2;
        int z = regionZ * 80 + (random.nextInt(60) + random.nextInt(60)) / 2;
        return chunkX == x && chunkZ == z && world.getWorldChunkManager().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 32, FOREST);
    }

    private static int top(Block[] blocksIn, int x, int z) {
        for (int y = 255; y >= 0; y--) {
            Block block = blocksIn[(x * 16 + z) * 256 + y];
            if (block != null && block != Blocks.air) {
                return y;
            }
        }
        return 0;
    }

    private List<Placed> mansion(World world, int chunkX, int chunkZ) {
        long seed = world.getSeed();
        if (probe == null || probeSeed != seed) {
            probe = new ChunkProviderGenerate(world, seed, false);
            probeSeed = seed;
            cache.clear();
        }
        Long key = Long.valueOf((long) chunkX << 32 ^ (long) chunkZ & 0xFFFFFFFFL);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Random random = new Random(seed);
        long a = random.nextLong();
        long b = random.nextLong();
        random.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ seed);
        MansionLayout.atm rotation = MansionLayout.atm.values()[random.nextInt(4)];
        Block[] column = new Block[65536];
        probe.func_147424_a(chunkX, chunkZ, column);
        int dx = rotation == MansionLayout.atm.b || rotation == MansionLayout.atm.c ? -5 : 5;
        int dz = rotation == MansionLayout.atm.c || rotation == MansionLayout.atm.d ? -5 : 5;
        int y = Math.min(Math.min(top(column, 7, 7), top(column, 7, 7 + dz)), Math.min(top(column, 7 + dx, 7), top(column, 7 + dx, 7 + dz)));
        List<Placed> result = null;
        if (y >= 60) {
            List<MansionLayout.i> pieces = new ArrayList<MansionLayout.i>();
            MansionLayout.a(null, new MansionLayout.et(chunkX * 16 + 8, y + 1, chunkZ * 16 + 8), rotation, pieces, random);
            result = new ArrayList<Placed>();
            for (MansionLayout.i piece : pieces) {
                result.add(new Placed(piece));
            }
        }
        cache.put(key, result);
        return result;
    }

    private static void place(World world, Random random, Placed placed, int minX, int minZ) {
        MansionLayout.i piece = placed.piece;
        Template template = template(piece.name);
        for (Entry entry : template.entries) {
            int[] at = transform(entry.x, entry.z, piece.mirror, piece.rotation);
            int x = piece.pos.x + at[0];
            int y = piece.pos.y + entry.y;
            int z = piece.pos.z + at[1];
            if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                continue;
            }
            if (entry.name.charAt(0) == '#') {
                marker(world, random, entry.name.substring(1), x, y, z, piece);
                continue;
            }
            if (entry.block == null) {
                continue;
            }
            world.setBlock(x, y, z, entry.block, meta(entry, piece.mirror, piece.rotation), 2);
            if (entry.block == Blocks.mob_spawner) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileEntityMobSpawner) {
                    ((TileEntityMobSpawner) tile).func_145881_a().setEntityName("Spider");
                }
            } else if (entry.block == Blocks.flower_pot && entry.meta == 1) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileEntityFlowerPot) {
                    ((TileEntityFlowerPot) tile).func_145964_a(Item.getItemFromBlock(Blocks.cactus), 0);
                    world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                }
            }
        }
    }

    private static void marker(World world, Random random, String name, int x, int y, int z, MansionLayout.i piece) {
        if (name.startsWith("Chest")) {
            String face = "ChestNorth".equals(name) ? "n" : ("ChestEast".equals(name) ? "e" : ("ChestSouth".equals(name) ? "s" : "w"));
            int dir = direction(face, MansionLayout.ary.a, piece.rotation);
            world.setBlock(x, y, z, Blocks.chest, new int[] {2, 5, 3, 4}[dir], 2);
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityChest) {
                fillChest((TileEntityChest) tile, random);
            }
        } else if ("Mage".equals(name) || "Warrior".equals(name)) {
            world.setBlockToAir(x, y, z);
            EntityLiving mob = "Mage".equals(name) ? new Illagers.Evoker(world) : new Illagers.Vindicator(world);
            mob.setLocationAndAngles((double) x + 0.5D, (double) y, (double) z + 0.5D, 0.0F, 0.0F);
            mob.onSpawnWithEgg(null);
            mob.func_110163_bv();
            world.spawnEntityInWorld(mob);
        }
    }

    private static void put(TileEntityChest chest, Random random, ItemStack stack) {
        int size = chest.getSizeInventory();
        int slot = random.nextInt(size);
        for (int i = 0; i < size; i++) {
            if (chest.getStackInSlot((slot + i) % size) == null) {
                chest.setInventorySlotContents((slot + i) % size, stack);
                return;
            }
        }
    }

    /** loot_tables/chests/woodland_mansion.json, without the items 1.7.10 does not have. */
    private static void fillChest(TileEntityChest chest, Random random) {
        Item[] rare = {Items.lead, Items.golden_apple, Items.record_13, Items.record_cat, Items.name_tag, Items.chainmail_chestplate, Items.diamond_hoe, Items.diamond_chestplate};
        int[] rareWeight = {20, 15, 15, 15, 20, 10, 15, 5};
        int rolls = 1 + random.nextInt(3);
        for (int i = 0; i < rolls; i++) {
            int roll = random.nextInt(115);
            int k = 0;
            while (roll >= rareWeight[k]) {
                roll -= rareWeight[k++];
            }
            put(chest, random, new ItemStack(rare[k]));
        }
        Item[] common = {Items.iron_ingot, Items.gold_ingot, Items.bread, Items.wheat, Items.bucket, Items.redstone, Items.coal, Items.melon_seeds, Items.pumpkin_seeds, Items112.beetrootSeeds};
        int[] commonWeight = {10, 5, 20, 20, 10, 15, 15, 10, 10, 10};
        int[] commonMax = {4, 4, 1, 4, 1, 4, 4, 4, 4, 4};
        rolls = 1 + random.nextInt(4);
        for (int i = 0; i < rolls; i++) {
            int roll = random.nextInt(115);
            int k = 0;
            while (roll >= commonWeight[k]) {
                roll -= commonWeight[k++];
            }
            put(chest, random, new ItemStack(common[k], 1 + random.nextInt(commonMax[k])));
        }
        Item[] bones = {Items.bone, Items.gunpowder, Items.rotten_flesh, Items.string};
        for (int i = 0; i < 3; i++) {
            put(chest, random, new ItemStack(bones[random.nextInt(4)], 1 + random.nextInt(8)));
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        if (world.provider.dimensionId != 0 || !world.getWorldInfo().isMapFeaturesEnabled()
                || world.getWorldInfo().getTerrainType() == WorldType.FLAT) {
            return;
        }
        int minX = chunkX * 16 + 8;
        int minZ = chunkZ * 16 + 8;
        for (int sx = chunkX - REACH; sx <= chunkX + REACH; sx++) {
            for (int sz = chunkZ - REACH; sz <= chunkZ + REACH; sz++) {
                if (!isGrid(world, sx, sz)) {
                    continue;
                }
                List<Placed> mansion = mansion(world, sx, sz);
                if (mansion == null) {
                    continue;
                }
                int base = Integer.MAX_VALUE;
                for (Placed placed : mansion) {
                    base = Math.min(base, placed.piece.pos.y);
                    if (placed.maxX >= minX && placed.minX <= minX + 15 && placed.maxZ >= minZ && placed.minZ <= minZ + 15) {
                        place(world, random, placed, minX, minZ);
                    }
                }
                foundation(world, mansion, base, minX, minZ);
            }
        }
    }

    /** WoodlandMansion.Start.generateStructure: cobblestone from under the building down to the ground. */
    private static void foundation(World world, List<Placed> mansion, int base, int minX, int minZ) {
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                boolean under = false;
                for (Placed placed : mansion) {
                    if (placed.piece.pos.y == base && x >= placed.minX && x <= placed.maxX && z >= placed.minZ && z <= placed.maxZ) {
                        under = true;
                        break;
                    }
                }
                if (!under || world.isAirBlock(x, base, z)) {
                    continue;
                }
                for (int y = base - 1; y > 1; y--) {
                    if (!world.isAirBlock(x, y, z) && !world.getBlock(x, y, z).getMaterial().isLiquid()) {
                        break;
                    }
                    world.setBlock(x, y, z, Blocks.cobblestone, 0, 2);
                }
            }
        }
    }
}
