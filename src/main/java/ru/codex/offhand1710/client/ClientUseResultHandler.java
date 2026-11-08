package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import ru.codex.offhand1710.ItemShield;
import ru.codex.offhand1710.OffhandInventory;
import ru.codex.offhand1710.OffhandMessages;

public final class ClientUseResultHandler implements IMessageHandler<OffhandMessages.UseResult, IMessage> {
    @Override
    public IMessage onMessage(final OffhandMessages.UseResult message, MessageContext context) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {
                    return;
                }
                OffhandInventory offhand = OffhandInventory.get(mc.thePlayer);
                if ((message.flags & OffhandMessages.RESULT_SHIELD_DISABLED) != 0) {
                    // An axe knocked the shield down: lower it in whichever hand holds it.
                    offhand.shieldCooldown = message.useCount;
                    ItemStack main = mc.thePlayer.getItemInUse();
                    if (mc.thePlayer.isUsingItem() && main != null && main.getItem() instanceof ItemShield) {
                        mc.thePlayer.clearItemInUse();
                    }
                    return;
                }
                if ((message.flags & OffhandMessages.RESULT_START_USE) != 0) {
                    OffhandUseHandler.consumePredicted(message.sequence);
                    offhand.useCount = message.useCount;
                    OffhandClientState.startUse(offhand.getStack());
                    return;
                }
                if ((message.flags & OffhandMessages.RESULT_STOP_USE) != 0) {
                    if ((message.flags & OffhandMessages.RESULT_FINISHED) != 0) {
                        OffhandClientState.finishUse(mc.thePlayer);
                    }
                    offhand.useCount = 0;
                    return;
                }
                if (OffhandUseHandler.consumePredicted(message.sequence)) {
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
