package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityBrewingStand;

/**
 * The brewing stand of 1.9 - 1.12.2 (TileEntityBrewingStand, ContainerBrewingStand): a fifth slot for blaze powder,
 * one powder for 20 brews, the fuel shown in the window; hoppers feed the fuel from the sides, take only bottles from
 * below and only glass bottles out of the ingredient slot. -Dmc112.brewFuel17 keeps the stand of 1.7.10.
 */
public final class BrewFuel112 {
    public static final boolean OFF = System.getProperty("mc112.brewFuel17") != null;
    public static final int FUEL_SLOT = 4;

    private static final Map<TileEntityBrewingStand, int[]> FUEL = new WeakHashMap<TileEntityBrewingStand, int[]>();
    private static final Map<Container, int[]> SENT = new WeakHashMap<Container, int[]>();
    private static Field stacks, brewTime, filled, ingredient, standOf;
    private static Method canBrew, brew, addSlot, merge;

    private BrewFuel112() {
    }

    private static void reflect() {
        if (stacks == null) {
            stacks = ReflectionHelper.findField(TileEntityBrewingStand.class, "field_145945_j", "brewingItemStacks");
            brewTime = ReflectionHelper.findField(TileEntityBrewingStand.class, "field_145946_k", "brewTime");
            filled = ReflectionHelper.findField(TileEntityBrewingStand.class, "field_145943_l", "filledSlots");
            ingredient = ReflectionHelper.findField(TileEntityBrewingStand.class, "field_145944_m", "ingredientID");
            standOf = ReflectionHelper.findField(ContainerBrewingStand.class, "field_75188_e", "tileBrewingStand");
            canBrew = ReflectionHelper.findMethod(TileEntityBrewingStand.class, null, new String[] {"func_145934_k", "canBrew"});
            brew = ReflectionHelper.findMethod(TileEntityBrewingStand.class, null, new String[] {"func_145940_l", "brewPotions"});
            addSlot = ReflectionHelper.findMethod(Container.class, null, new String[] {"func_75146_a", "addSlotToContainer"}, Slot.class);
            merge = ReflectionHelper.findMethod(Container.class, null, new String[] {"func_75135_a", "mergeItemStack"}, ItemStack.class, int.class, int.class, boolean.class);
        }
    }

    public static synchronized int fuel(TileEntityBrewingStand stand) {
        int[] f = FUEL.get(stand);
        return f == null ? 0 : f[0];
    }

    public static synchronized void setFuel(TileEntityBrewingStand stand, int value) {
        int[] f = FUEL.get(stand);
        if (f == null) {
            f = new int[1];
            FUEL.put(stand, f);
        }
        f[0] = value;
    }

    // ---- TileEntityBrewingStand ----

    /** TileEntityBrewingStand.update of 1.12.2 in place of updateEntity. */
    public static boolean update(TileEntityBrewingStand stand) {
        if (OFF) {
            return false;
        }
        try {
            reflect();
            ItemStack[] items = (ItemStack[]) stacks.get(stand);
            ItemStack powder = items.length > FUEL_SLOT ? items[FUEL_SLOT] : null;
            if (fuel(stand) <= 0 && powder != null && powder.getItem() == Items.blaze_powder) {
                setFuel(stand, 20);
                if (--powder.stackSize <= 0) {
                    items[FUEL_SLOT] = null;
                }
                stand.markDirty();
            }
            boolean can = (Boolean) canBrew.invoke(stand);
            int time = brewTime.getInt(stand);
            if (time > 0) {
                time--;
                brewTime.setInt(stand, time);
                if (time == 0 && can) {
                    brew.invoke(stand);
                    stand.markDirty();
                } else if (!can) {
                    brewTime.setInt(stand, 0);
                    stand.markDirty();
                } else if (ingredient.get(stand) != items[3].getItem()) {
                    brewTime.setInt(stand, 0);
                    stand.markDirty();
                }
            } else if (can && fuel(stand) > 0) {
                setFuel(stand, fuel(stand) - 1);
                brewTime.setInt(stand, 400);
                ingredient.set(stand, items[3].getItem());
                stand.markDirty();
            }
            if (stand.getWorldObj() != null && !stand.getWorldObj().isRemote) {
                int now = stand.getFilledSlots();
                if (now != filled.getInt(stand)) {
                    filled.setInt(stand, now);
                    stand.getWorldObj().setBlockMetadataWithNotify(stand.xCoord, stand.yCoord, stand.zCoord, now, 2);
                }
            }
            return true;
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").error("1.12 brewing off: " + e);
            return false;
        }
    }

    /** writeToNBT / readFromNBT: "Fuel" as a byte, as 1.12.2 keeps it. */
    public static void write(TileEntityBrewingStand stand, NBTTagCompound nbt) {
        nbt.setByte("Fuel", (byte) fuel(stand));
    }

    public static void read(TileEntityBrewingStand stand, NBTTagCompound nbt) {
        setFuel(stand, nbt.getByte("Fuel"));
    }

    /** isItemValidForSlot: the fuel slot takes blaze powder only; -1 leaves the others to 1.7.10. */
    public static int valid(int slot, ItemStack stack) {
        if (OFF || slot != FUEL_SLOT) {
            return -1;
        }
        return stack != null && stack.getItem() == Items.blaze_powder ? 1 : 0;
    }

    private static final int[] UP = {3};
    private static final int[] DOWN = {0, 1, 2, 3};
    private static final int[] SIDES = {0, 1, 2, 4};

    /** getSlotsForFace of 1.12.2: the top fills the ingredient, the sides the bottles and the fuel. */
    public static int[] slots(int side) {
        return side == 1 ? UP : side == 0 ? DOWN : SIDES;
    }

    /** canExtractItem of 1.12.2: from the ingredient slot only a glass bottle (what a dragon's breath brew leaves). */
    public static boolean extract(int slot, ItemStack stack) {
        return slot != 3 || stack != null && stack.getItem() == Items.glass_bottle;
    }

    // ---- ContainerBrewingStand ----

    /** After the ingredient slot: the fuel slot at (17, 17). */
    public static void addFuelSlot(ContainerBrewingStand c, InventoryPlayer player, TileEntityBrewingStand stand) {
        if (OFF) {
            return;
        }
        try {
            reflect();
            addSlot.invoke(c, new FuelSlot(stand, FUEL_SLOT, 17, 17));
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").error("brewing fuel slot: " + e);
        }
    }

    public static final class FuelSlot extends Slot {
        public FuelSlot(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() == Items.blaze_powder;
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
        }
    }

    /** detectAndSendChanges: the fuel goes out as progress bar 1, as in 1.12.2. */
    @SuppressWarnings("unchecked")
    public static void sendFuel(ContainerBrewingStand c, List<ICrafting> crafters) {
        if (OFF) {
            return;
        }
        try {
            reflect();
            int now = fuel((TileEntityBrewingStand) standOf.get(c));
            int[] last;
            synchronized (SENT) {
                last = SENT.get(c);
                if (last == null) {
                    last = new int[] {-1};
                    SENT.put(c, last);
                }
            }
            if (last[0] != now) {
                for (ICrafting crafter : crafters) {
                    crafter.sendProgressBarUpdate(c, 1, now);
                }
                last[0] = now;
            }
        } catch (Exception e) {
            // the bar stays as it is
        }
    }

    /** updateProgressBar: bar 1 is the fuel; true when taken. */
    public static boolean progress(ContainerBrewingStand c, int id, int value) {
        if (OFF || id != 1) {
            return false;
        }
        try {
            reflect();
            setFuel((TileEntityBrewingStand) standOf.get(c), value);
        } catch (Exception e) {
            // nothing to show
        }
        return true;
    }

    /** ContainerBrewingStand.transferStackInSlot of 1.12.2 with the fuel slot (5 slots of the stand, then the player). */
    @SuppressWarnings("unchecked")
    public static ItemStack transfer(ContainerBrewingStand c, EntityPlayer player, int index) {
        try {
            reflect();
            ItemStack result = null;
            Slot slot = (Slot) c.inventorySlots.get(index);
            if (slot == null || !slot.getHasStack()) {
                return null;
            }
            ItemStack stack = slot.getStack();
            result = stack.copy();
            Slot ingredientSlot = (Slot) c.inventorySlots.get(3);
            if ((index < 0 || index > 2) && index != 3 && index != 4) {
                if (ingredientSlot.isItemValid(stack)) {
                    if (!mergeTo(c, stack, 3, 4, false)) {
                        return null;
                    }
                } else if (bottle(result) && result.stackSize == 1) {
                    if (!mergeTo(c, stack, 0, 3, false)) {
                        return null;
                    }
                } else if (stack.getItem() == Items.blaze_powder) {
                    if (!mergeTo(c, stack, 4, 5, false)) {
                        return null;
                    }
                } else if (index >= 5 && index < 32) {
                    if (!mergeTo(c, stack, 32, 41, false)) {
                        return null;
                    }
                } else if (index >= 32 && index < 41) {
                    if (!mergeTo(c, stack, 5, 32, false)) {
                        return null;
                    }
                } else if (!mergeTo(c, stack, 5, 41, false)) {
                    return null;
                }
            } else {
                if (!mergeTo(c, stack, 5, 41, true)) {
                    return null;
                }
                slot.onSlotChange(stack, result);
            }
            if (stack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            if (stack.stackSize == result.stackSize) {
                return null;
            }
            slot.onPickupFromSlot(player, stack);
            return result;
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").error("brewing shift-click: " + e);
            return null;
        }
    }

    private static boolean bottle(ItemStack s) {
        Item i = s.getItem();
        return i instanceof ItemPotion || i == Items.glass_bottle || i == Brew112.lingering;
    }

    private static boolean mergeTo(Container c, ItemStack stack, int from, int to, boolean reverse) throws Exception {
        return (Boolean) merge.invoke(c, stack, from, to, reverse);
    }
}
