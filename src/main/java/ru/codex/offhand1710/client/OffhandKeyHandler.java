package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandMessages;

/** "Swap Item In Hands", key F by default as in 1.12.2 (rebindable in the controls menu). */
public final class OffhandKeyHandler {
    public static final KeyBinding SWAP_KEY =
            new KeyBinding("key.mc112bridge.swap_offhand", Keyboard.KEY_F, "key.categories.mc112bridge");

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null || minecraft.currentScreen != null) {
            return;
        }
        while (SWAP_KEY.isPressed()) {
            MC112Bridge.NETWORK.channel.sendToServer(new OffhandMessages.SwapRequest());
        }
    }
}
