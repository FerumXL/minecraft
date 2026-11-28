package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;

import ru.codex.offhand1710.Teams112;

/** The client side of Teams112: the options the server sent, and name tags hidden by nametagVisibility. */
public final class TeamsClient {
    /** RenderLivingBase.canRenderName of 1.12.2: a player's name tag follows the nametagVisibility of their team. */
    @SubscribeEvent
    public void onSpecials(RenderLivingEvent.Specials.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entity instanceof EntityPlayer && mc.thePlayer != null && !Teams112.nameShown(mc.thePlayer, event.entity)) {
            event.setCanceled(true);
        }
    }

    /** A new server starts with no options until it sends its own. */
    @SubscribeEvent
    public void onConnect(cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Teams112.CLIENT_NAMETAG.clear();
        Teams112.CLIENT_COLLISION.clear();
    }

    public static final class Handler implements IMessageHandler<Teams112.Sync, IMessage> {
        @Override
        public IMessage onMessage(Teams112.Sync message, MessageContext ctx) {
            message.apply();
            return null;
        }
    }
}
