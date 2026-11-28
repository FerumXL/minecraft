package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * The enchanting table of 1.8 - 1.12.2 (ContainerEnchantment, EntityPlayer.xpSeed): a lapis slot, a seed per player
 * that changes only after enchanting (the offers no longer change when the item is put back), a clue of one
 * enchantment for each offer, and a cost of 1 - 3 levels and 1 - 3 lapis lazuli while the level of the offer is only
 * a requirement. -Dmc112.enchant17 leaves the table of 1.7.10.
 */
public final class Enchant112 {
    public static final boolean OFF = System.getProperty("mc112.enchant17") != null;
    private static final String SEED = "mc112XpSeed";

    public Enchant112() {
    }

    // ---- EntityPlayer.xpSeed (kept in the persisted Forge data, so it survives death as in 1.12.2) ----

    public static int xpSeed(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound kept = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!kept.hasKey(SEED)) {
            kept.setInteger(SEED, player.getRNG().nextInt());
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, kept);
        }
        return kept.getInteger(SEED);
    }

    /** EntityPlayer.onEnchant of 1.12.2: the levels paid and a new seed. */
    static void onEnchant(EntityPlayer player, int levels) {
        // addExperienceLevel clamps at 0 as onEnchant does, and EntityPlayerMP then sends the new level to the client
        player.addExperienceLevel(-levels);
        NBTTagCompound data = player.getEntityData();
        NBTTagCompound kept = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        kept.setInteger(SEED, player.getRNG().nextInt());
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, kept);
    }

    // ---- EnchantmentHelper of 1.12.2 ----

    /** EnchantmentHelper.buildEnchantmentList of 1.12.2 without treasure enchantments. */
    public static List<EnchantmentData> buildEnchantmentList(Random rand, ItemStack stack, int level) {
        List<EnchantmentData> list = new ArrayList<EnchantmentData>();
        int ability = stack.getItem().getItemEnchantability(stack);
        if (ability <= 0) {
            return list;
        }
        level = level + 1 + rand.nextInt(ability / 4 + 1) + rand.nextInt(ability / 4 + 1);
        float f = (rand.nextFloat() + rand.nextFloat() - 1.0F) * 0.15F;
        level = Math.max(1, Math.round((float) level + (float) level * f));
        List<EnchantmentData> candidates = datas(level, stack);
        if (!candidates.isEmpty()) {
            list.add((EnchantmentData) WeightedRandom.getRandomItem(rand, candidates));
            while (rand.nextInt(50) <= level) {
                Enchantment last = list.get(list.size() - 1).enchantmentobj;
                for (Iterator<EnchantmentData> it = candidates.iterator(); it.hasNext();) {
                    Enchantment e = it.next().enchantmentobj;
                    if (e == last || !last.canApplyTogether(e) || !e.canApplyTogether(last)) {
                        it.remove();
                    }
                }
                if (candidates.isEmpty()) {
                    break;
                }
                list.add((EnchantmentData) WeightedRandom.getRandomItem(rand, candidates));
                level /= 2;
            }
        }
        return list;
    }

    /** EnchantmentHelper.getEnchantmentDatas of 1.12.2 (allowTreasure false): the highest level each one reaches. */
    @SuppressWarnings("unchecked")
    static List<EnchantmentData> datas(int level, ItemStack stack) {
        List<EnchantmentData> out = new ArrayList<EnchantmentData>();
        Map<Integer, EnchantmentData> map = EnchantmentHelper.mapEnchantmentData(level, stack);
        if (map != null) {
            for (EnchantmentData d : map.values()) {
                if (!Content119.isTreasure(d.enchantmentobj)) {
                    out.add(d);
                }
            }
        }
        return out;
    }

    // ---- opening the table ----

    /** BlockEnchantmentTable.onBlockActivated, on the server: the window of 1.12.2 instead of that of 1.7.10. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (OFF || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || player.worldObj.isRemote || !(player instanceof EntityPlayerMP)) {
            return;
        }
        World world = player.worldObj;
        if (!(world.getBlock(event.x, event.y, event.z) instanceof BlockEnchantmentTable)) {
            return;
        }
        ItemStack held = player.getHeldItem();
        boolean useBlock = !player.isSneaking() || held == null || held.getItem().doesSneakBypassUse(world, event.x, event.y, event.z, player);
        if (!useBlock) {
            return;
        }
        event.setCanceled(true);
        TileEntity tile = world.getTileEntity(event.x, event.y, event.z);
        String name = tile instanceof TileEntityEnchantmentTable && ((TileEntityEnchantmentTable) tile).func_145921_b() ? ((TileEntityEnchantmentTable) tile).func_145919_a() : "";
        MC112Bridge.NETWORK.channel.sendTo(new Name(name), (EntityPlayerMP) player);
        player.openGui(MC112Bridge.instance(), Items112.GUI_ENCHANT, world, event.x, event.y, event.z);
    }

    /** The custom name of the table (from an anvil), sent before the window opens: FML does not carry it. */
    public static final class Name implements IMessage {
        public String name = "";

        public Name() {
        }

        Name(String name) {
            this.name = name;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            name = cpw.mods.fml.common.network.ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String(buf, name);
        }
    }

    // ---- ContainerEnchantment of 1.12.2 ----

    public static final class Table extends Container {
        public final InventoryBasic table = new InventoryBasic("Enchant", true, 2) {
            @Override
            public int getInventoryStackLimit() {
                return 64;
            }

            @Override
            public void markDirty() {
                super.markDirty();
                onCraftMatrixChanged(this);
            }
        };
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final Random rand = new Random();
        public int xpSeed;
        public final int[] enchantLevels = new int[3];
        public final int[] enchantClue = {-1, -1, -1};
        public final int[] worldClue = {-1, -1, -1};
        private final int[] sent = new int[10];

        public Table(InventoryPlayer inventory, World world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.xpSeed = xpSeed(inventory.player);
            addSlotToContainer(new Slot(table, 0, 15, 47) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return true;
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }
            });
            addSlotToContainer(new Slot(table, 1, 35, 47) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return isLapis(stack);
                }
            });
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
                }
            }
            for (int k = 0; k < 9; ++k) {
                addSlotToContainer(new Slot(inventory, k, 8 + k * 18, 142));
            }
            java.util.Arrays.fill(sent, Integer.MIN_VALUE);
        }

        /** Forge 1.12.2: anything in the gemLapis ore dictionary entry (the blue dye of vanilla). */
        public static boolean isLapis(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            if (stack.getItem() == Items.dye && stack.getItemDamage() == 4) {
                return true;
            }
            for (int id : net.minecraftforge.oredict.OreDictionary.getOreIDs(stack)) {
                if ("gemLapis".equals(net.minecraftforge.oredict.OreDictionary.getOreName(id))) {
                    return true;
                }
            }
            return false;
        }

        public int lapisAmount() {
            ItemStack s = table.getStackInSlot(1);
            return s == null ? 0 : s.stackSize;
        }

        private int[] values() {
            return new int[] {enchantLevels[0], enchantLevels[1], enchantLevels[2], xpSeed & -16, enchantClue[0], enchantClue[1], enchantClue[2],
                worldClue[0], worldClue[1], worldClue[2]};
        }

        @Override
        public void addCraftingToCrafters(ICrafting listener) {
            super.addCraftingToCrafters(listener);
            int[] v = values();
            for (int i = 0; i < v.length; i++) {
                listener.sendProgressBarUpdate(this, i, v[i]);
            }
        }

        @Override
        public void detectAndSendChanges() {
            super.detectAndSendChanges();
            int[] v = values();
            for (int i = 0; i < v.length; i++) {
                if (v[i] != sent[i]) {
                    for (Object o : crafters) {
                        ((ICrafting) o).sendProgressBarUpdate(this, i, v[i]);
                    }
                    sent[i] = v[i];
                }
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void updateProgressBar(int id, int data) {
            if (id >= 0 && id <= 2) {
                enchantLevels[id] = data;
            } else if (id == 3) {
                xpSeed = data;
            } else if (id >= 4 && id <= 6) {
                enchantClue[id - 4] = data;
            } else if (id >= 7 && id <= 9) {
                worldClue[id - 7] = data;
            } else {
                super.updateProgressBar(id, data);
            }
        }

        @Override
        public void onCraftMatrixChanged(net.minecraft.inventory.IInventory inventory) {
            if (inventory != table) {
                return;
            }
            ItemStack stack = inventory.getStackInSlot(0);
            if (stack != null && stack.isItemEnchantable()) {
                if (!world.isRemote) {
                    float power = 0;
                    for (int j = -1; j <= 1; ++j) {
                        for (int k = -1; k <= 1; ++k) {
                            if ((j != 0 || k != 0) && world.isAirBlock(x + k, y, z + j) && world.isAirBlock(x + k, y + 1, z + j)) {
                                power += ForgeHooks.getEnchantPower(world, x + k * 2, y, z + j * 2);
                                power += ForgeHooks.getEnchantPower(world, x + k * 2, y + 1, z + j * 2);
                                if (k != 0 && j != 0) {
                                    power += ForgeHooks.getEnchantPower(world, x + k * 2, y, z + j);
                                    power += ForgeHooks.getEnchantPower(world, x + k * 2, y + 1, z + j);
                                    power += ForgeHooks.getEnchantPower(world, x + k, y, z + j * 2);
                                    power += ForgeHooks.getEnchantPower(world, x + k, y + 1, z + j * 2);
                                }
                            }
                        }
                    }
                    rand.setSeed((long) xpSeed);
                    for (int i = 0; i < 3; ++i) {
                        enchantLevels[i] = EnchantmentHelper.calcItemStackEnchantability(rand, i, (int) power, stack);
                        enchantClue[i] = -1;
                        worldClue[i] = -1;
                        if (enchantLevels[i] < i + 1) {
                            enchantLevels[i] = 0;
                        }
                    }
                    for (int i = 0; i < 3; ++i) {
                        if (enchantLevels[i] > 0) {
                            List<EnchantmentData> list = enchantmentList(stack, i, enchantLevels[i]);
                            if (!list.isEmpty()) {
                                EnchantmentData d = list.get(rand.nextInt(list.size()));
                                enchantClue[i] = d.enchantmentobj.effectId;
                                worldClue[i] = d.enchantmentLevel;
                            }
                        }
                    }
                    detectAndSendChanges();
                }
            } else {
                for (int i = 0; i < 3; ++i) {
                    enchantLevels[i] = 0;
                    enchantClue[i] = -1;
                    worldClue[i] = -1;
                }
            }
        }

        /** ContainerEnchantment.getEnchantmentList of 1.12.2: a book keeps one enchantment fewer. */
        public List<EnchantmentData> enchantmentList(ItemStack stack, int slot, int level) {
            rand.setSeed((long) (xpSeed + slot));
            List<EnchantmentData> list = buildEnchantmentList(rand, stack, level);
            if (stack.getItem() == Items.book && list.size() > 1) {
                list.remove(rand.nextInt(list.size()));
            }
            return list;
        }

        @Override
        public boolean enchantItem(EntityPlayer player, int id) {
            if (id < 0 || id > 2) {
                return false;
            }
            ItemStack stack = table.getStackInSlot(0);
            ItemStack lapis = table.getStackInSlot(1);
            int cost = id + 1;
            if ((lapis == null || lapis.stackSize < cost) && !player.capabilities.isCreativeMode) {
                return false;
            }
            if (enchantLevels[id] > 0 && stack != null
                && (player.experienceLevel >= cost && player.experienceLevel >= enchantLevels[id] || player.capabilities.isCreativeMode)) {
                if (!world.isRemote) {
                    List<EnchantmentData> list = enchantmentList(stack, id, enchantLevels[id]);
                    if (!list.isEmpty()) {
                        onEnchant(player, cost);
                        boolean book = stack.getItem() == Items.book;
                        if (book) {
                            stack = new ItemStack(Items.enchanted_book);
                            table.setInventorySlotContents(0, stack);
                        }
                        for (EnchantmentData d : list) {
                            if (book) {
                                Items.enchanted_book.addEnchantment(stack, d);
                            } else {
                                stack.addEnchantment(d.enchantmentobj, d.enchantmentLevel);
                            }
                        }
                        if (!player.capabilities.isCreativeMode) {
                            lapis.stackSize -= cost;
                            if (lapis.stackSize <= 0) {
                                table.setInventorySlotContents(1, null);
                            }
                        }
                        Triggers112.enchanted(true, player);
                        table.markDirty();
                        xpSeed = xpSeed(player);
                        onCraftMatrixChanged(table);
                        Sounds112.enchanted(world, x, y, z);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public void onContainerClosed(EntityPlayer player) {
            super.onContainerClosed(player);
            if (world.isRemote) {
                return;
            }
            // Container.clearContainer of 1.12.2: back into the inventory, dropped only when it is full or the player dead
            for (int i = 0; i < table.getSizeInventory(); ++i) {
                ItemStack s = table.getStackInSlotOnClosing(i);
                if (s != null && (!player.isEntityAlive() || !player.inventory.addItemStackToInventory(s))) {
                    player.dropPlayerItemWithRandomChoice(s, false);
                }
            }
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return world.getBlock(x, y, z) == Blocks.enchanting_table
                && player.getDistanceSq((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D) <= 64.0D;
        }

        /** ContainerEnchantment.transferStackInSlot of 1.12.2. */
        @Override
        public ItemStack transferStackInSlot(EntityPlayer player, int index) {
            ItemStack copy = null;
            Slot slot = (Slot) inventorySlots.get(index);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                copy = stack.copy();
                if (index == 0 || index == 1) {
                    if (!mergeItemStack(stack, 2, 38, true)) {
                        return null;
                    }
                } else if (isLapis(stack)) {
                    if (!mergeItemStack(stack, 1, 2, true)) {
                        return null;
                    }
                } else {
                    Slot first = (Slot) inventorySlots.get(0);
                    if (first.getHasStack() || !first.isItemValid(stack)) {
                        return null;
                    }
                    if (stack.hasTagCompound() && stack.stackSize == 1) {
                        first.putStack(stack.copy());
                        stack.stackSize = 0;
                    } else if (stack.stackSize >= 1) {
                        first.putStack(new ItemStack(stack.getItem(), 1, stack.getItemDamage()));
                        --stack.stackSize;
                    }
                }
                if (stack.stackSize == 0) {
                    slot.putStack(null);
                } else {
                    slot.onSlotChanged();
                }
                if (stack.stackSize == copy.stackSize) {
                    return null;
                }
                slot.onPickupFromSlot(player, stack);
            }
            return copy;
        }
    }
}
