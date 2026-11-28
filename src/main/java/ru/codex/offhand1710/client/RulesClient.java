package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import ru.codex.offhand1710.Rules112;

/**
 * reducedDebugInfo on the client: the F3 screen keeps the frame, chunk, entity and dimension lines and drops the
 * position, facing, biome and light ones (GuiOverlayDebug of 1.12.2 with hasReducedDebug).
 */
public final class RulesClient {
    public static volatile boolean reduced;
    public static volatile boolean limitedCrafting;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onText(RenderGameOverlayEvent.Text event) {
        if (!reduced || Rules112.OFF || event.left == null || !Debug112.OFF) {
            return;
        }
        // GuiIngameForge puts a spacer (null) after the dimension line; what follows is the player's position
        int spacer = event.left.indexOf(null);
        if (spacer >= 0) {
            while (event.left.size() > spacer + 1) {
                event.left.remove(event.left.size() - 1);
            }
        }
    }

    /** GuiIngame.renderAttackIndicator of 1.12.2 draws the crosshair only in the first person view. */
    @SubscribeEvent
    public void onCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && !Rules112.OFF
            && net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
            event.setCanceled(true);
        }
    }

    /** A new server starts with the full screen until it says otherwise. */
    @SubscribeEvent
    public void onConnect(cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        reduced = false;
        limitedCrafting = false;
    }

    public static final class Handler implements IMessageHandler<Rules112.Debug, IMessage> {
        @Override
        public IMessage onMessage(Rules112.Debug message, MessageContext ctx) {
            reduced = message.reduced;
            limitedCrafting = message.limited;
            return null;
        }
    }
}
