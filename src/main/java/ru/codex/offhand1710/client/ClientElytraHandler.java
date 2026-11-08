package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import ru.codex.offhand1710.OffhandInventory;
import ru.codex.offhand1710.OffhandMessages;

public final class ClientElytraHandler implements IMessageHandler<OffhandMessages.ElytraState, IMessage> {
    @Override
    public IMessage onMessage(final OffhandMessages.ElytraState message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                Entity entity = mc.theWorld == null ? null : mc.theWorld.getEntityByID(message.entityId);
                if (entity instanceof EntityPlayer) {
                    OffhandInventory state = OffhandInventory.get((EntityPlayer) entity);
                    state.elytraFlying = message.flying;
                    if (message.boost > 0) {
                        state.elytraBoost = Math.max(state.elytraBoost, message.boost);
                    }
                }
            }
        });
        return null;
    }
}
