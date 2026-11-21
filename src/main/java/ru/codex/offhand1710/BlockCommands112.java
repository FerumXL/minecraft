package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * /fill, /clone, /testforblock and /testforblocks as CommandFill, CommandClone, CommandTestForBlock and CommandCompare
 * of 1.12.2: the argument order, the modes, the filters by block and state, the limits and the messages. Block names
 * and states of 1.12.2 go through Names112.
 */
public final class BlockCommands112 {
    private BlockCommands112() {
    }

    // ---- shared ----

    /** CommandBase.parseBlockPos of 1.12.2 (whole blocks; ~ from the sender's block). */
    static int[] pos(ICommandSender sender, String[] args, int from) {
        ChunkCoordinates base = sender.getPlayerCoordinates();
        return new int[] {
            MathHelper.floor_double(CommandBase.func_110666_a(sender, base.posX, args[from])),
            MathHelper.floor_double(CommandBase.func_110666_a(sender, base.posY, args[from + 1])),
            MathHelper.floor_double(CommandBase.func_110666_a(sender, base.posZ, args[from + 2]))
        };
    }

    /** {Block, meta} of 1.7.10 for a 1.12.2 block and its data or state (null data: the default state). */
    static Object[] state(String name, String data) {
        Object[] b = Names112.OFF ? plain(name, data) : Names112.block(name, data);
        if (b == null) {
            throw new CommandException("commands.give.block.notFound", name);
        }
        return new Object[] {Block.getBlockFromName((String) b[0]), b[1]};
    }

    private static Object[] plain(String name, String data) {
        if (Block.getBlockFromName(name) == null) {
            return null;
        }
        return new Object[] {name, Integer.valueOf(data == null ? 0 : CommandBase.parseIntBounded(null, data, 0, 15))};
    }

    /**
     * CommandBase.convertArgToBlockStatePredicate of 1.12.2 against a 1.7.10 block: null, "-1" and "*" take any
     * metadata of the block, a number one metadata, a state only the properties it names.
     */
    public static boolean matches(String name, String data, Block block, int meta) {
        boolean any = data == null || "-1".equals(data) || "*".equals(data);
        String full = Names112.ns(name);
        java.util.Map<String, String> want = null;
        int number = -1;
        if (!any) {
            if (Names112.isNumber(data)) {
                number = Integer.parseInt(data);
            } else {
                if (States112.metaOf(full, data) == -1) {
                    throw new CommandException("commands.generic.blockstate.invalid", data, full);
                }
                want = new java.util.HashMap<String, String>();
                for (String kv : data.split(",")) {
                    int eq = kv.indexOf('=');
                    if (eq > 0) {
                        want.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                    }
                }
            }
        }
        String here = Block.blockRegistry.getNameForObject(block);
        for (int m = 0; m < 16; m++) {
            if (number >= 0 && m != number) {
                continue;
            }
            if (want != null) {
                java.util.Map<String, String> p = States112.props(full, m);
                if (p == null || !containsAll(full, p, want)) {
                    continue;
                }
            }
            Object[] b;
            try {
                b = Names112.OFF ? plain(name, String.valueOf(m)) : Names112.block(name, String.valueOf(m));
            } catch (CommandException e) {
                continue;
            }
            if (b != null && b[0].equals(here) && (any || ((Integer) b[1]).intValue() == meta)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAll(String full, java.util.Map<String, String> have, java.util.Map<String, String> want) {
        for (java.util.Map.Entry<String, String> e : want.entrySet()) {
            // properties 1.12.2 takes from the neighbours are not in the metadata: they cannot be told apart here
            if (States112.stored(full, e.getKey()) && !e.getValue().equals(have.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    static NBTTagCompound tag(ICommandSender sender, String[] args, int from, String error) {
        try {
            NBTBase t = JsonToNBT.func_150315_a(CommandBase.func_82360_a(sender, args, from));
            if (!(t instanceof NBTTagCompound)) {
                throw new CommandException(error, "Not a valid tag");
            }
            return (NBTTagCompound) t;
        } catch (NBTException e) {
            throw new CommandException(error, e.getMessage());
        }
    }

    static boolean loaded(World w, int x0, int y0, int z0, int x1, int y1, int z1) {
        if (y0 < 0 || y1 >= 256) {
            return false;
        }
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!w.getChunkProvider().chunkExists(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clearInventory(World w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof IInventory) {
            IInventory inv = (IInventory) te;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                inv.setInventorySlotContents(i, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static List<String> blockNames(String[] args) {
        return CommandBase.getListOfStringsFromIterableMatchingLastWord(args, Names112.blockCompletions());
    }

    // ---- /fill ----

    public static final class Fill extends CommandBase {
        @Override
        public String getCommandName() {
            return "fill";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.fill.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 7) {
                throw new WrongUsageException("commands.fill.usage");
            }
            int[] a = pos(sender, args, 0);
            int[] b = pos(sender, args, 3);
            Object[] st = state(args[6], args.length >= 8 ? args[7] : null);
            Block block = (Block) st[0];
            int meta = ((Integer) st[1]).intValue();
            int x0 = Math.min(a[0], b[0]), y0 = Math.min(a[1], b[1]), z0 = Math.min(a[2], b[2]);
            int x1 = Math.max(a[0], b[0]), y1 = Math.max(a[1], b[1]), z1 = Math.max(a[2], b[2]);
            long volume = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
            if (volume > 32768L) {
                throw new CommandException("commands.fill.tooManyBlocks", volume, 32768);
            }
            World w = sender.getEntityWorld();
            if (!loaded(w, x0, y0, z0, x1, y1, z1)) {
                throw new CommandException("commands.fill.outOfWorld");
            }
            NBTTagCompound tag = null;
            if (args.length >= 11 && block.hasTileEntity(meta)) {
                tag = tag(sender, args, 10, "commands.fill.tagError");
            }
            String mode = args.length >= 9 ? args[8] : null;
            List<int[]> changed = new ArrayList<int[]>();
            int count = 0;
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        if (mode != null) {
                            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                            if (!"outline".equals(mode) && !"hollow".equals(mode) || edge) {
                                if ("destroy".equals(mode)) {
                                    w.func_147480_a(x, y, z, true);
                                } else if ("keep".equals(mode)) {
                                    if (!w.isAirBlock(x, y, z)) {
                                        continue;
                                    }
                                } else if ("replace".equals(mode) && !block.hasTileEntity(meta) && args.length > 9) {
                                    if (!matches(args[9], args.length > 10 ? args[10] : null, w.getBlock(x, y, z), w.getBlockMetadata(x, y, z))) {
                                        continue;
                                    }
                                }
                            } else {
                                if ("hollow".equals(mode)) {
                                    w.setBlock(x, y, z, Blocks.air, 0, 2);
                                    changed.add(new int[] {x, y, z});
                                }
                                continue;
                            }
                        }
                        clearInventory(w, x, y, z);
                        if (w.setBlock(x, y, z, block, meta, 2)) {
                            changed.add(new int[] {x, y, z});
                            count++;
                            if (tag != null) {
                                TileEntity te = w.getTileEntity(x, y, z);
                                if (te != null) {
                                    NBTTagCompound t = (NBTTagCompound) tag.copy();
                                    t.setInteger("x", x);
                                    t.setInteger("y", y);
                                    t.setInteger("z", z);
                                    te.readFromNBT(t);
                                }
                            }
                        }
                    }
                }
            }
            for (int[] p : changed) {
                w.notifyBlockChange(p[0], p[1], p[2], w.getBlock(p[0], p[1], p[2]));
            }
            if (count <= 0) {
                throw new CommandException("commands.fill.failed");
            }
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, count);
            func_152373_a(sender, this, "commands.fill.success", count);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 7) {
                return blockNames(args);
            }
            if (args.length == 9) {
                return getListOfStringsMatchingLastWord(args, "replace", "destroy", "keep", "hollow", "outline");
            }
            if (args.length == 10 && "replace".equals(args[8])) {
                return blockNames(args);
            }
            return null;
        }
    }

    // ---- /clone ----

    private static final class Copy {
        final int x, y, z;
        final Block block;
        final int meta;
        final NBTTagCompound tile;

        Copy(int x, int y, int z, Block block, int meta, NBTTagCompound tile) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.tile = tile;
        }
    }

    public static final class Clone extends CommandBase {
        @Override
        public String getCommandName() {
            return "clone";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.clone.usage";
        }

        @Override
        @SuppressWarnings("unchecked")
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 9) {
                throw new WrongUsageException("commands.clone.usage");
            }
            int[] a = pos(sender, args, 0);
            int[] b = pos(sender, args, 3);
            int[] to = pos(sender, args, 6);
            int x0 = Math.min(a[0], b[0]), y0 = Math.min(a[1], b[1]), z0 = Math.min(a[2], b[2]);
            int x1 = Math.max(a[0], b[0]), y1 = Math.max(a[1], b[1]), z1 = Math.max(a[2], b[2]);
            int dx = to[0] - x0, dy = to[1] - y0, dz = to[2] - z0;
            long volume = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
            if (volume > 32768L) {
                throw new CommandException("commands.clone.tooManyBlocks", volume, 32768);
            }
            boolean overlap = x0 <= x1 + dx && x1 >= x0 + dx && y0 <= y1 + dy && y1 >= y0 + dy && z0 <= z1 + dz && z1 >= z0 + dz;
            if ((args.length < 11 || !"force".equals(args[10]) && !"move".equals(args[10])) && overlap) {
                throw new CommandException("commands.clone.noOverlap");
            }
            boolean move = args.length >= 11 && "move".equals(args[10]);
            World w = sender.getEntityWorld();
            if (!loaded(w, x0, y0, z0, x1, y1, z1) || !loaded(w, x0 + dx, y0 + dy, z0 + dz, x1 + dx, y1 + dy, z1 + dz)) {
                throw new CommandException("commands.clone.outOfWorld");
            }
            boolean masked = false;
            String filter = null;
            String filterData = null;
            if (args.length >= 10) {
                if ("masked".equals(args[9])) {
                    masked = true;
                } else if ("filtered".equals(args[9])) {
                    if (args.length < 12) {
                        throw new WrongUsageException("commands.clone.usage");
                    }
                    filter = args[11];
                    state(filter, null);
                    filterData = args.length >= 13 ? args[12] : null;
                }
            }
            List<Copy> full = new ArrayList<Copy>();
            List<Copy> tiles = new ArrayList<Copy>();
            List<Copy> other = new ArrayList<Copy>();
            LinkedList<int[]> sources = new LinkedList<int[]>();
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        Block blk = w.getBlock(x, y, z);
                        int meta = w.getBlockMetadata(x, y, z);
                        if (masked && blk == Blocks.air || filter != null && !matches(filter, filterData, blk, meta)) {
                            continue;
                        }
                        TileEntity te = w.getTileEntity(x, y, z);
                        if (te != null) {
                            NBTTagCompound t = new NBTTagCompound();
                            te.writeToNBT(t);
                            tiles.add(new Copy(x + dx, y + dy, z + dz, blk, meta, t));
                            sources.addLast(new int[] {x, y, z});
                        } else if (blk.isNormalCube()) {
                            full.add(new Copy(x + dx, y + dy, z + dz, blk, meta, null));
                            sources.addLast(new int[] {x, y, z});
                        } else {
                            other.add(new Copy(x + dx, y + dy, z + dz, blk, meta, null));
                            sources.addFirst(new int[] {x, y, z});
                        }
                    }
                }
            }
            Block barrier = Content119.barrier != null ? Content119.barrier : Blocks.bedrock;
            if (move) {
                for (int[] p : sources) {
                    clearInventory(w, p[0], p[1], p[2]);
                    w.setBlock(p[0], p[1], p[2], barrier, 0, 2);
                }
                for (int[] p : sources) {
                    w.setBlock(p[0], p[1], p[2], Blocks.air, 0, 3);
                }
            }
            List<Copy> all = new ArrayList<Copy>(full);
            all.addAll(tiles);
            all.addAll(other);
            List<Copy> reversed = new ArrayList<Copy>(all);
            Collections.reverse(reversed);
            for (Copy c : reversed) {
                clearInventory(w, c.x, c.y, c.z);
                w.setBlock(c.x, c.y, c.z, barrier, 0, 2);
            }
            int count = 0;
            for (Copy c : all) {
                if (w.setBlock(c.x, c.y, c.z, c.block, c.meta, 2)) {
                    count++;
                }
            }
            for (Copy c : tiles) {
                TileEntity te = w.getTileEntity(c.x, c.y, c.z);
                if (c.tile != null && te != null) {
                    c.tile.setInteger("x", c.x);
                    c.tile.setInteger("y", c.y);
                    c.tile.setInteger("z", c.z);
                    te.readFromNBT(c.tile);
                    te.markDirty();
                }
                w.setBlock(c.x, c.y, c.z, c.block, c.meta, 2);
            }
            for (Copy c : reversed) {
                w.notifyBlockChange(c.x, c.y, c.z, c.block);
            }
            // the scheduled ticks inside the box go along (repeaters, water)
            if (w instanceof WorldServer) {
                long now = w.getTotalWorldTime();
                for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
                    for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                        Chunk chunk = w.getChunkFromChunkCoords(cx, cz);
                        List<NextTickListEntry> pending = ((WorldServer) w).getPendingBlockUpdates(chunk, false);
                        if (pending == null) {
                            continue;
                        }
                        for (NextTickListEntry e : pending) {
                            if (e.xCoord >= x0 && e.xCoord <= x1 && e.yCoord >= y0 && e.yCoord <= y1 && e.zCoord >= z0 && e.zCoord <= z1) {
                                ((WorldServer) w).func_147446_b(e.xCoord + dx, e.yCoord + dy, e.zCoord + dz, e.func_151351_a(),
                                    (int) (e.scheduledTime - now), e.priority);
                            }
                        }
                    }
                }
            }
            if (count <= 0) {
                throw new CommandException("commands.clone.failed");
            }
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, count);
            func_152373_a(sender, this, "commands.clone.success", count);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 10) {
                return getListOfStringsMatchingLastWord(args, "replace", "masked", "filtered");
            }
            if (args.length == 11) {
                return getListOfStringsMatchingLastWord(args, "normal", "force", "move");
            }
            if (args.length == 12 && "filtered".equals(args[9])) {
                return blockNames(args);
            }
            return null;
        }
    }

    // ---- /testforblock ----

    public static final class TestForBlock extends CommandBase {
        @Override
        public String getCommandName() {
            return "testforblock";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.testforblock.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 4) {
                throw new WrongUsageException("commands.testforblock.usage");
            }
            int[] p = pos(sender, args, 0);
            Object[] want;
            try {
                want = state(args[3], null);
            } catch (CommandException e) {
                // CommandTestForBlock of 1.12.2 names the block with the key of /setblock
                throw new CommandException("commands.setblock.notFound", args[3]);
            }
            World w = sender.getEntityWorld();
            if (!loaded(w, p[0], p[1], p[2], p[0], p[1], p[2])) {
                throw new CommandException("commands.testforblock.outOfWorld");
            }
            NBTTagCompound tag = args.length >= 6 ? tag(sender, args, 5, "commands.setblock.tagError") : null;
            Block here = w.getBlock(p[0], p[1], p[2]);
            int meta = w.getBlockMetadata(p[0], p[1], p[2]);
            if (!matches(args[3], null, here, meta)) {
                throw new CommandException("commands.testforblock.failed.tile", p[0], p[1], p[2], here.getLocalizedName(), ((Block) want[0]).getLocalizedName());
            }
            if (args.length >= 5 && !matches(args[3], args[4], here, meta)) {
                throw new CommandException("commands.testforblock.failed.data", p[0], p[1], p[2], meta, args[4]);
            }
            if (tag != null) {
                TileEntity te = w.getTileEntity(p[0], p[1], p[2]);
                if (te == null) {
                    throw new CommandException("commands.testforblock.failed.tileEntity", p[0], p[1], p[2]);
                }
                NBTTagCompound have = new NBTTagCompound();
                te.writeToNBT(have);
                if (!Selector112.nbtContains(tag, have)) {
                    throw new CommandException("commands.testforblock.failed.nbt", p[0], p[1], p[2]);
                }
            }
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, 1);
            func_152373_a(sender, this, "commands.testforblock.success", p[0], p[1], p[2]);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 4 ? blockNames(args) : null;
        }
    }

    // ---- /testforblocks ----

    public static final class TestForBlocks extends CommandBase {
        @Override
        public String getCommandName() {
            return "testforblocks";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.compare.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 9) {
                throw new WrongUsageException("commands.compare.usage");
            }
            int[] a = pos(sender, args, 0);
            int[] b = pos(sender, args, 3);
            int[] to = pos(sender, args, 6);
            int x0 = Math.min(a[0], b[0]), y0 = Math.min(a[1], b[1]), z0 = Math.min(a[2], b[2]);
            int x1 = Math.max(a[0], b[0]), y1 = Math.max(a[1], b[1]), z1 = Math.max(a[2], b[2]);
            int dx = to[0] - x0, dy = to[1] - y0, dz = to[2] - z0;
            long volume = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
            if (volume > 524288L) {
                throw new CommandException("commands.compare.tooManyBlocks", volume, 524288);
            }
            World w = sender.getEntityWorld();
            if (!loaded(w, x0, y0, z0, x1, y1, z1) || !loaded(w, x0 + dx, y0 + dy, z0 + dz, x1 + dx, y1 + dy, z1 + dz)) {
                throw new CommandException("commands.compare.outOfWorld");
            }
            boolean masked = args.length > 9 && "masked".equals(args[9]);
            int count = 0;
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        Block blk = w.getBlock(x, y, z);
                        if (masked && blk == Blocks.air) {
                            continue;
                        }
                        if (blk != w.getBlock(x + dx, y + dy, z + dz) || w.getBlockMetadata(x, y, z) != w.getBlockMetadata(x + dx, y + dy, z + dz)) {
                            throw new CommandException("commands.compare.failed");
                        }
                        TileEntity t1 = w.getTileEntity(x, y, z);
                        TileEntity t2 = w.getTileEntity(x + dx, y + dy, z + dz);
                        if (t1 != null) {
                            if (t2 == null) {
                                throw new CommandException("commands.compare.failed");
                            }
                            NBTTagCompound n1 = new NBTTagCompound();
                            NBTTagCompound n2 = new NBTTagCompound();
                            t1.writeToNBT(n1);
                            t2.writeToNBT(n2);
                            for (String k : new String[] {"x", "y", "z"}) {
                                n1.removeTag(k);
                                n2.removeTag(k);
                            }
                            if (!n1.equals(n2)) {
                                throw new CommandException("commands.compare.failed");
                            }
                        } else if (t2 != null) {
                            throw new CommandException("commands.compare.failed");
                        }
                        count++;
                    }
                }
            }
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, count);
            func_152373_a(sender, this, "commands.compare.success", count);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 10 ? getListOfStringsMatchingLastWord(args, "masked", "all") : null;
        }
    }
}
