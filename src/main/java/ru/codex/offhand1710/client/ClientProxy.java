package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import ru.codex.offhand1710.CommonProxy;
import ru.codex.offhand1710.OffhandInventory;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInitialize() {
        ClientRegistry.registerKeyBinding(OffhandKeyHandler.SWAP_KEY);

        // FML bus: key input and client ticks.
        FMLCommonHandler.instance().bus().register(new OffhandKeyHandler());
        FMLCommonHandler.instance().bus().register(new OffhandUseHandler());
        FMLCommonHandler.instance().bus().register(new OffhandClientState());

        // Forge bus: rendering and screens.
        MinecraftForge.EVENT_BUS.register(new OffhandHud());
        MinecraftForge.EVENT_BUS.register(new OffhandPlayerRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandFirstPersonRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandGuiHandler());

        // Diagnostic only: active when mc112bridge-glprobe.txt exists in the game directory.
        if (new java.io.File(Minecraft.getMinecraft().mcDataDir, "mc112bridge-glprobe.txt").exists()) {
            GlStackProbe probe = new GlStackProbe();
            FMLCommonHandler.instance().bus().register(probe);
            MinecraftForge.EVENT_BUS.register(probe);
            cpw.mods.fml.common.FMLLog.info("[mc112bridge GL probe] enabled");
        }
    }

    public static void scheduleOffhandSync(final int entityId, final ItemStack stack) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft minecraft = Minecraft.getMinecraft();
                Entity entity = minecraft.theWorld == null ? null : minecraft.theWorld.getEntityByID(entityId);
                if (entity instanceof EntityPlayer) {
                    OffhandInventory.get((EntityPlayer) entity).setStack(stack == null ? null : stack.copy());
                }
            }
        });
    }
}
