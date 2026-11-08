package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import ru.codex.offhand1710.Elytra112;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandInventory;
import ru.codex.offhand1710.OffhandMessages;

/**
 * The glide of the local player, EntityLivingBase.travel of 1.12.2. At the start of the player's tick the motion for
 * this tick is computed, vanilla moves the player by it (the movement input is zero while gliding), and at the end of
 * the tick the air drag and gravity vanilla added afterwards are replaced by the computed motion again.
 */
public final class ElytraClient {
    /** Not in 1.12.2 (asked for by the mod's author): gliding cannot climb above the build limit. */
    private static final double MAX_HEIGHT = 256.0D;

    private boolean lastJump;
    private boolean simulating;
    private double savedX;
    private double savedY;
    private double savedZ;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!event.player.worldObj.isRemote) {
            return;
        }
        OffhandInventory state = OffhandInventory.get(event.player);
        if (event.player != mc.thePlayer) {
            if (event.phase == TickEvent.Phase.START) {
                state.elytraTicks = state.elytraFlying ? state.elytraTicks + 1 : 0;
            }
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            tickStart(event.player, state);
        } else if (simulating) {
            simulating = false;
            EntityPlayer player = event.player;
            player.motionX = player.motionX == 0.0D ? 0.0D : savedX;
            player.motionZ = player.motionZ == 0.0D ? 0.0D : savedZ;
            if (!player.isCollidedVertically) {
                player.motionY = savedY;
            }
        }
    }

    /** Minecraft.rightClickMouse of 1.12.2: a rocket used in the air makes the held item dip and come back up. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR && event.entityPlayer == mc.thePlayer
                && OffhandInventory.get(mc.thePlayer).elytraFlying) {
            ItemStack held = mc.thePlayer.getCurrentEquippedItem();
            if (held != null && held.getItem() == Items.fireworks) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2();
            }
        }
    }

    private void tickStart(EntityPlayer player, OffhandInventory state) {
        boolean jump = Minecraft.getMinecraft().thePlayer.movementInput != null
                && Minecraft.getMinecraft().thePlayer.movementInput.jump;
        if (jump && !lastJump && player.motionY < 0.0D && !state.elytraFlying && Elytra112.canFly(player)) {
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.ElytraStart());
        }
        lastJump = jump;
        if (state.elytraFlying && !Elytra112.canFly(player)) {
            state.elytraFlying = false;
            state.elytraBoost = 0;
        }
        if (!state.elytraFlying) {
            state.elytraTicks = 0;
            return;
        }
        state.elytraTicks++;
        Vec3 look = player.getLookVec();
        if (state.elytraBoost > 0) {
            // EntityFireworkRocket.onUpdate of 1.12.2, for a rocket attached to the player.
            state.elytraBoost--;
            player.motionX += look.xCoord * 0.1D + (look.xCoord * 1.5D - player.motionX) * 0.5D;
            player.motionY += look.yCoord * 0.1D + (look.yCoord * 1.5D - player.motionY) * 0.5D;
            player.motionZ += look.zCoord * 0.1D + (look.zCoord * 1.5D - player.motionZ) * 0.5D;
            player.worldObj.spawnParticle("fireworksSpark", player.posX, player.posY - 1.0D, player.posZ, 0.0D, 0.0D, 0.0D);
        }
        if (player.motionY > -0.5D) {
            player.fallDistance = 1.0F;
        }
        float pitch = player.rotationPitch * 0.017453292F;
        double lookHorizontal = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
        double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double lookLength = look.lengthVector();
        float lift = MathHelper.cos(pitch);
        lift = (float) ((double) lift * (double) lift * Math.min(1.0D, lookLength / 0.4D));
        player.motionY += -0.08D + (double) lift * 0.06D;
        if (player.motionY < 0.0D && lookHorizontal > 0.0D) {
            double glide = player.motionY * -0.1D * (double) lift;
            player.motionY += glide;
            player.motionX += look.xCoord * glide / lookHorizontal;
            player.motionZ += look.zCoord * glide / lookHorizontal;
        }
        if (pitch < 0.0F) {
            double climb = speed * (double) (-MathHelper.sin(pitch)) * 0.04D;
            player.motionY += climb * 3.2D;
            player.motionX -= look.xCoord * climb / lookHorizontal;
            player.motionZ -= look.zCoord * climb / lookHorizontal;
        }
        if (lookHorizontal > 0.0D) {
            player.motionX += (look.xCoord / lookHorizontal * speed - player.motionX) * 0.1D;
            player.motionZ += (look.zCoord / lookHorizontal * speed - player.motionZ) * 0.1D;
        }
        player.motionX *= 0.9900000095367432D;
        player.motionY *= 0.9800000190734863D;
        player.motionZ *= 0.9900000095367432D;
        if (player.boundingBox.minY >= MAX_HEIGHT && player.motionY > 0.0D) {
            player.motionY = 0.0D; // the ceiling of the glide; a rocket cannot push the player higher either
        }
        savedX = player.motionX;
        savedY = player.motionY;
        savedZ = player.motionZ;
        simulating = true;
    }
}
