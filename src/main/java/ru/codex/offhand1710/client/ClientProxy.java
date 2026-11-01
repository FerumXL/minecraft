package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.input.Keyboard;
import ru.codex.offhand1710.CommonProxy;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.OffhandInventory;
import ru.codex.offhand1710.OffhandMessages;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInitialize() {
        SimpleNetworkWrapper channel = MC112Bridge.NETWORK.channel;
        channel.registerMessage(ClientSyncHandler.class, OffhandMessages.Sync.class, 2, Side.CLIENT);
        ClientRegistry.registerKeyBinding(OffhandKeyHandler.SWAP_KEY);
        ClientRegistry.registerKeyBinding(OffhandKeyHandler.USE_KEY);
        FMLCommonHandler.instance().bus().register(new OffhandKeyHandler());
        MinecraftForge.EVENT_BUS.register(new OffhandHud());
        MinecraftForge.EVENT_BUS.register(new OffhandPlayerRenderer());
    }

    public static void scheduleOffhandSync(final int entityId, final ItemStack stack) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft minecraft = Minecraft.getMinecraft();
                Entity entity = minecraft.theWorld == null ? null : minecraft.theWorld.getEntityByID(entityId);
                if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
                    OffhandInventory.get((net.minecraft.entity.player.EntityPlayer) entity)
                            .setStack(stack == null ? null : stack.copy());
                }
            }
        });
    }
}

