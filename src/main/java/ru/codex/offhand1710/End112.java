package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeEndDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;

/**
 * The End of 1.9-1.12.2 on 1.7.10: outer islands (the density of ChunkGeneratorEnd, supplied through InitNoiseField), chorus
 * plants, end cities and ships (the piece logic of StructureEndCityPieces with the original templates) and a gateway
 * that opens when the dragon dies.
 */
public final class End112 implements IWorldGenerator {
    public static Block chorusPlant;
    public static Block chorusFlower;
    public static Block endRod;
    public static Block gateway;
    public static Block dragonHead;
    public static Item chorusFruit;
    /** Set by the client proxy. */
    public static int chorusRenderId;
    public static int rodRenderId;

    private static final int SEED = 10387313;
    private static final int CITY_REACH = 9;

    public static void register() {
        chorusPlant = new BlockChorusPlant();
        chorusFlower = new BlockChorusFlower();
        endRod = new BlockEndRod();
        gateway = new BlockGateway();
        GameRegistry.registerBlock(chorusPlant, "chorus_plant");
        GameRegistry.registerBlock(chorusFlower, "chorus_flower");
        GameRegistry.registerBlock(endRod, "end_rod");
        GameRegistry.registerBlock(gateway, "end_gateway");
        dragonHead = new BlockDragonHead();
        GameRegistry.registerBlock(dragonHead, "dragon_head");
        GameRegistry.registerTileEntity(BlockDragonHead.Tile.class, "mc112bridge.dragon_head");
        chorusFruit = new ItemChorusFruit();
        GameRegistry.registerItem(chorusFruit, "chorus_fruit");
        GameRegistry.addSmelting(chorusFruit, new ItemStack(PortedBlocks.poppedChorusFruit), 0.1F);
        GameRegistry.addShapedRecipe(new ItemStack(endRod, 4), "b", "p", 'b', Items.blaze_rod, 'p', PortedBlocks.poppedChorusFruit);
        End112 end = new End112();
        MinecraftForge.EVENT_BUS.register(end);
        GameRegistry.registerWorldGenerator(end, 8);
        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(Crystal.class, "EndCrystal112", 40, MC112Bridge.instance(), 256, 20, false);
        BiomeGenBase.sky.theBiomeDecorator = new Decorator();
    }

    /** WorldGenSpikes of 1.12.2 replaces the scattered towers of 1.7.10: a ring of ten around the centre. */
    private static final class Decorator extends BiomeEndDecorator {
        private long ringSeed = Long.MIN_VALUE;
        private int[][] ring;

        private int[][] ring(long seed) {
            if (ring == null || ringSeed != seed) {
                ringSeed = seed;
                List<Integer> order = new ArrayList<Integer>();
                for (int i = 0; i < 10; i++) {
                    order.add(Integer.valueOf(i));
                }
                java.util.Collections.shuffle(order, new Random(seed));
                ring = new int[10][];
                for (int i = 0; i < 10; i++) {
                    double angle = 2.0D * (-Math.PI + 0.3141592653589793D * (double) i);
                    int j = order.get(i).intValue();
                    ring[i] = new int[] {MathHelper.floor_double(42.0D * Math.cos(angle)), MathHelper.floor_double(42.0D * Math.sin(angle)),
                            2 + j / 3, 76 + j * 3, j == 1 || j == 2 ? 1 : 0};
                }
            }
            return ring;
        }

        @Override
        protected void genDecorations(BiomeGenBase biome) {
            int minX = chunk_X + 8;
            int minZ = chunk_Z + 8;
            long x = (long) (chunk_X >> 4);
            long z = (long) (chunk_Z >> 4);
            if (x * x + z * z <= 64L) {
                for (int[] spike : ring(currentWorld.getSeed())) {
                    placeSpike(currentWorld, spike, minX, minZ);
                }
            }
            if (minX == -8 && minZ == -8) {
                placePodium(currentWorld, currentWorld.getHeightValue(0, 0), false, minX, minZ);
            }
            if (chunk_X == 0 && chunk_Z == 0) {
                EntityDragon dragon = new EntityDragon(currentWorld);
                dragon.setLocationAndAngles(0.0D, 128.0D, 0.0D, randomGenerator.nextFloat() * 360.0F, 0.0F);
                currentWorld.spawnEntityInWorld(dragon);
            }
        }

        private static void set(World world, int x, int y, int z, Block block, int minX, int minZ) {
            if (x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16) {
                world.setBlock(x, y, z, block, 0, 2);
            }
        }

        /** WorldGenEndPodium of 1.12.2: on the surface at the centre, empty until the dragon dies. */
        static void placePodium(World world, int surface, boolean active, int minX, int minZ) {
            for (int y = surface - 1; y <= surface + 32; y++) {
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        double distance = (double) (x * x + z * z);
                        if (distance > 12.25D) {
                            continue;
                        }
                        if (y < surface) {
                            set(world, x, y, z, distance <= 6.25D ? Blocks.bedrock : Blocks.end_stone, minX, minZ);
                        } else if (y > surface) {
                            set(world, x, y, z, Blocks.air, minX, minZ);
                        } else if (distance > 6.25D) {
                            set(world, x, y, z, Blocks.bedrock, minX, minZ);
                        } else {
                            set(world, x, y, z, active ? Blocks.end_portal : Blocks.air, minX, minZ);
                        }
                    }
                }
            }
            for (int y = surface; y < surface + 4; y++) {
                set(world, 0, y, 0, Blocks.bedrock, minX, minZ);
            }
            int[][] torches = {{-1, 0, 2}, {1, 0, 1}, {0, -1, 4}, {0, 1, 3}};
            for (int[] torch : torches) {
                if (torch[0] >= minX && torch[0] < minX + 16 && torch[1] >= minZ && torch[1] < minZ + 16) {
                    world.setBlock(torch[0], surface + 2, torch[1], Blocks.torch, torch[2], 2);
                }
            }
        }

        private static void placeSpike(World world, int[] s, int minX, int minZ) {
            int cx = s[0];
            int cz = s[1];
            int r = s[2];
            int height = s[3];
            if (cx + r + 2 < minX || cx - r - 2 >= minX + 16 || cz + r + 2 < minZ || cz - r - 2 >= minZ + 16) {
                return;
            }
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > r * r + 1) {
                        continue;
                    }
                    for (int y = 0; y <= height + 10; y++) {
                        if (y < height) {
                            set(world, x, y, z, Blocks.obsidian, minX, minZ);
                        } else if (y > 65) {
                            set(world, x, y, z, Blocks.air, minX, minZ);
                        }
                    }
                }
            }
            if (s[4] != 0) {
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        for (int k = 0; k <= 3; k++) {
                            if (Math.abs(i) == 2 || Math.abs(j) == 2 || k == 3) {
                                set(world, cx + i, height + k, cz + j, Blocks.iron_bars, minX, minZ);
                            }
                        }
                    }
                }
            }
            set(world, cx, height, cz, Blocks.bedrock, minX, minZ);
            set(world, cx, height + 1, cz, Blocks.fire, minX, minZ); // once, at generation; the crystal no longer places it
            if (cx >= minX && cx < minX + 16 && cz >= minZ && cz < minZ + 16) {
                Crystal crystal = new Crystal(world);
                // Measured in the game: the base plate of the 1.7.10 crystal model must wrap the bedrock block, as in 1.12.2.
                crystal.setLocationAndAngles((double) cx + 0.5D, (double) (height - 2), (double) cz + 0.5D, 0.0F, 0.0F);
                world.spawnEntityInWorld(crystal);
            }
        }
    }

    /**
     * The end crystal without the line of 1.7.10 that puts fire under it from onUpdate on both sides. That fire does not
     * stay, so every crystal changed a block each tick, and each change recalculated light across the open End: 1 FPS.
     */
    public static final class Crystal extends net.minecraft.entity.item.EntityEnderCrystal {
        public Crystal(World world) {
            super(world);
        }

        @Override
        public void onUpdate() {
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            innerRotation++;
            dataWatcher.updateObject(8, Integer.valueOf(health));
        }
    }

    /** Crystals saved by older versions or placed by other code become the fixed ones as they enter the world. */
    @SubscribeEvent
    public void onJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity.getClass() == net.minecraft.entity.item.EntityEnderCrystal.class) {
            Crystal crystal = new Crystal(event.world);
            crystal.setLocationAndAngles(event.entity.posX, event.entity.posY, event.entity.posZ, 0.0F, 0.0F);
            event.setCanceled(true);
            event.world.spawnEntityInWorld(crystal);
        }
    }

    // ---------------------------------------------------------------- terrain

    private static long noiseSeed = Long.MIN_VALUE;
    private static NoiseGeneratorOctaves noise1;
    private static NoiseGeneratorOctaves noise2;
    private static NoiseGeneratorOctaves noise3;
    private static NoiseGeneratorSimplex islands;
    private static double[] data1;
    private static double[] data2;
    private static double[] data3;

    private static void ensureNoise() {
        long seed = MinecraftServer.getServer().worldServers[0].getSeed();
        if (seed != noiseSeed) {
            noiseSeed = seed;
            Random random = new Random(seed);
            noise1 = new NoiseGeneratorOctaves(random, 16);
            noise2 = new NoiseGeneratorOctaves(random, 16);
            noise3 = new NoiseGeneratorOctaves(random, 8);
            new NoiseGeneratorOctaves(random, 10);
            new NoiseGeneratorOctaves(random, 16);
            islands = new NoiseGeneratorSimplex(random);
        }
    }

    /** ChunkGeneratorEnd.getIslandHeightValue. */
    private static float islandHeight(int x, int z, int offsetX, int offsetZ) {
        float fx = (float) (x * 2 + offsetX);
        float fz = (float) (z * 2 + offsetZ);
        float height = 100.0F - MathHelper.sqrt_float(fx * fx + fz * fz) * 8.0F;
        height = Math.max(-100.0F, Math.min(80.0F, height));
        for (int i = -12; i <= 12; i++) {
            for (int j = -12; j <= 12; j++) {
                long cx = (long) (x + i);
                long cz = (long) (z + j);
                if (cx * cx + cz * cz > 4096L && islands.func_151605_a((double) cx, (double) cz) < -0.8999999761581421D) {
                    float size = (Math.abs((float) cx) * 3439.0F + Math.abs((float) cz) * 147.0F) % 13.0F + 9.0F;
                    fx = (float) (offsetX - i * 2);
                    fz = (float) (offsetZ - j * 2);
                    float other = 100.0F - MathHelper.sqrt_float(fx * fx + fz * fz) * size;
                    other = Math.max(-100.0F, Math.min(80.0F, other));
                    height = Math.max(height, other);
                }
            }
        }
        return height;
    }

    /** ChunkGeneratorEnd.getHeights. */
    @SubscribeEvent
    public void onNoise(ChunkProviderEvent.InitNoiseField event) {
        if (!(event.chunkProvider instanceof ChunkProviderEnd)) {
            return;
        }
        ensureNoise();
        double[] field = event.noisefield != null ? event.noisefield : new double[event.sizeX * event.sizeY * event.sizeZ];
        double xz = 684.412D * 2.0D;
        data1 = noise3.generateNoiseOctaves(data1, event.posX, event.posY, event.posZ, event.sizeX, event.sizeY, event.sizeZ, xz / 80.0D, 4.277575000000001D, xz / 80.0D);
        data2 = noise1.generateNoiseOctaves(data2, event.posX, event.posY, event.posZ, event.sizeX, event.sizeY, event.sizeZ, xz, 684.412D, xz);
        data3 = noise2.generateNoiseOctaves(data3, event.posX, event.posY, event.posZ, event.sizeX, event.sizeY, event.sizeZ, xz, 684.412D, xz);
        int chunkX = event.posX / 2;
        int chunkZ = event.posZ / 2;
        int index = 0;
        for (int x = 0; x < event.sizeX; x++) {
            for (int z = 0; z < event.sizeZ; z++) {
                float height = islandHeight(chunkX, chunkZ, x, z);
                for (int y = 0; y < event.sizeY; y++) {
                    double low = data2[index] / 512.0D;
                    double high = data3[index] / 512.0D;
                    double mix = (data1[index] / 10.0D + 1.0D) / 2.0D;
                    double density = mix < 0.0D ? low : (mix > 1.0D ? high : low + (high - low) * mix);
                    density = density - 8.0D + (double) height;
                    if (y > event.sizeY / 2 - 2) {
                        double top = Math.max(0.0D, Math.min(1.0D, (double) ((float) (y - (event.sizeY / 2 - 2)) / 64.0F)));
                        density = density * (1.0D - top) + -3000.0D * top;
                    }
                    if (y < 8) {
                        double bottom = (double) ((float) (8 - y) / 7.0F);
                        density = density * (1.0D - bottom) + -30.0D * bottom;
                    }
                    field[index++] = density;
                }
            }
        }
        event.noisefield = field;
        event.setResult(Event.Result.DENY);
    }

    // ---------------------------------------------------------------- decoration

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider provider) {
        if (world.provider.dimensionId != 1) {
            return;
        }
        ensureNoise();
        generateCities(world, random, chunkX, chunkZ);
        long distance = (long) chunkX * (long) chunkX + (long) chunkZ * (long) chunkZ;
        if (distance <= 4096L) {
            return;
        }
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        float height = islandHeight(chunkX, chunkZ, 1, 1);
        if (height < -20.0F && random.nextInt(14) == 0) {
            smallIsland(world, random, baseX + random.nextInt(16) + 8, 55 + random.nextInt(16), baseZ + random.nextInt(16) + 8);
            if (random.nextInt(4) == 0) {
                smallIsland(world, random, baseX + random.nextInt(16) + 8, 55 + random.nextInt(16), baseZ + random.nextInt(16) + 8);
            }
        }
        if (height > 40.0F) {
            int count = random.nextInt(5);
            for (int i = 0; i < count; i++) {
                int x = baseX + random.nextInt(16) + 8;
                int z = baseZ + random.nextInt(16) + 8;
                int top = world.getHeightValue(x, z);
                if (top > 0 && world.isAirBlock(x, top, z) && world.getBlock(x, top - 1, z) == Blocks.end_stone) {
                    BlockChorusFlower.generatePlant(world, x, top, z, random, 8);
                }
            }
        }
    }

    /** WorldGenEndIsland. */
    private static void smallIsland(World world, Random random, int x, int y, int z) {
        float radius = (float) (random.nextInt(3) + 4);
        for (int level = 0; radius > 0.5F; level--) {
            int reach = MathHelper.ceiling_float_int(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if ((float) (dx * dx + dz * dz) <= (radius + 1.0F) * (radius + 1.0F)) {
                        world.setBlock(x + dx, y + level, z + dz, Blocks.end_stone, 0, 2);
                    }
                }
            }
            radius -= (float) random.nextInt(2) + 0.5F;
        }
    }

    // ---------------------------------------------------------------- end cities

    private static final class Entry {
        int x;
        int y;
        int z;
        String name;
        String value;
    }

    private static final class Template {
        int sizeX;
        int sizeY;
        int sizeZ;
        final List<Entry> entries = new ArrayList<Entry>();
    }

    private static final Map<String, Template> TEMPLATES = new HashMap<String, Template>();

    private static Template template(String name) {
        Template template = TEMPLATES.get(name);
        if (template != null) {
            return template;
        }
        template = new Template();
        InputStream stream = End112.class.getResourceAsStream("/assets/mc112bridge/structures/endcity/" + name + ".txt");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String[] size = reader.readLine().split(" ");
            template.sizeX = Integer.parseInt(size[0]);
            template.sizeY = Integer.parseInt(size[1]);
            template.sizeZ = Integer.parseInt(size[2]);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length < 4) {
                    continue;
                }
                Entry entry = new Entry();
                entry.x = Integer.parseInt(parts[0]);
                entry.y = Integer.parseInt(parts[1]);
                entry.z = Integer.parseInt(parts[2]);
                entry.name = parts[3];
                entry.value = parts.length > 4 ? parts[4] : "-";
                template.entries.add(entry);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read end city piece " + name, e);
        }
        TEMPLATES.put(name, template);
        return template;
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

    private static final class Piece {
        final String name;
        final int x;
        final int y;
        final int z;
        final int rotation;
        final boolean overwrite;
        final int minX;
        final int minZ;
        final int maxX;
        final int maxZ;
        final int maxY;
        int depth;

        Piece(String name, int x, int y, int z, int rotation, boolean overwrite) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
            this.overwrite = overwrite;
            Template template = template(name);
            int ax = x + rotX(template.sizeX - 1, template.sizeZ - 1, rotation);
            int az = z + rotZ(template.sizeX - 1, template.sizeZ - 1, rotation);
            minX = Math.min(x, ax);
            maxX = Math.max(x, ax);
            minZ = Math.min(z, az);
            maxZ = Math.max(z, az);
            maxY = y + template.sizeY - 1;
        }

        boolean intersects(Piece o) {
            return maxX >= o.minX && minX <= o.maxX && maxZ >= o.minZ && minZ <= o.maxZ && maxY >= o.y && y <= o.maxY;
        }
    }

    private static Piece child(Piece parent, int dx, int dy, int dz, String name, int rotation, boolean overwrite) {
        return new Piece(name, parent.x + rotX(dx, dz, parent.rotation), parent.y + dy, parent.z + rotZ(dx, dz, parent.rotation), rotation, overwrite);
    }

    private static Piece add(List<Piece> list, Piece piece) {
        list.add(piece);
        return piece;
    }

    private interface Gen {
        boolean run(int depth, Piece parent, int[] offset, List<Piece> out, Random random);
    }

    private static boolean shipPlaced;

    private static final Gen HOUSE = new Gen() {
        @Override
        public boolean run(int depth, Piece parent, int[] offset, List<Piece> out, Random random) {
            if (depth > 8) {
                return false;
            }
            int rot = parent.rotation;
            Piece p = add(out, child(parent, offset[0], offset[1], offset[2], "base_floor", rot, true));
            int kind = random.nextInt(3);
            if (kind == 0) {
                add(out, child(p, -1, 4, -1, "base_roof", rot, true));
            } else if (kind == 1) {
                p = add(out, child(p, -1, 0, -1, "second_floor_2", rot, false));
                p = add(out, child(p, -1, 8, -1, "second_roof", rot, false));
                recurse(TOWER, depth + 1, p, null, out, random);
            } else {
                p = add(out, child(p, -1, 0, -1, "second_floor_2", rot, false));
                p = add(out, child(p, -1, 4, -1, "third_floor_c", rot, false));
                p = add(out, child(p, -1, 8, -1, "third_roof", rot, true));
                recurse(TOWER, depth + 1, p, null, out, random);
            }
            return true;
        }
    };

    private static final int[][] TOWER_BRIDGES = {{0, 1, -1, 0}, {1, 6, -1, 1}, {3, 0, -1, 5}, {2, 5, -1, 6}};
    private static final int[][] FAT_BRIDGES = {{0, 4, -1, 0}, {1, 12, -1, 4}, {3, 0, -1, 8}, {2, 8, -1, 12}};

    private static final Gen TOWER = new Gen() {
        @Override
        public boolean run(int depth, Piece parent, int[] offset, List<Piece> out, Random random) {
            int rot = parent.rotation;
            Piece p = add(out, child(parent, 3 + random.nextInt(2), -3, 3 + random.nextInt(2), "tower_base", rot, true));
            p = add(out, child(p, 0, 7, 0, "tower_piece", rot, true));
            Piece floor = random.nextInt(3) == 0 ? p : null;
            int height = 1 + random.nextInt(3);
            for (int i = 0; i < height; i++) {
                p = add(out, child(p, 0, 4, 0, "tower_piece", rot, true));
                if (i < height - 1 && random.nextBoolean()) {
                    floor = p;
                }
            }
            if (floor != null) {
                for (int[] bridge : TOWER_BRIDGES) {
                    if (random.nextBoolean()) {
                        Piece end = add(out, child(floor, bridge[1], bridge[2], bridge[3], "bridge_end", (rot + bridge[0]) % 4, true));
                        recurse(BRIDGE, depth + 1, end, null, out, random);
                    }
                }
                add(out, child(p, -1, 4, -1, "tower_top", rot, true));
            } else {
                if (depth != 7) {
                    return recurse(FAT, depth + 1, p, null, out, random);
                }
                add(out, child(p, -1, 4, -1, "tower_top", rot, true));
            }
            return true;
        }
    };

    private static final Gen BRIDGE = new Gen() {
        @Override
        public boolean run(int depth, Piece parent, int[] offset, List<Piece> out, Random random) {
            int rot = parent.rotation;
            int length = random.nextInt(4) + 1;
            Piece p = add(out, child(parent, 0, 0, -4, "bridge_piece", rot, true));
            p.depth = -1;
            int rise = 0;
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    p = add(out, child(p, 0, rise, -4, "bridge_piece", rot, true));
                    rise = 0;
                } else {
                    if (random.nextBoolean()) {
                        p = add(out, child(p, 0, rise, -4, "bridge_steep_stairs", rot, true));
                    } else {
                        p = add(out, child(p, 0, rise, -8, "bridge_gentle_stairs", rot, true));
                    }
                    rise = 4;
                }
            }
            if (!shipPlaced && random.nextInt(10 - depth) == 0) {
                add(out, child(p, -8 + random.nextInt(8), rise, -70 + random.nextInt(10), "ship", rot, true));
                shipPlaced = true;
            } else if (!recurse(HOUSE, depth + 1, p, new int[] {-3, rise + 1, -11}, out, random)) {
                return false;
            }
            p = add(out, child(p, 4, rise, 0, "bridge_end", (rot + 2) % 4, true));
            p.depth = -1;
            return true;
        }
    };

    private static final Gen FAT = new Gen() {
        @Override
        public boolean run(int depth, Piece parent, int[] offset, List<Piece> out, Random random) {
            int rot = parent.rotation;
            Piece p = add(out, child(parent, -3, 4, -3, "fat_tower_base", rot, true));
            p = add(out, child(p, 0, 4, 0, "fat_tower_middle", rot, true));
            for (int i = 0; i < 2 && random.nextInt(3) != 0; i++) {
                p = add(out, child(p, 0, 8, 0, "fat_tower_middle", rot, true));
                for (int[] bridge : FAT_BRIDGES) {
                    if (random.nextBoolean()) {
                        Piece end = add(out, child(p, bridge[1], bridge[2], bridge[3], "bridge_end", (rot + bridge[0]) % 4, true));
                        recurse(BRIDGE, depth + 1, end, null, out, random);
                    }
                }
            }
            add(out, child(p, -2, 8, -2, "fat_tower_top", rot, true));
            return true;
        }
    };

    private static boolean recurse(Gen gen, int depth, Piece parent, int[] offset, List<Piece> out, Random random) {
        if (depth > 8) {
            return false;
        }
        List<Piece> group = new ArrayList<Piece>();
        int[] where = offset != null ? offset : new int[3];
        if (!gen.run(depth, parent, where, group, random)) {
            return false;
        }
        int id = random.nextInt();
        for (Piece piece : group) {
            piece.depth = id;
            for (Piece existing : out) {
                if (existing.intersects(piece)) {
                    if (existing.depth != parent.depth) {
                        return false;
                    }
                    break;
                }
            }
        }
        out.addAll(group);
        return true;
    }

    private static List<Piece> buildCity(int x, int y, int z, int rotation, Random random) {
        shipPlaced = false;
        List<Piece> pieces = new ArrayList<Piece>();
        Piece p = add(pieces, new Piece("base_floor", x, y, z, rotation, true));
        p = add(pieces, child(p, -1, 0, -1, "second_floor", rotation, false));
        p = add(pieces, child(p, -1, 4, -1, "third_floor", rotation, false));
        p = add(pieces, child(p, -1, 8, -1, "third_roof", rotation, true));
        recurse(TOWER, 1, p, null, pieces, random);
        return pieces;
    }

    private final Map<Long, List<Piece>> cities = new LinkedHashMap<Long, List<Piece>>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Piece>> eldest) {
            return size() > 8;
        }
    };
    private ChunkProviderEnd probe;
    private long probeSeed;

    static boolean isGrid(World world, int chunkX, int chunkZ) {
        int regionX = (chunkX < 0 ? chunkX - 19 : chunkX) / 20;
        int regionZ = (chunkZ < 0 ? chunkZ - 19 : chunkZ) / 20;
        Random random = world.setRandomSeed(regionX, regionZ, SEED);
        int x = regionX * 20 + (random.nextInt(9) + random.nextInt(9)) / 2;
        int z = regionZ * 20 + (random.nextInt(9) + random.nextInt(9)) / 2;
        return chunkX == x && chunkZ == z && (long) chunkX * (long) chunkX + (long) chunkZ * (long) chunkZ > 4096L
                && islandHeight(chunkX, chunkZ, 1, 1) >= 0.0F;
    }

    private static int top(Block[] blocks, int x, int z) {
        for (int y = 127; y >= 0; y--) {
            Block block = blocks[(x * 16 + z) * 128 + y];
            if (block != null && block != Blocks.air) {
                return y;
            }
        }
        return 0;
    }

    private List<Piece> city(World world, int chunkX, int chunkZ) {
        long seed = world.getSeed();
        if (probe == null || probeSeed != seed) {
            probe = new ChunkProviderEnd(world, seed);
            probeSeed = seed;
            cities.clear();
        }
        Long key = Long.valueOf((long) chunkX << 32 ^ (long) chunkZ & 0xFFFFFFFFL);
        if (cities.containsKey(key)) {
            return cities.get(key);
        }
        List<Piece> result = null;
        Random shape = new Random((long) (chunkX + chunkZ * SEED));
        int rotation = shape.nextInt(4);
        Block[] blocks = new Block[32768];
        probe.func_147420_a(chunkX, chunkZ, blocks, null);
        int dx = rotation == 1 || rotation == 2 ? -5 : 5;
        int dz = rotation == 2 || rotation == 3 ? -5 : 5;
        int y = Math.min(Math.min(top(blocks, 7, 7), top(blocks, 7, 7 + dz)), Math.min(top(blocks, 7 + dx, 7), top(blocks, 7 + dx, 7 + dz)));
        if (y >= 60) {
            Random random = new Random(seed);
            long a = random.nextLong();
            long b = random.nextLong();
            random.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ seed);
            result = buildCity(chunkX * 16 + 8, y, chunkZ * 16 + 8, rotation, random);
        }
        cities.put(key, result);
        return result;
    }

    private void generateCities(World world, Random random, int chunkX, int chunkZ) {
        int minX = chunkX * 16 + 8;
        int minZ = chunkZ * 16 + 8;
        for (int sx = chunkX - CITY_REACH; sx <= chunkX + CITY_REACH; sx++) {
            for (int sz = chunkZ - CITY_REACH; sz <= chunkZ + CITY_REACH; sz++) {
                if (!isGrid(world, sx, sz)) {
                    continue;
                }
                List<Piece> pieces = city(world, sx, sz);
                if (pieces == null) {
                    continue;
                }
                for (Piece piece : pieces) {
                    if (piece.maxX >= minX && piece.minX <= minX + 15 && piece.maxZ >= minZ && piece.minZ <= minZ + 15) {
                        place(world, random, piece, minX, minZ);
                    }
                }
            }
        }
    }

    private static int dirIndex(char c) {
        return "nesw".indexOf(c);
    }

    private static int side(int dir) {
        return new int[] {2, 5, 3, 4}[dir];
    }

    private static int stair(int dir) {
        return new int[] {3, 0, 2, 1}[dir];
    }

    private static void place(World world, Random random, Piece piece, int minX, int minZ) {
        Template template = template(piece.name);
        for (Entry entry : template.entries) {
            int x = piece.x + rotX(entry.x, entry.z, piece.rotation);
            int y = piece.y + entry.y;
            int z = piece.z + rotZ(entry.x, entry.z, piece.rotation);
            if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                continue;
            }
            if (entry.name.charAt(0) == '#') {
                marker(world, random, entry.name.substring(1), x, y, z, piece.rotation, minX, minZ);
                continue;
            }
            Block block;
            int meta = 0;
            String value = entry.value;
            String name = entry.name;
            if ("air".equals(name)) {
                if (!piece.overwrite) {
                    continue;
                }
                block = Blocks.air;
            } else if ("end_bricks".equals(name)) {
                block = PortedBlocks.endBricks;
            } else if ("purpur_block".equals(name)) {
                block = PortedBlocks.purpurBlock;
            } else if ("obsidian".equals(name)) {
                block = Blocks.obsidian;
            } else if ("stained_glass".equals(name)) {
                block = Blocks.stained_glass;
                meta = 2;
            } else if ("brewing_stand".equals(name)) {
                block = Blocks.brewing_stand;
            } else if ("purpur_pillar".equals(name)) {
                block = PortedBlocks.purpurPillar;
                char axis = value.charAt(0);
                if (axis != 'y') {
                    meta = (axis == 'x') == (piece.rotation % 2 == 0) ? 4 : 8;
                }
            } else if ("purpur_slab".equals(name)) {
                block = PortedBlocks.purpurSlab;
                meta = value.charAt(0) == 't' ? 8 : 0;
            } else if ("purpur_stairs".equals(name)) {
                block = PortedBlocks.purpurStairs;
                meta = stair((dirIndex(value.charAt(0)) + piece.rotation) % 4) + (value.charAt(1) == 't' ? 4 : 0);
            } else if ("chest".equals(name) || "ender_chest".equals(name) || "ladder".equals(name)) {
                block = "chest".equals(name) ? Blocks.chest : ("ladder".equals(name) ? Blocks.ladder : Blocks.ender_chest);
                meta = side((dirIndex(value.charAt(0)) + piece.rotation) % 4);
            } else if ("dragon_head".equals(name)) {
                block = dragonHead;
                meta = side((dirIndex(value.charAt(0)) + piece.rotation) % 4);
            } else if ("end_rod".equals(name)) {
                block = endRod;
                if ("up".equals(value)) {
                    meta = 1;
                } else if ("down".equals(value)) {
                    meta = 0;
                } else {
                    meta = side((dirIndex(value.charAt(0)) + piece.rotation) % 4);
                }
            } else {
                continue;
            }
            world.setBlock(x, y, z, block, meta, 2);
        }
    }

    private static void marker(World world, Random random, String name, int x, int y, int z, int rotation, int minX, int minZ) {
        if (name.startsWith("Chest")) {
            TileEntity tile = world.getTileEntity(x, y - 1, z);
            world.setBlockToAir(x, y, z);
            if (tile instanceof TileEntityChest) {
                fillChest((TileEntityChest) tile, random);
            }
        } else if (name.startsWith("Sentry")) {
            MoreMobs.Shulker shulker = new MoreMobs.Shulker(world);
            shulker.setLocationAndAngles((double) x + 0.5D, (double) y, (double) z + 0.5D, 0.0F, 0.0F);
            shulker.func_110163_bv();
            world.spawnEntityInWorld(shulker);
        } else if (name.startsWith("Elytra")) {
            EntityItemFrame frame = new EntityItemFrame(world, x, y, z, rotation % 4);
            frame.setDisplayedItem(new ItemStack(Elytra112.elytra));
            world.spawnEntityInWorld(frame);
        }
    }

    /** loot_tables/chests/end_city_treasure.json */
    private static void fillChest(TileEntityChest chest, Random random) {
        Item[] gear = {Items.diamond_sword, Items.diamond_boots, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_helmet,
                Items.diamond_pickaxe, Items.diamond_shovel, Items.iron_sword, Items.iron_boots, Items.iron_chestplate, Items.iron_leggings,
                Items.iron_helmet, Items.iron_pickaxe, Items.iron_shovel};
        int rolls = 2 + random.nextInt(5);
        for (int roll = 0; roll < rolls; roll++) {
            if (random.nextInt(43 + gear.length * 3) < 5) {
                chest.setInventorySlotContents(random.nextInt(chest.getSizeInventory()), new ItemStack(Items112.beetrootSeeds, 1 + random.nextInt(10)));
                continue;
            }
            int pick = random.nextInt(38 + gear.length * 3);
            ItemStack stack;
            if (pick < 5) {
                stack = new ItemStack(Items.diamond, 2 + random.nextInt(6));
            } else if (pick < 15) {
                stack = new ItemStack(Items.iron_ingot, 4 + random.nextInt(5));
            } else if (pick < 30) {
                stack = new ItemStack(Items.gold_ingot, 2 + random.nextInt(6));
            } else if (pick < 32) {
                stack = new ItemStack(Items.emerald, 2 + random.nextInt(5));
            } else if (pick < 35) {
                stack = new ItemStack(Items.saddle);
            } else if (pick < 36) {
                stack = new ItemStack(Items.iron_horse_armor);
            } else if (pick < 37) {
                stack = new ItemStack(Items.golden_horse_armor);
            } else if (pick < 38) {
                stack = new ItemStack(Items.diamond_horse_armor);
            } else {
                stack = new ItemStack(gear[(pick - 38) / 3]);
                EnchantmentHelper.addRandomEnchantment(random, stack, 20 + random.nextInt(20));
            }
            int size = chest.getSizeInventory();
            int slot = random.nextInt(size);
            for (int i = 0; i < size; i++) {
                if (chest.getStackInSlot((slot + i) % size) == null) {
                    chest.setInventorySlotContents((slot + i) % size, stack);
                    break;
                }
            }
        }
    }

    /**
     * 1.7.10 would build its own exit portal at a fixed height where the dragon dies. The last tick of the death is taken
     * over instead: the experience of that tick, then the portal is lit in the frame on the surface at the centre.
     */
    @SubscribeEvent
    public void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof EntityDragon) || event.entityLiving.worldObj.isRemote
                || ((EntityDragon) event.entityLiving).deathTicks < 199) {
            return;
        }
        EntityDragon dragon = (EntityDragon) event.entityLiving;
        World world = dragon.worldObj;
        for (int xp = 2000; xp > 0;) {
            int orb = net.minecraft.entity.item.EntityXPOrb.getXPSplit(xp);
            xp -= orb;
            world.spawnEntityInWorld(new net.minecraft.entity.item.EntityXPOrb(world, dragon.posX, dragon.posY, dragon.posZ, orb));
        }
        int top = world.getHeightValue(0, 0);
        int surface = world.getBlock(0, top - 1, 0) == Blocks.bedrock && world.getBlock(0, top - 4, 0) == Blocks.bedrock ? top - 4 : top;
        BlockEndPortal.field_149948_a = true;
        Decorator.placePodium(world, surface, true, -8, -8);
        BlockEndPortal.field_149948_a = false;
        world.setBlock(0, surface + 4, 0, Blocks.dragon_egg, 0, 2);
        dragon.setDead();
        event.setCanceled(true);
    }

    // ---------------------------------------------------------------- gateways

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityDragon && !event.entityLiving.worldObj.isRemote) {
            World world = event.entityLiving.worldObj;
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            int x = MathHelper.floor_double(Math.cos(angle) * 96.0D);
            int z = MathHelper.floor_double(Math.sin(angle) * 96.0D);
            buildGateway(world, x, groundOrPlatform(world, x, z), z);
        }
    }

    static void buildGateway(World world, int x, int y, int z) {
        world.setBlock(x, y - 1, z, Blocks.bedrock, 0, 2);
        for (int i = 0; i < 3; i++) {
            world.setBlock(x, y + i, z, gateway, 0, 2);
        }
        world.setBlock(x, y + 3, z, Blocks.bedrock, 0, 2);
    }

    /** The height to stand on at this place; over the void a small end stone platform is built first. */
    static int groundOrPlatform(World world, int x, int z) {
        world.getChunkProvider().provideChunk(x >> 4, z >> 4);
        int top = world.getHeightValue(x, z);
        if (top > 0) {
            return top;
        }
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz <= 25) {
                    world.setBlock(x + dx, 74, z + dz, Blocks.end_stone, 0, 2);
                }
            }
        }
        return 75;
    }

    /** A simple stand-in for the end gateway: enter it to reach the outer islands, or to come back. */
    public static final class BlockGateway extends BlockEndPortal {
        public BlockGateway() {
            super(Material.portal);
            setBlockName(MC112Bridge.MOD_ID + ".end_gateway");
            setBlockUnbreakable();
            setResistance(6000000.0F);
            setLightLevel(1.0F);
        }

        /** The vanilla portal block removes itself outside the Overworld when placed. */
        @Override
        public void onBlockAdded(World world, int x, int y, int z) {
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        }

        @Override
        public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
            if (world.isRemote || entity.ridingEntity != null || entity.riddenByEntity != null || !(entity instanceof EntityPlayer)
                    || entity.timeUntilPortal > 0) {
                return;
            }
            entity.timeUntilPortal = 100;
            double length = Math.sqrt((double) x * (double) x + (double) z * (double) z);
            double target = length > 500.0D ? 96.0D : 1024.0D;
            int tx = MathHelper.floor_double((double) x / length * target);
            int tz = MathHelper.floor_double((double) z / length * target);
            int top = groundOrPlatform(world, tx, tz);
            if (world.getBlock(tx + 3, top, tz) != gateway) {
                buildGateway(world, tx + 3, top, tz);
            }
            ((EntityPlayer) entity).setPositionAndUpdate((double) tx + 0.5D, (double) top + 0.5D, (double) tz + 0.5D);
        }
    }

    // ---------------------------------------------------------------- blocks and items

    /** BlockChorusPlant. Connects to chorus above, below and at the sides, and stands on end stone. */
    public static final class BlockChorusPlant extends Block {
        BlockChorusPlant() {
            super(Material.plants);
            setBlockName(MC112Bridge.MOD_ID + ".chorus_plant");
            setBlockTextureName(MC112Bridge.MOD_ID + ":chorus_plant");
            setCreativeTab(CreativeTabs.tabDecorations);
            setHardness(0.4F);
            setStepSound(soundTypeWood);
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return chorusRenderId;
        }

        static boolean connects(IBlockAccess world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            return block == chorusPlant || block == chorusFlower;
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            float min = 0.1875F;
            float max = 0.8125F;
            setBlockBounds(connects(world, x - 1, y, z) ? 0.0F : min, connects(world, x, y - 1, z) || world.getBlock(x, y - 1, z) == Blocks.end_stone ? 0.0F : min,
                    connects(world, x, y, z - 1) ? 0.0F : min, connects(world, x + 1, y, z) ? 1.0F : max, connects(world, x, y + 1, z) ? 1.0F : max,
                    connects(world, x, y, z + 1) ? 1.0F : max);
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
            setBlockBoundsBasedOnState(world, x, y, z);
            return super.getCollisionBoundingBoxFromPool(world, x, y, z);
        }

        boolean canStay(World world, int x, int y, int z) {
            boolean air = world.isAirBlock(x, y + 1, z) || world.isAirBlock(x, y - 1, z);
            int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] s : sides) {
                Block block = world.getBlock(x + s[0], y, z + s[1]);
                if (block == this) {
                    if (!air) {
                        return false;
                    }
                    Block below = world.getBlock(x + s[0], y - 1, z + s[1]);
                    if (below == this || below == Blocks.end_stone) {
                        return true;
                    }
                }
            }
            Block below = world.getBlock(x, y - 1, z);
            return below == this || below == Blocks.end_stone;
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
            if (!canStay(world, x, y, z)) {
                world.scheduleBlockUpdate(x, y, z, this, 1);
            }
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (!canStay(world, x, y, z)) {
                world.func_147480_a(x, y, z, true);
            }
        }

        @Override
        public boolean canPlaceBlockAt(World world, int x, int y, int z) {
            return super.canPlaceBlockAt(world, x, y, z) && canStay(world, x, y, z);
        }

        @Override
        public Item getItemDropped(int meta, Random random, int fortune) {
            return chorusFruit;
        }

        @Override
        public int quantityDropped(Random random) {
            return random.nextInt(2);
        }
    }

    /** BlockChorusFlower: the growing tip; meta is its age, 5 is dead. */
    public static final class BlockChorusFlower extends Block {
        private IIcon dead;

        BlockChorusFlower() {
            super(Material.plants);
            setBlockName(MC112Bridge.MOD_ID + ".chorus_flower");
            setBlockTextureName(MC112Bridge.MOD_ID + ":chorus_flower");
            setCreativeTab(CreativeTabs.tabDecorations);
            setHardness(0.4F);
            setStepSound(soundTypeWood);
            setTickRandomly(true);
        }

        @Override
        public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
            super.registerBlockIcons(register);
            dead = register.registerIcon(MC112Bridge.MOD_ID + ":chorus_flower_dead");
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            return meta >= 5 ? dead : blockIcon;
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return chorusRenderId;
        }

        private static boolean surrounded(World world, int x, int y, int z, int ignoreX, int ignoreZ) {
            int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] s : sides) {
                if ((s[0] != ignoreX || s[1] != ignoreZ) && !world.isAirBlock(x + s[0], y, z + s[1])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (!canStay(world, x, y, z)) {
                world.func_147480_a(x, y, z, true);
                return;
            }
            if (!world.isAirBlock(x, y + 1, z) || y + 1 >= 256) {
                return;
            }
            int age = world.getBlockMetadata(x, y, z);
            if (age >= 5) {
                return;
            }
            boolean grow = false;
            boolean island = false;
            Block below = world.getBlock(x, y - 1, z);
            if (below == Blocks.end_stone) {
                grow = true;
            } else if (below == chorusPlant) {
                int height = 1;
                for (int i = 0; i < 4; i++) {
                    Block block = world.getBlock(x, y - height - 1, z);
                    if (block != chorusPlant) {
                        if (block == Blocks.end_stone) {
                            island = true;
                        }
                        break;
                    }
                    height++;
                }
                int limit = island ? 5 : 4;
                if (height < 2 || random.nextInt(limit) >= height) {
                    grow = true;
                }
            } else if (below.getMaterial() == Material.air) {
                grow = true;
            }
            if (grow && surrounded(world, x, y + 1, z, 0, 0) && world.isAirBlock(x, y + 2, z)) {
                world.setBlock(x, y, z, chorusPlant, 0, 2);
                world.setBlock(x, y + 1, z, this, age, 2);
            } else if (age < 4) {
                int tries = random.nextInt(4);
                if (island) {
                    tries++;
                }
                boolean branched = false;
                int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int i = 0; i < tries; i++) {
                    int[] s = sides[random.nextInt(4)];
                    int nx = x + s[0];
                    int nz = z + s[1];
                    if (world.isAirBlock(nx, y, nz) && world.isAirBlock(nx, y - 1, nz) && surrounded(world, nx, y, nz, -s[0], -s[1])) {
                        world.setBlock(nx, y, nz, this, age + 1, 2);
                        branched = true;
                    }
                }
                if (branched) {
                    world.setBlock(x, y, z, chorusPlant, 0, 2);
                } else {
                    world.setBlockMetadataWithNotify(x, y, z, 5, 2);
                }
            } else {
                world.setBlockMetadataWithNotify(x, y, z, 5, 2);
            }
        }

        boolean canStay(World world, int x, int y, int z) {
            Block below = world.getBlock(x, y - 1, z);
            if (below == chorusPlant || below == Blocks.end_stone) {
                return true;
            }
            if (below.getMaterial() != Material.air) {
                return false;
            }
            int plants = 0;
            int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] s : sides) {
                Block block = world.getBlock(x + s[0], y, z + s[1]);
                if (block == chorusPlant) {
                    plants++;
                } else if (block.getMaterial() != Material.air) {
                    return false;
                }
            }
            return plants == 1;
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
            if (!canStay(world, x, y, z)) {
                world.scheduleBlockUpdate(x, y, z, this, 1);
            }
        }

        @Override
        public boolean canPlaceBlockAt(World world, int x, int y, int z) {
            return super.canPlaceBlockAt(world, x, y, z) && canStay(world, x, y, z);
        }

        @Override
        public int quantityDropped(Random random) {
            return 0;
        }

        @Override
        public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
            super.harvestBlock(world, player, x, y, z, meta);
            dropBlockAsItem(world, x, y, z, new ItemStack(this));
        }

        /** BlockChorusFlower.generatePlant. */
        public static void generatePlant(World world, int x, int y, int z, Random random, int spread) {
            world.setBlock(x, y, z, chorusPlant, 0, 2);
            grow(world, x, y, z, random, x, z, spread, 0);
        }

        private static boolean clear(World world, int x, int y, int z, int skipX, int skipZ) {
            return surrounded(world, x, y, z, skipX, skipZ);
        }

        private static void grow(World world, int x, int y, int z, Random random, int originX, int originZ, int spread, int depth) {
            int height = random.nextInt(4) + 1;
            if (depth == 0) {
                height++;
            }
            for (int i = 0; i < height; i++) {
                if (!clear(world, x, y + i + 1, z, 0, 0)) {
                    return;
                }
                world.setBlock(x, y + i + 1, z, chorusPlant, 0, 2);
            }
            boolean branched = false;
            if (depth < 4) {
                int tries = random.nextInt(4);
                if (depth == 0) {
                    tries++;
                }
                int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int i = 0; i < tries; i++) {
                    int[] s = sides[random.nextInt(4)];
                    int nx = x + s[0];
                    int nz = z + s[1];
                    int ny = y + height;
                    if (Math.abs(nx - originX) < spread && Math.abs(nz - originZ) < spread && world.isAirBlock(nx, ny, nz)
                            && world.isAirBlock(nx, ny - 1, nz) && clear(world, nx, ny, nz, -s[0], -s[1])) {
                        branched = true;
                        world.setBlock(nx, ny, nz, chorusPlant, 0, 2);
                        grow(world, nx, ny, nz, random, originX, originZ, spread, depth + 1);
                    }
                }
            }
            if (!branched) {
                world.setBlock(x, y + height, z, chorusFlower, 5, 2);
            }
        }
    }

    /** BlockEndRod: a light on any face; meta is the direction it points to (0 down, 1 up, 2-5 sides). */
    public static final class BlockEndRod extends Block {
        BlockEndRod() {
            super(Material.circuits);
            setBlockName(MC112Bridge.MOD_ID + ".end_rod");
            setBlockTextureName(MC112Bridge.MOD_ID + ":end_rod");
            setCreativeTab(CreativeTabs.tabDecorations);
            setLightLevel(0.9375F);
            setStepSound(soundTypeWood);
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return rodRenderId;
        }

        @Override
        public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
            return side;
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            int meta = world.getBlockMetadata(x, y, z);
            float min = 0.375F;
            float max = 0.625F;
            if (meta < 2) {
                setBlockBounds(min, 0.0F, min, max, 1.0F, max);
            } else if (meta < 4) {
                setBlockBounds(min, min, 0.0F, max, max, 1.0F);
            } else {
                setBlockBounds(0.0F, min, min, 1.0F, max, max);
            }
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
            setBlockBoundsBasedOnState(world, x, y, z);
            return super.getCollisionBoundingBoxFromPool(world, x, y, z);
        }
    }

    /** ItemChorusFruit: eaten even when full, and teleports the eater up to 8 blocks away. */
    public static final class ItemChorusFruit extends ItemFood {
        ItemChorusFruit() {
            super(4, 0.3F, false);
            setAlwaysEdible();
            setUnlocalizedName(MC112Bridge.MOD_ID + ".chorus_fruit");
            setTextureName(MC112Bridge.MOD_ID + ":chorus_fruit");
            setCreativeTab(CreativeTabs.tabMaterials);
        }

        @Override
        protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
            super.onFoodEaten(stack, world, player);
            if (world.isRemote) {
                return;
            }
            for (int i = 0; i < 16; i++) {
                int x = MathHelper.floor_double(player.posX + (world.rand.nextDouble() - 0.5D) * 16.0D);
                int z = MathHelper.floor_double(player.posZ + (world.rand.nextDouble() - 0.5D) * 16.0D);
                int y = MathHelper.floor_double(player.posY) + world.rand.nextInt(16) - 8;
                if (y > 1 && world.getBlock(x, y - 1, z).getMaterial().blocksMovement() && world.isAirBlock(x, y, z) && world.isAirBlock(x, y + 1, z)) {
                    player.setPositionAndUpdate((double) x + 0.5D, (double) y, (double) z + 0.5D);
                    world.playSoundAtEntity(player, "mob.endermen.portal", 1.0F, 1.0F);
                    player.fallDistance = 0.0F;
                    return;
                }
            }
        }
    }

}
