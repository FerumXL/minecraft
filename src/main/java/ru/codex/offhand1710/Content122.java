package ru.codex.offhand1710;

import java.util.Random;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;

/** Chain and repeating command blocks, /stopsound by source and name, the main hand choice. */
public final class Content122 {
    public static final int WATCH_LEFT = 30;
    public static ModeBlock chain;
    public static ModeBlock repeating;
    private static final int MAX_CHAIN = 65536;

    private Content122() {
    }

    public static void register() {
        chain = new ModeBlock("chain_command_block");
        repeating = new ModeBlock("repeating_command_block");
        GameRegistry.registerBlock(chain, "chain_command_block");
        GameRegistry.registerBlock(repeating, "repeating_command_block");
        GameRegistry.registerTileEntity(ModeTile.class, "mc112bridge.mode_command_block");
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    public static final class Events {
        @SubscribeEvent
        public void onConstruct(EntityEvent.EntityConstructing event) {
            if (event.entity instanceof EntityPlayer) {
                event.entity.getDataWatcher().addObject(WATCH_LEFT, Byte.valueOf((byte) 0));
            }
        }
    }

    public static boolean isLeftHanded(EntityPlayer player) {
        return player.getDataWatcher().getWatchableObjectByte(WATCH_LEFT) != 0;
    }

    private static CommandBlockLogic logic(TileEntity tile) {
        return ((TileEntityCommandBlock) tile).func_145993_a();
    }

    private static void clearSuccess(CommandBlockLogic logic) {
        ReflectionHelper.setPrivateValue(CommandBlockLogic.class, logic, Integer.valueOf(0), "successCount", "field_145764_b");
    }

    private static int facing(int meta) {
        return meta & 7;
    }

    private static boolean conditionMet(World world, int x, int y, int z, int meta) {
        if ((meta & 8) == 0) {
            return true;
        }
        int back = Facing.oppositeSide[facing(meta)];
        TileEntity tile = world.getTileEntity(x + Facing.offsetsXForSide[back], y + Facing.offsetsYForSide[back], z + Facing.offsetsZForSide[back]);
        return tile instanceof TileEntityCommandBlock && logic(tile).func_145760_g() > 0;
    }

    private static boolean active(World world, int x, int y, int z, ModeTile tile) {
        return tile.auto || world.isBlockIndirectlyGettingPowered(x, y, z);
    }

    /** Runs the chain blocks that follow (x, y, z) in the given direction. */
    private static void runChain(World world, int x, int y, int z, int direction) {
        for (int n = 0; n < MAX_CHAIN; n++) {
            x += Facing.offsetsXForSide[direction];
            y += Facing.offsetsYForSide[direction];
            z += Facing.offsetsZForSide[direction];
            if (world.getBlock(x, y, z) != chain) {
                return;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ModeTile)) {
                return;
            }
            int meta = world.getBlockMetadata(x, y, z);
            if (active(world, x, y, z, (ModeTile) tile) && conditionMet(world, x, y, z, meta)) {
                logic(tile).func_145755_a(world);
            } else {
                clearSuccess(logic(tile));
            }
            direction = facing(meta);
        }
    }

    /** Called from the end of BlockCommandBlock.updateTick: a plain block starts the chain that faces away from it. */
    public static void afterImpulse(World world, int x, int y, int z) {
        if (world.isRemote || world.getBlock(x, y, z) != Blocks.command_block) {
            return;
        }
        for (int side = 0; side < 6; side++) {
            int a = x + Facing.offsetsXForSide[side];
            int b = y + Facing.offsetsYForSide[side];
            int c = z + Facing.offsetsZForSide[side];
            if (world.getBlock(a, b, c) == chain && facing(world.getBlockMetadata(a, b, c)) == side) {
                runChain(world, x, y, z, side);
                return;
            }
        }
    }

    public static final class ModeTile extends TileEntityCommandBlock {
        public boolean auto;

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setBoolean("auto", auto);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            auto = tag.getBoolean("auto");
        }

        @Override
        public void updateEntity() {
            if (worldObj.isRemote || worldObj.getBlock(xCoord, yCoord, zCoord) != repeating) {
                return;
            }
            int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            if (active(worldObj, xCoord, yCoord, zCoord, this)) {
                if (conditionMet(worldObj, xCoord, yCoord, zCoord, meta)) {
                    func_145993_a().func_145755_a(worldObj);
                } else {
                    clearSuccess(func_145993_a());
                }
                runChain(worldObj, xCoord, yCoord, zCoord, facing(meta));
            }
        }
    }

    /** Metadata: facing in the low bits, 8 = conditional. */
    public static final class ModeBlock extends BlockCommandBlock {
        private final String name;
        private final IIcon[] icons = new IIcon[4];

        ModeBlock(String name) {
            this.name = name;
            setBlockUnbreakable();
            setResistance(6000000.0F);
            setBlockName(MC112Bridge.MOD_ID + "." + name);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new ModeTile();
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            String p = MC112Bridge.MOD_ID + ":" + name + "_";
            icons[0] = register.registerIcon(p + "front");
            icons[1] = register.registerIcon(p + "back");
            icons[2] = register.registerIcon(p + "side");
            icons[3] = register.registerIcon(p + "conditional");
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            int f = facing(meta);
            if (side == f) {
                return icons[0];
            }
            if (side == Facing.oppositeSide[f]) {
                return icons[1];
            }
            return icons[(meta & 8) != 0 ? 3 : 2];
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
        }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            world.setBlockMetadataWithNotify(x, y, z, Facing.oppositeSide[BlockPistonBase.determineOrientation(world, x, y, z, placer)], 2);
            super.onBlockPlacedBy(world, x, y, z, placer, stack);
        }
    }

    /** Client to server: the mode buttons added to the command block screen. mode 0 impulse, 1 chain, 2 repeating. */
    public static final class ModeMessage implements IMessage {
        public int x;
        public int y;
        public int z;
        public int mode;
        public boolean conditional;
        public boolean auto;

        public ModeMessage() {
        }

        public ModeMessage(int x, int y, int z, int mode, boolean conditional, boolean auto) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mode = mode;
            this.conditional = conditional;
            this.auto = auto;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            x = b.readInt();
            y = b.readInt();
            z = b.readInt();
            mode = b.readByte();
            conditional = b.readBoolean();
            auto = b.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(x);
            b.writeInt(y);
            b.writeInt(z);
            b.writeByte(mode);
            b.writeBoolean(conditional);
            b.writeBoolean(auto);
        }
    }

    public static void applyMode(World world, int x, int y, int z, int mode, boolean conditional, boolean auto) {
        TileEntity old = world.getTileEntity(x, y, z);
        if (!(old instanceof TileEntityCommandBlock)) {
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        logic(old).func_145758_a(tag);
        int meta = world.getBlockMetadata(x, y, z);
        int direction = old instanceof ModeTile ? facing(meta) : 1;
        Block block = mode == 1 ? chain : (mode == 2 ? repeating : Blocks.command_block);
        world.setBlock(x, y, z, block, mode == 0 ? 0 : direction | (conditional ? 8 : 0), 3);
        TileEntity now = world.getTileEntity(x, y, z);
        if (now instanceof TileEntityCommandBlock) {
            logic(now).func_145759_b(tag);
            if (now instanceof ModeTile) {
                ((ModeTile) now).auto = auto;
            }
            now.markDirty();
            world.markBlockForUpdate(x, y, z);
        }
    }

    public static final class ModeHandler implements IMessageHandler<ModeMessage, IMessage> {
        @Override
        public IMessage onMessage(ModeMessage m, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (player.capabilities.isCreativeMode && player.canCommandSenderUseCommand(2, "") && player.getDistanceSq(m.x, m.y, m.z) < 100.0D) {
                applyMode(player.worldObj, m.x, m.y, m.z, m.mode, m.conditional, m.auto);
            }
            return null;
        }
    }

    public static final class HandMessage implements IMessage {
        public boolean left;

        public HandMessage() {
        }

        public HandMessage(boolean left) {
            this.left = left;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            left = b.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeBoolean(left);
        }
    }

    public static final class HandHandler implements IMessageHandler<HandMessage, IMessage> {
        @Override
        public IMessage onMessage(HandMessage m, MessageContext context) {
            context.getServerHandler().playerEntity.getDataWatcher().updateObject(WATCH_LEFT, Byte.valueOf((byte) (m.left ? 1 : 0)));
            return null;
        }
    }

    public static final class CommandStopSound extends CommandBase {
        private static final String SOURCES = " master music record weather block hostile neutral player ambient voice ";

        @Override
        public String getCommandName() {
            return "stopsound";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/stopsound <player> [source] [sound]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1 || args.length > 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            String source = args.length > 1 ? args[1] : "";
            if (source.length() > 0 && !SOURCES.contains(" " + source + " ")) {
                throw new WrongUsageException("Unknown sound source " + source);
            }
            MC112Bridge.NETWORK.channel.sendTo(new Content121.TitleMessage("stopsound", source + "|" + (args.length > 2 ? args[2] : "")), getPlayer(sender, args[0]));
        }
    }
}
