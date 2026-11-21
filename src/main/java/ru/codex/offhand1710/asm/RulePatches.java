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
        if (System.getProperty("mc112.border17") == null && (name.equals("net.minecraft.world.storage.WorldInfo")
            || name.equals("net.minecraft.world.World") || name.equals("net.minecraft.client.gui.GuiIngame"))) {
            bytes = border(name, bytes);
        }
        if (System.getProperty("mc112.stats17") == null
            && (name.equals("net.minecraft.command.CommandHandler") || name.equals("net.minecraft.command.server.CommandBlockLogic"))) {
            return stats(name, bytes);
        }
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

    private static final String BORDER = "ru/codex/offhand1710/Border112";

    /** The world border of 1.12.2: level.dat keys, the wall in the collision boxes, the red vignette. */
    private static byte[] border(String name, byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        int count = 0;
        for (MethodNode m : node.methods) {
            if (name.endsWith("WorldInfo") && (m.name.equals("<init>") && m.desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)V")
                || Transformer.named(m, "updateTagCompound", "func_76064_a"))) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BORDER, m.name.equals("<init>") ? "read" : "write",
                    "(Lnet/minecraft/world/storage/WorldInfo;Lnet/minecraft/nbt/NBTTagCompound;)V", false));
                Transformer.beforeReturns(m, call);
                count++;
            } else if (name.equals("net.minecraft.world.World") && Transformer.named(m, "getCollidingBoundingBoxes", "func_72945_a")
                && m.desc.equals("(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;")) {
                for (AbstractInsnNode a : m.instructions.toArray()) {
                    if (a.getOpcode() == Opcodes.ARETURN) {
                        InsnList call = new InsnList();
                        call.add(new InsnNode(Opcodes.DUP));
                        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        call.add(new VarInsnNode(Opcodes.ALOAD, 2));
                        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BORDER, "collide",
                            "(Ljava/util/List;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)V", false));
                        m.instructions.insertBefore(a, call);
                        count++;
                    }
                }
            } else if (name.endsWith("GuiIngame") && Transformer.named(m, "renderVignette", "func_73829_a")) {
                for (AbstractInsnNode a : m.instructions.toArray()) {
                    if (a instanceof MethodInsnNode && ((MethodInsnNode) a).name.equals("glColor4f")) {
                        ((MethodInsnNode) a).owner = "ru/codex/offhand1710/client/Border112Client";
                        ((MethodInsnNode) a).name = "vignetteColor";
                        count++;
                        break;
                    }
                }
            }
        }
        System.out.println("[mc112bridge] 1.12 world border in " + name + ": " + count);
        return Transformer.write(node);
    }

    private static final String STATS = "ru/codex/offhand1710/Stats112";

    /** CommandResultStats of 1.12.2: SuccessCount and AffectedEntities from executeCommand, CommandStats in the command block NBT. */
    private static byte[] stats(String name, byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        int count = 0;
        for (MethodNode m : node.methods) {
            if (name.endsWith("CommandHandler") && Transformer.named(m, "executeCommand", "func_71556_a")
                && m.desc.equals("(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)I")) {
                for (AbstractInsnNode a : m.instructions.toArray()) {
                    if (a.getOpcode() == Opcodes.IRETURN) {
                        InsnList call = new InsnList();
                        call.add(new InsnNode(Opcodes.DUP));
                        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATS, "done", "(ILnet/minecraft/command/ICommandSender;)V", false));
                        m.instructions.insertBefore(a, call);
                        count++;
                    } else if (a instanceof MethodInsnNode && ((MethodInsnNode) a).owner.equals("net/minecraft/command/ICommand")
                        && (((MethodInsnNode) a).name.equals("processCommand") || ((MethodInsnNode) a).name.equals("func_71515_b"))) {
                        InsnList call = new InsnList();
                        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATS, "running", "(Lnet/minecraft/command/ICommandSender;)V", false));
                        m.instructions.insertBefore(a, call);
                        count++;
                    }
                }
                m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, STATS, "enter", "()V", false));
                count++;
            } else if (name.endsWith("CommandBlockLogic") && (m.name.equals("func_145758_a") || m.name.equals("func_145759_b"))
                && m.desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)V")) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATS, m.name.equals("func_145758_a") ? "writeLogic" : "readLogic",
                    "(Lnet/minecraft/command/server/CommandBlockLogic;Lnet/minecraft/nbt/NBTTagCompound;)V", false));
                Transformer.beforeReturns(m, call);
                count++;
            }
        }
        System.out.println("[mc112bridge] 1.12 command stats in " + name + ": " + count);
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
