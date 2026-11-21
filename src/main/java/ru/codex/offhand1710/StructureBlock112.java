package ru.codex.offhand1710;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * The structure block and structure void of 1.12.2 (BlockStructure, TileEntityStructure, Template, BlockStructureVoid).
 * Modes by metadata: 0 save, 1 load, 2 corner, 3 data. Saved structures go to <world>/structures/<name>.nbt with the
 * keys of a 1.12.2 template (size, palette, blocks, entities, author, DataVersion); a palette entry carries the 1.7.10
 * block name and its "Meta". Only operators in creative mode open and use it, as in 1.12.2.
 */
public final class StructureBlock112 {
    public static Block structureBlock;
    public static Block structureVoid;
    public static final String[] MODES = {"SAVE", "LOAD", "CORNER", "DATA"};
    public static final String[] ROTATIONS = {"NONE", "CLOCKWISE_90", "CLOCKWISE_180", "COUNTERCLOCKWISE_90"};
    public static final String[] MIRRORS = {"NONE", "LEFT_RIGHT", "FRONT_BACK"};
    public static final int GUI = 2;

    private StructureBlock112() {
    }

    public static void register() {
        structureBlock = new BlockStructure();
        GameRegistry.registerBlock(structureBlock, "structure_block");
        GameRegistry.registerTileEntity(Tile.class, "mc112bridge.structure_block");
        structureVoid = new BlockVoid();
        GameRegistry.registerBlock(structureVoid, "structure_void");
    }

    public static boolean mayUse(EntityPlayer p) {
        if (!p.capabilities.isCreativeMode) {
            return false;
        }
        return !(p instanceof EntityPlayerMP) || ((EntityPlayerMP) p).canCommandSenderUseCommand(2, "");
    }

    // ------------------------------------------------------------------------------------------------- blocks

    public static final class BlockStructure extends BlockContainer {
        private final IIcon[] icons = new IIcon[4];

        BlockStructure() {
            super(Material.iron);
            setBlockName(MC112Bridge.MOD_ID + ".structure_block");
            setBlockUnbreakable();
            setResistance(6000000.0F);
            setBlockTextureName(MC112Bridge.MOD_ID + ":structure_block");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new Tile();
        }

        @Override
        public void registerBlockIcons(IIconRegister reg) {
            blockIcon = reg.registerIcon(MC112Bridge.MOD_ID + ":structure_block");
            String[] n = {"save", "load", "corner", "data"};
            for (int i = 0; i < 4; i++) {
                icons[i] = reg.registerIcon(MC112Bridge.MOD_ID + ":structure_block_" + n[i]);
            }
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            return meta >= 0 && meta < 4 ? icons[meta] : blockIcon;
        }

        /** BlockStructure.getStateForPlacement: a new block is in DATA mode; createdBy records the author. */
        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            world.setBlockMetadataWithNotify(x, y, z, 3, 2);
            TileEntity t = world.getTileEntity(x, y, z);
            if (t instanceof Tile && placer instanceof EntityPlayer) {
                ((Tile) t).author = placer.getCommandSenderName();
                ((Tile) t).mode = 3;
            }
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
            if (!mayUse(player)) {
                return false;
            }
            TileEntity t = world.getTileEntity(x, y, z);
            if (world.isRemote && t instanceof Tile) {
                MC112Bridge.proxy.openStructure((Tile) t);
            }
            return true;
        }

        /** BlockStructure.neighborChanged: on a rising redstone edge SAVE saves and LOAD loads. */
        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
            if (world.isRemote) {
                return;
            }
            TileEntity t = world.getTileEntity(x, y, z);
            if (!(t instanceof Tile)) {
                return;
            }
            Tile tile = (Tile) t;
            boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
            if (powered && !tile.powered) {
                if (tile.mode == 0) {
                    tile.save(true);
                } else if (tile.mode == 1) {
                    tile.load(true);
                }
            }
            tile.powered = powered;
        }
    }

    /** BlockStructureVoid: invisible, no collision, replaceable; a saved structure leaves its cells out. */
    public static final class BlockVoid extends Block {
        BlockVoid() {
            super(Material.air);
            setBlockName(MC112Bridge.MOD_ID + ".structure_void");
            setBlockTextureName(MC112Bridge.MOD_ID + ":structure_void");
            setBlockBounds(0.3F, 0.3F, 0.3F, 0.7F, 0.7F, 0.7F);
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
            return -1;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
            return null;
        }

        @Override
        public boolean isReplaceable(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------- tile

    public static final class Tile extends TileEntity {
        public String name = "", author = "", metadata = "";
        public int posX, posY = 1, posZ, sizeX, sizeY, sizeZ;
        public int rotation, mirror, mode = 3;
        public boolean ignoreEntities = true, powered, showAir, showBoundingBox = true;
        public float integrity = 1.0F;
        public long seed;

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            writeFields(tag);
        }

        public void writeFields(NBTTagCompound tag) {
            tag.setString("name", name);
            tag.setString("author", author);
            tag.setString("metadata", metadata);
            tag.setInteger("posX", posX);
            tag.setInteger("posY", posY);
            tag.setInteger("posZ", posZ);
            tag.setInteger("sizeX", sizeX);
            tag.setInteger("sizeY", sizeY);
            tag.setInteger("sizeZ", sizeZ);
            tag.setString("rotation", ROTATIONS[rotation]);
            tag.setString("mirror", MIRRORS[mirror]);
            tag.setString("mode", MODES[mode]);
            tag.setBoolean("ignoreEntities", ignoreEntities);
            tag.setBoolean("powered", powered);
            tag.setBoolean("showair", showAir);
            tag.setBoolean("showboundingbox", showBoundingBox);
            tag.setFloat("integrity", integrity);
            tag.setLong("seed", seed);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            readFields(tag);
        }

        public void readFields(NBTTagCompound tag) {
            name = tag.getString("name");
            author = tag.getString("author");
            metadata = tag.getString("metadata");
            posX = clamp(tag.getInteger("posX"), -32, 32);
            posY = clamp(tag.getInteger("posY"), -32, 32);
            posZ = clamp(tag.getInteger("posZ"), -32, 32);
            sizeX = clamp(tag.getInteger("sizeX"), 0, 32);
            sizeY = clamp(tag.getInteger("sizeY"), 0, 32);
            sizeZ = clamp(tag.getInteger("sizeZ"), 0, 32);
            rotation = Math.max(0, java.util.Arrays.asList(ROTATIONS).indexOf(tag.getString("rotation")));
            mirror = Math.max(0, java.util.Arrays.asList(MIRRORS).indexOf(tag.getString("mirror")));
            int m = java.util.Arrays.asList(MODES).indexOf(tag.getString("mode"));
            mode = m < 0 ? 3 : m;
            ignoreEntities = tag.getBoolean("ignoreEntities");
            powered = tag.getBoolean("powered");
            showAir = tag.getBoolean("showair");
            showBoundingBox = tag.getBoolean("showboundingbox");
            integrity = tag.hasKey("integrity") ? Math.max(0.0F, Math.min(1.0F, tag.getFloat("integrity"))) : 1.0F;
            seed = tag.getLong("seed");
        }

        private static int clamp(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 7, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
        }

        @Override
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        @Override
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public double getMaxRenderDistanceSquared() {
            return 96.0D * 96.0D;
        }

        void changed() {
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != mode) {
                worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, mode, 2);
            }
        }

        File file() {
            File dir = new File(((WorldServer) worldObj).getChunkSaveLocation(), "structures");
            String safe = name.replaceAll("[^a-zA-Z0-9_.\\-/:]", "_");
            safe = safe.contains(":") ? safe.substring(safe.indexOf(':') + 1) : safe;
            return new File(dir, safe.replace('/', File.separatorChar) + ".nbt");
        }

        /** TileEntityStructure.save -> Template.takeBlocksFromWorld + TemplateManager.writeTemplate. */
        public boolean save(boolean writeFile) {
            if (mode != 0 || worldObj.isRemote || name.isEmpty() || sizeX < 1 || sizeY < 1 || sizeZ < 1) {
                return false;
            }
            int ox = xCoord + posX, oy = yCoord + posY, oz = zCoord + posZ;
            NBTTagCompound t = new NBTTagCompound();
            NBTTagList palette = new NBTTagList();
            Map<String, Integer> index = new HashMap<String, Integer>();
            NBTTagList blocks = new NBTTagList();
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    for (int z = 0; z < sizeZ; z++) {
                        Block b = worldObj.getBlock(ox + x, oy + y, oz + z);
                        if (b == structureVoid) {
                            continue;
                        }
                        int meta = worldObj.getBlockMetadata(ox + x, oy + y, oz + z);
                        String id = Block.blockRegistry.getNameForObject(b);
                        String key = id + "#" + meta;
                        Integer state = index.get(key);
                        if (state == null) {
                            state = index.size();
                            index.put(key, state);
                            NBTTagCompound p = new NBTTagCompound();
                            p.setString("Name", id);
                            p.setInteger("Meta", meta);
                            palette.appendTag(p);
                        }
                        NBTTagCompound e = new NBTTagCompound();
                        e.setTag("pos", ints(x, y, z));
                        e.setInteger("state", state);
                        TileEntity te = worldObj.getTileEntity(ox + x, oy + y, oz + z);
                        if (te != null) {
                            NBTTagCompound nbt = new NBTTagCompound();
                            te.writeToNBT(nbt);
                            nbt.removeTag("x");
                            nbt.removeTag("y");
                            nbt.removeTag("z");
                            e.setTag("nbt", nbt);
                        }
                        blocks.appendTag(e);
                    }
                }
            }
            NBTTagList entities = new NBTTagList();
            if (!ignoreEntities) {
                AxisAlignedBB box = AxisAlignedBB.getBoundingBox(ox, oy, oz, ox + sizeX, oy + sizeY, oz + sizeZ);
                for (Object o : worldObj.getEntitiesWithinAABBExcludingEntity(null, box)) {
                    Entity en = (Entity) o;
                    if (en instanceof EntityPlayer) {
                        continue;
                    }
                    NBTTagCompound nbt = new NBTTagCompound();
                    if (!en.writeToNBTOptional(nbt)) {
                        continue;
                    }
                    NBTTagCompound e = new NBTTagCompound();
                    NBTTagList pos = new NBTTagList();
                    pos.appendTag(new NBTTagDouble(en.posX - ox));
                    pos.appendTag(new NBTTagDouble(en.posY - oy));
                    pos.appendTag(new NBTTagDouble(en.posZ - oz));
                    e.setTag("pos", pos);
                    e.setTag("blockPos", ints((int) Math.floor(en.posX) - ox, (int) Math.floor(en.posY) - oy, (int) Math.floor(en.posZ) - oz));
                    e.setTag("nbt", nbt);
                    entities.appendTag(e);
                }
            }
            t.setTag("size", ints(sizeX, sizeY, sizeZ));
            t.setTag("palette", palette);
            t.setTag("blocks", blocks);
            t.setTag("entities", entities);
            t.setString("author", author);
            t.setInteger("DataVersion", 1343);
            t.setString("mc112bridgeFormat", "1.7.10 block names and metadata");
            if (writeFile) {
                try {
                    File f = file();
                    f.getParentFile().mkdirs();
                    FileOutputStream out = new FileOutputStream(f);
                    try {
                        CompressedStreamTools.writeCompressed(t, out);
                    } finally {
                        out.close();
                    }
                } catch (Exception ex) {
                    return false;
                }
            }
            return true;
        }

        /** TileEntityStructure.load -> Template.addBlocksToWorld with mirror, rotation, integrity and seed. */
        public boolean load(boolean fromFile) {
            if (mode != 1 || worldObj.isRemote || name.isEmpty()) {
                return false;
            }
            NBTTagCompound t;
            try {
                File f = file();
                if (!f.exists()) {
                    return false;
                }
                FileInputStream in = new FileInputStream(f);
                try {
                    t = CompressedStreamTools.readCompressed(in);
                } finally {
                    in.close();
                }
            } catch (Exception ex) {
                return false;
            }
            NBTTagList sz = t.getTagList("size", 3);
            sizeX = ((NBTTagInt) sz.removeTag(0)).func_150287_d();
            sizeY = ((NBTTagInt) sz.removeTag(0)).func_150287_d();
            sizeZ = ((NBTTagInt) sz.removeTag(0)).func_150287_d();
            NBTTagList palette = t.getTagList("palette", 10);
            Block[] pb = new Block[palette.tagCount()];
            int[] pm = new int[palette.tagCount()];
            for (int i = 0; i < pb.length; i++) {
                NBTTagCompound p = palette.getCompoundTagAt(i);
                Block b = Block.getBlockFromName(p.getString("Name"));
                pb[i] = b == null ? Blocks.air : b;
                pm[i] = p.getInteger("Meta");
            }
            int ox = xCoord + posX, oy = yCoord + posY, oz = zCoord + posZ;
            Random rand = seed == 0L ? new Random() : new Random(seed);
            NBTTagList blocks = t.getTagList("blocks", 10);
            List<int[]> placed = new ArrayList<int[]>();
            for (int i = 0; i < blocks.tagCount(); i++) {
                NBTTagCompound e = blocks.getCompoundTagAt(i);
                if (integrity < 1.0F && rand.nextFloat() > integrity) {
                    continue;
                }
                // getIntArray of 1.7.10 throws on a tag of another type: 1.12.2 writes pos as a list of ints
                int[] rel = e.func_150299_b("pos") == 11 ? e.getIntArray("pos") : new int[0];
                if (rel.length < 3) {
                    NBTTagList l = e.getTagList("pos", 3);
                    rel = new int[] {((NBTTagInt) l.removeTag(0)).func_150287_d(), ((NBTTagInt) l.removeTag(0)).func_150287_d(), ((NBTTagInt) l.removeTag(0)).func_150287_d()};
                }
                int[] w = transform(rel[0], rel[1], rel[2]);
                int x = ox + w[0], y = oy + w[1], z = oz + w[2];
                int s = e.getInteger("state");
                Block b = s >= 0 && s < pb.length ? pb[s] : Blocks.air;
                worldObj.setBlock(x, y, z, b, s >= 0 && s < pm.length ? pm[s] & 15 : 0, 2);
                if (e.hasKey("nbt")) {
                    TileEntity te = worldObj.getTileEntity(x, y, z);
                    if (te != null) {
                        NBTTagCompound nbt = (NBTTagCompound) e.getCompoundTag("nbt").copy();
                        nbt.setInteger("x", x);
                        nbt.setInteger("y", y);
                        nbt.setInteger("z", z);
                        te.readFromNBT(nbt);
                        te.markDirty();
                    }
                }
                placed.add(new int[] {x, y, z});
            }
            // Block.rotateBlock turns the 1.7.10 metadata of stairs, logs, doors, torches and the like
            for (int[] p : placed) {
                Block b = worldObj.getBlock(p[0], p[1], p[2]);
                for (int r = 0; r < rotation; r++) {
                    b.rotateBlock(worldObj, p[0], p[1], p[2], ForgeDirection.UP);
                }
                worldObj.markBlockForUpdate(p[0], p[1], p[2]);
            }
            if (!ignoreEntities) {
                NBTTagList entities = t.getTagList("entities", 10);
                for (int i = 0; i < entities.tagCount(); i++) {
                    NBTTagCompound e = entities.getCompoundTagAt(i);
                    NBTTagList pos = e.getTagList("pos", 6);
                    double ex = pos.func_150309_d(0), ey = pos.func_150309_d(1), ez = pos.func_150309_d(2);
                    double[] w = transformD(ex, ey, ez);
                    NBTTagCompound nbt = (NBTTagCompound) e.getCompoundTag("nbt").copy();
                    nbt.removeTag("UUIDMost");
                    nbt.removeTag("UUIDLeast");
                    Entity en = EntityList.createEntityFromNBT(nbt, worldObj);
                    if (en != null) {
                        en.setLocationAndAngles(ox + w[0], oy + w[1], oz + w[2], en.rotationYaw + 90.0F * rotation, en.rotationPitch);
                        worldObj.spawnEntityInWorld(en);
                    }
                }
            }
            changed();
            return true;
        }

        /** Template.transformedBlockPos: mirror first (LEFT_RIGHT flips z, FRONT_BACK flips x), then rotation. */
        int[] transform(int x, int y, int z) {
            if (mirror == 1) {
                z = -z;
            } else if (mirror == 2) {
                x = -x;
            }
            switch (rotation) {
                case 1:
                    return new int[] {-z, y, x};
                case 2:
                    return new int[] {-x, y, -z};
                case 3:
                    return new int[] {z, y, -x};
                default:
                    return new int[] {x, y, z};
            }
        }

        double[] transformD(double x, double y, double z) {
            if (mirror == 1) {
                z = 1.0D - z;
            } else if (mirror == 2) {
                x = 1.0D - x;
            }
            switch (rotation) {
                case 1:
                    return new double[] {1.0D - z, y, x};
                case 2:
                    return new double[] {1.0D - x, y, 1.0D - z};
                case 3:
                    return new double[] {z, y, 1.0D - x};
                default:
                    return new double[] {x, y, z};
            }
        }

        /** TileEntityStructure.detectSize: the corner blocks of the same name within 80 blocks frame the structure. */
        public boolean detectSize() {
            if (mode != 0 || name.isEmpty()) {
                return false;
            }
            List<int[]> corners = new ArrayList<int[]>();
            // this tile does not tick, so 1.7.10 keeps it out of loadedTileEntityList: look in the loaded chunks around
            for (int cx = (xCoord - 80) >> 4; cx <= (xCoord + 80) >> 4; cx++) {
                for (int cz = (zCoord - 80) >> 4; cz <= (zCoord + 80) >> 4; cz++) {
                    if (!worldObj.getChunkProvider().chunkExists(cx, cz)) {
                        continue;
                    }
                    for (Object o : worldObj.getChunkFromChunkCoords(cx, cz).chunkTileEntityMap.values()) {
                        if (o instanceof Tile && o != this) {
                            Tile c = (Tile) o;
                            if (c.mode == 2 && name.equals(c.name) && Math.abs(c.xCoord - xCoord) <= 80 && Math.abs(c.yCoord - yCoord) <= 80 && Math.abs(c.zCoord - zCoord) <= 80) {
                                corners.add(new int[] {c.xCoord, c.yCoord, c.zCoord});
                            }
                        }
                    }
                }
            }
            if (corners.isEmpty()) {
                return false;
            }
            int[] lo = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, hi = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            if (corners.size() == 1) {
                corners.add(new int[] {xCoord, yCoord, zCoord});
            }
            for (int[] c : corners) {
                for (int k = 0; k < 3; k++) {
                    lo[k] = Math.min(lo[k], c[k]);
                    hi[k] = Math.max(hi[k], c[k]);
                }
            }
            if (hi[0] - lo[0] > 1 && hi[1] - lo[1] > 1 && hi[2] - lo[2] > 1) {
                posX = lo[0] - xCoord + 1;
                posY = lo[1] - yCoord + 1;
                posZ = lo[2] - zCoord + 1;
                sizeX = hi[0] - lo[0] - 1;
                sizeY = hi[1] - lo[1] - 1;
                sizeZ = hi[2] - lo[2] - 1;
                changed();
                return true;
            }
            return false;
        }
    }

    static NBTTagList ints(int... v) {
        NBTTagList l = new NBTTagList();
        for (int i : v) {
            l.appendTag(new NBTTagInt(i));
        }
        return l;
    }

    // ------------------------------------------------------------------------------------------------- packet

    /** Client -> server: the fields of the window and what to do (0 only update, 1 save, 2 load, 3 detect size). */
    public static final class Msg implements IMessage {
        public int x, y, z, action;
        public NBTTagCompound fields;

        public Msg() {
        }

        public Msg(Tile t, int action) {
            x = t.xCoord;
            y = t.yCoord;
            z = t.zCoord;
            this.action = action;
            fields = new NBTTagCompound();
            t.writeFields(fields);
        }

        @Override
        public void fromBytes(ByteBuf b) {
            x = b.readInt();
            y = b.readInt();
            z = b.readInt();
            action = b.readByte();
            fields = ByteBufUtils.readTag(b);
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(x);
            b.writeInt(y);
            b.writeInt(z);
            b.writeByte(action);
            ByteBufUtils.writeTag(b, fields);
        }
    }

    public static final class Handler implements IMessageHandler<Msg, IMessage> {
        @Override
        public IMessage onMessage(Msg m, MessageContext context) {
            EntityPlayerMP p = context.getServerHandler().playerEntity;
            if (!mayUse(p) || p.getDistanceSq(m.x + 0.5D, m.y + 0.5D, m.z + 0.5D) > 64.0D * 64.0D) {
                return null;
            }
            TileEntity te = p.worldObj.getTileEntity(m.x, m.y, m.z);
            if (!(te instanceof Tile) || m.fields == null) {
                return null;
            }
            Tile t = (Tile) te;
            boolean powered = t.powered;
            String author = t.author;
            t.readFields(m.fields);
            t.powered = powered;
            t.author = author;
            t.changed();
            String n = t.name;
            if (m.action == 1) {
                p.addChatMessage(new ChatComponentTranslation(t.save(true) ? "structure_block.save_success" : "structure_block.save_failure", n));
            } else if (m.action == 2) {
                p.addChatMessage(new ChatComponentTranslation(t.load(true) ? "structure_block.load_success" : "structure_block.load_not_found", n));
            } else if (m.action == 3) {
                p.addChatMessage(new ChatComponentTranslation(t.detectSize() ? "structure_block.size_success" : "structure_block.size_failure", n));
            }
            return null;
        }
    }
}
