package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import java.io.File;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.lwjgl.input.Keyboard;

/**
 * Saved toolbars of 1.12 (CreativeSettings, HotbarSnapshot, GuiContainerCreative.handleHotbarSnapshots): in creative
 * mode C + 1..9 saves the hotbar, X + 1..9 puts it back; the tab "Saved Toolbars" shows them. Kept in
 * mc112bridge-hotbar.nbt in the game folder (hotbar.nbt of 1.12 has other item ids). -Dmc112.hotbars17 turns it off.
 */
public final class Hotbars112 {
    public static final boolean OFF = System.getProperty("mc112.hotbars17") != null;
    public static final KeyBinding SAVE = new KeyBinding("key.saveToolbarActivator", Keyboard.KEY_C, "key.categories.creative");
    public static final KeyBinding LOAD = new KeyBinding("key.loadToolbarActivator", Keyboard.KEY_X, "key.categories.creative");
    private static ItemStack[][] snapshots;

    static void register() {
        if (OFF) {
            return;
        }
        ClientRegistry.registerKeyBinding(SAVE);
        ClientRegistry.registerKeyBinding(LOAD);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new Hotbars112());
        new Tab();
    }

    private static File file() {
        return new File(Minecraft.getMinecraft().mcDataDir, "mc112bridge-hotbar.nbt");
    }

    public static synchronized ItemStack[][] snapshots() {
        if (snapshots == null) {
            snapshots = new ItemStack[9][9];
            try {
                File f = file();
                if (f.exists()) {
                    NBTTagCompound tag = CompressedStreamTools.read(f);
                    for (int i = 0; i < 9; i++) {
                        NBTTagList list = tag.getTagList(String.valueOf(i), 10);
                        for (int j = 0; j < list.tagCount() && j < 9; j++) {
                            NBTTagCompound c = list.getCompoundTagAt(j);
                            snapshots[i][j] = c.hasNoTags() ? null : ItemStack.loadItemStackFromNBT(c);
                        }
                    }
                }
            } catch (Exception e) {
                cpw.mods.fml.common.FMLLog.warning("[mc112bridge] could not read %s: %s", file(), e);
            }
        }
        return snapshots;
    }

    /** CreativeSettings.write. */
    private static synchronized void write() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int i = 0; i < 9; i++) {
            NBTTagList list = new NBTTagList();
            for (int j = 0; j < 9; j++) {
                ItemStack s = snapshots()[i][j];
                list.appendTag(s == null ? new NBTTagCompound() : s.writeToNBT(new NBTTagCompound()));
            }
            tag.setTag(String.valueOf(i), list);
        }
        try {
            CompressedStreamTools.safeWrite(tag, file());
        } catch (Exception e) {
            cpw.mods.fml.common.FMLLog.warning("[mc112bridge] could not write %s: %s", file(), e);
        }
    }

    private static boolean empty(int index) {
        for (ItemStack s : snapshots()[index]) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    /** Minecraft.processKeyBinds of 1.12.2: a hotbar key with C or X held in creative mode. */
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP p = mc.thePlayer;
        if (p == null || mc.currentScreen != null || !p.capabilities.isCreativeMode || !Keyboard.getEventKeyState()) {
            return;
        }
        boolean save = SAVE.getIsKeyPressed();
        boolean load = LOAD.getIsKeyPressed();
        if (!save && !load) {
            return;
        }
        int key = Keyboard.getEventKey();
        for (int i = 0; i < 9; i++) {
            if (mc.gameSettings.keyBindsHotbar[i].getKeyCode() == key) {
                // the hotbar key must not also select the slot
                while (mc.gameSettings.keyBindsHotbar[i].isPressed()) {
                    // consumed
                }
                handle(mc, i, load, save);
                return;
            }
        }
    }

    /** GuiContainerCreative.handleHotbarSnapshots of 1.12.2. */
    public static void handle(Minecraft mc, int index, boolean load, boolean save) {
        EntityClientPlayerMP p = mc.thePlayer;
        if (load) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = snapshots()[index][i];
                ItemStack copy = s == null ? null : s.copy();
                p.inventory.setInventorySlotContents(i, copy);
                mc.playerController.sendSlotPacket(copy, 36 + i);
            }
            p.inventoryContainer.detectAndSendChanges();
        } else if (save) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.inventory.getStackInSlot(i);
                snapshots()[index][i] = s == null ? null : s.copy();
            }
            String slot = GameSettings.getKeyDisplayString(mc.gameSettings.keyBindsHotbar[index].getKeyCode());
            String loadKey = GameSettings.getKeyDisplayString(LOAD.getKeyCode());
            mc.ingameGUI.func_110326_a(I18n.format("inventory.hotbarSaved", loadKey, slot), false);
            write();
        }
    }

    /** CreativeTabs.HOTBAR of 1.12.2: nine rows, a saved toolbar each, or a paper telling how to save one. */
    public static final class Tab extends CreativeTabs {
        Tab() {
            super("hotbar");
        }

        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(Blocks.bookshelf);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void displayAllReleventItems(List list) {
            Minecraft mc = Minecraft.getMinecraft();
            for (int i = 0; i < 9; i++) {
                if (empty(i)) {
                    for (int j = 0; j < 9; j++) {
                        if (j == i) {
                            ItemStack paper = new ItemStack(Items.paper);
                            String key = GameSettings.getKeyDisplayString(mc.gameSettings.keyBindsHotbar[i].getKeyCode());
                            String saveKey = GameSettings.getKeyDisplayString(SAVE.getKeyCode());
                            paper.setStackDisplayName(I18n.format("inventory.hotbarInfo", saveKey, key));
                            list.add(paper);
                        } else {
                            list.add(null);
                        }
                    }
                } else {
                    for (ItemStack s : snapshots()[i]) {
                        list.add(s == null ? null : s.copy());
                    }
                }
            }
        }
    }
}
