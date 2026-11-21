package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.PortedBlocks;
import ru.codex.offhand1710.Sounds112;

/**
 * Block sounds that 1.7.10 shares and 1.12.2 tells apart, renamed by the block where they play: chest and ender
 * chest, wooden and iron doors and trapdoors, fence gates (1.7.10 aux events 1003/1006), the note block instruments
 * of 1.12 (TileEntityNote: clay, gold, wool, packed ice, bone block under it; harp is note/harp2) and the hoe
 * (1.7.10 plays the farmland step sound).
 */
public final class Sounds112Client {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSound(PlaySoundEvent17 event) {
        if (Sounds112.OFF || event.sound == null || event.name == null) {
            return;
        }
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }
        ISound s = event.sound;
        if (!"minecraft".equals(s.getPositionedSoundLocation().getResourceDomain())) {
            return;
        }
        String to = rename(world, event.name, s.getXPosF(), s.getYPosF(), s.getZPosF(), s.getVolume());
        if (to == null) {
            return;
        }
        float volume = s.getVolume();
        float pitch = s.getPitch();
        if ("item.hoe.till".equals(to)) {
            volume = 1.0F;
            pitch = 1.0F;
        }
        Sounds112.last = to;
        event.result = new PositionedSoundRecord(new ResourceLocation(MC112Bridge.MOD_ID, to), volume, pitch, s.getXPosF(), s.getYPosF(), s.getZPosF());
    }

    public static String rename(World world, String name, float x, float y, float z, float volume) {
        int bx = MathHelper.floor_float(x);
        int by = MathHelper.floor_float(y);
        int bz = MathHelper.floor_float(z);
        if ("random.chestopen".equals(name) || "random.chestclosed".equals(name)) {
            boolean open = "random.chestopen".equals(name);
            Block b = world.getBlock(bx, by, bz);
            if (b == Blocks.ender_chest) {
                return open ? "block.enderchest.open" : "block.enderchest.close";
            }
            if (b instanceof BlockChest) {
                return open ? "block.chest.open" : "block.chest.close";
            }
            return null;
        }
        if ("random.door_open".equals(name) || "random.door_close".equals(name)) {
            String end = "random.door_open".equals(name) ? ".open" : ".close";
            Block b = world.getBlock(bx, by, bz);
            boolean iron = b.getMaterial() == Material.iron;
            if (b instanceof BlockDoor) {
                return (iron ? "block.iron_door" : "block.wooden_door") + end;
            }
            if (b instanceof BlockTrapDoor) {
                return (iron ? "block.iron_trapdoor" : "block.wooden_trapdoor") + end;
            }
            if (b instanceof BlockFenceGate) {
                return "block.fence_gate" + end;
            }
            return null;
        }
        if (name.startsWith("note.")) {
            Block below = world.getBlock(bx, by - 1, bz);
            if (below == Blocks.clay) {
                return "block.note.flute";
            }
            if (below == Blocks.gold_block) {
                return "block.note.bell";
            }
            if (below == Blocks.wool) {
                return "block.note.guitar";
            }
            if (below == Blocks.packed_ice) {
                return "block.note.chime";
            }
            if (below == PortedBlocks.boneBlock && below != null) {
                return "block.note.xylophone";
            }
            return "note.harp".equals(name) ? "block.note.harp" : null;
        }
        if ("step.gravel".equals(name) && volume >= 0.9F && world.getBlock(bx, by, bz) == Blocks.farmland
            && Math.abs(x - bx - 0.5F) < 0.01F && Math.abs(z - bz - 0.5F) < 0.01F) {
            // ItemHoe: the step sound of the new farmland in the middle of the block, at volume (1 + 1) / 2
            return "item.hoe.till";
        }
        return null;
    }
}
