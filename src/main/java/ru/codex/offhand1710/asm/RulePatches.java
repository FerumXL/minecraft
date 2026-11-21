package ru.codex.offhand1710.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Hooks of the 1.12.2 game rules (ru.codex.offhand1710.Rules112): randomTickSpeed, showDeathMessages,
 * maxEntityCramming, doEntityDrops, sendCommandFeedback / logAdminCommands, disableElytraMovementCheck, spawnRadius.
 * Each insertion keeps the stack as it found it. Off with -Dmc112.gamerules17=1.
 */
final class RulePatches {
    private static final String HOOKS = "ru/codex/offhand1710/Rules112";

    private RulePatches() {
    }

    static byte[] transform(String name, byte[] bytes) {
        if (System.getProperty("mc112.gamerules17") != null) {
            return bytes;
        }
        String n = name;
        if (!(n.equals("net.minecraft.world.WorldServer") || n.equals("net.minecraft.entity.player.EntityPlayerMP")
            || n.equals("net.minecraft.entity.EntityLivingBase") || n.equals("net.minecraft.entity.Entity")
            || n.equals("net.minecraft.command.ServerCommandManager") || n.equals("net.minecraft.network.NetHandlerPlayServer")
            || n.equals("net.minecraft.world.WorldProvider"))) {
            return bytes;
        }
        ClassNode node = Transformer.read(bytes);
        int count = 0;
        for (MethodNode m : node.methods) {
            count += patch(n, m);
        }
        System.out.println("[mc112bridge] 1.12 game rules in " + name + ": " + count);
        return Transformer.write(node);
    }

    private static int patch(String cls, MethodNode m) {
        int count = 0;
        if (cls.endsWith("WorldServer") && m.name.equals("func_147456_g") && m.desc.equals("()V")) {
            // the 3 of "for (i = 0; i < 3; i++)" around the random block ticks of each section
            for (AbstractInsnNode a : m.instructions.toArray()) {
                AbstractInsnNode next = a.getNext();
                if (a.getOpcode() == Opcodes.ICONST_3 && next != null && next.getOpcode() == Opcodes.IF_ICMPGE) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "randomTicks", "(Lnet/minecraft/world/WorldServer;)I", false));
                    m.instructions.insert(a, call);
                    m.instructions.remove(a);
                    count++;
                }
            }
        } else if (cls.endsWith("EntityPlayerMP") && Transformer.named(m, "onDeath", "func_70645_a")) {
            for (AbstractInsnNode a : m.instructions.toArray()) {
                if (a instanceof MethodInsnNode && ((MethodInsnNode) a).owner.equals("net/minecraft/server/management/ServerConfigurationManager")
                    && (((MethodInsnNode) a).name.equals("sendChatMsg") || ((MethodInsnNode) a).name.equals("func_148539_a"))) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "deathMessage",
                        "(Lnet/minecraft/server/management/ServerConfigurationManager;Lnet/minecraft/util/IChatComponent;Lnet/minecraft/entity/player/EntityPlayerMP;)V", false));
                    m.instructions.insertBefore(a, call);
                    m.instructions.remove(a);
                    count++;
                }
            }
        } else if (cls.endsWith("EntityLivingBase") && Transformer.named(m, "collideWithNearbyEntities", "func_85033_bc")) {
            for (AbstractInsnNode a : m.instructions.toArray()) {
                if (a instanceof MethodInsnNode && ((MethodInsnNode) a).owner.equals("net/minecraft/world/World")
                    && ((MethodInsnNode) a).desc.endsWith(")Ljava/util/List;")) {
                    InsnList call = new InsnList();
                    call.add(new InsnNode(Opcodes.DUP));
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "cramming", "(Ljava/util/List;Lnet/minecraft/entity/EntityLivingBase;)V", false));
                    m.instructions.insert(a, call);
                    count++;
                    break;
                }
            }
        } else if (cls.equals("net.minecraft.entity.Entity") && Transformer.named(m, "entityDropItem", "func_70099_a")
            && m.desc.equals("(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/EntityItem;")) {
            LabelNode go = new LabelNode();
            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "noDrop", "(Lnet/minecraft/entity/Entity;)Z", false));
            list.add(new JumpInsnNode(Opcodes.IFEQ, go));
            list.add(new InsnNode(Opcodes.ACONST_NULL));
            list.add(new InsnNode(Opcodes.ARETURN));
            list.add(go);
            m.instructions.insert(list);
            count++;
        } else if (cls.endsWith("ServerCommandManager") && m.name.equals("func_152372_a")) {
            InsnList load = new InsnList();
            load.add(new VarInsnNode(Opcodes.ALOAD, 1));
            load.add(new VarInsnNode(Opcodes.ALOAD, 2));
            load.add(new VarInsnNode(Opcodes.ILOAD, 3));
            load.add(new VarInsnNode(Opcodes.ALOAD, 4));
            load.add(new VarInsnNode(Opcodes.ALOAD, 5));
            Transformer.guard(m, load, HOOKS, "notifyOps",
                "(Lnet/minecraft/command/ICommandSender;Lnet/minecraft/command/ICommand;ILjava/lang/String;[Ljava/lang/Object;)Z", Opcodes.RETURN, 0);
            count++;
        } else if (cls.endsWith("NetHandlerPlayServer") && Transformer.named(m, "processPlayer", "func_147347_a")) {
            for (AbstractInsnNode a : m.instructions.toArray()) {
                if (a instanceof LdcInsnNode && Double.valueOf(100.0D).equals(((LdcInsnNode) a).cst)) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "speedLimit", "(Lnet/minecraft/network/NetHandlerPlayServer;)D", false));
                    m.instructions.insert(a, call);
                    m.instructions.remove(a);
                    count++;
                }
            }
        } else if (cls.endsWith("WorldProvider") && m.name.equals("getRandomizedSpawnPoint") && m.desc.equals("()Lnet/minecraft/util/ChunkCoordinates;")) {
            LabelNode go = new LabelNode();
            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "spawnPoint", "(Lnet/minecraft/world/WorldProvider;)Lnet/minecraft/util/ChunkCoordinates;", false));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new JumpInsnNode(Opcodes.IFNULL, go));
            list.add(new InsnNode(Opcodes.ARETURN));
            list.add(go);
            list.add(new InsnNode(Opcodes.POP));
            m.instructions.insert(list);
            count++;
        }
        return count;
    }
}
