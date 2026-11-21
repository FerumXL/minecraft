package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * The names of 1.12.2 in the commands of 1.7.10: an item, block or entity id of 1.12.2 (with or without "minecraft:")
 * and the metadata or block state that goes with it become the 1.7.10 name and metadata, so the vanilla commands
 * keep doing the rest. Names that 1.7.10 or another mod already knows go through unchanged.
 * -Dmc112.names17 turns it off.
 */
public final class Names112 {
    public static final boolean OFF = System.getProperty("mc112.names17") != null;

    private Names112() {
    }

    public static String ns(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    /** {registry name, metadata} of 1.7.10 for a 1.12.2 item; null when neither knows it. */
    public static Object[] item(String name, int data) {
        String full = ns(name);
        ItemStack s = Data112.item(full, data);
        if (s == null || s.getItem() == null) {
            Object direct = Item.itemRegistry.getObject(name);
            return direct == null ? null : new Object[] {Item.itemRegistry.getNameForObject(direct), Integer.valueOf(data)};
        }
        return new Object[] {Item.itemRegistry.getNameForObject(s.getItem()), Integer.valueOf(s.getItemDamage())};
    }

    /**
     * The 1.7.10 block and metadata for a 1.12.2 block name and its data argument (a number, a state such as
     * "color=lime", "default", or null for none). A 1.12.2 block that this mod splits by colour (concrete, beds of the
     * items) goes through the item of that name and metadata.
     */
    public static Object[] block(String name, String data) {
        String full = ns(name);
        int meta = 0;
        if (data != null) {
            meta = metaArg(full, data);
        }
        // the item of that name and data, when it is a block of its own here (granite, lime concrete)
        String[] alias = Data112.alias(full, meta);
        String own = full.startsWith("minecraft:") ? MC112Bridge.MOD_ID + ":" + full.substring(10) : full;
        if (alias != null && !alias[0].equals(full) && !alias[0].equals(own)) {
            Object item = Item.itemRegistry.getObject(alias[0]);
            if (item instanceof ItemBlock) {
                ItemBlock ib = (ItemBlock) item;
                return new Object[] {Block.blockRegistry.getNameForObject(ib.field_150939_a), Integer.valueOf(ib.getMetadata(Integer.parseInt(alias[1])))};
            }
        }
        Block b = Block.blockRegistry.containsKey(name) ? Block.getBlockFromName(name) : States112.block17(full);
        if (b == null || b == net.minecraft.init.Blocks.air && !"minecraft:air".equals(full)) {
            return null;
        }
        return new Object[] {Block.blockRegistry.getNameForObject(b), Integer.valueOf(meta)};
    }

    /** CommandBase.convertArgToBlockState of 1.12.2: a number 0..15, or a state of the block. */
    public static int metaArg(String full, String data) {
        try {
            int i = Integer.parseInt(data);
            if (i < 0) {
                throw new NumberInvalidException("commands.generic.num.tooSmall", i, 0);
            }
            if (i > 15) {
                throw new NumberInvalidException("commands.generic.num.tooBig", i, 15);
            }
            return i;
        } catch (NumberFormatException e) {
            int m = States112.metaOf(full, data);
            if (m < 0) {
                throw new CommandException("commands.generic.blockstate.invalid", data, full);
            }
            return m;
        }
    }

    /** The 1.7.10 EntityList name for a 1.12.2 entity id ("zombie", "minecraft:zombie"), or the name as it is. */
    public static String entity(String name) {
        if (EntityList.stringToClassMapping.containsKey(name)) {
            return name;
        }
        String mapped = Data112.entityClassName(ns(name));
        return mapped == null ? name : mapped;
    }

    public static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Completion: the 1.12.2 names and every name of this game (other mods). */
    @SuppressWarnings("unchecked")
    public static List<String> itemCompletions() {
        Set<String> all = new TreeSet<String>(Data112.itemNames());
        for (Object o : Item.itemRegistry.getKeys()) {
            all.add(String.valueOf(o));
        }
        return new ArrayList<String>(all);
    }

    @SuppressWarnings("unchecked")
    public static List<String> blockCompletions() {
        Set<String> all = new TreeSet<String>();
        for (String k : States112.names()) {
            all.add(k.substring(0, k.indexOf('#')));
        }
        for (Object o : Block.blockRegistry.getKeys()) {
            all.add(String.valueOf(o));
        }
        return new ArrayList<String>(all);
    }

    @SuppressWarnings("unchecked")
    public static List<String> entityCompletions() {
        Set<String> all = new TreeSet<String>(Data112.entityIds());
        for (Object o : EntityList.stringToClassMapping.keySet()) {
            all.add(String.valueOf(o));
        }
        return new ArrayList<String>(all);
    }
}
