package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIVillagerMate;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/**
 * Villagers of 1.8 - 1.12.2 (EntityVillager.villagerInventory, EntityAIHarvestFarmland, EntityAIVillagerInteract,
 * EntityAIVillagerMate): an 8-slot inventory filled by picking up bread, wheat, seeds, carrots, potatoes and beetroots;
 * farmers harvest ripe crops and plant what they carry; a villager with plenty throws food to one that wants more; and
 * a villager breeds only when willing, which takes 3 bread or 12 carrots / potatoes / beetroots (or a trade).
 * Server side only. -Dmc112.villagers17 leaves the villagers of 1.7.10.
 */
public final class Villagers112 {
    public static final boolean OFF = System.getProperty("mc112.villagers17") != null;
    private static final String INVENTORY = "mc112Inventory";
    private static final String WILLING = "mc112Willing";
    private static final SideMap<EntityVillager, InventoryBasic> INVENTORIES = new SideMap<EntityVillager, InventoryBasic>();

    public Villagers112() {
    }

    // ---- inventory (saved in the Forge data of the villager, as "Inventory" of 1.12.2) ----

    public static InventoryBasic inventory(final EntityVillager v) {
        InventoryBasic inv = INVENTORIES.get(v);
        if (inv == null) {
            inv = new InventoryBasic("Items", false, 8);
            NBTTagList list = v.getEntityData().getTagList(INVENTORY, 10);
            for (int i = 0; i < list.tagCount() && i < 8; i++) {
                ItemStack s = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
                if (s != null) {
                    add(inv, s);
                }
            }
            final InventoryBasic made = inv;
            inv.func_110134_a(new net.minecraft.inventory.IInvBasic() {
                @Override
                public void onInventoryChanged(InventoryBasic changed) {
                    save(v, made);
                }
            });
            INVENTORIES.put(v, inv);
        }
        return inv;
    }

    private static void save(EntityVillager v, InventoryBasic inv) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null) {
                list.appendTag(s.writeToNBT(new NBTTagCompound()));
            }
        }
        v.getEntityData().setTag(INVENTORY, list);
    }

    /** InventoryBasic.addItem of 1.12.2: onto matching stacks first, then empty slots; what does not fit is returned. */
    public static ItemStack add(InventoryBasic inv, ItemStack stack) {
        ItemStack left = stack.copy();
        for (int i = 0; i < inv.getSizeInventory() && left.stackSize > 0; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null && s.getItem() == left.getItem() && s.getItemDamage() == left.getItemDamage() && ItemStack.areItemStackTagsEqual(s, left)) {
                int move = Math.min(left.stackSize, Math.min(inv.getInventoryStackLimit(), s.getMaxStackSize()) - s.stackSize);
                if (move > 0) {
                    s.stackSize += move;
                    left.stackSize -= move;
                }
            }
        }
        for (int i = 0; i < inv.getSizeInventory() && left.stackSize > 0; i++) {
            if (inv.getStackInSlot(i) == null) {
                inv.setInventorySlotContents(i, left.copy());
                left.stackSize = 0;
            }
        }
        inv.markDirty();
        return left;
    }

    /** EntityVillager.canVillagerPickupItem. */
    public static boolean pickable(Item item) {
        return item == Items.bread || item == Items.potato || item == Items.carrot || item == Items.wheat || item == Items.wheat_seeds
            || item == Items112.beetroot || item == Items112.beetrootSeeds;
    }

    private static boolean bulkFood(Item item) {
        return item == Items.potato || item == Items.carrot || item == Items112.beetroot;
    }

    /** EntityVillager.hasEnoughItems. */
    static boolean hasEnoughItems(EntityVillager v, int multiplier) {
        boolean farmer = v.getProfession() == 0;
        InventoryBasic inv = inventory(v);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s == null) {
                continue;
            }
            if (s.getItem() == Items.bread && s.stackSize >= 3 * multiplier || bulkFood(s.getItem()) && s.stackSize >= 12 * multiplier) {
                return true;
            }
            if (farmer && s.getItem() == Items.wheat && s.stackSize >= 9 * multiplier) {
                return true;
            }
        }
        return false;
    }

    /** EntityVillager.wantsMoreFood: a farmer that could feed two matings yet, any other villager that could not feed one. */
    public static boolean wantsMoreFood(EntityVillager v) {
        boolean farmer = v.getProfession() == 0;
        return farmer ? !hasEnoughItems(v, 5) : !hasEnoughItems(v, 1);
    }

    /** EntityVillager.isFarmItemInInventory. */
    public static boolean hasFarmItem(EntityVillager v) {
        InventoryBasic inv = inventory(v);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s != null && (s.getItem() == Items.wheat_seeds || s.getItem() == Items.potato || s.getItem() == Items.carrot || s.getItem() == Items112.beetrootSeeds)) {
                return true;
            }
        }
        return false;
    }

    // ---- willingness ----

    public static boolean isWilling(EntityVillager v) {
        return v.getEntityData().getBoolean(WILLING);
    }

    public static void setWilling(EntityVillager v, boolean willing) {
        v.getEntityData().setBoolean(WILLING, willing);
    }

    /** EntityVillager.getIsWillingToMate(true): eats 3 bread or 12 carrots / potatoes / beetroots to become willing. */
    public static boolean willing(EntityVillager v) {
        if (!isWilling(v) && hasEnoughItems(v, 1)) {
            InventoryBasic inv = inventory(v);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                int eat = s.getItem() == Items.bread && s.stackSize >= 3 ? 3 : bulkFood(s.getItem()) && s.stackSize >= 12 ? 12 : 0;
                if (eat > 0) {
                    inv.decrStackSize(i, eat);
                    inv.markDirty();
                    setWilling(v, true);
                    v.worldObj.setEntityState(v, (byte) 12);
                    break;
                }
            }
        }
        return isWilling(v);
    }

    // ---- events ----

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (OFF || event.world.isRemote || !(event.entity instanceof EntityVillager)) {
            return;
        }
        EntityVillager v = (EntityVillager) event.entity;
        if (has(v.tasks, Mate.class)) {
            return;
        }
        for (Object o : v.tasks.taskEntries.toArray()) {
            EntityAIBase a = ((EntityAITasks.EntityAITaskEntry) o).action;
            if (a instanceof EntityAIVillagerMate) {
                v.tasks.removeTask(a);
            }
        }
        v.tasks.addTask(6, new Mate(v));
        v.tasks.addTask(9, new Interact(v));
        if (!v.isChild() && v.getProfession() == 0) {
            v.tasks.addTask(6, new Harvest(v));
        }
    }

    private static boolean has(EntityAITasks tasks, Class<?> type) {
        for (Object o : tasks.taskEntries) {
            if (type.isInstance(((EntityAITasks.EntityAITaskEntry) o).action)) {
                return true;
            }
        }
        return false;
    }

    /** EntityLiving.onLivingUpdate with canPickUpLoot: items within the box grown by 1 sideways go into the inventory. */
    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (OFF || !(event.entityLiving instanceof EntityVillager) || event.entityLiving.worldObj.isRemote || event.entityLiving.isDead) {
            return;
        }
        EntityVillager v = (EntityVillager) event.entityLiving;
        World w = v.worldObj;
        if (!w.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
            return;
        }
        // a farmer that grew up starts harvesting (setAdditionalAItasks after onGrowingAdult)
        if (v.ticksExisted % 20 == 0 && !v.isChild() && v.getProfession() == 0 && !has(v.tasks, Harvest.class)) {
            v.tasks.addTask(6, new Harvest(v));
        }
        List<?> items = w.getEntitiesWithinAABB(EntityItem.class, v.boundingBox.expand(1.0D, 0.0D, 1.0D));
        for (Object o : items) {
            EntityItem item = (EntityItem) o;
            ItemStack s = item.getEntityItem();
            if (item.isDead || s == null || item.delayBeforeCanPickup > 0 || !pickable(s.getItem())) {
                continue;
            }
            ItemStack left = add(inventory(v), s);
            if (left.stackSize <= 0) {
                item.setDead();
            } else {
                s.stackSize = left.stackSize;
            }
        }
    }

    // ---- AI ----

    /** EntityAIVillagerMate of 1.12.2: the 1.7.10 task, both villagers willing. */
    public static final class Mate extends EntityAIBase {
        private final EntityVillager villager;
        private EntityVillager mate;
        private Village village;
        private int matingTimeout;

        Mate(EntityVillager villager) {
            this.villager = villager;
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (villager.getGrowingAge() != 0 || villager.getRNG().nextInt(500) != 0) {
                return false;
            }
            World w = villager.worldObj;
            village = w.villageCollectionObj.findNearestVillage(MathHelper.floor_double(villager.posX), MathHelper.floor_double(villager.posY),
                MathHelper.floor_double(villager.posZ), 0);
            if (village == null || !enoughDoors() || !willing(villager)) {
                return false;
            }
            Entity e = w.findNearestEntityWithinAABB(EntityVillager.class, villager.boundingBox.expand(8.0D, 3.0D, 8.0D), villager);
            if (e == null) {
                return false;
            }
            mate = (EntityVillager) e;
            return mate.getGrowingAge() == 0 && willing(mate);
        }

        @Override
        public void startExecuting() {
            matingTimeout = 300;
            villager.setMating(true);
        }

        @Override
        public void resetTask() {
            village = null;
            mate = null;
            villager.setMating(false);
        }

        @Override
        public boolean continueExecuting() {
            return matingTimeout >= 0 && enoughDoors() && villager.getGrowingAge() == 0 && isWilling(villager);
        }

        @Override
        public void updateTask() {
            --matingTimeout;
            villager.getLookHelper().setLookPositionWithEntity(mate, 10.0F, 30.0F);
            if (villager.getDistanceSqToEntity(mate) > 2.25D) {
                villager.getNavigator().tryMoveToEntityLiving(mate, 0.25D);
            } else if (matingTimeout == 0 && mate.isMating()) {
                giveBirth();
            }
            if (villager.getRNG().nextInt(35) == 0) {
                villager.worldObj.setEntityState(villager, (byte) 12);
            }
        }

        private boolean enoughDoors() {
            if (village == null || !village.isMatingSeason()) {
                return false;
            }
            int i = (int) ((float) village.getNumVillageDoors() * 0.35F);
            return village.getNumVillagers() < i;
        }

        private void giveBirth() {
            EntityVillager child = villager.createChild(mate);
            mate.setGrowingAge(6000);
            villager.setGrowingAge(6000);
            setWilling(mate, false);
            setWilling(villager, false);
            child.setGrowingAge(-24000);
            child.setLocationAndAngles(villager.posX, villager.posY, villager.posZ, 0.0F, 0.0F);
            villager.worldObj.spawnEntityInWorld(child);
            villager.worldObj.setEntityState(child, (byte) 12);
        }
    }

    /** EntityAIVillagerInteract of 1.12.2: half of a big stack of food thrown to a villager that wants more. */
    public static final class Interact extends EntityAIWatchClosest2 {
        private final EntityVillager villager;
        private int interactionDelay;

        public Interact(EntityVillager villager) {
            super(villager, EntityVillager.class, 3.0F, 0.02F);
            this.villager = villager;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            Entity other = villager.worldObj.findNearestEntityWithinAABB(EntityVillager.class, villager.boundingBox.expand(3.0D, 3.0D, 3.0D), villager);
            interactionDelay = hasEnoughItems(villager, 2) && other instanceof EntityVillager && wantsMoreFood((EntityVillager) other) ? 10 : 0;
        }

        @Override
        public void updateTask() {
            super.updateTask();
            if (interactionDelay > 0 && --interactionDelay == 0) {
                give();
            }
        }

        public void give() {
            InventoryBasic inv = inventory(villager);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                ItemStack out = null;
                Item item = s.getItem();
                if ((item == Items.bread || bulkFood(item)) && s.stackSize > 3) {
                    int j = s.stackSize / 2;
                    s.stackSize -= j;
                    out = new ItemStack(item, j, s.getItemDamage());
                } else if (item == Items.wheat && s.stackSize > 5) {
                    int j = s.stackSize / 2 / 3 * 3;
                    int k = j / 3;
                    s.stackSize -= j;
                    out = new ItemStack(Items.bread, k, 0);
                }
                if (s.stackSize <= 0) {
                    inv.setInventorySlotContents(i, null);
                }
                inv.markDirty();
                if (out != null && out.stackSize > 0) {
                    double y = villager.posY - 0.30000001192092896D + (double) villager.getEyeHeight();
                    EntityItem e = new EntityItem(villager.worldObj, villager.posX, y, villager.posZ, out);
                    float yaw = villager.rotationYawHead;
                    float pitch = villager.rotationPitch;
                    e.motionX = (double) (-MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F) * 0.3F);
                    e.motionZ = (double) (MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F) * 0.3F);
                    e.motionY = (double) (-MathHelper.sin(pitch * 0.017453292F) * 0.3F + 0.1F);
                    e.delayBeforeCanPickup = 10;
                    villager.worldObj.spawnEntityInWorld(e);
                    break;
                }
            }
        }
    }

    /** EntityAIHarvestFarmland (on EntityAIMoveToBlock) of 1.12.2 for farmers. */
    public static final class Harvest extends EntityAIBase {
        private final EntityVillager villager;
        private final double speed = 0.6D;
        private int runDelay;
        private int timeoutCounter;
        private int maxStayTicks;
        private int destX;
        private int destY;
        private int destZ;
        private boolean above;
        private boolean hasFarmItem;
        private boolean wantsToReap;
        /** 0 harvest, 1 plant, -1 nothing yet. */
        private int currentTask = -1;

        Harvest(EntityVillager villager) {
            this.villager = villager;
            setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            if (runDelay <= 0) {
                if (!villager.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")) {
                    return false;
                }
                currentTask = -1;
                hasFarmItem = hasFarmItem(villager);
                wantsToReap = wantsMoreFood(villager);
            }
            if (runDelay > 0) {
                --runDelay;
                return false;
            }
            runDelay = 200 + villager.getRNG().nextInt(200);
            return search();
        }

        @Override
        public boolean continueExecuting() {
            return currentTask >= 0 && timeoutCounter >= -maxStayTicks && timeoutCounter <= 1200 && shouldMoveTo(villager.worldObj, destX, destY, destZ);
        }

        @Override
        public void startExecuting() {
            villager.getNavigator().tryMoveToXYZ((double) destX + 0.5D, (double) (destY + 1), (double) destZ + 0.5D, speed);
            timeoutCounter = 0;
            maxStayTicks = villager.getRNG().nextInt(villager.getRNG().nextInt(1200) + 1200) + 1200;
        }

        @Override
        public void updateTask() {
            double dx = villager.posX - ((double) destX + 0.5D);
            double dy = villager.posY - ((double) destY + 1.5D);
            double dz = villager.posZ - ((double) destZ + 0.5D);
            if (dx * dx + dy * dy + dz * dz > 1.0D) {
                above = false;
                ++timeoutCounter;
                if (timeoutCounter % 40 == 0) {
                    villager.getNavigator().tryMoveToXYZ((double) destX + 0.5D, (double) (destY + 1), (double) destZ + 0.5D, speed);
                }
            } else {
                above = true;
                --timeoutCounter;
            }
            villager.getLookHelper().setLookPosition((double) destX + 0.5D, (double) (destY + 1), (double) destZ + 0.5D, 10.0F, (float) villager.getVerticalFaceSpeed());
            if (above) {
                World w = villager.worldObj;
                int y = destY + 1;
                Block b = w.getBlock(destX, y, destZ);
                if (currentTask == 0 && ripe(b, w.getBlockMetadata(destX, y, destZ))) {
                    w.func_147480_a(destX, y, destZ, true);
                } else if (currentTask == 1 && w.getBlock(destX, y, destZ).getMaterial() == Material.air) {
                    plant(w, destX, y, destZ);
                }
                currentTask = -1;
                runDelay = 10;
            }
        }

        private void plant(World w, int x, int y, int z) {
            InventoryBasic inv = inventory(villager);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                Block crop = s.getItem() == Items.wheat_seeds ? Blocks.wheat : s.getItem() == Items.potato ? Blocks.potatoes : s.getItem() == Items.carrot ? Blocks.carrots
                    : s.getItem() == Items112.beetrootSeeds ? Items112.beetroots : null;
                if (crop != null) {
                    w.setBlock(x, y, z, crop, 0, 3);
                    inv.decrStackSize(i, 1);
                    inv.markDirty();
                    return;
                }
            }
        }

        static boolean ripe(Block b, int meta) {
            if (b == Items112.beetroots) {
                return meta >= 3;
            }
            return b instanceof BlockCrops && meta >= 7;
        }

        /** EntityAIHarvestFarmland.shouldMoveTo: farmland with a ripe crop to reap or air to plant. */
        private boolean shouldMoveTo(World w, int x, int y, int z) {
            if (w.getBlock(x, y, z) != Blocks.farmland) {
                return false;
            }
            Block up = w.getBlock(x, y + 1, z);
            if (ripe(up, w.getBlockMetadata(x, y + 1, z)) && wantsToReap && (currentTask == 0 || currentTask < 0)) {
                currentTask = 0;
                return true;
            }
            if (up.getMaterial() == Material.air && hasFarmItem && (currentTask == 1 || currentTask < 0)) {
                currentTask = 1;
                return true;
            }
            return false;
        }

        /** EntityAIMoveToBlock.searchForDestination: 16 blocks around, one layer below and the one of the feet. */
        private boolean search() {
            int bx = MathHelper.floor_double(villager.posX);
            int by = MathHelper.floor_double(villager.posY);
            int bz = MathHelper.floor_double(villager.posZ);
            for (int k = 0; k <= 1; k = k > 0 ? -k : 1 - k) {
                for (int l = 0; l < 16; ++l) {
                    for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                        for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                            int x = bx + i1;
                            int y = by + k - 1;
                            int z = bz + j1;
                            if (villager.isWithinHomeDistance(x, y, z) && shouldMoveTo(villager.worldObj, x, y, z)) {
                                destX = x;
                                destY = y;
                                destZ = z;
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        /** For the autotest. */
        public boolean searchNow() {
            hasFarmItem = hasFarmItem(villager);
            wantsToReap = wantsMoreFood(villager);
            currentTask = -1;
            return search();
        }

        public int[] dest() {
            return new int[] {destX, destY, destZ, currentTask};
        }
    }
}
