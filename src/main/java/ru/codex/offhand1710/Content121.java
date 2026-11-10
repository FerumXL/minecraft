package ru.codex.offhand1710;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

/** Banner patterns on shields, /title, /stopsound and /locate. */
public final class Content121 {
    private Content121() {
    }

    public static void register() {
        CraftingManager.getInstance().getRecipeList().add(new ShieldPattern());
    }

    /** ShieldRecipes.Decoration: a shield without a pattern takes the look of a banner. */
    public static final class ShieldPattern implements IRecipe {
        private static ItemStack find(InventoryCrafting grid, boolean shield) {
            ItemStack found = null;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null && (stack.getItem() == PortedBlocks.shield) == shield) {
                    if (found != null) {
                        return null;
                    }
                    found = stack;
                }
            }
            return found;
        }

        @Override
        public boolean matches(InventoryCrafting grid, World world) {
            ItemStack shield = find(grid, true);
            ItemStack banner = find(grid, false);
            return shield != null && banner != null && banner.getItem() == Banner112.banner
                    && (!shield.hasTagCompound() || !shield.getTagCompound().hasKey("BlockEntityTag"));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            ItemStack shield = find(grid, true).copy();
            ItemStack banner = find(grid, false);
            shield.stackSize = 1;
            NBTTagCompound inner = banner.hasTagCompound() ? (NBTTagCompound) banner.getTagCompound().getCompoundTag("BlockEntityTag").copy() : new NBTTagCompound();
            inner.setInteger("Base", banner.getItemDamage() & 15);
            if (!shield.hasTagCompound()) {
                shield.setTagCompound(new NBTTagCompound());
            }
            shield.getTagCompound().setTag("BlockEntityTag", inner);
            return shield;
        }

        @Override
        public int getRecipeSize() {
            return 2;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    /** Server to client: action is title, subtitle, actionbar, clear, reset, times or stopsound. */
    public static final class TitleMessage implements IMessage {
        public String action = "";
        public String text = "";
        public int fadeIn;
        public int stay;
        public int fadeOut;

        public TitleMessage() {
        }

        TitleMessage(String action, String text) {
            this(action, text, 0, 0, 0);
        }

        TitleMessage(String action, String text, int fadeIn, int stay, int fadeOut) {
            this.action = action;
            this.text = text;
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            action = ByteBufUtils.readUTF8String(buffer);
            text = ByteBufUtils.readUTF8String(buffer);
            fadeIn = buffer.readInt();
            stay = buffer.readInt();
            fadeOut = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            ByteBufUtils.writeUTF8String(buffer, action);
            ByteBufUtils.writeUTF8String(buffer, text);
            buffer.writeInt(fadeIn);
            buffer.writeInt(stay);
            buffer.writeInt(fadeOut);
        }
    }

    public static final class CommandTitle extends CommandBase {
        @Override
        public String getCommandName() {
            return "title";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/title <player> <title|subtitle|actionbar|clear|reset|times> [text or fadeIn stay fadeOut]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            EntityPlayerMP player = getPlayer(sender, args[0]);
            String action = args[1];
            TitleMessage message;
            if ("clear".equals(action) || "reset".equals(action)) {
                message = new TitleMessage(action, "", 0, 0, 0);
            } else if ("times".equals(action)) {
                if (args.length < 5) {
                    throw new WrongUsageException(getCommandUsage(sender));
                }
                message = new TitleMessage(action, "", parseIntWithMin(sender, args[2], 0), parseIntWithMin(sender, args[3], 0), parseIntWithMin(sender, args[4], 0));
            } else if (("title".equals(action) || "subtitle".equals(action) || "actionbar".equals(action)) && args.length >= 3) {
                String raw = func_82360_a(sender, args, 2);
                IChatComponent component;
                try {
                    component = IChatComponent.Serializer.func_150699_a(raw);
                } catch (RuntimeException e) {
                    component = new ChatComponentText(raw);
                }
                message = new TitleMessage(action, component == null ? raw : component.getFormattedText(), 0, 0, 0);
            } else {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            MC112Bridge.NETWORK.channel.sendTo(message, player);
            sender.addChatMessage(new ChatComponentText("Title command sent"));
        }
    }

    /** Nearest structure in block coordinates, or null. Names as in 1.12.2. */
    public static int[] locate(World world, String name, int blockX, int blockZ) {
        if ("Stronghold".equals(name)) {
            ChunkPosition p = world.findClosestStructure("Stronghold", blockX, 64, blockZ);
            return p == null ? null : new int[] {p.chunkPosX, p.chunkPosZ};
        }
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        for (int r = 0; r <= 1500; r++) {
            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {
                    if (Math.max(Math.abs(a), Math.abs(b)) == r && matches(world, name, cx + a, cz + b)) {
                        return new int[] {(cx + a) * 16 + 8, (cz + b) * 16 + 8};
                    }
                }
            }
        }
        return null;
    }

    private static boolean matches(World world, String name, int x, int z) {
        if ("Monument".equals(name)) {
            return MonumentGenerator.isMonumentChunk(world, x, z);
        }
        if ("Mansion".equals(name)) {
            return MansionGenerator.isGrid(world, x, z);
        }
        if ("EndCity".equals(name)) {
            return End112.isGrid(world, x, z);
        }
        return false;
    }

    public static final class CommandLocate extends CommandBase {
        @Override
        public String getCommandName() {
            return "locate";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/locate <Stronghold|Monument|Mansion|EndCity>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            ChunkCoordinates c = sender.getPlayerCoordinates();
            int[] found = locate(sender.getEntityWorld(), args[0], c.posX, c.posZ);
            sender.addChatMessage(new ChatComponentText(found == null ? "Unable to locate " + args[0] : "The nearest " + args[0] + " is at block " + found[0] + " ~ " + found[1]));
        }
    }

}
