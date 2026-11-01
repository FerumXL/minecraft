package ru.codex.offhand1710;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class OffhandEvents {
    private static final ConcurrentMap<UUID, Boolean> SWAP_REQUESTS = new ConcurrentHashMap<UUID, Boolean>();
    private static final ConcurrentMap<UUID, Integer> USE_REQUESTS = new ConcurrentHashMap<UUID, Integer>();

    public static void queueSwap(UUID playerId) {
        SWAP_REQUESTS.put(playerId, Boolean.TRUE);
    }

    public static void queueUse(UUID playerId, int entityId) {
        USE_REQUESTS.put(playerId, Integer.valueOf(entityId));
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        OffhandInventory.register(event.entity);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        OffhandInventory.get(event.entityPlayer).copyFrom(OffhandInventory.get(event.original));
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

        UUID id = event.player.getUniqueID();
        if (SWAP_REQUESTS.remove(id) != null) {
            swap((EntityPlayerMP) event.player);
        }
        Integer entityId = USE_REQUESTS.remove(id);
        if (entityId != null) {
            useOffhand((EntityPlayerMP) event.player, entityId.intValue());
        }
    }

    private static void swap(EntityPlayerMP player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.getStackInSlot(slot);
        ItemStack other = offhand.getStack();
        player.inventory.setInventorySlotContents(slot, other);
        offhand.setStack(main);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        OffhandMessages.synchronize(player);
    }

    private static void useOffhand(EntityPlayerMP player, int entityId) {
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack stack = offhand.getStack();
        if (stack == null || stack.stackSize <= 0) {
            return;
        }

        World world = player.worldObj;
        if (entityId >= 0) {
            Entity target = world.getEntityByID(entityId);
            if (target instanceof EntityLivingBase && player.getDistanceSqToEntity(target) <= 36.0D
                    && player.canEntityBeSeen(target)) {
                stack.getItem().itemInteractionForEntity(stack, player, (EntityLivingBase) target);
            }
        } else {
            MovingObjectPosition hit = player.rayTrace(5.0D, 1.0F);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int x = hit.blockX;
                int y = hit.blockY;
                int z = hit.blockZ;
                int side = hit.sideHit;
                float hx = (float) (hit.hitVec.xCoord - x);
                float hy = (float) (hit.hitVec.yCoord - y);
                float hz = (float) (hit.hitVec.zCoord - z);
                boolean used = stack.getItem().onItemUseFirst(stack, player, world, x, y, z, side, hx, hy, hz);
                if (!used) {
                    int mainSlot = player.inventory.currentItem;
                    ItemStack main = player.inventory.getStackInSlot(mainSlot);
                    Block block = world.getBlock(x, y, z);
                    player.inventory.setInventorySlotContents(mainSlot, stack);
                    try {
                        used = block.onBlockActivated(world, x, y, z, player, side, hx, hy, hz);
                    } finally {
                        stack = player.inventory.getStackInSlot(mainSlot);
                        player.inventory.setInventorySlotContents(mainSlot, main);
                    }
                }
                if (!used && stack != null) {
                    stack.getItem().onItemUse(stack, player, world, x, y, z, side, hx, hy, hz);
                }
            } else {
                ItemStack result = stack.getItem().onItemRightClick(stack, world, player);
                if (result != null) {
                    stack = result;
                }
            }
        }

        offhand.setStack(stack != null && stack.stackSize > 0 ? stack : null);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        OffhandMessages.synchronize(player);
    }
}

