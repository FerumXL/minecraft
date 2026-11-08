package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * The igloo of 1.12.2 (ComponentScatteredFeaturePieces.Igloo) from its structure templates, converted to text files:
 * "x y z block meta kind facing". Placed on the grid of MapGenScatteredFeature, in Ice Plains and Cold Taiga.
 */
public final class IglooGenerator implements IWorldGenerator {
    private static final int SPACING = 32;
    private static final int SEPARATION = 8;
    private static final int SEED = 14357617;
    private static final String DIRECTIONS = "nesw";

    private static final class Entry {
        int x;
        int y;
        int z;
        Block block;
        String name;
        int meta;
        String kind;
        int facing;
    }

    private static Map<String, Block> blocks;

    private static Map<String, Block> blocks() {
        if (blocks == null) {
            Map<String, Block> map = new HashMap<String, Block>();
            map.put("air", Blocks.air);
            map.put("snow", Blocks.snow);
            map.put("ice", Blocks.ice);
            map.put("crafting_table", Blocks.crafting_table);
            map.put("web", Blocks.web);
            map.put("iron_bars", Blocks.iron_bars);
            map.put("furnace", Blocks.furnace);
            map.put("chest", Blocks.chest);
            map.put("ladder", Blocks.ladder);
            map.put("wall_sign", Blocks.wall_sign);
            map.put("carpet", Blocks.carpet);
            map.put("redstone_torch", Blocks.redstone_torch);
            map.put("torch", Blocks.torch);
            map.put("bed", Blocks.bed);
            map.put("trapdoor", Blocks.trapdoor);
            map.put("stonebrick", Blocks.stonebrick);
            map.put("stone", Blocks.stone);
            map.put("andesite", PortedBlocks.polishedAndesite);
            map.put("monster_egg", Blocks.monster_egg);
            map.put("brewing_stand", Blocks.brewing_stand);
            map.put("flower_pot", Blocks.flower_pot);
            map.put("cauldron", Blocks.cauldron);
            map.put("spruce_stairs", Blocks.spruce_stairs);
            map.put("wooden_slab", Blocks.wooden_slab);
            blocks = map;
        }
        return blocks;
    }

    private static final Map<String, List<Entry>> TEMPLATES = new HashMap<String, List<Entry>>();

    private static List<Entry> template(String name) {
        List<Entry> entries = TEMPLATES.get(name);
        if (entries != null) {
            return entries;
        }
        entries = new ArrayList<Entry>();
        InputStream stream = IglooGenerator.class.getResourceAsStream("/assets/mc112bridge/structures/" + name + ".txt");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                String[] parts = line.split(" ");
                Entry entry = new Entry();
                entry.x = Integer.parseInt(parts[0]);
                entry.y = Integer.parseInt(parts[1]);
                entry.z = Integer.parseInt(parts[2]);
                entry.name = parts[3];
                entry.block = blocks().get(parts[3]);
                entry.meta = Integer.parseInt(parts[4]);
                entry.kind = parts[5];
                entry.facing = DIRECTIONS.indexOf(parts[6].charAt(0));
                entries.add(entry);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read structure " + name, e);
        }
        TEMPLATES.put(name, entries);
        return entries;
    }

    private static int rotX(int x, int z, int rotation) {
        switch (rotation) {
            case 1:
                return -z;
            case 2:
                return -x;
            case 3:
                return z;
            default:
                return x;
        }
    }

    private static int rotZ(int x, int z, int rotation) {
        switch (rotation) {
            case 1:
                return x;
            case 2:
                return -z;
            case 3:
                return -x;
            default:
                return z;
        }
    }

    /** Metadata of the rotated facing (n, e, s, w), the values of the 1.12.2 getMetaFromState, unchanged since 1.7.10. */
    private static int facingMeta(String kind, int facing) {
        int[] table;
        if ("side".equals(kind)) {
            table = new int[] {2, 5, 3, 4};
        } else if ("torch".equals(kind)) {
            table = new int[] {4, 1, 3, 2};
        } else if ("bed".equals(kind)) {
            table = new int[] {2, 3, 0, 1};
        } else if ("trap".equals(kind)) {
            table = new int[] {0, 3, 1, 2};
        } else if ("stair".equals(kind)) {
            table = new int[] {3, 0, 2, 1};
        } else {
            return 0;
        }
        return table[facing];
    }

    private static void place(World world, String name, int originX, int originY, int originZ, int rotation) {
        for (Entry entry : template(name)) {
            int x = originX + rotX(entry.x, entry.z, rotation);
            int y = originY + entry.y;
            int z = originZ + rotZ(entry.x, entry.z, rotation);
            int meta = entry.meta;
            if (entry.facing >= 0) {
                meta += facingMeta(entry.kind, (entry.facing + rotation) % 4);
            }
            world.setBlock(x, y, z, entry.block, meta, 2);
            fillTileEntity(world, entry.name, x, y, z);
        }
    }

    private static void fillTileEntity(World world, String name, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if ("wall_sign".equals(name) && tile instanceof TileEntitySign) {
            ((TileEntitySign) tile).signText = new String[] {"", "<----", "---->", ""};
        } else if ("brewing_stand".equals(name) && tile instanceof TileEntityBrewingStand) {
            // A splash potion of weakness: the cure of the zombie villager needs it and the golden apple of the chest.
            ((TileEntityBrewingStand) tile).setInventorySlotContents(1, new ItemStack(Items.potionitem, 1, 16392));
        } else if ("flower_pot".equals(name) && tile instanceof TileEntityFlowerPot) {
            ((TileEntityFlowerPot) tile).func_145964_a(Item.getItemFromBlock(Blocks.cactus), 0);
        }
    }

    /** loot_tables/chests/igloo_chest.json */
    private static void fillChest(TileEntityChest chest, Random random) {
        Item[] items = {Items.apple, Items.coal, Items.gold_nugget, Items.stone_axe, Items.rotten_flesh, Items.emerald, Items.wheat};
        int[] weights = {15, 15, 10, 2, 10, 1, 10};
        int[] least = {1, 1, 1, 1, 1, 1, 2};
        int[] most = {3, 4, 3, 1, 1, 1, 3};
        int rolls = 2 + random.nextInt(7);
        for (int roll = 0; roll < rolls; roll++) {
            int pick = random.nextInt(63);
            int index = 0;
            while (pick >= weights[index]) {
                pick -= weights[index];
                index++;
            }
            int count = least[index] + random.nextInt(most[index] - least[index] + 1);
            putInFreeSlot(chest, random, new ItemStack(items[index], count));
        }
        putInFreeSlot(chest, random, new ItemStack(Items.golden_apple));
    }

    private static void putInFreeSlot(TileEntityChest chest, Random random, ItemStack stack) {
        int size = chest.getSizeInventory();
        int slot = random.nextInt(size);
        for (int i = 0; i < size; i++) {
            int candidate = (slot + i) % size;
            if (chest.getStackInSlot(candidate) == null) {
                chest.setInventorySlotContents(candidate, stack);
                return;
            }
        }
    }

    private static void spawn(World world, EntityLiving entity, int originX, int originY, int originZ, double x, double y, double z,
            int rotation) {
        double worldX;
        double worldZ;
        switch (rotation) {
            case 1:
                worldX = 1.0D - z;
                worldZ = x;
                break;
            case 2:
                worldX = 1.0D - x;
                worldZ = 1.0D - z;
                break;
            case 3:
                worldX = z;
                worldZ = 1.0D - x;
                break;
            default:
                worldX = x;
                worldZ = z;
                break;
        }
        entity.setLocationAndAngles((double) originX + worldX, (double) originY + y, (double) originZ + worldZ, 0.0F, 0.0F);
        entity.func_110163_bv();
        world.spawnEntityInWorld(entity);
    }

    /** MapGenScatteredFeature.canSpawnStructureAtCoords. */
    private static boolean isGridChunk(World world, int chunkX, int chunkZ) {
        int regionX = chunkX < 0 ? chunkX - (SPACING - 1) : chunkX;
        int regionZ = chunkZ < 0 ? chunkZ - (SPACING - 1) : chunkZ;
        regionX /= SPACING;
        regionZ /= SPACING;
        Random random = world.setRandomSeed(regionX, regionZ, SEED);
        return chunkX == regionX * SPACING + random.nextInt(SPACING - SEPARATION)
                && chunkZ == regionZ * SPACING + random.nextInt(SPACING - SEPARATION);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        long started = GenProbe.ENABLED ? System.nanoTime() : 0L;
        tryGenerate(random, chunkX, chunkZ, world);
        if (GenProbe.ENABLED) {
            GenProbe.igloo(System.nanoTime() - started);
        }
    }

    private void tryGenerate(Random random, int chunkX, int chunkZ, World world) {
        if (world.provider.dimensionId != 0 || !world.getWorldInfo().isMapFeaturesEnabled()
                || world.getWorldInfo().getTerrainType() == WorldType.FLAT || !isGridChunk(world, chunkX, chunkZ)) {
            return;
        }
        BiomeGenBase biome = world.getBiomeGenForCoords(chunkX * 16 + 8, chunkZ * 16 + 8);
        if (biome != BiomeGenBase.icePlains && biome != BiomeGenBase.coldTaiga) {
            return;
        }
        // The centre of the populated area: the turned igloo stays inside it and no new chunks are generated.
        int x = chunkX * 16 + 16;
        int z = chunkZ * 16 + 16;
        int rotation = random.nextInt(4);

        // StructureComponent.offsetToAverageGroundLevel(world, box, -1)
        int total = 0;
        int columns = 0;
        for (int dx = 0; dx < 7; dx++) {
            for (int dz = 0; dz < 8; dz++) {
                total += Math.max(world.getTopSolidOrLiquidBlock(x + rotX(dx, dz, rotation), z + rotZ(dx, dz, rotation)),
                        world.provider.getAverageGroundLevel());
                columns++;
            }
        }
        int y = total / columns - 1;

        place(world, "igloo_top", x, y, z, rotation);
        if (random.nextDouble() < 0.5D) {
            int depth = random.nextInt(8) + 4;
            int shaftX = rotX(3, 5, rotation) - rotX(1, 1, rotation);
            int shaftZ = rotZ(3, 5, rotation) - rotZ(1, 1, rotation);
            for (int level = 0; level < depth * 3; level++) {
                place(world, "igloo_middle", x + shaftX, y - 1 - level - 2, z + shaftZ, rotation);
            }
            int roomX = x + rotX(3, 5, rotation) - rotX(3, 7, rotation);
            int roomY = y - 1 - depth * 3 - 5;
            int roomZ = z + rotZ(3, 5, rotation) - rotZ(3, 7, rotation);
            place(world, "igloo_bottom", roomX, roomY, roomZ, rotation);
            TileEntity chest = world.getTileEntity(roomX + rotX(1, 6, rotation), roomY + 1, roomZ + rotZ(1, 6, rotation));
            if (chest instanceof TileEntityChest) {
                fillChest((TileEntityChest) chest, random);
            }
            EntityVillager villager = new EntityVillager(world, 2);
            spawn(world, villager, roomX, roomY, roomZ, 2.5D, 1.0D, 1.5D, rotation);
            EntityZombie zombie = new EntityZombie(world);
            zombie.setVillager(true);
            spawn(world, zombie, roomX, roomY, roomZ, 4.5D, 1.0D, 1.300000011920929D, rotation);
        } else {
            world.setBlock(x + rotX(3, 5, rotation), y, z + rotZ(3, 5, rotation), Blocks.snow, 0, 2);
        }
    }
}
