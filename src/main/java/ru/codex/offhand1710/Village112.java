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

    /** The blacksmith and the church of 1.12 build with plain cobblestone in every village type. */
    public static boolean keepBlock(StructureComponent piece, Block block) {
        return block == Blocks.cobblestone && (piece instanceof StructureVillagePieces.House2 || piece instanceof StructureVillagePieces.Church);
    }

    /** StructureVillagePieces.Field.getRandomCropType of 1.12.2. */
    public static Block cropType(Random random) {
        switch (random.nextInt(10)) {
            case 0:
            case 1:
                return Blocks.carrots;
            case 2:
            case 3:
                return Blocks.potatoes;
            case 4:
                return Items112.beetroots;
            default:
                return Blocks.wheat;
        }
    }

    /**
     * The age of a planted crop: 1.12 draws MathHelper.getInt(random, max / 3, max), for wheat, carrots and potatoes the
     * 2..7 of 1.7.10; beetroots (max 3) get 1..3. nextInt(6) % 3 is the nextInt(3) of the same random state.
     */
    public static int cropAge(Block block, int age) {
        return block == Items112.beetroots && age >= 2 ? (age - 2) % 3 + 1 : age;
    }

    /** StructureComponent.getMetadataWithOffset: a village door of coordBaseMode 2 turns as in 1.12; -1 = vanilla. */
    public static int doorMeta(StructureComponent piece, int mode, Block block, int meta) {
        return mode == 2 && piece instanceof StructureVillagePieces.Village && isWoodDoor(block) ? (meta + 2) & 3 : -1;
    }

    /** placeDoorAtCurrentPosition: the door, then (village pieces) the upper half hinge of 1.12: right for coordBaseMode 0 and 1. */
    public static void placeDoor(World world, int x, int y, int z, int meta, Block door, StructureComponent piece, int mode) {
        net.minecraft.item.ItemDoor.placeDoorBlock(world, x, y, z, meta, door);
        if (piece instanceof StructureVillagePieces.Village && world.getBlock(x, y + 1, z) == door) {
            world.setBlockMetadataWithNotify(x, y + 1, z, 8 | (mode == 0 || mode == 1 ? 1 : 0), 2);
        }
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

    /**
     * EntityAIMoveIndoors.shouldExecute of 1.12.2: no going indoors by day unless it rains in a biome without rain, never
     * without a sky. 1.7.10 asked BiomeGenBase.canSpawnLightningBolt (Biome.canRain of 1.12) the other way round, so
     * villagers of savanna, desert and snowy villages ran indoors at speed 1.0 all day long.
     */
    public static boolean stayOutside(net.minecraft.entity.EntityCreature e) {
        World w = e.worldObj;
        if (w.provider.hasNoSky) {
            return true;
        }
        if (!w.isDaytime()) {
            return false;
        }
        int x = net.minecraft.util.MathHelper.floor_double(e.posX);
        int z = net.minecraft.util.MathHelper.floor_double(e.posZ);
        return !(w.isRaining() && !w.getBiomeGenForCoords(x, z).canSpawnLightningBolt());
    }

    private static Field modeField;

    /** StructureComponent.coordBaseMode (protected). */
    private static int mode(StructureComponent piece) {
        try {
            if (modeField == null) {
                modeField = cpw.mods.fml.relauncher.ReflectionHelper.findField(StructureComponent.class, "coordBaseMode", "field_74885_f");
            }
            return modeField.getInt(piece);
        } catch (Exception e) {
            return -1;
        }
    }

    /** World position of a piece-relative block (StructureComponent.getX/Y/ZWithOffset), or null when the mode is unknown. */
    private static int[] at(StructureComponent piece, int x, int y, int z) {
        StructureBoundingBox b = piece.getBoundingBox();
        switch (mode(piece)) {
            case 0:
                return new int[] {b.minX + x, b.minY + y, b.minZ + z};
            case 1:
                return new int[] {b.maxX - z, b.minY + y, b.minZ + x};
            case 2:
                return new int[] {b.minX + x, b.minY + y, b.maxZ - z};
            case 3:
                return new int[] {b.minX + z, b.minY + y, b.minZ + x};
            default:
                return null;
        }
    }

    private static boolean full(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).isOpaqueCube();
    }

    /** Metadata of a horizontal facing: north 2, south 3, west 4, east 5; offsets by facing. */
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};
    private static final int[] OPPOSITE = {0, 0, 3, 2, 5, 4};
    private static final int[] ROTATE_Y = {0, 0, 5, 4, 2, 3};

    /**
     * House2 (blacksmith) furnace of 1.12.2: the default north facing turned by the piece (1.7.10 lets
     * BlockFurnace.onBlockAdded pick a side from the neighbours).
     */
    public static void furnace(StructureComponent piece, World world, int x, int y, int z, StructureBoundingBox area) {
        int[] p = at(piece, x, y, z);
        if (p == null || !area.isVecInside(p[0], p[1], p[2]) || world.getBlock(p[0], p[1], p[2]) != Blocks.furnace) {
            return;
        }
        // north, rotated CLOCKWISE_90 for coordBaseMode 1 and 3 (the LEFT_RIGHT mirror keeps north); no turn by the
        // neighbours: the 1.12.2 worlds (plains mode 1: 5 with cobblestone west and air east, desert mode 0: 2) show none
        int m = mode(piece);
        world.setBlockMetadataWithNotify(p[0], p[1], p[2], m == 1 || m == 3 ? 5 : 2, 2);
    }

    /** House2 (blacksmith) chest of 1.12.2: StructureComponent.limitChestOrientation from the default north facing. */
    public static void chest(StructureComponent piece, World world, int x, int y, int z, StructureBoundingBox area) {
        int[] p = at(piece, x, y, z);
        if (p == null || !area.isVecInside(p[0], p[1], p[2]) || world.getBlock(p[0], p[1], p[2]) != Blocks.chest) {
            return;
        }
        int[] order = {2, 5, 3, 4};
        int found = -1;
        for (int f : order) {
            int nx = p[0] + DX[f];
            int nz = p[2] + DZ[f];
            if (world.getBlock(nx, p[1], nz) == Blocks.chest) {
                world.setBlockMetadataWithNotify(p[0], p[1], p[2], 2, 2);
                return;
            }
            if (full(world, nx, p[1], nz)) {
                if (found >= 0) {
                    found = -1;
                    break;
                }
                found = f;
            }
        }
        int facing;
        if (found >= 0) {
            facing = OPPOSITE[found];
        } else {
            facing = 2;
            if (full(world, p[0] + DX[facing], p[1], p[2] + DZ[facing])) {
                facing = OPPOSITE[facing];
            }
            if (full(world, p[0] + DX[facing], p[1], p[2] + DZ[facing])) {
                facing = ROTATE_Y[facing];
            }
            if (full(world, p[0] + DX[facing], p[1], p[2] + DZ[facing])) {
                facing = OPPOSITE[facing];
            }
        }
        world.setBlockMetadataWithNotify(p[0], p[1], p[2], facing, 2);
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
