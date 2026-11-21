package ru.codex.offhand1710.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
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

    /** True when the instructions just before n are ALOAD 0, GETFIELD randomGenerator, after ISTORE of treesPerChunk. */
    private static boolean prevIsTreesPerChunk(AbstractInsnNode n) {
        for (AbstractInsnNode p = n.getPrevious(), k = null; p != null && (k == null || k != p); p = p.getPrevious()) {
            if (p instanceof org.objectweb.asm.tree.FieldInsnNode) {
                String f = ((org.objectweb.asm.tree.FieldInsnNode) p).name;
                if (f.equals("treesPerChunk") || f.equals("field_76832_z")) {
                    return true;
                }
                if (!(f.equals("randomGenerator") || f.equals("field_76813_b"))) {
                    return false;
                }
            }
        }
        return false;
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

    /** IntCache: getIntCache, resetIntCache and getCacheSizes call ru.codex.offhand1710.IntCache112 (one cache per thread). */
    private static byte[] intCache(byte[] bytes) {
        ClassNode node = read(bytes);
        String h = "ru/codex/offhand1710/IntCache112";
        int done = 0;
        for (MethodNode m : node.methods) {
            InsnList l = new InsnList();
            if (named(m, "getIntCache", "func_76445_a") && "(I)[I".equals(m.desc)) {
                l.add(new VarInsnNode(Opcodes.ILOAD, 0));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h, "get", "(I)[I", false));
                l.add(new InsnNode(Opcodes.ARETURN));
            } else if (named(m, "resetIntCache", "func_76446_a") && "()V".equals(m.desc)) {
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h, "reset", "()V", false));
                l.add(new InsnNode(Opcodes.RETURN));
            } else if (named(m, "getCacheSizes", "func_85144_b") && "()Ljava/lang/String;".equals(m.desc)) {
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, h, "sizes", "()Ljava/lang/String;", false));
                l.add(new InsnNode(Opcodes.ARETURN));
            } else {
                continue;
            }
            m.instructions.clear();
            m.tryCatchBlocks.clear();
            if (m.localVariables != null) {
                m.localVariables.clear();
            }
            m.access &= ~Opcodes.ACC_SYNCHRONIZED;
            m.instructions.add(l);
            done++;
        }
        if (done != 3) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").warn("IntCache patch not applied: " + done + " of 3 methods");
            return bytes;
        }
        return write(node);
    }

    /**
     * World.func_147467_a (mood sound and relight checks of a ticked chunk): returns at once for a null chunk
     * (Hooks112.nullChunk logs it). WorldServer.updateBlocks passes getChunkFromChunkCoords, which is null when
     * ChunkProviderServer.loadChunk gets nothing from ChunkIOExecutor.syncChunkLoad for a chunk that is still being
     * saved; with OptiFine's limited chunk set and a large view distance this crashes the server ("Exception ticking
     * world", NPE at World.java:2856).
     */
    private static byte[] nullChunkGuard(byte[] bytes) {
        ClassNode node = read(bytes);
        for (MethodNode m : node.methods) {
            if ("func_147467_a".equals(m.name) && "(IILnet/minecraft/world/chunk/Chunk;)V".equals(m.desc)) {
                LabelNode go = new LabelNode();
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                l.add(new JumpInsnNode(Opcodes.IFNONNULL, go));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ILOAD, 1));
                l.add(new VarInsnNode(Opcodes.ILOAD, 2));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Hooks112", "nullChunk", "(" + WORLD + "II)V", false));
                l.add(new InsnNode(Opcodes.RETURN));
                l.add(go);
                m.instructions.insert(l);
                return write(node);
            }
        }
        org.apache.logging.log4j.LogManager.getLogger("mc112bridge").warn("Null chunk patch not applied: World.func_147467_a not found");
        return bytes;
    }

    /**
     * PlayerManager.PlayerInstance: its ChunkProviderServer.loadChunk calls (the Forge one with a Runnable, or the plain
     * func_73158_c of the Forge build the user runs) go through ChunkStream, and getChunkFromChunkCoords gives an empty
     * chunk instead of generating one that is still waiting in ChunkStream's queue.
     */
    private static byte[] chunkStream(byte[] bytes) {
        ClassNode node = read(bytes);
        String cs = "ru/codex/offhand1710/ChunkStream";
        String provider = "net/minecraft/world/gen/ChunkProviderServer";
        String chunk = "Lnet/minecraft/world/chunk/Chunk;";
        int loads = 0;
        int gets = 0;
        StringBuilder seen = new StringBuilder();
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (!(n instanceof MethodInsnNode) || n.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                    continue;
                }
                MethodInsnNode c = (MethodInsnNode) n;
                if (provider.equals(c.owner) && "loadChunk".equals(c.name) && ("(IILjava/lang/Runnable;)" + chunk).equals(c.desc)) {
                    m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, cs, "load", "(L" + provider + ";IILjava/lang/Runnable;)" + chunk, false));
                    loads++;
                } else if (provider.equals(c.owner) && ("loadChunk".equals(c.name) || "func_73158_c".equals(c.name)) && ("(II)" + chunk).equals(c.desc)) {
                    m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, cs, "loadSync", "(L" + provider + ";II)" + chunk, false));
                    loads++;
                } else if (("getChunkFromChunkCoords".equals(c.name) || "func_72964_e".equals(c.name)) && ("(II)" + chunk).equals(c.desc)
                        && (c.owner.equals("net/minecraft/world/World") || c.owner.equals("net/minecraft/world/WorldServer"))) {
                    // only where a chunk is looked at without being needed: leaving it and the inhabited-time count
                    boolean safe = named(m, "removePlayer", "func_73252_b") || named(m, "processChunk", "func_111194_a");
                    seen.append(" get:").append(m.name).append(safe ? "(replaced)" : "");
                    if (safe) {
                        m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, cs, "existing", "(Lnet/minecraft/world/World;II)" + chunk, false));
                        gets++;
                    }
                } else if (c.owner.contains("ChunkProvider")) {
                    seen.append(' ').append(c.owner).append('.').append(c.name).append(c.desc);
                }
            }
        }
        if (loads == 0) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").warn("Chunk streaming patch not applied: no loadChunk call in PlayerInstance; seen:" + seen);
            return bytes;
        }
        org.apache.logging.log4j.LogManager.getLogger("mc112bridge").info("Chunk streaming: PlayerInstance loadChunk calls " + loads + ", chunk reads " + gets + ";" + seen);
        return write(node);
    }
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || System.getenv("MC112_NOPATCH") != null) {
            return bytes;
        }
        bytes = SoundPatches.transform(transformedName, bytes);
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
        if ("net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
            // orientCamera: f1 = entity.yOffset - 1.62F; the elytra hook lowers the eye to 0.4 while gliding (1.12.2)
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (named(m, "orientCamera", "func_78467_g") && "(F)V".equals(m.desc)) {
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        AbstractInsnNode sub = n.getNext();
                        if (n instanceof org.objectweb.asm.tree.LdcInsnNode && Float.valueOf(1.62F).equals(((org.objectweb.asm.tree.LdcInsnNode) n).cst)
                            && sub != null && sub.getOpcode() == Opcodes.FSUB && n.getPrevious() != null && n.getPrevious().getOpcode() == Opcodes.GETFIELD) {
                            m.instructions.insert(sub, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/client/ElytraClient", "cameraShift", "(F)F", false));
                            break;
                        }
                    }
                }
            }
            // no return: the Client122 hooks further down patch EntityRenderer too (glow after the hand, 0.30.0)
            bytes = write(node);
        }
        if ("net.minecraft.world.biome.BiomeDecorator".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if (named(m, "genDecorations", "func_150513_a") && "(Lnet/minecraft/world/biome/BiomeGenBase;)V".equals(m.desc)) {
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        AbstractInsnNode call = n.getNext(), jump = call == null ? null : call.getNext();
                        if (n instanceof IntInsnNode && n.getOpcode() == Opcodes.BIPUSH && ((IntInsnNode) n).operand == 10
                            && call instanceof MethodInsnNode && "nextInt".equals(((MethodInsnNode) call).name)
                            && jump != null && jump.getOpcode() == Opcodes.IFNE && prevIsTreesPerChunk(n)) {
                            m.instructions.set(n, new VarInsnNode(Opcodes.ALOAD, 1));
                            m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Trees112", "noExtraTree",
                                "(Ljava/util/Random;Lnet/minecraft/world/biome/BiomeGenBase;)Z", false));
                            break;
                        }
                    }
                }
            }
            return write(node);
        }
        if ("com.hbm.lib.HbmWorldGen".equals(transformedName)) {
            // the old HBM surface generator builds its "dungeons" (DesertAtom001, Antenna, Radio, Factory...) straight
            // from IWorldGenerator.generate, not as structures: every such generate call goes through the village check
            ClassNode node = read(bytes);
            int patched = 0;
            for (MethodNode m : node.methods) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n instanceof MethodInsnNode && n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode mi = (MethodInsnNode) n;
                        if (mi.owner.startsWith("com/hbm/world/dungeon/") && ("func_76484_a".equals(mi.name) || "generate".equals(mi.name))
                            && "(Lnet/minecraft/world/World;Ljava/util/Random;III)Z".equals(mi.desc)) {
                            m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/HbmVillageGuard", "dungeon",
                                "(Lnet/minecraft/world/gen/feature/WorldGenerator;Lnet/minecraft/world/World;Ljava/util/Random;III)Z", false));
                            patched++;
                        }
                    }
                }
            }
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").info("HBM surface dungeons routed through the village check: " + patched + " calls");
            return write(node);
        }
        if ("com.hbm.world.gen.nbt.NBTStructure$GenStructure".equals(transformedName) || "com.hbm.world.gen.MapGenNTMFeatures".equals(transformedName)) {
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                // a start whose pieces reach into the area of a possible village keeps no pieces (large bunkers start far away)
                if (named(m, "getStructureStart", "func_75049_b") && "(II)Lnet/minecraft/world/gen/structure/StructureStart;".equals(m.desc)) {
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n.getOpcode() == Opcodes.ARETURN) {
                            InsnList call = new InsnList();
                            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            call.add(new VarInsnNode(Opcodes.ILOAD, 1));
                            call.add(new VarInsnNode(Opcodes.ILOAD, 2));
                            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/HbmVillageGuard", "filterStart",
                                "(Lnet/minecraft/world/gen/structure/StructureStart;Lnet/minecraft/world/gen/MapGenBase;II)Lnet/minecraft/world/gen/structure/StructureStart;", false));
                            m.instructions.insertBefore(n, call);
                        }
                    }
                }
                if (named(m, "canSpawnStructureAtCoords", "func_75047_a") && "(II)Z".equals(m.desc)) {
                    // MapGenNTMFeatures overwrites its arguments; it copies them to locals 3 and 4 first (ILOAD 1, ISTORE 3, ILOAD 2, ISTORE 4)
                    int keepX = 1, keepZ = 2;
                    AbstractInsnNode first = m.instructions.getFirst();
                    while (first != null && first.getOpcode() < 0) {
                        first = first.getNext();
                    }
                    if (transformedName.endsWith("MapGenNTMFeatures")) {
                        AbstractInsnNode s1 = first == null ? null : first.getNext(), s2 = s1 == null ? null : s1.getNext(), s3 = s2 == null ? null : s2.getNext();
                        if (first instanceof VarInsnNode && s2 instanceof VarInsnNode && s1 instanceof VarInsnNode && s1.getOpcode() == Opcodes.ISTORE && s3 instanceof VarInsnNode && s3.getOpcode() == Opcodes.ISTORE
                            && ((VarInsnNode) first).var == 1 && ((VarInsnNode) s2).var == 2) {
                            keepX = ((VarInsnNode) s1).var;
                            keepZ = ((VarInsnNode) s3).var;
                        } else {
                            continue;
                        }
                    }
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n.getOpcode() == Opcodes.IRETURN) {
                            InsnList call = new InsnList();
                            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            call.add(new VarInsnNode(Opcodes.ILOAD, keepX));
                            call.add(new VarInsnNode(Opcodes.ILOAD, keepZ));
                            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/HbmVillageGuard", "allow", "(ZLnet/minecraft/world/gen/MapGenBase;II)Z", false));
                            m.instructions.insertBefore(n, call);
                        }
                    }
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
            Villages.structureDoors(node);
            return write(node);
        }
        // every wooden door is a door for villages, villagers, zombies and the path finder (1.12: BlockDoor of wood)
        if ("net.minecraft.village.VillageCollection".equals(transformedName) || "net.minecraft.village.Village".equals(transformedName)
            || "net.minecraft.entity.ai.EntityAIDoorInteract".equals(transformedName)) {
            return Villages.doors(transformedName, bytes, 1);
        }
        if ("net.minecraft.world.gen.layer.IntCache".equals(transformedName)) {
            return intCache(bytes);
        }
        if (("net.minecraft.world.gen.structure.StructureNetherBridgePieces$Corridor".equals(transformedName)
            || "net.minecraft.world.gen.structure.StructureNetherBridgePieces$Corridor2".equals(transformedName)) && System.getProperty("mc112.fortressLoot17") == null) {
            // fortress chests from loot_tables/chests/nether_bridge.json (obsidian, 2..4 rolls) instead of the 1.7.10 list and 2..5
            ClassNode node = read(bytes);
            int n = 0;
            for (MethodNode m : node.methods) {
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                    if (i.getOpcode() == Opcodes.GETSTATIC && ("field_111019_a".equals(((FieldInsnNode) i).name) || "primaryChestLoot".equals(((FieldInsnNode) i).name))
                        && "[Lnet/minecraft/util/WeightedRandomChestContent;".equals(((FieldInsnNode) i).desc)) {
                        AbstractInsnNode list = new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Loot112", "netherBridge", "()[Lnet/minecraft/util/WeightedRandomChestContent;", false);
                        m.instructions.set(i, list);
                        i = list;
                        // 2 + rand.nextInt(4) follows: the bound becomes the 1.12 roll span
                        for (AbstractInsnNode j = i.getNext(); j != null && j.getOpcode() != Opcodes.INVOKEVIRTUAL; j = j.getNext()) {
                            if (j.getOpcode() == Opcodes.ICONST_4) {
                                m.instructions.set(j, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Loot112", "netherBridgeSpan", "()I", false));
                                break;
                            }
                        }
                        n++;
                    }
                }
            }
            System.out.println("[mc112bridge] fortress chest loot 1.12 in " + transformedName + ": " + n);
            return write(node);
        }
        if ("net.minecraft.util.WeightedRandomChestContent".equals(transformedName) && System.getProperty("mc112.lootPools17") == null) {
            // chests and dispensers whose list comes from a 1.12.2 table are filled by its pools (Loot112.fillPools)
            ClassNode node = read(bytes);
            int n = 0;
            for (MethodNode m : node.methods) {
                boolean chest = named(m, "generateChestContents", "func_76293_a") && m.desc.endsWith("Lnet/minecraft/inventory/IInventory;I)V");
                boolean dispenser = named(m, "generateDispenserContents", "func_150706_a");
                if ((chest || dispenser) && (m.access & Opcodes.ACC_STATIC) != 0) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    load.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    load.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    guard(m, load, "ru/codex/offhand1710/Loot112", "fillPools", "(Ljava/util/Random;[Lnet/minecraft/util/WeightedRandomChestContent;Lnet/minecraft/inventory/IInventory;)Z", Opcodes.RETURN, 0);
                    n++;
                }
            }
            System.out.println("[mc112bridge] chest loot by 1.12 pools: " + n + " methods");
            return write(node);
        }
        if ("net.minecraft.world.chunk.Chunk".equals(transformedName) && System.getProperty("mc112.traceBlocks") != null) {
            // debugging only: who sets which block where (BlockTrace)
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if ("func_150807_a".equals(m.name) && "(IIILnet/minecraft/block/Block;I)Z".equals(m.desc)) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    call.add(new VarInsnNode(Opcodes.ILOAD, 3));
                    call.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/BlockTrace", "onSet", "(Lnet/minecraft/world/chunk/Chunk;IIILnet/minecraft/block/Block;)V", false));
                    m.instructions.insert(call);
                }
            }
            return write(node);
        }
        if ("net.minecraft.world.World".equals(transformedName)) {
            return nullChunkGuard(bytes);
        }
        if ("net.minecraft.server.management.PlayerManager$PlayerInstance".equals(transformedName)) {
            return chunkStream(bytes);
        }
        if ("net.minecraft.world.WorldServer".equals(transformedName)) {
            // updateBlocks ticks only loaded chunks (1.12 ticks the loaded ones); 1.7.10 generates every chunk of the
            // active set in view distance it asks for, which defeats ChunkStream's generation budget
            ClassNode node = read(bytes);
            for (MethodNode m : node.methods) {
                if ("func_147456_g".equals(m.name) && "()V".equals(m.desc)) {
                    // the thunderstorm bolt goes through SkeletonTrap112.strike, which may make it a skeleton trap (1.12)
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n instanceof MethodInsnNode && ("addWeatherEffect".equals(((MethodInsnNode) n).name) || "func_72942_c".equals(((MethodInsnNode) n).name))
                            && "(Lnet/minecraft/entity/Entity;)Z".equals(((MethodInsnNode) n).desc)) {
                            m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/SkeletonTrap112", "strike",
                                "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z", false));
                        }
                    }
                    // after super.func_147456_g(), which builds the active set (setActivePlayerChunksAndCheckLight)
                    for (AbstractInsnNode n : m.instructions.toArray()) {
                        if (n.getOpcode() == Opcodes.INVOKESPECIAL && "func_147456_g".equals(((MethodInsnNode) n).name)) {
                            InsnList l = new InsnList();
                            l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/ChunkStream", "filterActive", "(Lnet/minecraft/world/WorldServer;)V", false));
                            m.instructions.insert(n, l);
                            // the skipped ones go back into the set when the method ends (OptiFine keeps its own
                            // bookkeeping of the chunks it still has to tick once)
                            InsnList back = new InsnList();
                            back.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            back.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/ChunkStream", "restoreActive", "(Lnet/minecraft/world/WorldServer;)V", false));
                            beforeReturns(m, back);
                            return write(node);
                        }
                    }
                }
            }
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").warn("Active chunk patch not applied: WorldServer.func_147456_g not found");
            return bytes;
        }
        if ("net.minecraft.entity.ai.EntityAIMoveIndoors".equals(transformedName)) {
            return Villages.moveIndoors(bytes);
        }
        if ("net.minecraft.pathfinding.PathFinder".equals(transformedName)) {
            return Villages.doors(transformedName, bytes, 2);
        }
        if ("net.minecraft.entity.Entity".equals(transformedName)) {
            return Entities.entity(bytes);
        }
        if ("net.minecraft.entity.monster.EntityZombie".equals(transformedName)) {
            return Entities.zombie(bytes);
        }
        if ("net.minecraft.inventory.ContainerEnchantment".equals(transformedName)) {
            return Entities.enchant(bytes);
        }
        if ("net.minecraft.item.ItemStack".equals(transformedName)) {
            return Entities.itemStack(bytes);
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
