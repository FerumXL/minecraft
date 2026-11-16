package ru.codex.offhand1710;

import java.lang.reflect.Field;
import java.util.Random;

import org.apache.logging.log4j.LogManager;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenDesert;
import net.minecraft.world.biome.BiomeGenSavanna;
import net.minecraft.world.biome.BiomeGenTaiga;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

/**
 * Village behaviour of 1.12.2 that the bytecode hooks of asm.Transformer call: the village type (plains, desert,
 * savanna, taiga) kept by every piece and saved as "Type", zombie villages (1 in 50, saved as "Zombie") and the road of
 * StructureVillagePieces.Path. The fields mc112Type and mc112Zombie are added to StructureVillagePieces.Village by the
 * transformer.
 */
public final class Village112 {
    public static final int PLAINS = 0;
    public static final int DESERT = 1;
    public static final int SAVANNA = 2;
    public static final int TAIGA = 3;
    private static final int SEA_LEVEL = 63;

    private static final String[] DESERT_NAMES = {"field_143014_b", "isDesertVillage", "isDesert"};
    private static final boolean FORCE_ZOMBIE = "1".equals(System.getProperty("mc112.zombieVillages", System.getenv("MC112_ZOMBIE")));

    private static Field zombieField;
    private static Field typeField;
    private static Field desertField;
    private static boolean desertMissing;

    private Village112() {
    }

    public static int typeOf(BiomeGenBase biome) {
        if (biome instanceof BiomeGenDesert) {
            return DESERT;
        }
        if (biome instanceof BiomeGenSavanna) {
            return SAVANNA;
        }
        if (biome instanceof BiomeGenTaiga) {
            return TAIGA;
        }
        return PLAINS;
    }

    public static boolean isDesert(int type) {
        return type == DESERT;
    }

    /** The desert flag of the start piece, found by its SRG or MCP name (the dev environment uses MCP names). */
    public static void setDesert(StructureComponent start, int type) {
        try {
            if (desertField == null) {
                for (String name : DESERT_NAMES) {
                    try {
                        Field f = StructureVillagePieces.Village.class.getDeclaredField(name);
                        f.setAccessible(true);
                        desertField = f;
                        break;
                    } catch (NoSuchFieldException e) {
                        // next name
                    }
                }
                if (desertField == null) {
                    if (!desertMissing) {
                        desertMissing = true;
                        LogManager.getLogger("mc112bridge").warn("Village patch not applied: desert flag field");
                    }
                    return;
                }
            }
            desertField.setBoolean(start, isDesert(type));
        } catch (IllegalAccessException e) {
            // left as vanilla set it
        }
    }

    /** The extra roll of the 1.12 Start: it decides a zombie village and keeps the random sequence of 1.12. */
    public static boolean zombieRoll(Random random) {
        boolean zombie = random.nextInt(50) == 0;
        // test switch: -Dmc112.zombieVillages=1 or MC112_ZOMBIE=1 makes every village a zombie village (same layout)
        return zombie || FORCE_ZOMBIE;
    }

    /** The biome given to the village block events; a piece loaded from disk has no start piece, its type stands in. */
    public static BiomeGenBase eventBiome(BiomeGenBase biome, int type) {
        if (biome != null) {
            return biome;
        }
        switch (type) {
            case DESERT:
                return BiomeGenBase.desert;
            case SAVANNA:
                return BiomeGenBase.savanna;
            case TAIGA:
                return BiomeGenBase.taiga;
            default:
                return BiomeGenBase.plains;
        }
    }

    public static void write(NBTTagCompound tag, int type, boolean zombie) {
        tag.setByte("Type", (byte) type);
        tag.setBoolean("Zombie", zombie);
    }

    /** Older saves have no "Type": their desert flag still tells a desert village, others stay plains as before. */
    public static int readType(NBTTagCompound tag, boolean desert) {
        return desert ? DESERT : tag.getByte("Type");
    }

    public static boolean readZombie(NBTTagCompound tag) {
        return tag.getBoolean("Zombie");
    }

    /** The blacksmith of 1.12 builds with plain cobblestone in every village type. */
    public static boolean keepBlock(StructureComponent piece, Block block) {
        return block == Blocks.cobblestone && piece instanceof StructureVillagePieces.House2;
    }

    /** A zombie village of 1.12 has no torches. */
    public static boolean skipBlock(boolean zombie, Block block) {
        return zombie && block == Blocks.torch;
    }

    private static boolean zombie(StructureComponent piece) {
        if (!(piece instanceof StructureVillagePieces.Village)) {
            return false;
        }
        try {
            if (zombieField == null) {
                zombieField = StructureVillagePieces.Village.class.getField("mc112Zombie");
            }
            return zombieField.getBoolean(piece);
        } catch (Exception e) {
            return false;
        }
    }

    /** A zombie village of 1.12 has no doors. */
    public static boolean skipDoor(StructureComponent piece) {
        return zombie(piece);
    }

    private static int type(StructureComponent piece) {
        if (!(piece instanceof StructureVillagePieces.Village)) {
            return PLAINS;
        }
        try {
            if (typeField == null) {
                typeField = StructureVillagePieces.Village.class.getField("mc112Type");
            }
            return typeField.getInt(piece);
        } catch (Exception e) {
            return PLAINS;
        }
    }

    /**
     * The door of StructureVillagePieces.Village.biomeDoor of 1.12.2: acacia in savanna villages, spruce in taiga
     * villages, oak otherwise; any other structure keeps the oak door.
     */
    public static Block door(StructureComponent piece) {
        int type = type(piece);
        // PortedBlocks.DOORS follows PortedBlocks.WOODS: spruce, birch, jungle, acacia, dark oak
        Block door = type == SAVANNA ? PortedBlocks.DOORS[3] : type == TAIGA ? PortedBlocks.DOORS[0] : null;
        return door == null ? Blocks.wooden_door : door;
    }

    /**
     * Stands in for "block == Blocks.wooden_door" in the village, door AI and path finding code of 1.7.10: every
     * wooden door counts, as BlockDoor with Material.WOOD does in 1.12.2.
     */
    public static boolean isWoodDoor(Block block) {
        return block == Blocks.wooden_door || block instanceof BlockDoor && block.getMaterial() == Material.wood;
    }

    /** Villagers of a zombie village spawn as zombie villagers. */
    public static boolean spawn(World world, Entity villager, boolean zombie) {
        if (!zombie) {
            return world.spawnEntityInWorld(villager);
        }
        EntityZombie entity = new EntityZombie(world);
        entity.setLocationAndAngles(villager.posX, villager.posY, villager.posZ, 0.0F, 0.0F);
        entity.onSpawnWithEgg(null);
        entity.setVillager(true);
        entity.func_110163_bv();
        return world.spawnEntityInWorld(entity);
    }

    private static void set(World world, int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, 2);
    }

    /**
     * StructureVillagePieces.Path.addComponentParts of 1.12.2: from the top block down to the sea level, grass becomes a
     * grass path, water gets a bridge of planks, sand gets gravel on cobblestone; anything else is passed over.
     */
    public static boolean path(StructureComponent piece, int type, World world, StructureBoundingBox area) {
        StructureBoundingBox box = piece.getBoundingBox();
        for (int x = box.minX; x <= box.maxX; x++) {
            for (int z = box.minZ; z <= box.maxZ; z++) {
                if (!area.isVecInside(x, 64, z)) {
                    continue;
                }
                int y = world.getTopSolidOrLiquidBlock(x, z) - 1;
                if (y < SEA_LEVEL) {
                    y = SEA_LEVEL - 1;
                }
                for (; y >= SEA_LEVEL - 1; y--) {
                    Block block = world.getBlock(x, y, z);
                    if (block == Blocks.grass && world.isAirBlock(x, y + 1, z)) {
                        set(world, x, y, z, PortedBlocks.grassPath, 0);
                        break;
                    }
                    if (block.getMaterial().isLiquid()) {
                        switch (type) {
                            case DESERT:
                                set(world, x, y, z, Blocks.sandstone, 2);
                                break;
                            case SAVANNA:
                                set(world, x, y, z, Blocks.planks, 4);
                                break;
                            case TAIGA:
                                set(world, x, y, z, Blocks.planks, 1);
                                break;
                            default:
                                set(world, x, y, z, Blocks.planks, 0);
                                break;
                        }
                        break;
                    }
                    if (block == Blocks.sand || block == Blocks.sandstone || block == PortedBlocks.redSandstone) {
                        if (type == DESERT) {
                            set(world, x, y, z, Blocks.sandstone, 0);
                            set(world, x, y - 1, z, Blocks.sandstone, 0);
                        } else {
                            set(world, x, y, z, Blocks.gravel, 0);
                            if (type == SAVANNA) {
                                set(world, x, y - 1, z, Blocks.log2, 0);
                            } else {
                                set(world, x, y - 1, z, Blocks.cobblestone, 0);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }
}
