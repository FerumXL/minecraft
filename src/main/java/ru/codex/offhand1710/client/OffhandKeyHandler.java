package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandMessages;

public final class OffhandKeyHandler {
    public static final KeyBinding SWAP_KEY =
            new KeyBinding("key.mc112bridge.swap_offhand", Keyboard.KEY_G, "key.categories.mc112bridge");
    public static final KeyBinding USE_KEY =
            new KeyBinding("key.mc112bridge.use_offhand", Keyboard.KEY_V, "key.categories.mc112bridge");

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null || minecraft.currentScreen != null) {
            return;
        }
        if (SWAP_KEY.isPressed()) {
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.SwapRequest());
        }
        if (USE_KEY.isPressed()) {
            int entityId = -1;
            MovingObjectPosition hit = minecraft.objectMouseOver;
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                    && hit.entityHit != null) {
                entityId = hit.entityHit.getEntityId();
            }
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.UseRequest(entityId));
        }
    }
}

