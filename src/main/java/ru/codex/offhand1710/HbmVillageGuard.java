package ru.codex.offhand1710;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenVillage;

/**
 * HBM places its NBT structures and scattered features in PopulateChunkEvent.Pre, before the village
 * is built. Village pieces then measure the ground on top of HBM blocks and float 4-7 blocks up.
 * Called from HBM's canSpawnStructureAtCoords: refuses an HBM start near a possible village start.
 */
public final class HbmVillageGuard {
    /** Chunks around a village start where HBM may not start a structure (village pieces reach ~6 chunks). */
    private static final int RADIUS = 7;
    private static Field worldField;

    private HbmVillageGuard() {}

    public static boolean allow(boolean ok, MapGenBase gen, int chunkX, int chunkZ) {
        if (!ok || "1".equals(System.getProperty("mc112.hbmInVillages"))) {
            return ok;
        }
        try {
            World world = world(gen);
            if (world == null || world.provider == null || world.provider.dimensionId != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
                return ok;
            }
            return !villageNear(world, chunkX, chunkZ);
        } catch (Throwable t) {
            return ok;
        }
    }

    /** Village pieces stay within 112 blocks of the start (MapGenVillage.Start of 1.12.2), so this is the village area. */
    private static final int VILLAGE_REACH = 112;

    /** From HBM's getStructureStart: a start whose box reaches into the area of a possible village loses its pieces. */
    public static net.minecraft.world.gen.structure.StructureStart filterStart(net.minecraft.world.gen.structure.StructureStart start, MapGenBase gen, int chunkX, int chunkZ) {
        if (start == null || "1".equals(System.getProperty("mc112.hbmInVillages"))) {
            return start;
        }
        try {
            World world = world(gen);
            if (world == null || world.provider == null || world.provider.dimensionId != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
                return start;
            }
            net.minecraft.world.gen.structure.StructureBoundingBox b0 = start.getBoundingBox();
            // a jigsaw start (NBTStructure$Start) holds its first piece only and grows later up to the rangeLimit of
            // its SpawnCondition, which GenStructure keeps in nextSpawn while it makes the start
            int reach = jigsawReach(gen);
            // a start without a box yet: its chunk and the largest reach
            net.minecraft.world.gen.structure.StructureBoundingBox b = b0 == null
                ? new net.minecraft.world.gen.structure.StructureBoundingBox(chunkX * 16 - 128, 0, chunkZ * 16 - 128, chunkX * 16 + 143, 255, chunkZ * 16 + 143)
                : new net.minecraft.world.gen.structure.StructureBoundingBox(b0.minX - reach, b0.minY, b0.minZ - reach, b0.maxX + reach, b0.maxY, b0.maxZ + reach);
            if (DEBUG) {
                org.apache.logging.log4j.LogManager.getLogger("mc112bridge").info("HBM start " + start.getClass().getName() + " chunk " + chunkX + "," + chunkZ + " hasBox=" + (b0 != null)
                    + " box " + b.minX + "," + b.minZ + ".." + b.maxX + "," + b.maxZ + " reach=" + reach + " pieces=" + start.getComponents().size() + " village=" + villageArea(world, b.minX, b.minZ, b.maxX, b.maxZ));
            }
            if (villageArea(world, b.minX, b.minZ, b.maxX, b.maxZ)) {
                start.getComponents().clear();
                blocked++;
            }
        } catch (Throwable ignored) {
        }
        return start;
    }

    /** Dungeons not built because they would reach into a village area (for tests). */
    public static int dungeonsBlocked;

    /**
     * From HbmWorldGen.generateSurface instead of dungeon.generate(world, rand, x, y, z): these buildings start at
     * (x, z) and reach up to about 80 blocks in +x and +z (DesertAtom001), so the square from 16 before to 96 past the
     * corner is checked against the village areas.
     */
    public static boolean dungeon(net.minecraft.world.gen.feature.WorldGenerator gen, World world, Random rand, int x, int y, int z) {
        if (!"1".equals(System.getProperty("mc112.hbmInVillages")) && world.provider != null && world.provider.dimensionId == 0
            && world.getWorldInfo().isMapFeaturesEnabled() && villageArea(world, x - 16, z - 16, x + 96, z + 96)) {
            dungeonsBlocked++;
            if (DEBUG) {
                org.apache.logging.log4j.LogManager.getLogger("mc112bridge").info("HBM dungeon " + gen.getClass().getName() + " at " + x + "," + y + "," + z + " not built: village area");
            }
            return false;
        }
        return gen.generate(world, rand, x, y, z);
    }

    static int jigsawReach(MapGenBase gen) {
        try {
            Field next = gen.getClass().getDeclaredField("nextSpawn");
            next.setAccessible(true);
            Object spawn = next.get(gen);
            if (spawn == null || spawn.getClass().getField("pools").get(spawn) == null) {
                return 0;
            }
            int range = spawn.getClass().getField("rangeLimit").getInt(spawn);
            return Math.max(0, Math.min(256, range));
        } catch (Throwable t) {
            return 0;
        }
    }

    private static final boolean DEBUG ="1".equals(System.getProperty("mc112.hbmDebug"));

    /** Starts emptied by filterStart (for tests). */
    public static int blocked;

    static boolean villageArea(World world, int x0, int z0, int x1, int z1) {
        int spacing = 32, separation = 8;
        int c0 = Math.floorDiv((x0 - VILLAGE_REACH) >> 4, spacing) - 1, c1 = Math.floorDiv((x1 + VILLAGE_REACH) >> 4, spacing) + 1;
        int d0 = Math.floorDiv((z0 - VILLAGE_REACH) >> 4, spacing) - 1, d1 = Math.floorDiv((z1 + VILLAGE_REACH) >> 4, spacing) + 1;
        for (int cx = c0; cx <= c1; cx++) {
            for (int cz = d0; cz <= d1; cz++) {
                Random r = world.setRandomSeed(cx, cz, 10387312);
                int vx = (cx * spacing + r.nextInt(spacing - separation)) * 16 + 8;
                int vz = (cz * spacing + r.nextInt(spacing - separation)) * 16 + 8;
                if (x1 >= vx - VILLAGE_REACH && x0 <= vx + VILLAGE_REACH && z1 >= vz - VILLAGE_REACH && z0 <= vz + VILLAGE_REACH
                    && world.getWorldChunkManager().areBiomesViable(vx, vz, 0, MapGenVillage.villageSpawnBiomes)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static World world(MapGenBase gen) throws Exception {
        if (worldField == null) {
            for (String n : new String[] {"worldObj", "field_75039_c"}) {
                try {
                    Field f = MapGenBase.class.getDeclaredField(n);
                    f.setAccessible(true);
                    worldField = f;
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return worldField == null ? null : (World) worldField.get(gen);
    }

    /** Same placement as MapGenVillage.canSpawnStructureAtCoords (spacing 32, separation 8, salt 10387312). */
    static boolean villageNear(World world, int chunkX, int chunkZ) {
        int spacing = 32, separation = 8;
        int minCell = Math.floorDiv(chunkX - RADIUS, spacing), maxCell = Math.floorDiv(chunkX + RADIUS, spacing);
        int minCellZ = Math.floorDiv(chunkZ - RADIUS, spacing), maxCellZ = Math.floorDiv(chunkZ + RADIUS, spacing);
        for (int cx = minCell; cx <= maxCell; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                Random r = world.setRandomSeed(cx, cz, 10387312);
                int vx = cx * spacing + r.nextInt(spacing - separation);
                int vz = cz * spacing + r.nextInt(spacing - separation);
                if (Math.abs(vx - chunkX) <= RADIUS && Math.abs(vz - chunkZ) <= RADIUS
                    && world.getWorldChunkManager().areBiomesViable(vx * 16 + 8, vz * 16 + 8, 0, MapGenVillage.villageSpawnBiomes)) {
                    return true;
                }
            }
        }
        return false;
    }
}
