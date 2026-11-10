package ru.codex.offhand1710.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.Content122;
import ru.codex.offhand1710.MC112Bridge;

/** /stopsound on the client, the command block mode buttons, subtitles and the left main hand. */
public final class Client122 {
    private static final Map<String, String> CAPTIONS = new HashMap<String, String>();
    private static final List<Caption> SHOWN = new ArrayList<Caption>();
    private static Configuration config;
    public static boolean subtitles;
    public static boolean leftHand;
    private GuiButton modeButton;
    private GuiButton condButton;
    private GuiButton autoButton;
    private GuiButton subtitleButton;
    private GuiButton handButton;
    private int mode;
    private boolean conditional;
    private boolean auto;
    private int bx;
    private int by;
    private int bz;

    private static final class Caption {
        String text;
        long time;
        double x;
        double y;
        double z;
    }

    public static int captionCount() {
        return SHOWN.size();
    }

    static void register() {
        config = new Configuration(new File(Minecraft.getMinecraft().mcDataDir, "config/mc112bridge-client.cfg"));
        subtitles = config.get("general", "showSubtitles", false).getBoolean(false);
        leftHand = config.get("general", "mainHandLeft", false).getBoolean(false);
        config.save();
        InputStream in = Client122.class.getResourceAsStream("/assets/mc112bridge/subtitles.txt");
        if (in != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    int i = line.indexOf('=');
                    if (i > 0) {
                        CAPTIONS.put(line.substring(0, i), line.substring(i + 1).trim());
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Client122 instance = new Client122();
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);
    }

    private static void save() {
        config.get("general", "showSubtitles", false).set(subtitles);
        config.get("general", "mainHandLeft", false).set(leftHand);
        config.save();
    }

    private static SoundCategory category(String source) {
        if ("master".equals(source)) return SoundCategory.MASTER;
        if ("music".equals(source)) return SoundCategory.MUSIC;
        if ("record".equals(source)) return SoundCategory.RECORDS;
        if ("weather".equals(source)) return SoundCategory.WEATHER;
        if ("block".equals(source)) return SoundCategory.BLOCKS;
        if ("hostile".equals(source)) return SoundCategory.MOBS;
        if ("neutral".equals(source)) return SoundCategory.ANIMALS;
        if ("player".equals(source)) return SoundCategory.PLAYERS;
        if ("ambient".equals(source)) return SoundCategory.AMBIENT;
        return null;
    }

    /** SoundManager.stop(sound, category) of 1.12.2. Text is "source|sound", either may be empty. */
    public static int stop(String text) {
        int bar = text.indexOf('|');
        String source = bar < 0 ? "" : text.substring(0, bar);
        String wanted = bar < 0 ? "" : text.substring(bar + 1);
        SoundCategory filter = category(source);
        SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
        SoundManager manager = ReflectionHelper.getPrivateValue(SoundHandler.class, handler, "sndManager", "field_147694_f");
        Map<?, ?> playing = ReflectionHelper.getPrivateValue(SoundManager.class, manager, "playingSounds", "field_148629_h");
        int stopped = 0;
        for (Object o : new ArrayList<Object>(playing.values())) {
            ISound sound = (ISound) o;
            ResourceLocation location = sound.getPositionedSoundLocation();
            SoundEventAccessorComposite event = handler.getSound(location);
            boolean sourceOk = filter == null || (event != null && event.getSoundCategory() == filter);
            boolean nameOk = wanted.length() == 0 || wanted.equals(location.toString()) || wanted.equals(location.getResourcePath());
            if (sourceOk && nameOk) {
                manager.stopSound(sound);
                stopped++;
            }
        }
        return stopped;
    }

    @SubscribeEvent
    public void onLogin(cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        sentHand = false;
    }

    private boolean sentHand;

    @SubscribeEvent
    public void onTick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (!sentHand && Minecraft.getMinecraft().thePlayer != null) {
            sentHand = true;
            MC112Bridge.NETWORK.channel.sendToServer(new Content122.HandMessage(leftHand));
        }
    }

    private static String onOff(String key, boolean on) {
        return StatCollector.translateToLocal(key) + ": " + StatCollector.translateToLocal(on ? "options.on" : "options.off");
    }

    private void text() {
        modeButton.displayString = StatCollector.translateToLocal(mode == 0 ? "advMode.mode.redstone" : (mode == 1 ? "advMode.mode.sequence" : "advMode.mode.auto"));
        condButton.displayString = StatCollector.translateToLocal(conditional ? "advMode.mode.conditional" : "advMode.mode.unconditional");
        autoButton.displayString = StatCollector.translateToLocal(auto ? "advMode.mode.autoexec.bat" : "advMode.mode.redstoneTriggered");
    }

    private String handText() {
        return StatCollector.translateToLocal("options.mainHand") + ": " + StatCollector.translateToLocal(leftHand ? "options.mainHand.left" : "options.mainHand.right");
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        int w = event.gui.width;
        int h = event.gui.height;
        if (event.gui instanceof GuiScreenOptionsSounds) {
            subtitleButton = new GuiButton(9101, w / 2 - 155, h / 6 + 120, 150, 20, onOff("options.showSubtitles", subtitles));
            handButton = new GuiButton(9102, w / 2 + 5, h / 6 + 120, 150, 20, handText());
            event.buttonList.add(subtitleButton);
            event.buttonList.add(handButton);
        } else if (event.gui instanceof GuiCommandBlock) {
            CommandBlockLogic logic = ReflectionHelper.getPrivateValue(GuiCommandBlock.class, (GuiCommandBlock) event.gui, "localCommandBlock", "field_146489_h");
            ChunkCoordinates c = logic.getPlayerCoordinates();
            if (logic.func_145751_f() != 0) {
                return;
            }
            bx = c.posX;
            by = c.posY;
            bz = c.posZ;
            TileEntity tile = Minecraft.getMinecraft().theWorld.getTileEntity(bx, by, bz);
            mode = 0;
            conditional = false;
            auto = false;
            if (tile instanceof Content122.ModeTile) {
                mode = tile.getBlockType() == Content122.chain ? 1 : 2;
                conditional = (tile.getBlockMetadata() & 8) != 0;
                auto = ((Content122.ModeTile) tile).auto;
            }
            modeButton = new GuiButton(9201, w / 2 - 150, 16, 96, 20, "");
            condButton = new GuiButton(9202, w / 2 - 48, 16, 96, 20, "");
            autoButton = new GuiButton(9203, w / 2 + 54, 16, 96, 20, "");
            event.buttonList.add(modeButton);
            event.buttonList.add(condButton);
            event.buttonList.add(autoButton);
            text();
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        int id = event.button.id;
        if (id == 9101) {
            subtitles = !subtitles;
            save();
            event.button.displayString = onOff("options.showSubtitles", subtitles);
        } else if (id == 9102) {
            leftHand = !leftHand;
            save();
            event.button.displayString = handText();
            if (Minecraft.getMinecraft().thePlayer != null) {
                MC112Bridge.NETWORK.channel.sendToServer(new Content122.HandMessage(leftHand));
            }
        } else if (id >= 9201 && id <= 9203) {
            if (id == 9201) {
                mode = (mode + 1) % 3;
            } else if (id == 9202) {
                conditional = !conditional;
            } else {
                auto = !auto;
            }
            text();
            MC112Bridge.NETWORK.channel.sendToServer(new Content122.ModeMessage(bx, by, bz, mode, conditional, auto));
        }
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent17 event) {
        if (!subtitles || event.sound == null) {
            return;
        }
        String key = CAPTIONS.get(event.sound.getPositionedSoundLocation().toString());
        if (key == null) {
            key = CAPTIONS.get(event.name);
        }
        if (key == null) {
            return;
        }
        String text = StatCollector.translateToLocal(key);
        for (Caption c : SHOWN) {
            if (c.text.equals(text)) {
                c.time = System.currentTimeMillis();
                c.x = event.sound.getXPosF();
                c.y = event.sound.getYPosF();
                c.z = event.sound.getZPosF();
                return;
            }
        }
        Caption c = new Caption();
        c.text = text;
        c.time = System.currentTimeMillis();
        c.x = event.sound.getXPosF();
        c.y = event.sound.getYPosF();
        c.z = event.sound.getZPosF();
        SHOWN.add(c);
    }

    /** GuiSubtitleOverlay; layout reconstructed from memory of 1.9, the texts come from 1.12.2. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !subtitles || SHOWN.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        long now = System.currentTimeMillis();
        int widest = 0;
        for (Iterator<Caption> it = SHOWN.iterator(); it.hasNext();) {
            Caption c = it.next();
            if (now - c.time > 3000L) {
                it.remove();
            } else {
                widest = Math.max(widest, font.getStringWidth(c.text));
            }
        }
        widest += font.getStringWidth("<") * 2 + font.getStringWidth(" ") * 2;
        int w = event.resolution.getScaledWidth();
        int h = event.resolution.getScaledHeight();
        Vec3 look = mc.renderViewEntity.getLookVec();
        int row = 0;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (Caption c : SHOWN) {
            double dx = c.x - mc.renderViewEntity.posX;
            double dz = c.z - mc.renderViewEntity.posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            double side = len < 1.0E-3D ? 0.0D : (dx * -look.zCoord + dz * look.xCoord) / len;
            double front = len < 1.0E-3D ? 1.0D : (dx * look.xCoord + dz * look.zCoord) / len;
            int fade = 255 - (int) ((now - c.time) * 180L / 3000L);
            int color = fade << 16 | fade << 8 | fade | 0xFF000000;
            int cx = w - widest / 2 - 2;
            int y = h - 30 - row * 10;
            Gui.drawRect(cx - widest / 2 - 1, y - 1, cx + widest / 2 + 1, y + 9, 0xCC000000);
            GL11.glEnable(GL11.GL_BLEND);
            font.drawString(c.text, cx - font.getStringWidth(c.text) / 2, y, color);
            if (front < 0.5D || Math.abs(side) > 0.5D) {
                if (side < 0.0D) {
                    font.drawString("<", cx - widest / 2, y, color);
                } else {
                    font.drawString(">", cx + widest / 2 - font.getStringWidth(">"), y, color);
                }
            }
            row++;
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Called by the coremod at the start and end of EntityRenderer.renderHand: the hand pass is drawn mirrored. */
    public static void handBegin() {
        if (leftHand) {
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(GL11.GL_CW);
        }
    }

    public static void handEnd() {
        GL11.glFrontFace(GL11.GL_CCW);
    }

    private static void mirror(EntityPlayer player, float partial) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial - RenderManager.renderPosX;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial - RenderManager.renderPosZ;
        float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partial;
        GL11.glTranslated(x, 0.0D, z);
        GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(-1.0F, 1.0F, 1.0F);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(-x, 0.0D, -z);
    }

    private float partial;

    @SubscribeEvent
    public void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (Content122.isLeftHanded(event.entityPlayer)) {
            partial = event.partialRenderTick;
            GL11.glPushMatrix();
            mirror(event.entityPlayer, partial);
            GL11.glFrontFace(GL11.GL_CW);
        }
    }

    @SubscribeEvent
    public void onPlayerPost(RenderPlayerEvent.Post event) {
        if (Content122.isLeftHanded(event.entityPlayer)) {
            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
        }
    }

    /** The name above a mirrored player must stay readable. */
    @SubscribeEvent
    public void onSpecialsPre(RenderLivingEvent.Specials.Pre event) {
        if (event.entity instanceof EntityPlayer && Content122.isLeftHanded((EntityPlayer) event.entity)) {
            GL11.glPushMatrix();
            mirror((EntityPlayer) event.entity, partial);
            GL11.glFrontFace(GL11.GL_CCW);
        }
    }

    @SubscribeEvent
    public void onSpecialsPost(RenderLivingEvent.Specials.Post event) {
        if (event.entity instanceof EntityPlayer && Content122.isLeftHanded((EntityPlayer) event.entity)) {
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glPopMatrix();
        }
    }

}
