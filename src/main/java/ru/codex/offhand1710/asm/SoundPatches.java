package ru.codex.offhand1710.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Calls into ru.codex.offhand1710.Sounds112 where 1.12.2 plays a sound that 1.7.10 does not (buckets, bottles,
 * armour, item frames, brewing, the enchanting table, eyes of ender, the fishing rod, the witch, the squid, the snowman).
 * Every insertion is a self-contained call that leaves the stack as it found it: no new locals, no new branches.
 * Off with -Dmc112.sounds17=1.
 */
final class SoundPatches {
    private static final String HOOKS = "ru/codex/offhand1710/Sounds112";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String PLAYER = "Lnet/minecraft/entity/player/EntityPlayer;";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";

    private SoundPatches() {
    }

    static byte[] transform(String name, byte[] bytes) {
        if (System.getProperty("mc112.sounds17") != null || !name.startsWith("net.minecraft.")) {
            return bytes;
        }
        String n = name.substring("net.minecraft.".length());
        if (!(n.equals("entity.passive.EntitySquid") || n.equals("entity.monster.EntitySnowman") || n.equals("entity.monster.EntityWitch") || n.equals("item.ItemBucket")
            || n.equals("item.ItemGlassBottle") || n.equals("block.BlockCauldron") || n.equals("item.ItemArmor")
            || n.equals("entity.item.EntityItemFrame") || n.equals("entity.item.EntityPainting") || n.equals("entity.EntityLeashKnot")
            || n.equals("item.ItemHangingEntity") || n.equals("tileentity.TileEntityBrewingStand") || n.equals("block.BlockFurnace")
            || n.equals("inventory.ContainerEnchantment") || n.equals("item.ItemEnderEye") || n.equals("entity.item.EntityEnderEye")
            || n.equals("item.ItemFishingRod"))) {
            return bytes;
        }
        ClassNode node = Transformer.read(bytes);
        int count = 0;
        if (n.equals("entity.monster.EntitySnowman")) {
            // EntityGolem calls all three sounds "none"; 1.12 gives the snowman its own hurt and death sounds
            boolean srg = false;
            for (MethodNode m : node.methods) {
                srg |= m.name.startsWith("func_");
            }
            node.methods.add(constant(srg ? "func_70621_aR" : "getHurtSound", "mc112bridge:entity.snowman.hurt"));
            node.methods.add(constant(srg ? "func_70673_aS" : "getDeathSound", "mc112bridge:entity.snowman.death"));
            count += 2;
        }
        for (MethodNode m : node.methods) {
            count += patch(n, node, m);
        }
        System.out.println("[mc112bridge] 1.12 sounds in " + name + ": " + count);
        return Transformer.write(node);
    }

    private static int patch(String n, ClassNode node, MethodNode m) {
        int c = 0;
        if (n.equals("entity.passive.EntitySquid")) {
            String s = Transformer.named(m, "getLivingSound", "func_70639_aQ") ? "ambient"
                : Transformer.named(m, "getHurtSound", "func_70621_aR") ? "hurt" : Transformer.named(m, "getDeathSound", "func_70673_aS") ? "death" : null;
            if (s != null) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.ACONST_NULL) {
                        m.instructions.set(i, new LdcInsnNode("mc112bridge:entity.squid." + s));
                        c++;
                    }
                }
            }
        } else if (n.equals("entity.monster.EntityWitch")) {
            if (Transformer.named(m, "attackEntityWithRangedAttack", "func_82196_d")) {
                // after "if (getAggressive()) return;": the potion is thrown
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.NEW) {
                        m.instructions.insertBefore(i, self("witchThrow"));
                        c++;
                        break;
                    }
                }
            } else if (Transformer.named(m, "onLivingUpdate", "func_70636_d")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (isCall(i, "setAggressive", "func_82197_f") && i.getPrevious().getOpcode() == Opcodes.ICONST_1) {
                        m.instructions.insert(i, self("witchDrink"));
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemBucket")) {
            if (Transformer.named(m, "onItemRightClick", "func_77659_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.INVOKESPECIAL && "func_150910_a".equals(((MethodInsnNode) i).name)) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.DUP));
                        l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                        l.add(call("bucketFill", "(Lnet/minecraft/item/Item;" + PLAYER + ")V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            } else if (Transformer.named(m, "tryPlaceContainedLiquid", "func_77875_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.ICONST_1 && i.getNext() != null && i.getNext().getOpcode() == Opcodes.IRETURN) {
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        l.add(new VarInsnNode(Opcodes.ILOAD, 2));
                        l.add(new VarInsnNode(Opcodes.ILOAD, 3));
                        l.add(new VarInsnNode(Opcodes.ILOAD, 4));
                        l.add(call("bucketEmpty", "(Lnet/minecraft/item/Item;" + WORLD + "III)V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemGlassBottle")) {
            if (Transformer.named(m, "onItemRightClick", "func_77659_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.GETSTATIC && ("water".equals(((FieldInsnNode) i).name) || "field_151586_h".equals(((FieldInsnNode) i).name))
                        && i.getNext() instanceof JumpInsnNode) {
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                        l.add(call("bottleFill", "(" + PLAYER + ")V"));
                        m.instructions.insert(i.getNext(), l);
                        c++;
                        break;
                    }
                }
            }
        } else if (n.equals("block.BlockCauldron")) {
            if (Transformer.named(m, "onBlockActivated", "func_149727_a")) {
                boolean bottle = false;
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.INVOKEVIRTUAL && "func_150024_a".equals(((MethodInsnNode) i).name) && i.getPrevious().getOpcode() == Opcodes.ICONST_3) {
                        // the water bucket fills the cauldron
                        m.instructions.insertBefore(i, at(1, "cauldronBucket"));
                        c++;
                    }
                    if (i.getOpcode() == Opcodes.GETSTATIC && ("glass_bottle".equals(((FieldInsnNode) i).name) || "field_151069_bo".equals(((FieldInsnNode) i).name))) {
                        bottle = true;
                    }
                    if (bottle && i.getOpcode() == Opcodes.IFLE) {
                        // "if (meta > 0)" of the glass bottle branch: the bottle is filled, in creative too
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 5));
                        l.add(call("bottleFill", "(" + PLAYER + ")V"));
                        m.instructions.insert(i, l);
                        bottle = false;
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemArmor")) {
            if (Transformer.named(m, "onItemRightClick", "func_77659_a")) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                l.add(call("armorRightClick", "(Lnet/minecraft/item/ItemStack;" + PLAYER + ")V"));
                m.instructions.insert(l);
                c++;
            }
        } else if (n.equals("entity.item.EntityItemFrame")) {
            if (Transformer.named(m, "onBroken", "func_110128_b")) {
                m.instructions.insert(self("hangingBroken"));
                c++;
            } else if (Transformer.named(m, "interactFirst", "func_130002_c")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (isCall(i, "setDisplayedItem", "func_82334_a")) {
                        m.instructions.insertBefore(i, self("frameAdd"));
                        c++;
                    } else if (isCall(i, "setItemRotation", "func_82336_g")) {
                        m.instructions.insertBefore(i, self("frameRotate"));
                        c++;
                    }
                }
            } else if (Transformer.named(m, "attackEntityFrom", "func_70097_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (isCall(i, "func_146065_b", "func_146065_b")) {
                        m.instructions.insertBefore(i, self("frameRemove"));
                        c++;
                    }
                }
            }
        } else if (n.equals("entity.item.EntityPainting")) {
            if (Transformer.named(m, "onBroken", "func_110128_b")) {
                m.instructions.insert(self("hangingBroken"));
                c++;
            }
        } else if (n.equals("entity.EntityLeashKnot")) {
            if (Transformer.named(m, "onBroken", "func_110128_b")) {
                m.instructions.insert(self("hangingBroken"));
                c++;
            } else if ("func_110129_a".equals(m.name) && (m.access & Opcodes.ACC_STATIC) != 0) {
                // a new knot on a fence; the method that only finds an old one is func_110130_b
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.ARETURN) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.DUP));
                        l.add(call("hangingPlaced", "(" + ENTITY + ")V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemHangingEntity")) {
            if (Transformer.named(m, "onItemUse", "func_77648_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (isCall(i, "spawnEntityInWorld", "func_72838_d")) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.DUP));
                        l.add(call("hangingPlaced", "(" + ENTITY + ")V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            }
        } else if (n.equals("tileentity.TileEntityBrewingStand")) {
            if (Transformer.named(m, "brewPotions", "func_145940_l")) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(call("brewed", "(Lnet/minecraft/tileentity/TileEntity;)V"));
                m.instructions.insert(l);
                c++;
            }
        } else if (n.equals("block.BlockFurnace")) {
            if (Transformer.named(m, "randomDisplayTick", "func_149734_b")) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new VarInsnNode(Opcodes.ILOAD, 2));
                l.add(new VarInsnNode(Opcodes.ILOAD, 3));
                l.add(new VarInsnNode(Opcodes.ILOAD, 4));
                l.add(new VarInsnNode(Opcodes.ALOAD, 5));
                l.add(call("furnaceTick", "(Lnet/minecraft/block/Block;" + WORLD + "IIILjava/util/Random;)V"));
                m.instructions.insert(l);
                c++;
            }
        } else if (n.equals("inventory.ContainerEnchantment")) {
            if (Transformer.named(m, "enchantItem", "func_75140_a")) {
                String world = field(node, "worldPointer", "field_75172_h");
                String x = field(node, "posX", "field_75173_i");
                String y = field(node, "posY", "field_75170_j");
                String z = field(node, "posZ", "field_75171_k");
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (world != null && x != null && y != null && z != null && i.getOpcode() == Opcodes.ICONST_1 && i.getNext() != null && i.getNext().getOpcode() == Opcodes.IRETURN) {
                        InsnList l = new InsnList();
                        l.add(get(node, world, WORLD));
                        l.add(get(node, x, "I"));
                        l.add(get(node, y, "I"));
                        l.add(get(node, z, "I"));
                        l.add(call("enchanted", "(" + WORLD + "III)V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemEnderEye")) {
            if (Transformer.named(m, "onItemUse", "func_77648_a")) {
                boolean frame = false;
                boolean portal = false;
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (!frame && isCall(i, "setBlockMetadataWithNotify", "func_72921_c")) {
                        m.instructions.insertBefore(i, at(3, "eyePlaced"));
                        frame = true;
                        c++;
                    }
                    if (!portal && i.getOpcode() == Opcodes.GETSTATIC && ("end_portal".equals(((FieldInsnNode) i).name) || "field_150384_bq".equals(((FieldInsnNode) i).name))) {
                        m.instructions.insertBefore(i, at(3, "portalOpened"));
                        portal = true;
                        c++;
                    }
                }
            }
        } else if (n.equals("entity.item.EntityEnderEye")) {
            if (Transformer.named(m, "onUpdate", "func_70071_h_")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (isCall(i, "setDead", "func_70106_y")) {
                        m.instructions.insertBefore(i, self("eyeDeath"));
                        c++;
                    }
                }
            }
        } else if (n.equals("item.ItemFishingRod")) {
            if (Transformer.named(m, "onItemRightClick", "func_77659_a")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i.getOpcode() == Opcodes.INVOKEVIRTUAL && "func_146034_e".equals(((MethodInsnNode) i).name)) {
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                        l.add(call("bobberRetrieve", "(" + PLAYER + ")V"));
                        m.instructions.insertBefore(i, l);
                        c++;
                    }
                }
            }
        }
        return c;
    }

    /** protected String name() { return value; } */
    private static MethodNode constant(String name, String value) {
        MethodNode m = new MethodNode(Opcodes.ACC_PROTECTED, name, "()Ljava/lang/String;", null, null);
        m.instructions.add(new LdcInsnNode(value));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        m.maxStack = 1;
        m.maxLocals = 1;
        return m;
    }

    private static boolean isCall(AbstractInsnNode i, String mcp, String srg) {
        return i instanceof MethodInsnNode && (mcp.equals(((MethodInsnNode) i).name) || srg.equals(((MethodInsnNode) i).name));
    }

    private static MethodInsnNode call(String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, desc, false);
    }

    /** Sounds112.name(this) for an entity. */
    private static InsnList self(String name) {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
        l.add(call(name, "(" + ENTITY + ")V"));
        return l;
    }

    /** Sounds112.name(world, x, y, z) with the world in local slot w and the coordinates right after it. */
    private static InsnList at(int w, String name) {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, w));
        l.add(new VarInsnNode(Opcodes.ILOAD, w + 1));
        l.add(new VarInsnNode(Opcodes.ILOAD, w + 2));
        l.add(new VarInsnNode(Opcodes.ILOAD, w + 3));
        l.add(call(name, "(" + WORLD + "III)V"));
        return l;
    }

    private static String field(ClassNode node, String mcp, String srg) {
        for (FieldNode f : node.fields) {
            if (f.name.equals(mcp) || f.name.equals(srg)) {
                return f.name;
            }
        }
        return null;
    }

    private static InsnList get(ClassNode node, String field, String desc) {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
        l.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, field, desc));
        return l;
    }
}
