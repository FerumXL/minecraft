package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import ru.codex.offhand1710.OffhandMessages;

public final class ClientUseResultHandler implements IMessageHandler<OffhandMessages.UseResult, IMessage> {
    @Override
    public IMessage onMessage(final OffhandMessages.UseResult message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (OffhandUseHandler.consumePredicted(message.sequence) || mc.thePlayer == null) {
                    return; // already animated when the click was predicted on the client
                }
                if ((message.flags & OffhandMessages.RESULT_SWING) != 0) {
                    OffhandClientState.startSwing(mc.thePlayer);
                }
                if ((message.flags & OffhandMessages.RESULT_EQUIP) != 0) {
                    OffhandClientState.resetEquip();
                }
            }
        });
        return null;
    }
}
