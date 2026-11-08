package ru.codex.offhand1710;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import ru.codex.offhand1710.MonumentShim.Bbg;
import ru.codex.offhand1710.MonumentShim.Dir;

/**
 * Ocean monuments on the grid of StructureOceanMonument (1.12.2). The layout of a monument depends only on the world
 * seed and its chunk, so it is rebuilt when needed and each populated chunk draws the part of it that falls inside.
 */
public final class MonumentGenerator implements IWorldGenerator {
    private static final int SPACING = 32;
    private static final int SEPARATION = 5;
    private static final int SEED = 10387313;
    private static final int REACH = 4;
    private static final List<BiomeGenBase> ALLOWED = Arrays.asList(BiomeGenBase.ocean, BiomeGenBase.deepOcean, BiomeGenBase.river,
            BiomeGenBase.frozenOcean, BiomeGenBase.frozenRiver);
    private static final List<BiomeGenBase> CENTER = Arrays.asList(BiomeGenBase.deepOcean);

    private final Map<Long, MonumentPieces.h> cache = new LinkedHashMap<Long, MonumentPieces.h>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, MonumentPieces.h> eldest) {
            return size() > 8;
        }
    };
    private long cacheSeed;

    /** StructureOceanMonument.canSpawnStructureAtCoords. */
    public static boolean isMonumentChunk(World world, int chunkX, int chunkZ) {
        int regionX = (chunkX < 0 ? chunkX - (SPACING - 1) : chunkX) / SPACING;
        int regionZ = (chunkZ < 0 ? chunkZ - (SPACING - 1) : chunkZ) / SPACING;
        Random random = world.setRandomSeed(regionX, regionZ, SEED);
        int x = regionX * SPACING + (random.nextInt(SPACING - SEPARATION) + random.nextInt(SPACING - SEPARATION)) / 2;
        int z = regionZ * SPACING + (random.nextInt(SPACING - SEPARATION) + random.nextInt(SPACING - SEPARATION)) / 2;
        if (chunkX != x || chunkZ != z) {
            return false;
        }
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        return world.getWorldChunkManager().areBiomesViable(blockX, blockZ, 16, CENTER)
                && world.getWorldChunkManager().areBiomesViable(blockX, blockZ, 29, ALLOWED);
    }

    /** True inside the building of a monument: guardians spawn there (the "Monument" spawn list). */
    public static boolean isInsideMonument(World world, int x, int y, int z) {
        if (y < 39 || y > 61 || !enabled(world)) {
            return false;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        for (int sx = chunkX - REACH; sx <= chunkX + REACH; sx++) {
            for (int sz = chunkZ - REACH; sz <= chunkZ + REACH; sz++) {
                int minX = sx * 16 + 8 - 29;
                int minZ = sz * 16 + 8 - 29;
                if (x >= minX && x < minX + 58 && z >= minZ && z < minZ + 58 && isMonumentChunk(world, sx, sz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean enabled(World world) {
        return world.provider.dimensionId == 0 && world.getWorldInfo().isMapFeaturesEnabled()
                && world.getWorldInfo().getTerrainType() != WorldType.FLAT;
    }

    private MonumentPieces.h building(World world, int chunkX, int chunkZ) {
        long worldSeed = world.getSeed();
        if (worldSeed != cacheSeed) {
            cache.clear();
            cacheSeed = worldSeed;
        }
        Long key = Long.valueOf((long) chunkX << 32 ^ (long) chunkZ & 0xFFFFFFFFL);
        MonumentPieces.h piece = cache.get(key);
        if (piece == null) {
            // StructureOceanMonument.Start.create
            Random random = new Random(worldSeed);
            long a = random.nextLong();
            long b = random.nextLong();
            random.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
            int x = chunkX * 16 + 8 - 29;
            int z = chunkZ * 16 + 8 - 29;
            piece = new MonumentPieces.h(random, x, z, Dir.randomHorizontal(random));
            cache.put(key, piece);
        }
        return piece;
    }

    /** The spawn list of a monument. 1.12.2 uses the monster list; 1.7.10 can only place water mobs through the water one. */
    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onPotentialSpawns(net.minecraftforge.event.world.WorldEvent.PotentialSpawns event) {
        if (event.type == net.minecraft.entity.EnumCreatureType.waterCreature && event.list != null
                && isInsideMonument(event.world, event.x, event.y, event.z)) {
            event.list.clear();
            event.list.add(new BiomeGenBase.SpawnListEntry(EntityGuardian.class, 1, 2, 4));
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        if (!enabled(world)) {
            return;
        }
        int minX = chunkX * 16 + 8;
        int minZ = chunkZ * 16 + 8;
        Bbg box = new Bbg(minX, 1, minZ, minX + 15, 255, minZ + 15);
        for (int sx = chunkX - REACH; sx <= chunkX + REACH; sx++) {
            for (int sz = chunkZ - REACH; sz <= chunkZ + REACH; sz++) {
                if (isMonumentChunk(world, sx, sz)) {
                    MonumentPieces.h piece = building(world, sx, sz);
                    piece.a(world, new Random(random.nextLong()), box);
                }
            }
        }
    }
}
