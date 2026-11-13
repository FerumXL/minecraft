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
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenDesert;
import net.minecraft.world.biome.BiomeGenSavanna;
import net.minecraft.world.biome.BiomeGenSwamp;
import net.minecraft.world.biome.BiomeGenTaiga;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.BiomeEvent;

/**
 * World generation that changed after 1.7.10 and is not a structure of its own: the look of villages per biome
 * (StructureVillagePieces.Village.getBiomeSpecificBlockState of 1.12.2), desert and swamp fossils, Nether magma.
 */
public final class WorldGen112 implements IWorldGenerator {
    private static final String[] FOSSILS = {"fossil_spine_01", "fossil_spine_02", "fossil_spine_03", "fossil_spine_04",
            "fossil_skull_01", "fossil_skull_02", "fossil_skull_03", "fossil_skull_04"};
    private static final Map<String, int[][]> POSITIONS = new HashMap<String, int[][]>();
    private static final Map<String, char[]> AXES = new HashMap<String, char[]>();
    private static final int SPRUCE = 0;
    private static final int ACACIA = 3;

    private WorldGenMinable magma;

    public static void register() {
        BiomeManager.addVillageBiome(BiomeGenBase.taiga, true);
        WorldGen112 generator = new WorldGen112();
        MinecraftForge.TERRAIN_GEN_BUS.register(generator);
        cpw.mods.fml.common.registry.GameRegistry.registerWorldGenerator(generator, 7);
    }

    private static boolean isTaiga(BiomeGenBase biome) {
        return biome instanceof BiomeGenTaiga;
    }

    private static boolean isSavanna(BiomeGenBase biome) {
        return biome instanceof BiomeGenSavanna;
    }

    @SubscribeEvent
    public void onVillageBlock(BiomeEvent.GetVillageBlockID event) {
        Block result = null;
        if (isTaiga(event.biome)) {
            if (event.original == Blocks.oak_stairs) {
                result = Blocks.spruce_stairs;
            } else if (event.original == Blocks.fence) {
                result = PortedBlocks.FENCES[SPRUCE];
            }
        } else if (isSavanna(event.biome)) {
            if (event.original == Blocks.log || event.original == Blocks.cobblestone) {
                result = Blocks.log2;
            } else if (event.original == Blocks.oak_stairs) {
                result = Blocks.acacia_stairs;
            } else if (event.original == Blocks.fence) {
                result = PortedBlocks.FENCES[ACACIA];
            }
        }
        if (result != null) {
            event.replacement = result;
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onVillageMeta(BiomeEvent.GetVillageBlockMeta event) {
        int meta = -1;
        if (isTaiga(event.biome)) {
            if (event.original == Blocks.log) {
                meta = event.type & 12 | 1;
            } else if (event.original == Blocks.planks) {
                meta = 1;
            }
        } else if (isSavanna(event.biome)) {
            if (event.original == Blocks.log) {
                meta = event.type & 12;
            } else if (event.original == Blocks.cobblestone) {
                meta = 0;
            } else if (event.original == Blocks.planks) {
                meta = 4;
            }
        }
        if (meta >= 0) {
            event.replacement = meta;
            event.setResult(Event.Result.DENY);
        }
    }

    private static void load(String name) {
        if (POSITIONS.containsKey(name)) {
            return;
        }
        InputStream stream = WorldGen112.class.getResourceAsStream("/assets/mc112bridge/structures/fossils/" + name + ".txt");
        List<int[]> blocks = new ArrayList<int[]>();
        StringBuilder axes = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String[] size = reader.readLine().split(" ");
            blocks.add(new int[] {Integer.parseInt(size[0]), Integer.parseInt(size[1]), Integer.parseInt(size[2])});
            axes.append('-');
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 5) {
                    blocks.add(new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
                    axes.append(parts[4].charAt(0));
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read fossil " + name, e);
        }
        POSITIONS.put(name, blocks.toArray(new int[blocks.size()][]));
        AXES.put(name, axes.toString().toCharArray());
    }

    private static int boneMeta(char axis, int rotation) {
        if (axis == 'x') {
            return rotation % 2 == 0 ? 4 : 8;
        }
        if (axis == 'z') {
            return rotation % 2 == 0 ? 8 : 4;
        }
        return 0;
    }

    private static void place(World world, Random random, String name, int baseX, int y, int baseZ, int rotation, float chance, Block block, boolean bone) {
        load(name);
        int[][] data = POSITIONS.get(name);
        char[] axes = AXES.get(name);
        int sizeX = data[0][0];
        int sizeZ = data[0][2];
        for (int i = 1; i < data.length; i++) {
            if (random.nextFloat() >= chance) {
                continue;
            }
            int x = data[i][0];
            int z = data[i][2];
            int worldX;
            int worldZ;
            switch (rotation) {
                case 1:
                    worldX = sizeZ - 1 - z;
                    worldZ = x;
                    break;
                case 2:
                    worldX = sizeX - 1 - x;
                    worldZ = sizeZ - 1 - z;
                    break;
                case 3:
                    worldX = z;
                    worldZ = sizeX - 1 - x;
                    break;
                default:
                    worldX = x;
                    worldZ = z;
                    break;
            }
            world.setBlock(baseX + worldX, y + data[i][1], baseZ + worldZ, block, bone ? boneMeta(axes[i], rotation) : 0, 2);
        }
    }

    /** WorldGenFossils of 1.12.2, placed in the populated area of the chunk. */
    private static void generateFossil(World world, Random random, int chunkX, int chunkZ) {
        String name = FOSSILS[random.nextInt(FOSSILS.length)];
        load(name);
        int[] size = POSITIONS.get(name)[0];
        int rotation = random.nextInt(4);
        int footX = rotation % 2 == 0 ? size[0] : size[2];
        int footZ = rotation % 2 == 0 ? size[2] : size[0];
        int baseX = chunkX * 16 + 8 + random.nextInt(16 - footX + 1);
        int baseZ = chunkZ * 16 + 8 + random.nextInt(16 - footZ + 1);
        int lowest = 256;
        for (int x = 0; x < footX; x++) {
            for (int z = 0; z < footZ; z++) {
                lowest = Math.min(lowest, world.getHeightValue(baseX + x, baseZ + z));
            }
        }
        int y = Math.max(lowest - 15 - random.nextInt(10), 10);
        place(world, random, name, baseX, y, baseZ, rotation, 0.9F, PortedBlocks.boneBlock, true);
        place(world, random, name + "_coal", baseX, y, baseZ, rotation, 0.1F, Blocks.coal_ore, false);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        if (world.provider.dimensionId == -1) {
            if (magma == null) {
                magma = new WorldGenMinable(MC112Bridge.magmaBlock, 0, 33, Blocks.netherrack);
            }
            for (int i = 0; i < 4; i++) {
                magma.generate(world, random, chunkX * 16 + random.nextInt(16), 26 + random.nextInt(10), chunkZ * 16 + random.nextInt(16));
            }
        } else if (world.provider.dimensionId == 0 && random.nextInt(64) == 0) {
            BiomeGenBase biome = world.getBiomeGenForCoords(chunkX * 16 + 8, chunkZ * 16 + 8);
            if (biome instanceof BiomeGenDesert || biome instanceof BiomeGenSwamp) {
                generateFossil(world, random, chunkX, chunkZ);
            }
        }
    }
}
