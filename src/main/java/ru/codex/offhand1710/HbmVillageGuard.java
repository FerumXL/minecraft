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
