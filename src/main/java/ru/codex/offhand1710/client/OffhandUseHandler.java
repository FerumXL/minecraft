package ru.codex.offhand1710.client;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandInventory;
import ru.codex.offhand1710.OffhandMessages;

/**
 * Right mouse button with two hands, as in 1.12.2: the main hand acts first; only if it did nothing does
 * the offhand get its turn.
 *
 * FML's client tick START runs before Minecraft.runTick reads the use key. While the offhand holds an item,
 * this handler takes the presses itself (the key is consumed, and the right-click delay is set like vanilla
 * does, so vanilla does not click a second time), runs the exact vanilla main-hand logic of
 * Minecraft.func_147121_ag and, when that did nothing, asks the server to use the offhand item on the same
 * target. With an empty offhand nothing is intercepted.
 */
public final class OffhandUseHandler {
    private static Field rightClickDelayTimer;
    private static boolean broken;

    private static Field delayField() {
        if (rightClickDelayTimer == null) {
            rightClickDelayTimer = ReflectionHelper.findField(Minecraft.class, "field_71467_ac", "rightClickDelayTimer");
        }
        return rightClickDelayTimer;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || broken) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null) {
            return;
        }
        OffhandInventory offhand = OffhandInventory.get(player);
        if (offhand.isUsing()) {
            try {
                holdOffhandUse(mc, offhand);
            } catch (Exception e) {
                broken = true;
                FMLLog.severe("[mc112bridge] offhand right click disabled: %s", e);
            }
            return;
        }
        if (offhand.getStack() == null || player.isUsingItem()) {
            return;
        }
        try {
            KeyBinding use = mc.gameSettings.keyBindUseItem;
            boolean clicked = false;
            while (use.isPressed()) {
                rightClick(mc, player);
                clicked = true;
            }
            if (!clicked && use.getIsKeyPressed() && delayField().getInt(mc) == 0 && !player.isUsingItem()) {
                rightClick(mc, player);
            }
        } catch (Exception e) {
            // Without access to the delay field vanilla would click a second time; fall back to vanilla only.
            broken = true;
            FMLLog.severe("[mc112bridge] offhand right click disabled: %s", e);
        }
    }

    private static Field leftClickCounter;

    /**
     * While the offhand item is in use, as Minecraft.runTick does for an item in use: releasing the use button
     * stops it (a bow shoots), clicks of both buttons are swallowed, and block breaking pauses.
     */
    private static void holdOffhandUse(Minecraft mc, OffhandInventory offhand) throws IllegalAccessException {
        KeyBinding use = mc.gameSettings.keyBindUseItem;
        if (!use.getIsKeyPressed()) {
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.Release());
            offhand.useCount = 0;
        }
        while (use.isPressed()) {
            // swallowed
        }
        while (mc.gameSettings.keyBindAttack.isPressed()) {
            // swallowed
        }
        delayField().setInt(mc, 4);
        if (leftClickCounter == null) {
            leftClickCounter = ReflectionHelper.findField(Minecraft.class, "field_71429_W", "leftClickCounter");
        }
        if (leftClickCounter.getInt(mc) < 2) {
            leftClickCounter.setInt(mc, 2);
        }
    }

    private static void rightClick(Minecraft mc, EntityClientPlayerMP player) throws IllegalAccessException {
        delayField().setInt(mc, 4);
        if (mainHandActed(mc, player)) {
            return;
        }
        OffhandMessages.UseRequest request = offhandRequest(mc);
        request.sequence = ++sequence;
        // Vanilla predicts block clicks on the client so placing has no delay; do the same for the offhand.
        if (request.kind == OffhandMessages.USE_BLOCK && predictBlockClick(mc, player, request)) {
            if (predicted.size() > 64) {
                predicted.clear(); // answers that never came (request replaced on the server)
            }
            predicted.add(Integer.valueOf(request.sequence));
        }
        MC112Bridge.NETWORK.channel.sendToServer(request);
    }

    private static int sequence;
    private static final Set<Integer> predicted = new HashSet<Integer>();

    /** True when the answer to this request is for a click already animated on the client. */
    static boolean consumePredicted(int requestSequence) {
        return predicted.remove(Integer.valueOf(requestSequence));
    }

    /**
     * Client half of PlayerControllerMP.onPlayerRightClick (1.7.10) for the offhand item, without the packet:
     * onItemUseFirst, block activation (skipped while sneaking with an item unless it bypasses), the
     * ItemBlock placement check, then tryPlaceItemIntoWorld (stack size kept in creative). The item sits in
     * the selected hotbar slot during the call, as on the server. The server answers with the real result and
     * resends the affected blocks, which corrects a wrong guess.
     */
    private static boolean predictBlockClick(Minecraft mc, EntityClientPlayerMP player,
            OffhandMessages.UseRequest request) {
        OffhandInventory offhand = OffhandInventory.get(player);
        ItemStack stack = offhand.getStack();
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        World world = mc.theWorld;
        int x = request.x;
        int y = request.y;
        int z = request.z;
        int side = request.side;
        int slot = player.inventory.currentItem;
        ItemStack main = player.inventory.mainInventory[slot];
        int sizeBefore = stack.stackSize;
        boolean creative = mc.playerController.isInCreativeMode();
        boolean acted;
        player.inventory.mainInventory[slot] = stack;
        try {
            if (stack.getItem().onItemUseFirst(stack, player, world, x, y, z, side,
                    request.hitX, request.hitY, request.hitZ)) {
                acted = true;
            } else {
                boolean activated = false;
                if (!player.isSneaking() || player.getHeldItem() == null
                        || player.getHeldItem().getItem().doesSneakBypassUse(world, x, y, z, player)) {
                    activated = world.getBlock(x, y, z).onBlockActivated(world, x, y, z, player, side,
                            request.hitX, request.hitY, request.hitZ);
                }
                if (activated) {
                    acted = true;
                } else if (stack.getItem() instanceof ItemBlock
                        && !((ItemBlock) stack.getItem()).func_150936_a(world, x, y, z, side, player, stack)) {
                    acted = false;
                } else if (creative) {
                    int damage = stack.getItemDamage();
                    int size = stack.stackSize;
                    acted = stack.tryPlaceItemIntoWorld(player, world, x, y, z, side,
                            request.hitX, request.hitY, request.hitZ);
                    stack.setItemDamage(damage);
                    stack.stackSize = size;
                } else {
                    acted = stack.tryPlaceItemIntoWorld(player, world, x, y, z, side,
                            request.hitX, request.hitY, request.hitZ);
                    if (acted && stack.stackSize <= 0) {
                        MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack));
                    }
                }
            }
        } finally {
            ItemStack after = player.inventory.mainInventory[slot];
            player.inventory.mainInventory[slot] = main;
            offhand.setStack(after != null && after.stackSize > 0 ? after : null);
        }
        if (acted) {
            OffhandClientState.startSwing(player);
            if (stack.stackSize != sizeBefore || creative) {
                OffhandClientState.resetEquip();
            }
        }
        return acted;
    }

    /** Vanilla Minecraft.func_147121_ag (1.7.10), returning whether the main hand did anything. */
    private static boolean mainHandActed(Minecraft mc, EntityClientPlayerMP player) {
        ItemStack main = player.inventory.getCurrentItem();
        MovingObjectPosition hit = mc.objectMouseOver;
        boolean tryAir = true;
        if (hit != null) {
            if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                if (mc.playerController.interactWithEntitySendPacket(player, hit.entityHit)) {
                    return true;
                }
            } else if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int x = hit.blockX;
                int y = hit.blockY;
                int z = hit.blockZ;
                if (!mc.theWorld.getBlock(x, y, z).isAir(mc.theWorld, x, y, z)) {
                    int sizeBefore = main != null ? main.stackSize : 0;
                    boolean allowed = !ForgeEventFactory.onPlayerInteract(player,
                            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, hit.sideHit, mc.theWorld).isCanceled();
                    if (!allowed) {
                        return true; // another mod took the click
                    }
                    if (mc.playerController.onPlayerRightClick(player, mc.theWorld, main, x, y, z, hit.sideHit,
                            hit.hitVec)) {
                        player.swingItem();
                        afterBlockClick(mc, player, main, sizeBefore);
                        return true;
                    }
                    if (main == null) {
                        return false; // vanilla stops here; with 1.12.2 hands the offhand gets its turn
                    }
                    afterBlockClick(mc, player, main, sizeBefore);
                }
            }
        }
        if (tryAir) {
            ItemStack current = player.inventory.getCurrentItem();
            boolean allowed = !ForgeEventFactory.onPlayerInteract(player,
                    PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, mc.theWorld).isCanceled();
            if (!allowed) {
                return true;
            }
            if (current != null) {
                int swingBefore = player.swingProgressInt;
                boolean wasSwinging = player.isSwingInProgress;
                if (mc.playerController.sendUseItem(player, mc.theWorld, current)) {
                    mc.entityRenderer.itemRenderer.resetEquippedProgress2();
                    return true;
                }
                // A rocket boosting the glide is the main hand's action; the offhand does not act as well.
                if (current.getItem() == net.minecraft.init.Items.fireworks
                        && ru.codex.offhand1710.OffhandInventory.get(player).elytraFlying) {
                    return true;
                }
                // Started eating, drinking, drawing a bow or blocking, or swung (fishing rod): the main hand acted.
                if (player.isUsingItem()) {
                    return true;
                }
                if (player.isSwingInProgress && player.swingProgressInt == -1
                        && (!wasSwinging || swingBefore != -1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void afterBlockClick(Minecraft mc, EntityClientPlayerMP player, ItemStack main, int sizeBefore) {
        if (main == null) {
            return;
        }
        if (main.stackSize == 0) {
            player.inventory.mainInventory[player.inventory.currentItem] = null;
        } else if (main.stackSize != sizeBefore || mc.playerController.isInCreativeMode()) {
            mc.entityRenderer.itemRenderer.resetEquippedProgress();
        }
    }

    private static OffhandMessages.UseRequest offhandRequest(Minecraft mc) {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit != null) {
            return OffhandMessages.UseRequest.entity(hit.entityHit.getEntityId());
        }
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && !mc.theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ)
                        .isAir(mc.theWorld, hit.blockX, hit.blockY, hit.blockZ)) {
            return OffhandMessages.UseRequest.block(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                    (float) (hit.hitVec.xCoord - hit.blockX), (float) (hit.hitVec.yCoord - hit.blockY),
                    (float) (hit.hitVec.zCoord - hit.blockZ));
        }
        return OffhandMessages.UseRequest.air();
    }
}
