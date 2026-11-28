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
    public void openStructure(ru.codex.offhand1710.StructureBlock112.Tile tile) {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new StructureGui(tile));
    }

    @Override
    public Object getClientGuiElement(int id, net.minecraft.entity.player.EntityPlayer player, net.minecraft.world.World world, int x, int y, int z) {
        if (id == ru.codex.offhand1710.Items112.GUI_LLAMA) {
            net.minecraft.entity.Entity e = world.getEntityByID(x);
            return e instanceof ru.codex.offhand1710.MoreMobs.Llama ? new LlamaGui(player.inventory, (ru.codex.offhand1710.MoreMobs.Llama) e, y) : null;
        }
        if (id == ru.codex.offhand1710.Items112.GUI_ENCHANT) {
            return new EnchantGui112(player.inventory, world, x, y, z);
        }
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x, y, z);
        return id == ru.codex.offhand1710.Items112.GUI_BOX && tile instanceof ru.codex.offhand1710.BlockShulkerBox.Tile ? Items112Client.boxScreen(player.inventory, (ru.codex.offhand1710.BlockShulkerBox.Tile) tile) : null;
    }

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
        if (!ru.codex.offhand1710.Sounds112.OFF) {
            MinecraftForge.EVENT_BUS.register(new Sounds112Client());
        }
        CombatClient combat = new CombatClient();
        FMLCommonHandler.instance().bus().register(combat);
        MinecraftForge.EVENT_BUS.register(combat);

        // Forge bus: rendering and screens.
        MinecraftForge.EVENT_BUS.register(new OffhandHud());
        Border112Client border = new Border112Client();
        MinecraftForge.EVENT_BUS.register(border);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(border);
        MinecraftForge.EVENT_BUS.register(new ArmorTooltip());
        RulesClient rules = new RulesClient();
        MinecraftForge.EVENT_BUS.register(rules);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(rules);
        MinecraftForge.EVENT_BUS.register(new Cooldowns112Client());
        FMLCommonHandler.instance().bus().register(new AutoJump112());
        Hotbars112.register();
        Particles112Client.register();
        TeamsClient teams = new TeamsClient();
        MinecraftForge.EVENT_BUS.register(teams);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(teams);
        Debug112 debug = new Debug112();
        MinecraftForge.EVENT_BUS.register(debug);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(debug);
        MinecraftForge.EVENT_BUS.register(new OffhandPlayerRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandFirstPersonRenderer());
        MinecraftForge.EVENT_BUS.register(new OffhandGuiHandler());
        MinecraftForge.EVENT_BUS.register(new WolfCollarColors());
        // Puts unbalanced GL matrix stacks back at the end of a frame (OpenGL error 1283 with OptiFine + HBM).
        FMLCommonHandler.instance().bus().register(new GlStackGuard());

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
        Items112Client.register();
        TitleOverlay.register();
        Client122.register();
        GlowOutline.register();
        DataClient.register();
        Banner112Client.register();
        Audit112.start();
        Scenarios.start();
        MinecraftForgeClient.registerItemRenderer(PortedBlocks.shield, new ShieldRenderer());
        EndRenderers.register();
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(ru.codex.offhand1710.End112.GatewayTile.class, new GatewayRender());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(ru.codex.offhand1710.StructureBlock112.Tile.class, new StructureGui.Render());
        DragonHeadRenderer dragonHead = new DragonHeadRenderer();
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(ru.codex.offhand1710.BlockDragonHead.Tile.class, dragonHead);
        MinecraftForgeClient.registerItemRenderer(net.minecraft.item.Item.getItemFromBlock(ru.codex.offhand1710.End112.dragonHead), dragonHead);
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.End112.Crystal.class, new CrystalRender());
        MobRenderers2.register();
        MoreMobsClient.register();
        FMLCommonHandler.instance().bus().register(new MoreMobsClient.Ticker());
        MinecraftForge.EVENT_BUS.register(new MoreMobsClient.ShoulderLayer());
        SpectatorClient spectator = new SpectatorClient();
        MinecraftForge.EVENT_BUS.register(spectator);
        FMLCommonHandler.instance().bus().register(spectator);
        SpectatorMenu menu = new SpectatorMenu();
        MinecraftForge.EVENT_BUS.register(menu);
        FMLCommonHandler.instance().bus().register(menu);
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.EntityGuardian.class, new RenderGuardian());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.EntityElderGuardian.class, new RenderGuardian());
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

    @Override
    public boolean limitedCrafting(net.minecraft.world.World w) {
        return w != null && w.isRemote ? RulesClient.limitedCrafting : super.limitedCrafting(w);
    }

    @Override
    public boolean knowsRecipe(net.minecraft.entity.player.EntityPlayer p, String id) {
        return p.worldObj.isRemote ? DataClient.knows(id) : super.knowsRecipe(p, id);
    }

    @Override
    public net.minecraft.entity.player.EntityPlayer clientPlayer() {
        return net.minecraft.client.Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public boolean isLocalPlayer(net.minecraft.entity.Entity e) {
        return e != null && e == net.minecraft.client.Minecraft.getMinecraft().thePlayer;
    }
}
