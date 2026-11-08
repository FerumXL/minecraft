package ru.codex.offhand1710;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/** BlockShulkerBox of 1.11: a chest that keeps its contents when broken. Metadata is the side it was placed on. */
public final class BlockShulkerBox extends BlockContainer {
    public final int color;

    public BlockShulkerBox(int color) {
        super(Material.rock);
        this.color = color;
        setBlockName(MC112Bridge.MOD_ID + ".shulker_box_" + MC112Bridge.COLOR_NAMES[color]);
        setBlockTextureName(MC112Bridge.MOD_ID + ":shulker_top_" + Items112.textureColor(color));
        setCreativeTab(CreativeTabs.tabDecorations);
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new Tile();
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
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        return side;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        int meta = world.getBlockMetadata(x, y, z);
        int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        int[] o = offsets[Math.min(meta, 5)];
        if (world.getBlock(x + o[0], y + o[1], z + o[2]).isNormalCube()) {
            return true; // the lid has no room to rise
        }
        player.openGui(MC112Bridge.instance(), Items112.GUI_BOX, world, x, y, z);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof Tile && stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag")) {
            ((Tile) tile).readItems(stack.getTagCompound().getCompoundTag("BlockEntityTag"));
        }
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (player.capabilities.isCreativeMode && tile instanceof Tile) {
            ((Tile) tile).creativeBroken = ((Tile) tile).isEmpty();
        }
    }

    /** The box always drops itself with its contents, whatever broke it. */
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof Tile && !world.isRemote && !((Tile) tile).creativeBroken) {
            ItemStack drop = new ItemStack(this);
            if (!((Tile) tile).isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound items = new NBTTagCompound();
                ((Tile) tile).writeItems(items);
                tag.setTag("BlockEntityTag", items);
                drop.setTagCompound(tag);
            }
            EntityItem entity = new EntityItem(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, drop);
            entity.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(entity);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    public static final class BoxItem extends ItemBlock {
        public BoxItem(Block block) {
            super(block);
            setMaxStackSize(1);
        }

        /** BlockShulkerBox.addInformation: up to five stacks, then "and N more". */
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("BlockEntityTag")) {
                return;
            }
            NBTTagList items = stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Items", 10);
            int shown = 0;
            int more = 0;
            for (int i = 0; i < items.tagCount(); i++) {
                ItemStack inside = ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i));
                if (inside == null) {
                    continue;
                }
                if (shown < 5) {
                    lines.add(inside.getDisplayName() + " x" + inside.stackSize);
                    shown++;
                } else {
                    more++;
                }
            }
            if (more > 0) {
                lines.add(StatCollector.translateToLocalFormatted("container.shulkerBox.more", Integer.valueOf(more)));
            }
        }
    }

    public static final class Tile extends TileEntity implements IInventory {
        private final ItemStack[] items = new ItemStack[27];
        private int viewers;
        private float progress;
        private float prevProgress;
        boolean creativeBroken;

        public float progress(float partial) {
            return prevProgress + (progress - prevProgress) * partial;
        }

        boolean isEmpty() {
            for (ItemStack stack : items) {
                if (stack != null) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void updateEntity() {
            prevProgress = progress;
            if (viewers > 0) {
                progress = Math.min(1.0F, progress + 0.1F);
            } else {
                progress = Math.max(0.0F, progress - 0.1F);
            }
        }

        @Override
        public boolean receiveClientEvent(int id, int value) {
            if (id == 1) {
                viewers = value;
                return true;
            }
            return super.receiveClientEvent(id, value);
        }

        void writeItems(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setByte("Slot", (byte) i);
                    items[i].writeToNBT(entry);
                    list.appendTag(entry);
                }
            }
            tag.setTag("Items", list);
        }

        void readItems(NBTTagCompound tag) {
            NBTTagList list = tag.getTagList("Items", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                int slot = entry.getByte("Slot") & 255;
                if (slot < items.length) {
                    items[slot] = ItemStack.loadItemStackFromNBT(entry);
                }
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            writeItems(tag);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            readItems(tag);
        }

        @Override
        public Packet getDescriptionPacket() {
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, new NBTTagCompound());
        }

        @Override
        public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        }

        @Override
        public int getSizeInventory() {
            return 27;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int count) {
            if (items[slot] == null) {
                return null;
            }
            ItemStack result;
            if (items[slot].stackSize <= count) {
                result = items[slot];
                items[slot] = null;
            } else {
                result = items[slot].splitStack(count);
            }
            markDirty();
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            ItemStack stack = items[slot];
            items[slot] = null;
            return stack;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            items[slot] = stack;
            markDirty();
        }

        @Override
        public String getInventoryName() {
            return getBlockType().getLocalizedName();
        }

        @Override
        public boolean hasCustomInventoryName() {
            return true;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                    && player.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64.0D;
        }

        private void sendViewers() {
            worldObj.addBlockEvent(xCoord, yCoord, zCoord, getBlockType(), 1, viewers);
        }

        @Override
        public void openInventory() {
            if (worldObj.isRemote) {
                return;
            }
            if (viewers++ == 0) {
                worldObj.playSoundEffect((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D, MC112Bridge.MOD_ID + ":block.shulker_box.open", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }
            sendViewers();
        }

        @Override
        public void closeInventory() {
            if (worldObj.isRemote) {
                return;
            }
            if (--viewers <= 0) {
                viewers = 0;
                worldObj.playSoundEffect((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D, MC112Bridge.MOD_ID + ":block.shulker_box.close", 0.5F, worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }
            sendViewers();
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return !(Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox);
        }
    }

    /** ContainerShulkerBox: 3 rows, a box does not go inside another box. */
    public static final class BoxContainer extends Container {
        private final IInventory box;

        public BoxContainer(InventoryPlayer player, IInventory box) {
            this.box = box;
            box.openInventory();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlotToContainer(new Slot(box, col + row * 9, 8 + col * 18, 18 + row * 18) {
                        @Override
                        public boolean isItemValid(ItemStack stack) {
                            return inventory.isItemValidForSlot(getSlotIndex(), stack);
                        }
                    });
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(player, col, 8 + col * 18, 142));
            }
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return box.isUseableByPlayer(player);
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer player, int index) {
            Slot slot = (Slot) inventorySlots.get(index);
            if (slot == null || !slot.getHasStack()) {
                return null;
            }
            ItemStack stack = slot.getStack();
            ItemStack copy = stack.copy();
            if (index < 27) {
                if (!mergeItemStack(stack, 27, 63, true)) {
                    return null;
                }
            } else if (Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox || !mergeItemStack(stack, 0, 27, false)) {
                return null;
            }
            if (stack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            return copy;
        }

        @Override
        public void onContainerClosed(EntityPlayer player) {
            super.onContainerClosed(player);
            box.closeInventory();
        }
    }

    /** ShulkerBoxRecipes.ShulkerBoxColoring: any box and one dye, contents are kept. */
    public static final class DyeRecipe implements IRecipe {
        private ItemStack box(InventoryCrafting grid) {
            ItemStack box = null;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null && Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox) {
                    box = stack;
                }
            }
            return box;
        }

        private ItemStack dye(InventoryCrafting grid) {
            ItemStack dye = null;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null && stack.getItem() == Items.dye) {
                    dye = stack;
                }
            }
            return dye;
        }

        @Override
        public boolean matches(InventoryCrafting grid, World world) {
            int count = 0;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                if (grid.getStackInSlot(i) != null) {
                    count++;
                }
            }
            return count == 2 && box(grid) != null && dye(grid) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            ItemStack result = new ItemStack(Items112.BOXES[15 - dye(grid).getItemDamage()]);
            ItemStack old = box(grid);
            if (old.hasTagCompound()) {
                result.setTagCompound((NBTTagCompound) old.getTagCompound().copy());
            }
            return result;
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

}
