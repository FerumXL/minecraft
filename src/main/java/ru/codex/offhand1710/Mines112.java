package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenMesa;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.event.terraingen.InitMapGenEvent;

/**
 * Mesa mineshafts of 1.10 - 1.12.2 (MapGenMineshaft.Type.MESA): a mineshaft whose start is in a mesa biome is not
 * buried at a random depth but raised so that its top is half its height plus 5 above sea level (it shows in the
 * canyons), and it is built of dark oak planks and fences. -Dmc112.mesaMines17 leaves the mineshafts of 1.7.10.
 */
public final class Mines112 {
    public static final boolean OFF = System.getProperty("mc112.mesaMines17") != null;

    public Mines112() {
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        if (!OFF && event.type == InitMapGenEvent.EventType.MINESHAFT && event.newGen != null && event.newGen.getClass() == MapGenMineshaft.class) {
            event.newGen = new Gen();
        }
    }

    static boolean mesa(World world, int chunkX, int chunkZ) {
        BiomeGenBase b = world.getBiomeGenForCoords((chunkX << 4) + 8, (chunkZ << 4) + 8);
        return b instanceof BiomeGenMesa;
    }

    /** StructureMineshaftStart of 1.12.2 for MESA: maxY becomes seaLevel + ySize / 2 + 5 (63 in 1.7.10 worlds). */
    public static void raise(StructureStart start) {
        StructureBoundingBox box = start.getBoundingBox();
        int ySize = box.maxY - box.minY + 1;
        int delta = 63 + ySize / 2 + 5 - box.maxY;
        box.offset(0, delta, 0);
        for (Object o : start.getComponents()) {
            ((StructureComponent) o).getBoundingBox().offset(0, delta, 0);
        }
    }

    /** The planks and fences of 1.7.10 inside the pieces of a mesa mineshaft, within one chunk area, made dark oak. */
    public static int darken(World world, StructureStart start, StructureBoundingBox area) {
        Block fence = PortedBlocks.FENCES[4];
        int changed = 0;
        for (Object o : start.getComponents()) {
            StructureBoundingBox b = ((StructureComponent) o).getBoundingBox();
            if (!b.intersectsWith(area)) {
                continue;
            }
            int x0 = Math.max(b.minX, area.minX);
            int x1 = Math.min(b.maxX, area.maxX);
            int z0 = Math.max(b.minZ, area.minZ);
            int z1 = Math.min(b.maxZ, area.maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = Math.max(1, b.minY - 1); y <= Math.min(255, b.maxY); y++) {
                        Block block = world.getBlock(x, y, z);
                        if (block == Blocks.planks && world.getBlockMetadata(x, y, z) == 0) {
                            world.setBlock(x, y, z, Blocks.planks, 5, 2);
                            changed++;
                        } else if (block == Blocks.fence) {
                            world.setBlock(x, y, z, fence, 0, 2);
                            changed++;
                        }
                    }
                }
            }
        }
        return changed;
    }

    /** MapGenMineshaft with the MESA type of 1.12.2. */
    public static final class Gen extends MapGenMineshaft {
        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            StructureStart start = super.getStructureStart(chunkX, chunkZ);
            if (mesa(worldObj, chunkX, chunkZ)) {
                raise(start);
            }
            return start;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean generateStructuresInChunk(World world, java.util.Random rand, int chunkX, int chunkZ) {
            boolean made = super.generateStructuresInChunk(world, rand, chunkX, chunkZ);
            int k = (chunkX << 4) + 8;
            int l = (chunkZ << 4) + 8;
            StructureBoundingBox area = new StructureBoundingBox(k, l, k + 15, l + 15);
            for (StructureStart s : ((Map<Long, StructureStart>) structureMap).values()) {
                if (s.isSizeableStructure() && s.getBoundingBox().intersectsWith(area) && mesa(world, s.func_143019_e(), s.func_143018_f())) {
                    darken(world, s, area);
                }
            }
            return made;
        }
    }
}
