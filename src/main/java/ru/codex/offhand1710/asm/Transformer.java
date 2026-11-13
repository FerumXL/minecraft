package ru.codex.offhand1710.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Runs after the Forge deobfuscation transformer, so class names are readable and members are MCP or SRG. */
public final class Transformer implements IClassTransformer {
    private static final String WORLD = "Lnet/minecraft/world/World;";

    static boolean named(MethodNode m, String mcp, String srg) {
        return m.name.equals(mcp) || m.name.equals(srg);
    }

    static void beforeReturns(MethodNode method, InsnList call) {
        AbstractInsnNode[] all = method.instructions.toArray();
        for (AbstractInsnNode node : all) {
            if (node.getOpcode() == Opcodes.RETURN) {
                InsnList copy = new InsnList();
                for (AbstractInsnNode c = call.getFirst(); c != null; c = c.getNext()) {
                    copy.add(c.clone(null));
                }
                method.instructions.insertBefore(node, copy);
            }
        }
    }

    static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /** if (Hooks.name(args)) return <constant>; at the start of a method. Package use: next hooks. */
    static void guard(MethodNode method, InsnList load, String owner, String name, String desc, int returnOpcode, int constant) {
        LabelNode go = new LabelNode();
        InsnList list = new InsnList();
        list.add(load);
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false));
        list.add(new JumpInsnNode(Opcodes.IFEQ, go));
        if (returnOpcode == Opcodes.IRETURN) {
            list.add(new InsnNode(constant == 0 ? Opcodes.ICONST_0 : Opcodes.ICONST_1));
        }
        list.add(new InsnNode(returnOpcode));
        list.add(go);
        method.instructions.insert(list);
    }

    static AbstractInsnNode previousCode(AbstractInsnNode n) {
        AbstractInsnNode p = n.getPrevious();
        while (p != null && p.getOpcode() < 0) {
            p = p.getPrevious();
        }
        return p;
    }

    /**
     * Passes the local "is this outside the window" of a GuiContainer mouse method through the book. The local is the
     * one tested just before the first -999; the value goes through the hook right where it is stored.
     */
    private static void outsideTest(MethodNode m, String owner, String gui) {
        int local = -1;
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.SIPUSH && ((IntInsnNode) n).operand == -999) {
                AbstractInsnNode jump = previousCode(n);
                AbstractInsnNode load = jump == null ? null : previousCode(jump);
                if (load != null && jump.getOpcode() == Opcodes.IFEQ && load.getOpcode() == Opcodes.ILOAD) {
                    local = ((VarInsnNode) load).var;
                }
                break;
            }
        }
        if (local < 0) {
            return;
        }
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.ISTORE && ((VarInsnNode) n).var == local) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ILOAD, 1));
                l.add(new VarInsnNode(Opcodes.ILOAD, 2));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "outsideWindow", "(Z" + gui + "II)Z", false));
                m.instructions.insertBefore(n, l);
                return;
            }
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || System.getenv("MC112_NOPATCH") != null) {
            return bytes;
        }
        if ("net.minecraft.block.BlockCommandBlock".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (named(m, "updateTick", "func_149674_a")) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 3));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 4));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Content122", "afterImpulse", "(" + WORLD + "III)V", false));
                    beforeReturns(m, call);
                }
            }
            return write(node);
        }
        if ("net.minecraft.tileentity.TileEntityBrewingStand".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (named(m, "canBrew", "func_145934_k")) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    guard(m, load, "ru/codex/offhand1710/Brew112", "canBrew", "(Lnet/minecraft/tileentity/TileEntityBrewingStand;)Z", Opcodes.IRETURN, 1);
                }
            }
            return write(node);
        }
        if ("net.minecraft.item.ItemGlassBottle".equals(transformedName)) {
            ClassNode node = read(bytes);
            String stack = "Lnet/minecraft/item/ItemStack;";
            for (MethodNode m : node.methods) {
                if (named(m, "onItemRightClick", "func_77659_a")) {
                    LabelNode go = new LabelNode();
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    l.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Brew112", "bottle", "(" + stack + WORLD + "Lnet/minecraft/entity/player/EntityPlayer;)" + stack, false));
                    l.add(new InsnNode(Opcodes.DUP));
                    l.add(new JumpInsnNode(Opcodes.IFNULL, go));
                    l.add(new InsnNode(Opcodes.ARETURN));
                    l.add(go);
                    l.add(new InsnNode(Opcodes.POP));
                    m.instructions.insert(l);
                }
            }
            return write(node);
        }
        if ("net.minecraft.entity.projectile.EntityFishHook".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKESTATIC && "func_151386_g".equals(((MethodInsnNode) n).name)) {
                        MethodInsnNode call = (MethodInsnNode) n;
                        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Maps112", "fishingLuck", call.desc, false));
                    }
                }
            }
            return write(node);
        }
        if ("net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
            ClassNode node = read(bytes);
            String h = "ru/codex/offhand1710/Content119";
            String living = "Lnet/minecraft/entity/EntityLivingBase;";
            for (MethodNode m : node.methods) {
                if (named(m, "moveEntityWithHeading", "func_70612_e")) {
                    boolean flying = false;
                    int drag = 0;
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (!flying && n.getOpcode() == Opcodes.INVOKEVIRTUAL && (((MethodInsnNode) n).name.equals("moveFlying") || ((MethodInsnNode) n).name.equals("func_70060_a"))) {
                            flying = true;
                            InsnList l = new InsnList();
                            l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h, "waterAcceleration", "(F" + living + ")F", false));
                            m.instructions.insertBefore(n, l);
                        } else if (flying && drag < 3 && n instanceof org.objectweb.asm.tree.LdcInsnNode && Double.valueOf(0.800000011920929D).equals(((org.objectweb.asm.tree.LdcInsnNode) n).cst)) {
                            drag++;
                            if (drag != 2) {
                                InsnList l = new InsnList();
                                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h, "waterDrag", "(D" + living + ")D", false));
                                m.instructions.insert(n, l);
                            }
                        }
                    }
                }
            }
            return write(node);
        }
        if ("net.minecraft.block.BlockPistonBase".equals(transformedName)) {
            ClassNode node = read(bytes);
            String p = "ru/codex/offhand1710/Piston112";
            String base = "Lnet/minecraft/block/BlockPistonBase;";
            for (MethodNode m : node.methods) {
                if (named(m, "tryExtend", "func_150079_i")) {
                    LabelNode go = new LabelNode();
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    for (int i = 2; i <= 5; i++) {
                        l.add(new VarInsnNode(Opcodes.ILOAD, i));
                    }
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p, "extend", "(" + base + WORLD + "IIII)Z", false));
                    l.add(new JumpInsnNode(Opcodes.IFEQ, go));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p, "result", "()Z", false));
                    l.add(new InsnNode(Opcodes.IRETURN));
                    l.add(go);
                    m.instructions.insert(l);
                } else if (named(m, "onBlockEventReceived", "func_149696_a")) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    load.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    for (int i = 2; i <= 6; i++) {
                        load.add(new VarInsnNode(Opcodes.ILOAD, i));
                    }
                    guard(m, load, p, "retract", "(" + base + WORLD + "IIIII)Z", Opcodes.IRETURN, 1);
                }
            }
            return write(node);
        }
        if ("net.minecraft.tileentity.TileEntityPiston".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (m.name.equals("func_145863_a") && m.desc.equals("(FF)V")) {
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(new VarInsnNode(Opcodes.FLOAD, 1));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Piston112", "bounce", "(Lnet/minecraft/tileentity/TileEntityPiston;F)V", false));
                    m.instructions.insert(l);
                }
            }
            return write(node);
        }
        if ("net.minecraft.client.gui.inventory.GuiContainer".equals(transformedName)) {
            ClassNode node = read(bytes);
            String owner = "ru/codex/offhand1710/client/DataClient";
            String gui = "Lnet/minecraft/client/gui/inventory/GuiContainer;";
            for (MethodNode m : node.methods) {
                boolean click = named(m, "mouseClicked", "func_73864_a") && "(III)V".equals(m.desc);
                if (click || named(m, "mouseMovedOrUp", "func_146286_b") && "(III)V".equals(m.desc)) {
                    outsideTest(m, owner, gui);
                }
                if (click) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    for (int i = 1; i <= 3; i++) {
                        load.add(new VarInsnNode(Opcodes.ILOAD, i));
                    }
                    guard(m, load, owner, "bookClick", "(" + gui + "III)Z", Opcodes.RETURN, 0);
                } else if (named(m, "keyTyped", "func_73869_a") && "(CI)V".equals(m.desc)) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    load.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    load.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    guard(m, load, owner, "bookKey", "(" + gui + "CI)Z", Opcodes.RETURN, 0);
                }
            }
            return write(node);
        }
        if (transformedName.startsWith("net.minecraft.world.gen.structure.StructureVillagePieces")) {
            return Villages.transform(transformedName.substring("net.minecraft.world.gen.structure.StructureVillagePieces".length()), bytes);
        }
        if ("net.minecraft.world.gen.structure.StructureComponent".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (named(m, "placeDoorAtCurrentPosition", "func_74881_a")) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    guard(m, load, "ru/codex/offhand1710/Village112", "skipDoor", "(Lnet/minecraft/world/gen/structure/StructureComponent;)Z", Opcodes.RETURN, 0);
                }
            }
            return write(node);
        }
        if ("net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
            ClassNode node = read(bytes);
            String hook = "ru/codex/offhand1710/client/Client122";
            for (MethodNode m : node.methods) {
                if (named(m, "renderHand", "func_78476_b")) {
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n.getOpcode() == Opcodes.INVOKESTATIC && "glLoadIdentity".equals(((MethodInsnNode) n).name)) {
                            m.instructions.insert(n, new MethodInsnNode(Opcodes.INVOKESTATIC, hook, "handBegin", "()V", false));
                            break;
                        }
                    }
                    InsnList call = new InsnList();
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hook, "handEnd", "()V", false));
                    beforeReturns(m, call);
                } else if (named(m, "updateCameraAndRender", "func_78480_b") && "(F)V".equals(m.desc)) {
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && "(FJ)V".equals(((MethodInsnNode) n).desc)
                            && ("renderWorld".equals(((MethodInsnNode) n).name) || "func_78471_a".equals(((MethodInsnNode) n).name))) {
                            m.instructions.insert(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/client/GlowOutline", "afterWorld", "()V", false));
                        }
                    }
                }
            }
            return write(node);
        }
        return bytes;
    }
}
