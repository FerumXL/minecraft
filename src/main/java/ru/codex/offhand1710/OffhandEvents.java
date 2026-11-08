package ru.codex.offhand1710;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class OffhandEvents {
    private static final ConcurrentMap<UUID, Boolean> SWAP_REQUESTS = new ConcurrentHashMap<UUID, Boolean>();
    private static final ConcurrentMap<UUID, OffhandMessages.UseRequest> USE_REQUESTS =
            new ConcurrentHashMap<UUID, OffhandMessages.UseRequest>();
    private static final ConcurrentMap<UUID, OffhandMessages.CreativeSet> CREATIVE_SETS =
            new ConcurrentHashMap<UUID, OffhandMessages.CreativeSet>();

    public static void queueSwap(UUID playerId) {
        SWAP_REQUESTS.put(playerId, Boolean.TRUE);
    }

    public static void queueUse(UUID playerId, OffhandMessages.UseRequest request) {
        USE_REQUESTS.put(playerId, request);
    }

    public static void queueCreativeSet(UUID playerId, OffhandMessages.CreativeSet message) {
        CREATIVE_SETS.put(playerId, message);
    }

    private static final ConcurrentMap<UUID, Boolean> RELEASES = new ConcurrentHashMap<UUID, Boolean>();

    public static void queueRelease(UUID playerId) {
        RELEASES.put(playerId, Boolean.TRUE);
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        OffhandInventory.register(event.entity);
    }

    /** The container exists only after the player constructor; both sides add the slot when the player joins. */
    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer) {
            OffhandContainer.addSlot((EntityPlayer) event.entity);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // After a death without keepInventory the offhand was already dropped (onPlayerDrops), so this copies null.
        OffhandInventory.get(event.entityPlayer).copyFrom(OffhandInventory.get(event.original));
    }

    /**
     * Fired only when keepInventory is off: the offhand item drops with the rest of the inventory, as in 1.12.2.
     * Highest priority so mods that collect the drops (graves, corpses) get the offhand item too.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.entityPlayer;
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack stack = offhand.getStack();
        if (stack == null) {
            return;
        }
        // Same spread as InventoryPlayer.dropAllItems -> EntityPlayer.func_146097_a(stack, true, false).
        EntityItem item = new EntityItem(player.worldObj, player.posX,
                player.posY - 0.30000001192092896D + (double) player.getEyeHeight(), player.posZ, stack);
        item.delayBeforeCanPickup = 40;
        float strength = player.getRNG().nextFloat() * 0.5F;
        float angle = player.getRNG().nextFloat() * (float) Math.PI * 2.0F;
        item.motionX = (double) (-MathHelper.sin(angle) * strength);
        item.motionZ = (double) (MathHelper.cos(angle) * strength);
        item.motionY = 0.20000000298023224D;
        event.drops.add(item);
        offhand.setStack(null);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        OffhandMessages.synchronize((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        OffhandMessages.synchronize((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        if (SWAP_REQUESTS.remove(id) != null) {
            swap(player);
        }
        OffhandMessages.CreativeSet creative = CREATIVE_SETS.remove(id);
        if (creative != null) {
            creativeSet(player, creative.stack);
        }
        OffhandMessages.UseRequest use = USE_REQUESTS.remove(id);
        if (use != null) {
            useOffhand(player, use);
        }
        if (RELEASES.remove(id) != null) {
            releaseUse(player);
        }
        tickUse(player);
        OffhandInventory offhand = OffhandInventory.get(player);
        if (offhand.shieldCooldown > 0) {
            offhand.shieldCooldown--;
        }
    }

    /** Ends the offhand held use without finishing or releasing it (shield broken or knocked down). */
    static void stopOffhandUse(EntityPlayerMP player) {
        endUse(player, 0);
    }

    /** EntityPlayer.onUpdate (1.7.10) for the offhand item in use. */
    private static void tickUse(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        if (offhand.useCount <= 0) {
            return;
        }
        ItemStack stack = offhand.getStack();
        if (stack == null || stack != offhand.useStack) {
            endUse(player, 0); // the offhand item was swapped or taken away
            return;
        }
        int count = ForgeEventFactory.onItemUseTick(player, stack, offhand.useCount);
        if (count <= 0) {
            finishUse(player);
            return;
        }
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.mainInventory[slot];
        player.inventory.mainInventory[slot] = stack;
        try {
            stack.getItem().onUsingTick(stack, player, count);
        } finally {
            player.inventory.mainInventory[slot] = main;
        }
        offhand.useCount = count - 1;
        if (offhand.useCount == 0) {
            finishUse(player);
        }
    }

    /** EntityPlayer.onItemUseFinish: eat the food, drink the potion or milk... */
    private static void finishUse(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack stack = offhand.useStack;
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.mainInventory[slot];
        player.inventory.mainInventory[slot] = stack;
        ItemStack result;
        try {
            result = stack.onFoodEaten(player.worldObj, player);
            result = ForgeEventFactory.onItemUseFinish(player, stack, 0, result);
        } finally {
            player.inventory.mainInventory[slot] = main;
        }
        offhand.setStack(result != null && result.stackSize > 0 ? result : null);
        endUse(player, OffhandMessages.RESULT_FINISHED);
    }

    /** EntityPlayer.stopUsingItem: the button was released (a bow shoots). */
    private static void releaseUse(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack stack = offhand.useStack;
        if (offhand.useCount <= 0 || stack == null) {
            return;
        }
        if (stack != offhand.getStack()) {
            endUse(player, 0);
            return;
        }
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.mainInventory[slot];
        player.inventory.mainInventory[slot] = stack;
        try {
            if (!ForgeEventFactory.onUseItemStop(player, stack, offhand.useCount)) {
                stack.onPlayerStoppedUsing(player.worldObj, player, offhand.useCount);
            }
        } finally {
            player.inventory.mainInventory[slot] = main;
        }
        offhand.setStack(stack.stackSize > 0 ? stack : null);
        endUse(player, 0);
    }

    private static void endUse(EntityPlayerMP player, int extraFlags) {
        OffhandInventory offhand = OffhandInventory.get(player);
        offhand.useCount = 0;
        offhand.useStack = null;
        player.inventoryContainer.detectAndSendChanges();
        OffhandMessages.synchronize(player);
        OffhandMessages.sendResult(player, 0, OffhandMessages.RESULT_STOP_USE | extraFlags);
    }

    private static void swap(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.getStackInSlot(slot);
        player.inventory.setInventorySlotContents(slot, offhand.getStack());
        offhand.setStack(main);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        OffhandMessages.synchronize(player);
    }

    private static void creativeSet(EntityPlayerMP player, ItemStack stack) {
        // Same trust and limits as vanilla's creative inventory packet.
        if (!player.theItemInWorldManager.isCreative()) {
            return;
        }
        if (stack != null && (stack.getItemDamage() < 0 || stack.stackSize <= 0 || stack.stackSize > 64)) {
            return;
        }
        OffhandInventory.get(player).setStack(stack);
        player.inventoryContainer.detectAndSendChanges();
        OffhandMessages.synchronize(player);
    }

    /**
     * The offhand turn of a right click (1.12.2 order: entity, then block, then use in the air).
     *
     * The offhand item is put into the selected hotbar slot for the duration of the call and the vanilla
     * server logic runs on it, so blocks, buckets, leads, shears, spawn eggs and every other item behave
     * exactly as in the main hand. Afterwards the hotbar slot gets its own item back and the offhand gets the
     * result (smaller stack, filled bucket, nothing).
     */
    private static void useOffhand(EntityPlayerMP player, OffhandMessages.UseRequest request) {
        OffhandInventory offhand = OffhandInventory.get(player);
        if (offhand.getStack() == null || player.isUsingItem() || offhand.isUsing()) {
            return;
        }
        if (request.kind == OffhandMessages.USE_AIR && Elytra112.useFirework(player, offhand.getStack())) {
            if (offhand.getStack().stackSize <= 0) {
                offhand.setStack(null);
            }
            player.inventoryContainer.detectAndSendChanges();
            OffhandMessages.synchronize(player);
            OffhandMessages.sendResult(player, request.sequence, OffhandMessages.RESULT_EQUIP);
            return;
        }
        World world = player.worldObj;
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.mainInventory[slot];
        ItemStack before = offhand.getStack();
        int sizeBefore = before.stackSize;
        player.inventory.mainInventory[slot] = before;

        int flags = 0;
        int useTicks = 0;
        try {
            boolean acted = false;
            if (request.kind == OffhandMessages.USE_ENTITY) {
                Entity target = world.getEntityByID(request.entityId);
                // Same reach check as vanilla's entity interaction packet.
                if (target != null && target != player) {
                    double reach = player.canEntityBeSeen(target) ? 36.0D : 9.0D;
                    if (player.getDistanceSqToEntity(target) < reach) {
                        acted = player.interactWith(target);
                    }
                }
            } else if (request.kind == OffhandMessages.USE_BLOCK) {
                // Same reach and spawn protection checks as vanilla's block placement packet.
                if (player.getDistanceSq(request.x + 0.5D, request.y + 0.5D, request.z + 0.5D) < 64.0D
                        && !player.mcServer.isBlockProtected(world, request.x, request.y, request.z, player)) {
                    acted = player.theItemInWorldManager.activateBlockOrUseItem(player, world,
                            player.inventory.mainInventory[slot], request.x, request.y, request.z, request.side,
                            request.hitX, request.hitY, request.hitZ);
                    if (acted) {
                        flags |= OffhandMessages.RESULT_SWING;
                        ItemStack now = player.inventory.mainInventory[slot];
                        if (now != null && (now.stackSize != sizeBefore || player.theItemInWorldManager.isCreative())) {
                            flags |= OffhandMessages.RESULT_EQUIP;
                        }
                    }
                    // As vanilla after a block placement packet: resend the clicked block and the one next to
                    // the clicked face, so a wrong client-side prediction is undone.
                    resendBlock(player, world, request.x, request.y, request.z);
                    resendBlock(player, world, request.x + OFFSET_X[request.side & 7],
                            request.y + OFFSET_Y[request.side & 7], request.z + OFFSET_Z[request.side & 7]);
                }
            }
            if (!acted) {
                ItemStack current = player.inventory.mainInventory[slot];
                if (current != null && player.theItemInWorldManager.tryUseItem(player, world, current)) {
                    flags |= OffhandMessages.RESULT_EQUIP;
                }
            }
        } finally {
            ItemStack after = player.inventory.mainInventory[slot];
            player.inventory.mainInventory[slot] = main;
            // A held use (food, potion, bow) was started on the hotbar slot that held the offhand item. Vanilla
            // would drop it next tick (the selected item is not the one in use), so it moves to the offhand.
            if (player.isUsingItem() && player.getItemInUse() == after) {
                useTicks = player.getItemInUseCount();
                player.clearItemInUse();
            }
            offhand.setStack(after != null && after.stackSize > 0 ? after : null);
            if (useTicks > 0 && offhand.getStack() == after) {
                offhand.useCount = useTicks;
                offhand.useStack = after;
            }
            // tryUseItem may have sent the hotbar slot while it held the offhand item; resend the real contents.
            player.sendContainerToPlayer(player.inventoryContainer);
            OffhandMessages.synchronize(player);
        }
        // Always answered: the client matches answers to the requests it already animated by prediction.
        if (offhand.isUsing()) {
            OffhandMessages.sendResult(player, request.sequence, OffhandMessages.RESULT_START_USE, offhand.useCount);
        } else {
            OffhandMessages.sendResult(player, request.sequence, flags);
        }
        if ((flags & OffhandMessages.RESULT_SWING) != 0) {
            OffhandMessages.broadcastSwing(player); // the others see the left arm swing (1.12.2 swingArm)
        }
    }

    /** Neighbour of a block on side 0..5 (down, up, north, south, west, east); 6 and 7 mean "same block". */
    private static final int[] OFFSET_X = {0, 0, 0, 0, -1, 1, 0, 0};
    private static final int[] OFFSET_Y = {-1, 1, 0, 0, 0, 0, 0, 0};
    private static final int[] OFFSET_Z = {0, 0, -1, 1, 0, 0, 0, 0};

    private static void resendBlock(EntityPlayerMP player, World world, int x, int y, int z) {
        player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
    }
}
