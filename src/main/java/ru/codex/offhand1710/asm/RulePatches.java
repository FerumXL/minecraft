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
        if (System.getProperty("mc112.border17") == null && (name.equals("net.minecraft.server.management.ServerConfigurationManager")
            || name.equals("net.minecraft.world.storage.WorldInfo")
            || name.equals("net.minecraft.world.World") || name.equals("net.minecraft.client.gui.GuiIngame"))) {
            bytes = border(name, bytes);
        }
        if (System.getProperty("mc112.stats17") == null
            && (name.equals("net.minecraft.command.CommandHandler") || name.equals("net.minecraft.command.server.CommandBlockLogic"))) {
            return stats(name, bytes);
        }
        if (name.equals("net.minecraft.world.gen.ChunkProviderServer") && System.getProperty("mc112.gamerules17") == null) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                if (Transformer.named(m, "provideChunk", "func_73154_d") && m.desc.equals("(II)Lnet/minecraft/world/chunk/Chunk;")) {
                    LabelNode go = new LabelNode();
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    list.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/ChunkStream", "spectatorEmpty",
                        "(Lnet/minecraft/world/gen/ChunkProviderServer;II)Lnet/minecraft/world/chunk/Chunk;", false));
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new JumpInsnNode(Opcodes.IFNULL, go));
                    list.add(new InsnNode(Opcodes.ARETURN));
                    list.add(go);
                    list.add(new InsnNode(Opcodes.POP));
                    m.instructions.insert(list);
                    count++;
                }
            }
            System.out.println("[mc112bridge] spectatorsGenerateChunks in ChunkProviderServer: " + count);
            bytes = Transformer.write(node);
        }
        if (System.getProperty("mc112.armor17") == null && (name.equals("net.minecraft.enchantment.EnchantmentProtection")
            || name.equals("net.minecraft.enchantment.EnchantmentHelper"))) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                InsnList list = new InsnList();
                if (name.endsWith("EnchantmentProtection") && Transformer.named(m, "calcModifierDamage", "func_77318_a")
                    && m.desc.equals("(ILnet/minecraft/util/DamageSource;)I")) {
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Armor112", "protection",
                        "(Lnet/minecraft/enchantment/EnchantmentProtection;ILnet/minecraft/util/DamageSource;)I", false));
                } else if (name.endsWith("EnchantmentHelper") && Transformer.named(m, "getEnchantmentModifierDamage", "func_77508_a")
                    && m.desc.equals("([Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;)I")) {
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Armor112", "modifierSum",
                        "([Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;)I", false));
                } else {
                    continue;
                }
                LabelNode go = new LabelNode();
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new JumpInsnNode(Opcodes.IFLT, go));
                list.add(new InsnNode(Opcodes.IRETURN));
                list.add(go);
                list.add(new InsnNode(Opcodes.POP));
                m.instructions.insert(list);
                count++;
            }
            System.out.println("[mc112bridge] 1.12 protection in " + name + ": " + count);
            bytes = Transformer.write(node);
        }
        if (System.getProperty("mc112.armor17") == null && (name.equals("net.minecraftforge.common.ISpecialArmor$ArmorProperties")
            || name.equals("net.minecraft.entity.EntityLivingBase"))) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                boolean players = m.name.equals("ApplyArmor")
                    && m.desc.equals("(Lnet/minecraft/entity/EntityLivingBase;[Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;D)F");
                boolean mobs = name.endsWith("EntityLivingBase") && Transformer.named(m, "applyArmorCalculations", "func_70655_b")
                    && m.desc.equals("(Lnet/minecraft/util/DamageSource;F)F");
                if (!players && !mobs) {
                    continue;
                }
                // a result of -1 goes on with the old reckoning, anything else is returned
                LabelNode go = new LabelNode();
                InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                if (players) {
                    list.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    list.add(new VarInsnNode(Opcodes.DLOAD, 3));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Armor112", "player",
                        "(Lnet/minecraft/entity/EntityLivingBase;[Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;D)F", false));
                } else {
                    list.add(new VarInsnNode(Opcodes.FLOAD, 2));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Armor112", "mob",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)F", false));
                }
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new InsnNode(Opcodes.FCONST_0));
                list.add(new InsnNode(Opcodes.FCMPL));
                list.add(new JumpInsnNode(Opcodes.IFLT, go));
                list.add(new InsnNode(Opcodes.FRETURN));
                list.add(go);
                list.add(new InsnNode(Opcodes.POP));
                m.instructions.insert(list);
                count++;
            }
            System.out.println("[mc112bridge] 1.12 armor in " + name + ": " + count);
            bytes = Transformer.write(node);
        }
        if (System.getProperty("mc112.food17") == null && (name.equals("net.minecraft.entity.player.EntityPlayer")
            || name.equals("net.minecraft.util.DamageSource") || name.equals("net.minecraft.block.Block"))) {
            // the exhaustion of 1.9 - 1.12.2: swimming 0.01 a metre (0.015), walking free (0.01), jumps 0.05 and 0.2
            // (0.2 and 0.8), an attack 0.1 (0.3), damage taken 0.1 (0.3)
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                if (name.equals("net.minecraft.block.Block") && Transformer.named(m, "harvestBlock", "func_149636_a")) {
                    count += swapFloat(m, 0.025F, 0.005F, 1);
                } else if (name.endsWith("DamageSource") && m.name.equals("<init>")) {
                    count += swapFloat(m, 0.3F, 0.1F, -1);
                } else if (name.endsWith("EntityPlayer") && Transformer.named(m, "jump", "func_70664_aZ") && m.desc.equals("()V")) {
                    count += swapFloat(m, 0.2F, 0.05F, -1);
                    count += swapFloat(m, 0.8F, 0.2F, -1);
                } else if (name.endsWith("EntityPlayer") && Transformer.named(m, "attackTargetEntityWithCurrentItem", "func_71059_n")) {
                    for (AbstractInsnNode a : m.instructions.toArray()) {
                        AbstractInsnNode next = Transformer.previousCode(a) == null ? null : a.getNext();
                        if (a instanceof LdcInsnNode && Float.valueOf(0.3F).equals(((LdcInsnNode) a).cst) && next instanceof MethodInsnNode
                            && (((MethodInsnNode) next).name.equals("addExhaustion") || ((MethodInsnNode) next).name.equals("func_71020_j"))) {
                            ((LdcInsnNode) a).cst = Float.valueOf(0.1F);
                            count++;
                        }
                    }
                } else if (name.endsWith("EntityPlayer") && Transformer.named(m, "addMovementStat", "func_71000_j")) {
                    count += swapFloat(m, 0.015F, 0.01F, -1);
                    // walking: the factor right after the sprint's addExhaustion becomes 0
                    boolean afterSprint = false;
                    for (AbstractInsnNode a : m.instructions.toArray()) {
                        if (a instanceof LdcInsnNode && Float.valueOf(0.099999994F).equals(((LdcInsnNode) a).cst)) {
                            afterSprint = true;
                        } else if (afterSprint && a instanceof MethodInsnNode
                            && (((MethodInsnNode) a).name.equals("addExhaustion") || ((MethodInsnNode) a).name.equals("func_71020_j"))) {
                            AbstractInsnNode n = a.getNext();
                            while (n != null && !(n instanceof LdcInsnNode)) {
                                n = n.getNext();
                            }
                            if (n != null && Float.valueOf(0.01F).equals(((LdcInsnNode) n).cst)) {
                                ((LdcInsnNode) n).cst = Float.valueOf(0.0F);
                                count++;
                            }
                            afterSprint = false;
                        }
                    }
                }
            }
            System.out.println("[mc112bridge] 1.12 exhaustion in " + name + ": " + count);
            bytes = Transformer.write(node);
        }
        if (name.equals("net.minecraft.util.FoodStats") && System.getProperty("mc112.food17") == null) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                if (Transformer.named(m, "onUpdate", "func_75118_a") && m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)V")) {
                    InsnList load = new InsnList();
                    load.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    load.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    Transformer.guard(m, load, "ru/codex/offhand1710/Food112", "update",
                        "(Lnet/minecraft/util/FoodStats;Lnet/minecraft/entity/player/EntityPlayer;)Z", Opcodes.RETURN, 0);
                    count++;
                }
            }
            System.out.println("[mc112bridge] 1.12 food: " + count);
            bytes = Transformer.write(node);
        }
        if (name.equals("net.minecraft.entity.boss.EntityDragon") && System.getProperty("mc112.dragon17") == null) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            String dragon = "Lnet/minecraft/entity/boss/EntityDragon;";
            String hooks = "ru/codex/offhand1710/Dragon112";
            for (MethodNode m : node.methods) {
                InsnList self = new InsnList();
                self.add(new VarInsnNode(Opcodes.ALOAD, 0));
                if (Transformer.named(m, "onLivingUpdate", "func_70636_d") && m.desc.equals("()V")) {
                    Transformer.guard(m, self, hooks, "update", "(" + dragon + ")Z", Opcodes.RETURN, 0);
                    count++;
                } else if (Transformer.named(m, "onDeathUpdate", "func_70609_aI") && m.desc.equals("()V")) {
                    Transformer.guard(m, self, hooks, "deathUpdate", "(" + dragon + ")Z", Opcodes.RETURN, 0);
                    count++;
                } else if (Transformer.named(m, "entityInit", "func_70088_a") && m.desc.equals("()V")) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hooks, "init", "(" + dragon + ")V", false));
                    Transformer.beforeReturns(m, call);
                    count++;
                } else if (Transformer.named(m, "attackEntityFromPart", "func_70965_a")
                    && m.desc.equals("(Lnet/minecraft/entity/boss/EntityDragonPart;Lnet/minecraft/util/DamageSource;F)Z")) {
                    // int result: -1 goes on with 1.7.10, 0 or 1 is returned
                    LabelNode go = new LabelNode();
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    list.add(new VarInsnNode(Opcodes.FLOAD, 3));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hooks, "attackPart",
                        "(" + dragon + "Lnet/minecraft/entity/boss/EntityDragonPart;Lnet/minecraft/util/DamageSource;F)I", false));
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new JumpInsnNode(Opcodes.IFLT, go));
                    list.add(new InsnNode(Opcodes.IRETURN));
                    list.add(go);
                    list.add(new InsnNode(Opcodes.POP));
                    m.instructions.insert(list);
                    count++;
                }
            }
            System.out.println("[mc112bridge] 1.12 ender dragon: " + count + " methods");
            bytes = Transformer.write(node);
        }
        if (name.equals("net.minecraft.world.SpawnerAnimals") && System.getProperty("mc112.spectatorSpawn17") == null) {
            ClassNode node = Transformer.read(bytes);
            int count = 0;
            for (MethodNode m : node.methods) {
                if (Transformer.named(m, "findChunksForSpawning", "func_77192_a")) {
                    for (AbstractInsnNode a : m.instructions.toArray()) {
                        if (a.getOpcode() == Opcodes.IRETURN) {
                            InsnList call = new InsnList();
                            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Spectator112", "showAfterSpawning", "(Lnet/minecraft/world/WorldServer;)V", false));
                            m.instructions.insertBefore(a, call);
                            count++;
                        }
                    }
                    InsnList start = new InsnList();
                    start.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    start.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Spectator112", "hideForSpawning", "(Lnet/minecraft/world/WorldServer;)V", false));
                    m.instructions.insert(start);
                    count++;
                }
            }
            System.out.println("[mc112bridge] 1.12 spawning skips spectators: " + count);
            return Transformer.write(node);
        }
        if (System.getProperty("mc112.gamerules17") != null) {
            return bytes;
        }
        String n = name;
        if (!(n.equals("net.minecraft.world.WorldServer") || n.equals("net.minecraft.entity.player.EntityPlayerMP")
            || n.equals("net.minecraft.entity.EntityLivingBase") || n.equals("net.minecraft.entity.Entity")
            || n.equals("net.minecraft.command.ServerCommandManager") || n.equals("net.minecraft.network.NetHandlerPlayServer")
            || n.equals("net.minecraft.world.WorldProvider") || n.equals("net.minecraft.item.crafting.CraftingManager"))) {
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

    /** Every float constant "from" in the method becomes "to"; returns how many. */
    private static int swapFloat(MethodNode m, float from, float to, int limit) {
        int n = 0;
        for (AbstractInsnNode a : m.instructions.toArray()) {
            if (a instanceof LdcInsnNode && Float.valueOf(from).equals(((LdcInsnNode) a).cst) && (limit < 0 || n < limit)) {
                ((LdcInsnNode) a).cst = Float.valueOf(to);
                n++;
            }
        }
        return n;
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
            } else if (name.endsWith("ServerConfigurationManager") && Transformer.named(m, "transferEntityToWorld", "func_82448_a")
                && m.desc.equals("(Lnet/minecraft/entity/Entity;ILnet/minecraft/world/WorldServer;Lnet/minecraft/world/WorldServer;Lnet/minecraft/world/Teleporter;)V")) {
                // PlayerList.transferEntityToWorld of 1.12.2: x and z kept 16 blocks inside the border of the world entered
                int axis = 0;
                for (AbstractInsnNode a : m.instructions.toArray()) {
                    if (a instanceof MethodInsnNode && ((MethodInsnNode) a).owner.equals("net/minecraft/util/MathHelper")
                        && ((MethodInsnNode) a).desc.equals("(III)I") && axis < 2) {
                        m.instructions.insertBefore(a, new VarInsnNode(Opcodes.ALOAD, 4));
                        MethodInsnNode call = (MethodInsnNode) a;
                        call.owner = BORDER;
                        call.name = axis == 0 ? "clampX" : "clampZ";
                        call.desc = "(IIILnet/minecraft/world/WorldServer;)I";
                        axis++;
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
        } else if (cls.endsWith("CraftingManager") && Transformer.named(m, "findMatchingRecipe", "func_82787_a")
            && m.desc.equals("(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Lnet/minecraft/item/ItemStack;")) {
            // doLimitedCrafting: every result goes through Crafting112.limit
            for (AbstractInsnNode a : m.instructions.toArray()) {
                if (a.getOpcode() == Opcodes.ARETURN) {
                    InsnList call = new InsnList();
                    call.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    call.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/codex/offhand1710/Crafting112", "limit",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Lnet/minecraft/item/ItemStack;", false));
                    m.instructions.insertBefore(a, call);
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
