package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import ru.codex.offhand1710.OffhandMessages;

/** Another player swung the offhand: start the third-person left arm swing. */
public final class ClientSwingHandler implements IMessageHandler<OffhandMessages.Swing, IMessage> {
    @Override
    public IMessage onMessage(final OffhandMessages.Swing message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.theWorld == null) {
                    return;
                }
                Entity entity = mc.theWorld.getEntityByID(message.entityId);
                if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                    OffhandClientState.startSwing((EntityPlayer) entity);
                }
            }
        });
        return null;
    }
}
