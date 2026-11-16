package ru.codex.offhand1710.asm;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Hooks of 1.12.2 behaviour in entity and item classes: Block.onLanded (slime and bed bounce for every entity), the
 * advancement triggers enter_block, cured_zombie_villager, enchanted_item and item_durability_changed. Each change logs
 * and is left out when its pattern is not found.
 */
final class Entities {
    private static final String TRIGGERS = "ru/codex/offhand1710/Triggers112";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";

    private Entities() {
    }

    private static void warn(String what) {
        LogManager.getLogger("mc112bridge").warn("Entity patch not applied: " + what);
    }

    private static boolean call(AbstractInsnNode n, int opcode, String desc, String... names) {
        if (n.getOpcode() != opcode || !desc.equals(((MethodInsnNode) n).desc)) {
            return false;
        }
        for (String name : names) {
            if (name.equals(((MethodInsnNode) n).name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Entity.moveEntity: "motionY = 0" after a vertical collision becomes Triggers112.landed(this) (Block.onLanded of
     * 1.12.2); Entity.func_145775_I (doBlockCollisions): each Block.onEntityCollidedWithBlock goes through
     * Triggers112.collide, which also fires enter_block.
     */
    static byte[] entity(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean landed = false;
        int collide = 0;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "moveEntity", "func_70091_d") && "(DDD)V".equals(m.desc)) {
                boolean afterFall = false;
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (call(n, Opcodes.INVOKEVIRTUAL, "(DZ)V", "updateFallState", "func_70064_a")) {
                        afterFall = true;
                    } else if (afterFall && n.getOpcode() == Opcodes.PUTFIELD && ("motionY".equals(((FieldInsnNode) n).name) || "field_70181_x".equals(((FieldInsnNode) n).name))) {
                        AbstractInsnNode zero = Transformer.previousCode(n);
                        if (zero != null && zero.getOpcode() == Opcodes.DCONST_0) {
                            m.instructions.remove(zero);
                            m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "landed", "(" + ENTITY + ")V", false));
                            landed = true;
                        }
                        break;
                    }
                }
            } else if ("func_145775_I".equals(m.name) && "()V".equals(m.desc)) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (call(n, Opcodes.INVOKEVIRTUAL, "(Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;)V", "onEntityCollidedWithBlock", "func_149670_a")) {
                        m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "collide", "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;)V", false));
                        collide++;
                    }
                }
            }
        }
        if (!landed) {
            warn("landing in moveEntity");
        }
        if (collide != 1) {
            warn("block collisions replaced " + collide + " of 1");
        }
        return Transformer.write(node);
    }

    /** EntityZombie.convertToVillager: Triggers112.cured(this) first. */
    static byte[] zombie(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean found = false;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "convertToVillager", "func_82232_p") && "()V".equals(m.desc)) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "cured", "(Lnet/minecraft/entity/monster/EntityZombie;)V", false));
                m.instructions.insert(l);
                found = true;
            }
        }
        if (!found) {
            warn("zombie conversion");
        }
        return Transformer.write(node);
    }

    /** ContainerEnchantment.enchantItem: the result goes through Triggers112.enchanted(result, player). */
    static byte[] enchant(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        int found = 0;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "enchantItem", "func_75140_a") && "(Lnet/minecraft/entity/player/EntityPlayer;I)Z".equals(m.desc)) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.IRETURN) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.DUP));
                        l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "enchanted", "(ZLnet/minecraft/entity/player/EntityPlayer;)V", false));
                        m.instructions.insertBefore(n, l);
                        found++;
                    }
                }
            }
        }
        if (found == 0) {
            warn("enchanting");
        }
        return Transformer.write(node);
    }

    /** ItemStack.damageItem: the damage before is kept in a new local, Triggers112.damaged gets it at every return. */
    static byte[] itemStack(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean found = false;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "damageItem", "func_77972_a") && "(ILnet/minecraft/entity/EntityLivingBase;)V".equals(m.desc)) {
                int local = m.maxLocals;
                m.maxLocals++;
                InsnList head = new InsnList();
                head.add(new VarInsnNode(Opcodes.ALOAD, 0));
                head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "damageBefore", "(Lnet/minecraft/item/ItemStack;)I", false));
                head.add(new VarInsnNode(Opcodes.ISTORE, local));
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, 2));
                call.add(new VarInsnNode(Opcodes.ILOAD, local));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, TRIGGERS, "damaged", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;I)V", false));
                Transformer.beforeReturns(m, call);
                m.instructions.insert(head);
                found = true;
            }
        }
        if (!found) {
            warn("item damage");
        }
        return Transformer.write(node);
    }
}
