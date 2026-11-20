package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.Spectator112;

/**
 * Client side of the spectator mode: other spectators are not drawn (unless this player is one too), the own hand,
 * hotbar, health and the overlay of the block the camera is in are hidden, and a left click on a living entity looks
 * through its eyes (EntityRenderer of 1.12.2 with a spectated entity) until sneaking.
 */
public final class SpectatorClient {
    public static final class Handler implements IMessageHandler<Spectator112.Msg, IMessage> {
        @Override
        public IMessage onMessage(final Spectator112.Msg message, MessageContext context) {
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override
                public void run() {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (message.on) {
                        Spectator112.CLIENT.add(message.entityId);
                    } else {
                        Spectator112.CLIENT.remove(message.entityId);
                        if (mc.thePlayer != null && mc.thePlayer.getEntityId() == message.entityId) {
                            mc.thePlayer.noClip = false;
                            mc.renderViewEntity = mc.thePlayer;
                        }
                    }
                }
            });
            return null;
        }
    }

    private static boolean self() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && Spectator112.is(mc.thePlayer);
    }

    @SubscribeEvent
    public void onPlayer(RenderPlayerEvent.Pre event) {
        EntityPlayer p = event.entityPlayer;
        // others do not see a spectator; a spectator looking through a mob does not see itself either
        if (Spectator112.is(p) && (p == Minecraft.getMinecraft().thePlayer || !self())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent event) {
        if (self()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (!self()) {
            return;
        }
        switch (event.type) {
            case HOTBAR:
            case HEALTH:
            case FOOD:
            case ARMOR:
            case EXPERIENCE:
            case AIR:
            case CROSSHAIRS:
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (self()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        // START: before Minecraft.runTick handles the attack key, so the click is taken here and not used to hit
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) {
            return;
        }
        if (!self()) {
            if (mc.renderViewEntity != mc.thePlayer && mc.renderViewEntity != null && mc.thePlayer != null && wasSpectating) {
                mc.renderViewEntity = mc.thePlayer;
            }
            wasSpectating = false;
            return;
        }
        if (mc.renderViewEntity != mc.thePlayer && (mc.thePlayer.isSneaking() || mc.renderViewEntity == null || mc.renderViewEntity.isDead)) {
            mc.renderViewEntity = mc.thePlayer;
            MC112Bridge.NETWORK.channel.sendToServer(new Spectator112.SpectateMsg(-1));
            wasSpectating = false;
        }
        while (mc.currentScreen == null && mc.gameSettings.keyBindAttack.isPressed()) {
            MovingObjectPosition hit = mc.objectMouseOver;
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit instanceof EntityLivingBase) {
                look((EntityLivingBase) hit.entityHit);
            }
        }
    }

    private static boolean wasSpectating;

    public static void look(EntityLivingBase target) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.renderViewEntity = target;
        wasSpectating = true;
        MC112Bridge.NETWORK.channel.sendToServer(new Spectator112.SpectateMsg(target.getEntityId()));
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            Spectator112.CLIENT.clear();
        }
    }
}
