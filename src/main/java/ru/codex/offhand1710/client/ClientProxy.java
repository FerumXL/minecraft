package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.MinecraftForgeClient;
import ru.codex.offhand1710.CommonProxy;
import ru.codex.offhand1710.PortedBlocks;
import ru.codex.offhand1710.OffhandInventory;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInitialize() {
        ClientRegistry.registerKeyBinding(OffhandKeyHandler.SWAP_KEY);

        // FML bus: key input and client ticks.
        FMLCommonHandler.instance().bus().register(new OffhandKeyHandler());
        FMLCommonHandler.instance().bus().register(new OffhandUseHandler());
        FMLCommonHandler.instance().bus().register(new OffhandClientState());
        PickBlockFix pickBlock = new PickBlockFix();
        FMLCommonHandler.instance().bus().register(pickBlock);
        MinecraftForge.EVENT_BUS.register(pickBlock);
        ElytraClient elytra = new ElytraClient();
        FMLCommonHandler.instance().bus().register(elytra);
        MinecraftForge.EVENT_BUS.register(elytra);
        MinecraftForge.EVENT_BUS.register(new ElytraRenderer());
        CombatClient combat = new CombatClient();
        FMLCommonHandler.instance().bus().register(combat);
        MinecraftForge.EVENT_BUS.register(combat);

        // Forge bus: rendering and screens.
        MinecraftForge.EVENT_BUS.register(new OffhandHud());
        MinecraftForge.EVENT_BUS.register(new OffhandPlayerRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandFirstPersonRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandGuiHandler());
        MinecraftForge.EVENT_BUS.register(new WolfCollarColors());

        // Diagnostic only: active when mc112bridge-glprobe.txt exists in the game directory.
        if (new java.io.File(Minecraft.getMinecraft().mcDataDir, "mc112bridge-glprobe.txt").exists()) {
            GlStackProbe probe = new GlStackProbe();
            FMLCommonHandler.instance().bus().register(probe);
            MinecraftForge.EVENT_BUS.register(probe);
            cpw.mods.fml.common.FMLLog.info("[mc112bridge GL probe] enabled");
        }
    }

    @Override
    public void initialize() {
        MinecraftForgeClient.registerItemRenderer(PortedBlocks.shield, new ShieldRenderer());
    }

    public static void scheduleOffhandSync(final int entityId, final ItemStack stack, final int useCount,
            final int shieldCooldown) {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                Minecraft minecraft = Minecraft.getMinecraft();
                Entity entity = minecraft.theWorld == null ? null : minecraft.theWorld.getEntityByID(entityId);
                if (entity instanceof EntityPlayer) {
                    OffhandInventory offhand = OffhandInventory.get((EntityPlayer) entity);
                    offhand.setStack(stack == null ? null : stack.copy());
                    offhand.shieldCooldown = shieldCooldown;
                    // Other players: the animation follows the server. The local player follows UseResult,
                    // only a stop is taken from here too.
                    if (entity != minecraft.thePlayer || useCount == 0) {
                        offhand.useCount = useCount;
                    }
                }
            }
        });
    }
}
