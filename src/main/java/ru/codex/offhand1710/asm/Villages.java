package ru.codex.offhand1710.asm;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Brings the village generation of 1.7.10 to 1.12.2: no biome test per piece, the start piece knows its own type,
 * the type and the zombie flag are kept by every piece and saved, the well ring is cobblestone, the road is the 1.12
 * one. Each change is skipped when its pattern is not found.
 */
final class Villages {
    private static final String PKG = "net/minecraft/world/gen/structure/";
    private static final String VILLAGE = PKG + "StructureVillagePieces$Village";
    private static final String START = PKG + "StructureVillagePieces$Start";
    private static final String HOOK = "ru/codex/offhand1710/Village112";
    private static final String BIOME = "Lnet/minecraft/world/biome/BiomeGenBase;";
    private static final String NBT = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String TYPE = "mc112Type";
    private static final String ZOMBIE = "mc112Zombie";
    /** The desert flag of StructureVillagePieces.Village: SRG name (also the dev name of these mappings) and later MCP names. */
    static final String[] DESERT = {"field_143014_b", "isDesertVillage", "isDesert"};

    private Villages() {
    }

    /** The name of the first of the candidate fields the class has, or null. */
    private static String fieldOf(ClassNode node, String[] names) {
        for (String name : names) {
            for (FieldNode f : node.fields) {
                if (name.equals(f.name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private static boolean named(MethodNode m, String srg, String... mcp) {
        if (m.name.equals(srg)) {
            return true;
        }
        for (String name : mcp) {
            if (m.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void warn(String what) {
        LogManager.getLogger("mc112bridge").warn("Village patch not applied: " + what);
    }

    private static boolean isOakDoor(AbstractInsnNode n) {
        if (n == null || n.getOpcode() != Opcodes.GETSTATIC) {
            return false;
        }
        FieldInsnNode f = (FieldInsnNode) n;
        return "net/minecraft/init/Blocks".equals(f.owner) && ("wooden_door".equals(f.name) || "field_150466_ao".equals(f.name));
    }

    private static AbstractInsnNode nextCode(AbstractInsnNode n) {
        AbstractInsnNode p = n.getNext();
        while (p != null && p.getOpcode() < 0) {
            p = p.getNext();
        }
        return p;
    }

    /**
     * "block == Blocks.wooden_door" (getstatic, if_acmpeq/if_acmpne) becomes Village112.isWoodDoor(block), so the
     * doors of the other woods count wherever the class tests for the oak door. Returns the number of tests replaced.
     */
    static int woodDoorTests(ClassNode node) {
        int done = 0;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode n : m.instructions.toArray()) {
                AbstractInsnNode next = isOakDoor(n) ? nextCode(n) : null;
                if (next != null && (next.getOpcode() == Opcodes.IF_ACMPEQ || next.getOpcode() == Opcodes.IF_ACMPNE)) {
                    LabelNode target = ((JumpInsnNode) next).label;
                    m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "isWoodDoor", "(Lnet/minecraft/block/Block;)Z", false));
                    m.instructions.set(next, new JumpInsnNode(next.getOpcode() == Opcodes.IF_ACMPEQ ? Opcodes.IFNE : Opcodes.IFEQ, target));
                    done++;
                }
            }
        }
        return done;
    }

    /** The door tests of a vanilla class; logs and leaves the class as it is when the count is not the expected one. */
    static byte[] doors(String name, byte[] bytes, int expected) {
        ClassNode node = Transformer.read(bytes);
        int done = woodDoorTests(node);
        if (done != expected) {
            LogManager.getLogger("mc112bridge").warn("Door patch not applied: " + name + " has " + done + " oak door tests, expected " + expected);
            return bytes;
        }
        return Transformer.write(node);
    }

    /**
     * EntityAIMoveIndoors.shouldExecute: the daytime test of 1.12.2 (Village112.stayOutside) first. 1.7.10 sends
     * villagers of biomes without rain (savanna, desert, snowy) indoors all day at speed 1.0, 2.8 times their walk.
     */
    static byte[] moveIndoors(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        for (MethodNode m : node.methods) {
            if (!named(m, "func_75250_a", "shouldExecute") || !"()Z".equals(m.desc)) {
                continue;
            }
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (n.getOpcode() == Opcodes.GETFIELD && node.name.equals(((FieldInsnNode) n).owner)
                        && "Lnet/minecraft/entity/EntityCreature;".equals(((FieldInsnNode) n).desc)) {
                    FieldInsnNode f = (FieldInsnNode) n;
                    LabelNode go = new LabelNode();
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(new FieldInsnNode(Opcodes.GETFIELD, f.owner, f.name, f.desc));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "stayOutside", "(Lnet/minecraft/entity/EntityCreature;)Z", false));
                    l.add(new JumpInsnNode(Opcodes.IFEQ, go));
                    l.add(new InsnNode(Opcodes.ICONST_0));
                    l.add(new InsnNode(Opcodes.IRETURN));
                    l.add(go);
                    m.instructions.insert(l);
                    return Transformer.write(node);
                }
            }
        }
        warn("EntityAIMoveIndoors daytime test");
        return bytes;
    }

    /**
     * StructureComponent: the door of placeDoorAtCurrentPosition is the biome door of the piece (Village112.door)
     * instead of the oak door, and getMetadataWithOffset turns every wooden door like the oak one.
     */
    static void structureDoors(ClassNode node) {
        int placed = 0;
        String mode = fieldOf(node, new String[] {"field_74885_f", "coordBaseMode"});
        String sc = "Lnet/minecraft/world/gen/structure/StructureComponent;";
        boolean offset = false;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "placeDoorAtCurrentPosition", "func_74881_a")) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    AbstractInsnNode next = isOakDoor(n) ? nextCode(n) : null;
                    if (next != null && next.getOpcode() == Opcodes.INVOKESTATIC) {
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "door", "(" + sc + ")Lnet/minecraft/block/Block;", false));
                        m.instructions.insertBefore(n, l);
                        m.instructions.remove(n);
                        if (mode != null && "(Lnet/minecraft/world/World;IIIILnet/minecraft/block/Block;)V".equals(((MethodInsnNode) next).desc)) {
                            // ItemDoor.placeDoorBlock, then the upper half gets the hinge of 1.12 (village pieces)
                            InsnList d = new InsnList();
                            d.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            d.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            d.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, mode, "I"));
                            m.instructions.insertBefore(next, d);
                            m.instructions.set(next, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "placeDoor", "(Lnet/minecraft/world/World;IIIILnet/minecraft/block/Block;" + sc + "I)V", false));
                        } else {
                            warn("door hinge");
                        }
                        placed++;
                    }
                }
            } else if (mode != null && Transformer.named(m, "getMetadataWithOffset", "func_151555_a") && "(Lnet/minecraft/block/Block;I)I".equals(m.desc)) {
                // a village door of coordBaseMode 2 turns by two as in 1.12 (Village112.doorMeta, -1 = not handled)
                LabelNode go = new LabelNode();
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, mode, "I"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new VarInsnNode(Opcodes.ILOAD, 2));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "doorMeta", "(" + sc + "ILnet/minecraft/block/Block;I)I", false));
                l.add(new InsnNode(Opcodes.DUP));
                l.add(new JumpInsnNode(Opcodes.IFLT, go));
                l.add(new InsnNode(Opcodes.IRETURN));
                l.add(go);
                l.add(new InsnNode(Opcodes.POP));
                m.instructions.insert(l);
                offset = true;
            }
        }
        if (placed != 1) {
            warn("biome door placed " + placed + " of 1");
        }
        if (!offset) {
            warn("door metadata of coordBaseMode 2");
        }
        int tests = woodDoorTests(node);
        if (tests != 1) {
            LogManager.getLogger("mc112bridge").warn("Door patch not applied: StructureComponent has " + tests + " oak door tests, expected 1");
        }
    }

    static byte[] transform(String inner, byte[] bytes) {
        if ("".equals(inner)) {
            return pieces(bytes);
        }
        if ("$Village".equals(inner)) {
            return village(bytes);
        }
        if ("$Start".equals(inner)) {
            return start(bytes);
        }
        if ("$Well".equals(inner)) {
            return well(bytes);
        }
        if ("$Path".equals(inner)) {
            return path(bytes);
        }
        if ("$Field1".equals(inner) || "$Field2".equals(inner)) {
            return field(inner, bytes);
        }
        if ("$House2".equals(inner)) {
            return blacksmith(bytes);
        }
        return bytes;
    }

    private static boolean intPush(AbstractInsnNode n) {
        int op = n == null ? -1 : n.getOpcode();
        return op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5 || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH;
    }

    /**
     * House2 (blacksmith): after each furnace and after the chest, Village112.furnace / Village112.chest set the facing
     * of 1.12 (1.7.10 lets BlockFurnace/BlockChest pick one from the neighbours). The x, y, z of the call are copied.
     */
    private static byte[] blacksmith(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        String sc = "Lnet/minecraft/world/gen/structure/StructureComponent;";
        String bb = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;";
        int furnaces = 0;
        int chests = 0;
        for (MethodNode m : node.methods) {
            if (!Transformer.named(m, "addComponentParts", "func_74875_a")) {
                continue;
            }
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (n.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) n;
                if ("(Lnet/minecraft/world/World;Lnet/minecraft/block/Block;IIIILnet/minecraft/world/gen/structure/StructureBoundingBox;)V".equals(call.desc)) {
                    // getstatic furnace, meta, x, y, z, aload bb, call
                    AbstractInsnNode area = Transformer.previousCode(n);
                    AbstractInsnNode z = Transformer.previousCode(area);
                    AbstractInsnNode y = Transformer.previousCode(z);
                    AbstractInsnNode x = Transformer.previousCode(y);
                    AbstractInsnNode meta = Transformer.previousCode(x);
                    AbstractInsnNode block = Transformer.previousCode(meta);
                    if (block == null || block.getOpcode() != Opcodes.GETSTATIC || area.getOpcode() != Opcodes.ALOAD || !intPush(x) || !intPush(y) || !intPush(z)) {
                        continue;
                    }
                    String f = ((FieldInsnNode) block).name;
                    if (!"furnace".equals(f) && !"field_150460_al".equals(f)) {
                        continue;
                    }
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    l.add(x.clone(null));
                    l.add(y.clone(null));
                    l.add(z.clone(null));
                    l.add(area.clone(null));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "furnace", "(" + sc + "Lnet/minecraft/world/World;III" + bb + ")V", false));
                    m.instructions.insert(n, l);
                    furnaces++;
                } else if ("(Lnet/minecraft/world/World;Lnet/minecraft/world/gen/structure/StructureBoundingBox;Ljava/util/Random;III[Lnet/minecraft/util/WeightedRandomChestContent;I)Z".equals(call.desc)) {
                    // aload world, aload bb, aload random, x, y, z, items..., count, call
                    AbstractInsnNode p = Transformer.previousCode(n);
                    AbstractInsnNode z = null;
                    for (int i = 0; i < 12 && p != null; i++) {
                        if (intPush(p) && intPush(Transformer.previousCode(p)) && intPush(Transformer.previousCode(Transformer.previousCode(p)))) {
                            z = p;
                            break;
                        }
                        p = Transformer.previousCode(p);
                    }
                    if (z == null) {
                        continue;
                    }
                    AbstractInsnNode y = Transformer.previousCode(z);
                    AbstractInsnNode x = Transformer.previousCode(y);
                    AbstractInsnNode random = Transformer.previousCode(x);
                    AbstractInsnNode area = Transformer.previousCode(random);
                    if (area == null || area.getOpcode() != Opcodes.ALOAD) {
                        continue;
                    }
                    InsnList l = new InsnList();
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    l.add(x.clone(null));
                    l.add(y.clone(null));
                    l.add(z.clone(null));
                    l.add(area.clone(null));
                    l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "chest", "(" + sc + "Lnet/minecraft/world/World;III" + bb + ")V", false));
                    // after the call: the boolean result is still on the stack, the hook leaves it there
                    m.instructions.insert(n, l);
                    chests++;
                }
            }
        }
        if (furnaces != 2 || chests != 1) {
            warn("blacksmith facing: furnaces " + furnaces + " of 2, chests " + chests + " of 1");
            return bytes;
        }
        return Transformer.write(node);
    }

    /**
     * Field1/Field2: the crop of 1.12 (Village112.cropType: nextInt(10), beetroots included) and the age of a beetroot
     * planted as 1.12 does (Village112.cropAge after each MathHelper.getRandomIntegerInRange, the block is below it).
     */
    private static byte[] field(String inner, byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        int choosers = 0;
        int ages = 0;
        for (MethodNode m : node.methods) {
            if ("(Ljava/util/Random;)Lnet/minecraft/block/Block;".equals(m.desc)) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "cropType", "(Ljava/util/Random;)Lnet/minecraft/block/Block;", false));
                l.add(new InsnNode(Opcodes.ARETURN));
                m.instructions.insert(l);
                choosers++;
            } else if (Transformer.named(m, "addComponentParts", "func_74875_a")) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKESTATIC && "(Ljava/util/Random;II)I".equals(((MethodInsnNode) n).desc)) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.DUP2));
                        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "cropAge", "(Lnet/minecraft/block/Block;I)I", false));
                        l.add(new InsnNode(Opcodes.SWAP));
                        l.add(new InsnNode(Opcodes.POP));
                        m.instructions.insert(n, l);
                        ages++;
                    }
                }
            }
        }
        int want = "$Field1".equals(inner) ? 8 : 4;
        if (choosers != 1 || ages != want) {
            warn("crops of " + inner + ": choosers " + choosers + " of 1, ages " + ages + " of " + want);
            return bytes;
        }
        return Transformer.write(node);
    }

    /** getNextVillageStructureComponent and getNextComponentVillagePath: the areBiomesViable test always passes. */
    private static byte[] pieces(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        int done = 0;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode n : m.instructions.toArray()) {
                if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && "(IIILjava/util/List;)Z".equals(((MethodInsnNode) n).desc)
                    && ("areBiomesViable".equals(((MethodInsnNode) n).name) || "func_76940_a".equals(((MethodInsnNode) n).name))) {
                    InsnList l = new InsnList();
                    l.add(new InsnNode(Opcodes.POP));
                    l.add(new InsnNode(Opcodes.POP2));
                    l.add(new InsnNode(Opcodes.POP2));
                    l.add(new InsnNode(Opcodes.ICONST_1));
                    m.instructions.insertBefore(n, l);
                    m.instructions.remove(n);
                    done++;
                }
            }
        }
        if (done != 2) {
            warn("biome tests replaced " + done + " of 2");
        }
        return Transformer.write(node);
    }

    private static FieldInsnNode field(int opcode, String name, String desc) {
        return new FieldInsnNode(opcode, VILLAGE, name, desc);
    }

    private static byte[] village(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, TYPE, "I", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, ZOMBIE, "Z", null, null));
        String desert = fieldOf(node, DESERT);
        if (desert == null) {
            warn("desert flag field");
        }
        for (FieldNode f : node.fields) {
            if (f.name.equals(desert) || "startPiece".equals(f.name)) {
                f.access = Opcodes.ACC_PUBLIC;
            }
        }
        for (MethodNode m : node.methods) {
            if ("<init>".equals(m.name) && ("(L" + START + ";I)V").equals(m.desc)) {
                // A new piece takes the type and the zombie flag of its start piece.
                LabelNode skip = new LabelNode();
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new JumpInsnNode(Opcodes.IFNULL, skip));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(field(Opcodes.GETFIELD, TYPE, "I"));
                l.add(field(Opcodes.PUTFIELD, TYPE, "I"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(field(Opcodes.GETFIELD, ZOMBIE, "Z"));
                l.add(field(Opcodes.PUTFIELD, ZOMBIE, "Z"));
                l.add(skip);
                AbstractInsnNode last = null;
                int returns = 0;
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.RETURN) {
                        last = n;
                        returns++;
                    }
                }
                if (returns == 1) {
                    m.instructions.insertBefore(last, l);
                } else {
                    warn("village constructor");
                }
            } else if (named(m, "func_143012_a", "writeStructureToNBT") && ("(" + NBT + ")V").equals(m.desc)) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(field(Opcodes.GETFIELD, TYPE, "I"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(field(Opcodes.GETFIELD, ZOMBIE, "Z"));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "write", "(" + NBT + "IZ)V", false));
                Transformer.beforeReturns(m, l);
            } else if (named(m, "func_143011_b", "readStructureFromNBT") && ("(" + NBT + ")V").equals(m.desc)) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                if (desert != null) {
                    l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    l.add(field(Opcodes.GETFIELD, desert, "Z"));
                } else {
                    l.add(new InsnNode(Opcodes.ICONST_0));
                }
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "readType", "(" + NBT + "Z)I", false));
                l.add(field(Opcodes.PUTFIELD, TYPE, "I"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "readZombie", "(" + NBT + ")Z", false));
                l.add(field(Opcodes.PUTFIELD, ZOMBIE, "Z"));
                Transformer.beforeReturns(m, l);
            } else if (named(m, "func_151558_b", "getBiomeSpecificBlock") || named(m, "func_151557_c", "getBiomeSpecificBlockMetadata")) {
                eventBiome(m);
                // the blacksmith keeps its cobblestone (and its meta) as in 1.12
                boolean block = m.desc.endsWith(")Lnet/minecraft/block/Block;");
                LabelNode go = new LabelNode();
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 1));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "keepBlock", "(Lnet/minecraft/world/gen/structure/StructureComponent;Lnet/minecraft/block/Block;)Z", false));
                l.add(new JumpInsnNode(Opcodes.IFEQ, go));
                l.add(new VarInsnNode(block ? Opcodes.ALOAD : Opcodes.ILOAD, block ? 1 : 2));
                l.add(new InsnNode(block ? Opcodes.ARETURN : Opcodes.IRETURN));
                l.add(go);
                m.instructions.insert(l);
            } else if (Transformer.named(m, "placeBlockAtCurrentPosition", "func_151550_a")) {
                InsnList load = new InsnList();
                load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                load.add(field(Opcodes.GETFIELD, ZOMBIE, "Z"));
                load.add(new VarInsnNode(Opcodes.ALOAD, 2));
                Transformer.guard(m, load, HOOK, "skipBlock", "(ZLnet/minecraft/block/Block;)Z", Opcodes.RETURN, 0);
            } else if (named(m, "func_74889_b", "getAverageGroundLevel")) {
                // 1.12: max(top, provider.getAverageGroundLevel() - 1)
                int done = 0;
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && "()I".equals(((MethodInsnNode) n).desc)
                            && ("func_76557_i".equals(((MethodInsnNode) n).name) || "getAverageGroundLevel".equals(((MethodInsnNode) n).name))) {
                        InsnList l = new InsnList();
                        l.add(new InsnNode(Opcodes.ICONST_1));
                        l.add(new InsnNode(Opcodes.ISUB));
                        m.instructions.insert(n, l);
                        done++;
                    }
                }
                if (done != 1) {
                    warn("average ground level " + done + " of 1");
                }
            } else if (Transformer.named(m, "spawnVillagers", "func_74893_a")) {
                boolean found = false;
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && "(Lnet/minecraft/entity/Entity;)Z".equals(((MethodInsnNode) n).desc)) {
                        InsnList l = new InsnList();
                        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        l.add(field(Opcodes.GETFIELD, ZOMBIE, "Z"));
                        m.instructions.insertBefore(n, l);
                        m.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "spawn", "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Z)Z", false));
                        found = true;
                    }
                }
                if (!found) {
                    warn("zombie villagers");
                }
            }
        }
        return Transformer.write(node);
    }

    /** The biome of the block events: a piece without start piece (loaded from disk) gives the biome of its type. */
    private static void eventBiome(MethodNode m) {
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(((MethodInsnNode) n).name)
                && ("(" + BIOME + "Lnet/minecraft/block/Block;I)V").equals(((MethodInsnNode) n).desc)) {
                AbstractInsnNode meta = Transformer.previousCode(n);
                AbstractInsnNode block = meta == null ? null : Transformer.previousCode(meta);
                if (block == null || block.getOpcode() != Opcodes.ALOAD || meta.getOpcode() != Opcodes.ILOAD) {
                    warn("event biome in " + m.name);
                    return;
                }
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(field(Opcodes.GETFIELD, TYPE, "I"));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "eventBiome", "(" + BIOME + "I)" + BIOME, false));
                m.instructions.insertBefore(block, l);
                return;
            }
        }
        warn("event biome in " + m.name);
    }

    /** The start piece gets its type, the desert flag, itself as start piece and the zombie roll of 1.12. */
    private static byte[] start(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean found = false;
        for (MethodNode m : node.methods) {
            if ("<init>".equals(m.name) && m.desc.startsWith("(Lnet/minecraft/world/biome/WorldChunkManager;ILjava/util/Random;")) {
                InsnList l = new InsnList();
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new FieldInsnNode(Opcodes.GETFIELD, START, "biome", BIOME));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "typeOf", "(" + BIOME + ")I", false));
                l.add(field(Opcodes.PUTFIELD, TYPE, "I"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(field(Opcodes.GETFIELD, TYPE, "I"));
                // the desert flag is set by reflection: Start is transformed before its superclass Village, so the
                // field name (SRG or MCP) is not known here
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "setDesert", "(Lnet/minecraft/world/gen/structure/StructureComponent;I)V", false));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(field(Opcodes.PUTFIELD, "startPiece", "L" + START + ";"));
                l.add(new VarInsnNode(Opcodes.ALOAD, 0));
                l.add(new VarInsnNode(Opcodes.ALOAD, 3));
                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "zombieRoll", "(Ljava/util/Random;)Z", false));
                l.add(field(Opcodes.PUTFIELD, ZOMBIE, "Z"));
                Transformer.beforeReturns(m, l);
                found = true;
            }
        }
        if (!found) {
            warn("start piece");
        }
        return Transformer.write(node);
    }

    /** The ring around the well is cobblestone (of the village type) as in 1.12, not gravel. */
    private static byte[] well(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean found = false;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "addComponentParts", "func_74875_a")) {
                for (AbstractInsnNode n : m.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/Blocks".equals(((FieldInsnNode) n).owner)) {
                        FieldInsnNode f = (FieldInsnNode) n;
                        if ("gravel".equals(f.name)) {
                            f.name = "cobblestone";
                            found = true;
                        } else if ("field_150351_n".equals(f.name)) {
                            f.name = "field_150347_e";
                            found = true;
                        }
                    }
                }
            }
        }
        if (!found) {
            warn("well ring");
        }
        return Transformer.write(node);
    }

    /** The road of 1.12: Village112.path does the work and the piece reports success. */
    private static byte[] path(byte[] bytes) {
        ClassNode node = Transformer.read(bytes);
        boolean found = false;
        for (MethodNode m : node.methods) {
            if (Transformer.named(m, "addComponentParts", "func_74875_a")) {
                InsnList load = new InsnList();
                load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                load.add(field(Opcodes.GETFIELD, TYPE, "I"));
                load.add(new VarInsnNode(Opcodes.ALOAD, 1));
                load.add(new VarInsnNode(Opcodes.ALOAD, 3));
                Transformer.guard(m, load, HOOK, "path", "(Lnet/minecraft/world/gen/structure/StructureComponent;ILnet/minecraft/world/World;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Z", Opcodes.IRETURN, 1);
                found = true;
            }
        }
        if (!found) {
            warn("road");
        }
        return Transformer.write(node);
    }
}
