package ru.codex.offhand1710.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandContainer;
import ru.codex.offhand1710.OffhandMessages;
import ru.codex.offhand1710.SlotOffhand;

/**
 * Listener on the client's own player container. The creative inventory edits that container locally and
 * reports every changed slot to its listeners; vanilla's listener sends a creative slot packet the server
 * rejects for slots above 44. This listener sends the offhand slot through the mod's own creative message.
 */
public final class OffhandCreativeSync implements ICrafting {
    @Override
    public void sendContainerAndContentsToPlayer(Container container, List items) {
    }

    @Override
    public void sendSlotContents(Container container, int slotIndex, ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || !mc.playerController.isInCreativeMode()) {
            return;
        }
        SlotOffhand slot = OffhandContainer.findSlot(container);
        if (slot != null && slot.slotNumber == slotIndex) {
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.CreativeSet(stack));
        }
    }

    @Override
    public void sendProgressBarUpdate(Container container, int id, int value) {
    }
}
