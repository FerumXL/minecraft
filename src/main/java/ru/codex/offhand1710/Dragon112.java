package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

/**
 * The ender dragon of 1.9 - 1.12.2 (EntityDragon, PhaseManager and the eleven phases of PhaseList): it circles the
 * pillars on a graph of 24 nodes, strafes players with fireballs, lands on the exit portal, breathes, scans, roars,
 * takes off, charges, and dies over the portal. Numbers and flow from the 1.12.2 bytecode; the code is written here.
 * Movement and body parts replace EntityDragon.onLivingUpdate (Transformer); the phase reaches the client in data
 * watcher slot 20. -Dmc112.dragon17 keeps the dragon of 1.7.10.
 */
public final class Dragon112 {
    public static final boolean OFF = System.getProperty("mc112.dragon17") != null;
    static final int WATCH_PHASE = 20;

    public static final int HOLDING = 0, STRAFE = 1, LANDING_APPROACH = 2, LANDING = 3, TAKEOFF = 4, SITTING_FLAMING = 5,
        SITTING_SCANNING = 6, SITTING_ATTACKING = 7, CHARGING = 8, DYING = 9, HOVER = 10;
    public static final String[] NAMES = {"HoldingPattern", "StrafePlayer", "LandingApproach", "Landing", "Takeoff", "SittingFlaming",
        "SittingScanning", "SittingAttacking", "ChargingPlayer", "Dying", "Hover"};

    /** Per dragon: the phase and what each phase keeps (the phase objects of 1.12.2 live as long as the dragon). */
    public static final class State {
        public int phase = -1;
        int[][] nodes;
        int[] links;
        // a path: points to fly through, the index of the next one
        int[][] path;
        int pathIndex;
        double[] target;
        boolean holdClockwise;
        boolean strafeClockwise;
        EntityLivingBase strafeTarget;
        int fireballCharge;
        int sitTicks;
        int flameCount;
        Brew112.Cloud breath;
        int chargeTimeout;
        boolean takeoffFirst;
        int dyingTicks;
        int sittingDamage;
        int growlTime = 100;
        int crystals = -1;
        List<EntityEnderCrystal> known = new ArrayList<EntityEnderCrystal>();
        int scanTick;
        public int phaseChanges;
        /** The phases in order (tests). */
        public final List<Integer> history = new ArrayList<Integer>();
        EntityDragonPart neck;
    }

    private static final Map<EntityDragon, State> STATES = new WeakHashMap<EntityDragon, State>();

    public static synchronized State state(EntityDragon d) {
        State s = STATES.get(d);
        if (s == null) {
            s = new State();
            STATES.put(d, s);
        }
        return s;
    }

    private Dragon112() {
    }

    // ---- EntityDragon hooks ----

    /** EntityDragon.entityInit: the phase slot of the data watcher (EntityDragon.PHASE of 1.12.2), Hover at first. */
    public static void init(EntityDragon d) {
        d.getDataWatcher().addObject(WATCH_PHASE, Integer.valueOf(HOVER));
    }

    public static int phase(EntityDragon d) {
        if (d.worldObj != null && d.worldObj.isRemote) {
            return d.getDataWatcher().getWatchableObjectInt(WATCH_PHASE);
        }
        State s = state(d);
        if (s.phase < 0) {
            // a dragon saved by this mod keeps its phase; one of the End without it circles (DragonFightManager)
            int saved = d.getEntityData().hasKey("DragonPhase") ? d.getEntityData().getInteger("DragonPhase") : (inEnd(d) ? HOLDING : HOVER);
            setPhase(d, s, saved < 0 || saved > HOVER ? HOVER : saved);
        }
        return s.phase;
    }

    static boolean inEnd(EntityDragon d) {
        return d.worldObj != null && d.worldObj.provider instanceof WorldProviderEnd;
    }

    /** PhaseManager.setPhase: the old phase ends, the new one starts. */
    public static void setPhase(EntityDragon d, State s, int phase) {
        if (s.phase == phase) {
            return;
        }
        if (s.phase == SITTING_FLAMING && s.breath != null) {
            s.breath.setDead();
            s.breath = null;
        }
        s.phase = phase;
        s.phaseChanges++;
        if (s.history.size() < 200) {
            s.history.add(phase);
        }
        if (!d.worldObj.isRemote) {
            d.getDataWatcher().updateObject(WATCH_PHASE, Integer.valueOf(phase));
            d.getEntityData().setInteger("DragonPhase", phase);
        }
        switch (phase) {
            case HOLDING:
            case LANDING_APPROACH:
                s.path = null;
                s.target = null;
                break;
            case STRAFE:
                s.fireballCharge = 0;
                s.target = null;
                s.path = null;
                s.strafeTarget = null;
                break;
            case LANDING:
            case HOVER:
            case CHARGING:
                s.target = null;
                s.chargeTimeout = 0;
                break;
            case TAKEOFF:
                s.takeoffFirst = true;
                s.path = null;
                s.target = null;
                break;
            case SITTING_FLAMING:
                s.sitTicks = 0;
                s.flameCount++;
                break;
            case SITTING_SCANNING:
            case SITTING_ATTACKING:
                s.sitTicks = 0;
                break;
            case DYING:
                s.target = null;
                s.dyingTicks = 0;
                break;
            default:
                break;
        }
    }

    /** IPhase.getIsStationary: the sitting phases and Hover. */
    public static boolean sitting(int phase) {
        return phase == SITTING_FLAMING || phase == SITTING_SCANNING || phase == SITTING_ATTACKING || phase == HOVER;
    }

    private static float maxRise(int phase) {
        return phase == CHARGING || phase == DYING ? 3.0F : phase == LANDING ? 1.5F : phase == HOVER ? 1.0F : 0.6F;
    }

    private static float yawFactor(EntityDragon d, int phase) {
        float speed = MathHelper.sqrt_double(d.motionX * d.motionX + d.motionZ * d.motionZ) + 1.0F;
        float capped = Math.min(speed, 40.0F);
        return phase == LANDING ? capped / speed : 0.7F / capped / speed;
    }

    // ---- reflection for the protected fields of EntityLivingBase / EntityLiving ----

    private static boolean broken;
    private static Field fIncrements, fNewX, fNewY, fNewZ, fNewYaw, fNewPitch, fRandomYaw;

    private static Field field(Class<?> c, String srg, String mcp) {
        return ReflectionHelper.findField(c, srg, mcp);
    }

    private static void fields() {
        if (fIncrements == null) {
            fIncrements = field(EntityLivingBase.class, "field_70716_bi", "newPosRotationIncrements");
            fNewX = field(EntityLivingBase.class, "field_70709_bj", "newPosX");
            fNewY = field(EntityLivingBase.class, "field_70710_bk", "newPosY");
            fNewZ = field(EntityLivingBase.class, "field_110152_bk", "newPosZ");
            fNewYaw = field(EntityLivingBase.class, "field_70712_bm", "newRotationYaw");
            fNewPitch = field(EntityLivingBase.class, "field_70705_bn", "newRotationPitch");
            fRandomYaw = field(EntityLivingBase.class, "field_70704_bt", "randomYawVelocity");
        }
    }

    private static float randomYaw(EntityDragon d) {
        try {
            return fRandomYaw.getFloat(d);
        } catch (IllegalAccessException e) {
            return 0.0F;
        }
    }

    private static void randomYaw(EntityDragon d, float v) {
        try {
            fRandomYaw.setFloat(d, v);
        } catch (IllegalAccessException e) {
            // stays
        }
    }

    // ---- EntityDragon.onLivingUpdate of 1.12.2 ----

    /** Replaces EntityDragon.onLivingUpdate; true when done (always, unless off). */
    public static boolean update(EntityDragon d) {
        if (OFF) {
            return false;
        }
        if (broken) {
            return false;
        }
        try {
            fields();
        } catch (RuntimeException e) {
            // a field not found: the dragon of 1.7.10 flies on rather than the server stopping
            broken = true;
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").error("1.12 dragon off: " + e);
            return false;
        }
        World w = d.worldObj;
        State s = state(d);
        neck(d, s);
        int phase = phase(d);
        if (w.isRemote) {
            {
                float now = MathHelper.cos(d.animTime * (float) Math.PI * 2.0F);
                float before = MathHelper.cos(d.prevAnimTime * (float) Math.PI * 2.0F);
                if (before <= -0.3F && now >= -0.3F) {
                    w.playSound(d.posX, d.posY, d.posZ, "mob.enderdragon.wings", 5.0F, 0.8F + d.getRNG().nextFloat() * 0.3F, false);
                }
                if (!sitting(phase) && --s.growlTime < 0) {
                    w.playSound(d.posX, d.posY, d.posZ, "mob.enderdragon.growl", 2.5F, 0.8F + d.getRNG().nextFloat() * 0.3F, false);
                    s.growlTime = 200 + d.getRNG().nextInt(200);
                }
            }
        }
        d.prevAnimTime = d.animTime;
        if (d.getHealth() <= 0.0F) {
            float fx = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            float fy = (d.getRNG().nextFloat() - 0.5F) * 4.0F;
            float fz = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            w.spawnParticle("largeexplode", d.posX + fx, d.posY + 2.0D + fy, d.posZ + fz, 0.0D, 0.0D, 0.0D);
            return true;
        }
        crystals(d, s);
        float flap = 0.2F / (MathHelper.sqrt_double(d.motionX * d.motionX + d.motionZ * d.motionZ) * 10.0F + 1.0F);
        flap *= (float) Math.pow(2.0D, d.motionY);
        if (sitting(phase)) {
            d.animTime += 0.1F;
        } else if (d.slowed) {
            d.animTime += flap * 0.5F;
        } else {
            d.animTime += flap;
        }
        d.rotationYaw = MathHelper.wrapAngleTo180_float(d.rotationYaw);
        if (d.ringBufferIndex < 0) {
            for (int i = 0; i < d.ringBuffer.length; i++) {
                d.ringBuffer[i][0] = d.rotationYaw;
                d.ringBuffer[i][1] = d.posY;
            }
        }
        if (++d.ringBufferIndex == d.ringBuffer.length) {
            d.ringBufferIndex = 0;
        }
        d.ringBuffer[d.ringBufferIndex][0] = d.rotationYaw;
        d.ringBuffer[d.ringBufferIndex][1] = d.posY;
        if (w.isRemote) {
            clientMove(d);
            clientTick(d, s, phase);
        } else {
            serverTick(d, s);
            if (s.phase != phase) {
                serverTick(d, s);
            }
            fly(d, s);
        }
        parts(d, s);
        return true;
    }

    /** The neck part of 1.9 (1.7.10 has head, body, three tail parts and two wings). */
    private static void neck(EntityDragon d, State s) {
        if (s.neck != null) {
            return;
        }
        s.neck = new EntityDragonPart(d, "neck", 6.0F, 6.0F);
        EntityDragonPart[] old = d.dragonPartArray;
        EntityDragonPart[] parts = new EntityDragonPart[old.length + 1];
        parts[0] = old[0];
        parts[1] = s.neck;
        System.arraycopy(old, 1, parts, 2, old.length - 1);
        d.dragonPartArray = parts;
    }

    private static void clientMove(EntityDragon d) {
        try {
            int n = fIncrements.getInt(d);
            if (n > 0) {
                double x = d.posX + (fNewX.getDouble(d) - d.posX) / n;
                double y = d.posY + (fNewY.getDouble(d) - d.posY) / n;
                double z = d.posZ + (fNewZ.getDouble(d) - d.posZ) / n;
                double yaw = MathHelper.wrapAngleTo180_double(fNewYaw.getDouble(d) - d.rotationYaw);
                d.rotationYaw = (float) (d.rotationYaw + yaw / n);
                d.rotationPitch = (float) (d.rotationPitch + (fNewPitch.getDouble(d) - d.rotationPitch) / n);
                fIncrements.setInt(d, n - 1);
                d.setPosition(x, y, z);
            }
        } catch (IllegalAccessException e) {
            // no glide this tick
        }
    }

    /** The movement of 1.12.2 towards the phase's target. */
    private static void fly(EntityDragon d, State s) {
        double[] t = s.phase == HOVER && s.target == null ? null : s.target;
        if (t == null) {
            return;
        }
        double dx = t[0] - d.posX;
        double dy = t[1] - d.posY;
        double dz = t[2] - d.posZ;
        double dist2 = dx * dx + dy * dy + dz * dz;
        float rise = maxRise(s.phase);
        dy = MathHelper.clamp_double(dy / MathHelper.sqrt_double(dx * dx + dz * dz), -rise, rise);
        d.motionY += dy * 0.10000000149011612D;
        d.rotationYaw = MathHelper.wrapAngleTo180_float(d.rotationYaw);
        double turn = MathHelper.clamp_double(MathHelper.wrapAngleTo180_double(180.0D - Math.atan2(dx, dz) * 57.2957763671875D - d.rotationYaw), -50.0D, 50.0D);
        Vec3 toTarget = Vec3.createVectorHelper(t[0] - d.posX, t[1] - d.posY, t[2] - d.posZ).normalize();
        Vec3 facing = Vec3.createVectorHelper(MathHelper.sin(d.rotationYaw * 0.017453292F), d.motionY, -MathHelper.cos(d.rotationYaw * 0.017453292F)).normalize();
        float align = Math.max(((float) facing.dotProduct(toTarget) + 0.5F) / 1.5F, 0.0F);
        float spin = randomYaw(d) * 0.8F;
        spin = (float) (spin + turn * yawFactor(d, s.phase));
        randomYaw(d, spin);
        d.rotationYaw += spin * 0.1F;
        float near = (float) (2.0D / (dist2 + 1.0D));
        d.moveFlying(0.0F, -1.0F, 0.06F * (align * near + (1.0F - near)));
        if (d.slowed) {
            d.moveEntity(d.motionX * 0.800000011920929D, d.motionY * 0.800000011920929D, d.motionZ * 0.800000011920929D);
        } else {
            d.moveEntity(d.motionX, d.motionY, d.motionZ);
        }
        Vec3 motion = Vec3.createVectorHelper(d.motionX, d.motionY, d.motionZ).normalize();
        float keep = ((float) motion.dotProduct(facing) + 1.0F) / 2.0F;
        keep = 0.8F + 0.15F * keep;
        d.motionX *= keep;
        d.motionZ *= keep;
        d.motionY *= 0.9100000262260437D;
    }

    /** The head, neck, body, wings and tail of 1.12.2, and what they hit. */
    private static void parts(EntityDragon d, State s) {
        int phase = phase(d);
        d.renderYawOffset = d.rotationYaw;
        d.dragonPartHead.width = d.dragonPartHead.height = 1.0F;
        s.neck.width = s.neck.height = 3.0F;
        d.dragonPartTail1.width = d.dragonPartTail1.height = 2.0F;
        d.dragonPartTail2.width = d.dragonPartTail2.height = 2.0F;
        d.dragonPartTail3.width = d.dragonPartTail3.height = 2.0F;
        d.dragonPartBody.height = 3.0F;
        d.dragonPartBody.width = 5.0F;
        d.dragonPartWing1.height = 2.0F;
        d.dragonPartWing1.width = 4.0F;
        d.dragonPartWing2.height = 3.0F;
        d.dragonPartWing2.width = 4.0F;
        double[][] before = new double[d.dragonPartArray.length][];
        for (int i = 0; i < d.dragonPartArray.length; i++) {
            before[i] = new double[] {d.dragonPartArray[i].posX, d.dragonPartArray[i].posY, d.dragonPartArray[i].posZ};
        }
        float pitch = (float) (d.getMovementOffsets(5, 1.0F)[1] - d.getMovementOffsets(10, 1.0F)[1]) * 10.0F * 0.017453292F;
        float cosPitch = MathHelper.cos(pitch);
        float sinPitch = MathHelper.sin(pitch);
        float yaw = d.rotationYaw * 0.017453292F;
        float sinYaw = MathHelper.sin(yaw);
        float cosYaw = MathHelper.cos(yaw);
        place(d.dragonPartBody, d.posX + sinYaw * 0.5F, d.posY, d.posZ - cosYaw * 0.5F);
        place(d.dragonPartWing1, d.posX + cosYaw * 4.5F, d.posY + 2.0D, d.posZ + sinYaw * 4.5F);
        place(d.dragonPartWing2, d.posX - cosYaw * 4.5F, d.posY + 2.0D, d.posZ - sinYaw * 4.5F);
        if (!d.worldObj.isRemote && d.hurtTime == 0) {
            collideWith(d, phase, d.worldObj.getEntitiesWithinAABBExcludingEntity(d, d.dragonPartWing1.boundingBox.expand(4.0D, 2.0D, 4.0D).offset(0.0D, -2.0D, 0.0D)));
            collideWith(d, phase, d.worldObj.getEntitiesWithinAABBExcludingEntity(d, d.dragonPartWing2.boundingBox.expand(4.0D, 2.0D, 4.0D).offset(0.0D, -2.0D, 0.0D)));
            attack(d, d.worldObj.getEntitiesWithinAABBExcludingEntity(d, d.dragonPartHead.boundingBox.expand(1.0D, 1.0D, 1.0D)));
            attack(d, d.worldObj.getEntitiesWithinAABBExcludingEntity(d, s.neck.boundingBox.expand(1.0D, 1.0D, 1.0D)));
        }
        double[] five = d.getMovementOffsets(5, 1.0F);
        float headSin = MathHelper.sin(d.rotationYaw * 0.017453292F - randomYaw(d) * 0.01F);
        float headCos = MathHelper.cos(d.rotationYaw * 0.017453292F - randomYaw(d) * 0.01F);
        float headY = sitting(phase) ? -1.0F : (float) (five[1] - d.getMovementOffsets(0, 1.0F)[1]);
        place(d.dragonPartHead, d.posX + headSin * 6.5F * cosPitch, d.posY + headY + sinPitch * 6.5F, d.posZ - headCos * 6.5F * cosPitch);
        place(s.neck, d.posX + headSin * 5.5F * cosPitch, d.posY + headY + sinPitch * 5.5F, d.posZ - headCos * 5.5F * cosPitch);
        EntityDragonPart[] tails = {d.dragonPartTail1, d.dragonPartTail2, d.dragonPartTail3};
        for (int i = 0; i < 3; i++) {
            double[] seg = d.getMovementOffsets(12 + i * 2, 1.0F);
            float a = d.rotationYaw * 0.017453292F + (float) MathHelper.wrapAngleTo180_double(seg[0] - five[0]) * 0.017453292F;
            float sa = MathHelper.sin(a);
            float ca = MathHelper.cos(a);
            float len = (float) (i + 1) * 2.0F;
            place(tails[i], d.posX - (sinYaw * 1.5F + sa * len) * cosPitch, d.posY + (seg[1] - five[1]) - (len + 1.5F) * sinPitch + 1.5D,
                d.posZ + (cosYaw * 1.5F + ca * len) * cosPitch);
        }
        if (!d.worldObj.isRemote) {
            d.slowed = destroy(d, d.dragonPartHead.boundingBox) | destroy(d, s.neck.boundingBox) | destroy(d, d.dragonPartBody.boundingBox);
        }
        for (int i = 0; i < d.dragonPartArray.length; i++) {
            d.dragonPartArray[i].prevPosX = before[i][0];
            d.dragonPartArray[i].prevPosY = before[i][1];
            d.dragonPartArray[i].prevPosZ = before[i][2];
        }
    }

    private static void place(EntityDragonPart p, double x, double y, double z) {
        p.onUpdate();
        p.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
    }

    /** collideWithEntities: wings push living things away, hurting them by 5 unless the dragon sits. */
    private static void collideWith(EntityDragon d, int phase, List<?> list) {
        double cx = (d.dragonPartBody.boundingBox.minX + d.dragonPartBody.boundingBox.maxX) / 2.0D;
        double cz = (d.dragonPartBody.boundingBox.minZ + d.dragonPartBody.boundingBox.maxZ) / 2.0D;
        for (Object o : list) {
            if (o instanceof EntityLivingBase) {
                Entity e = (Entity) o;
                double dx = e.posX - cx;
                double dz = e.posZ - cz;
                double dd = dx * dx + dz * dz;
                e.addVelocity(dx / dd * 4.0D, 0.20000000298023224D, dz / dd * 4.0D);
                if (!sitting(phase) && ((EntityLivingBase) e).func_142015_aE() < e.ticksExisted - 2) {
                    e.attackEntityFrom(DamageSource.causeMobDamage(d), 5.0F);
                }
            }
        }
    }

    /** attackEntitiesInList: the head and neck bite for 10. */
    private static void attack(EntityDragon d, List<?> list) {
        for (Object o : list) {
            if (o instanceof EntityLivingBase) {
                ((Entity) o).attackEntityFrom(DamageSource.causeMobDamage(d), 10.0F);
            }
        }
    }

    /** destroyBlocksInAABB of 1.12.2: true when something it may not break was in the way. */
    private static boolean destroy(EntityDragon d, AxisAlignedBB box) {
        World w = d.worldObj;
        int x0 = MathHelper.floor_double(box.minX), y0 = MathHelper.floor_double(box.minY), z0 = MathHelper.floor_double(box.minZ);
        int x1 = MathHelper.floor_double(box.maxX), y1 = MathHelper.floor_double(box.maxY), z1 = MathHelper.floor_double(box.maxZ);
        boolean blocked = false;
        boolean broke = false;
        boolean griefing = w.getGameRules().getGameRuleBooleanValue("mobGriefing");
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    Block b = w.getBlock(x, y, z);
                    Material m = b.getMaterial();
                    if (m == Material.air || m == Material.fire) {
                        continue;
                    }
                    if (!griefing || unbreakable(b)) {
                        blocked = true;
                    } else {
                        broke = w.setBlockToAir(x, y, z) || broke;
                    }
                }
            }
        }
        if (broke) {
            w.spawnParticle("largeexplode", box.minX + (box.maxX - box.minX) * d.getRNG().nextFloat(), box.minY + (box.maxY - box.minY) * d.getRNG().nextFloat(),
                box.minZ + (box.maxZ - box.minZ) * d.getRNG().nextFloat(), 0.0D, 0.0D, 0.0D);
        }
        return blocked;
    }

    private static boolean unbreakable(Block b) {
        return b == Content119.barrier || b == Blocks.obsidian || b == Blocks.end_stone || b == Blocks.bedrock || b == Blocks.end_portal
            || b == Blocks.end_portal_frame || b == Blocks.command_block || b == Content122.chain || b == Content122.repeating
            || b == Blocks.iron_bars || b == End112.gatewayBlock();
    }

    // ---- crystals (healing, and the count DragonFightManager keeps) ----

    private static void crystals(EntityDragon d, State s) {
        if (d.worldObj.isRemote) {
            return;
        }
        if (d.healingEnderCrystal != null) {
            if (d.healingEnderCrystal.isDead) {
                crystalDestroyed(d, s, d.healingEnderCrystal);
                d.healingEnderCrystal = null;
            } else if (d.ticksExisted % 10 == 0 && d.getHealth() < d.getMaxHealth()) {
                d.setHealth(d.getHealth() + 1.0F);
            }
        }
        for (int i = s.known.size() - 1; i >= 0; i--) {
            EntityEnderCrystal c = s.known.get(i);
            if (c.isDead) {
                s.known.remove(i);
                if (c != d.healingEnderCrystal) {
                    crystalDestroyed(d, s, c);
                }
                s.crystals = Math.max(0, s.crystals - 1);
            }
        }
        if (d.getRNG().nextInt(10) == 0) {
            EntityEnderCrystal best = null;
            double bestDist = Double.MAX_VALUE;
            for (Object o : d.worldObj.getEntitiesWithinAABB(EntityEnderCrystal.class, d.boundingBox.expand(32.0D, 32.0D, 32.0D))) {
                double dist = ((Entity) o).getDistanceSqToEntity(d);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = (EntityEnderCrystal) o;
                }
            }
            d.healingEnderCrystal = best;
        }
        if (inEnd(d) && (s.crystals < 0 || ++s.scanTick % 100 == 0)) {
            // DragonFightManager.findAliveCrystals: the crystals on the pillars (not those of a respawn on the portal)
            s.known.clear();
            for (Object o : d.worldObj.loadedEntityList) {
                if (o instanceof EntityEnderCrystal) {
                    Entity c = (Entity) o;
                    double r = Math.sqrt(c.posX * c.posX + c.posZ * c.posZ);
                    if (!c.isDead && r > 20.0D && r < 60.0D) {
                        s.known.add((EntityEnderCrystal) c);
                    }
                }
            }
            s.crystals = s.known.size();
        }
    }

    /** -1 outside the End (no DragonFightManager), else the crystals left on the pillars. */
    static int crystalsLeft(EntityDragon d, State s) {
        return inEnd(d) ? Math.max(0, s.crystals) : -1;
    }

    /** EntityDragon.onCrystalDestroyed and the phases' reaction: the healing crystal hurts the head by 10. */
    private static void crystalDestroyed(EntityDragon d, State s, EntityEnderCrystal c) {
        EntityPlayer by = nearestAttackable(d.worldObj, c.posX, c.posY, c.posZ, 64.0D, 64.0D);
        if (c == d.healingEnderCrystal) {
            DamageSource source = by == null ? DamageSource.setExplosionSource(null) : new EntityDamageSource("explosion.player", by).setExplosion();
            hurtPart(d, d.dragonPartHead, source, 10.0F);
        }
        if (s.phase == HOLDING && by != null && !by.capabilities.disableDamage) {
            strafe(d, s, by);
        }
    }

    // ---- damage ----

    /** EntityDragon.attackEntityFromPart of 1.12.2; returns 1 or 0 for the result, -1 to leave it to 1.7.10. */
    public static int attackPart(EntityDragon d, EntityDragonPart part, DamageSource source, float amount) {
        if (OFF) {
            return -1;
        }
        return hurtPart(d, part, source, amount) ? 1 : 0;
    }

    private static boolean hurtPart(EntityDragon d, EntityDragonPart part, DamageSource source, float amount) {
        State s = state(d);
        int phase = phase(d);
        if (sitting(phase) && phase != HOVER && source.getSourceOfDamage() instanceof EntityArrow) {
            // a sitting dragon is not hurt by arrows: they catch fire instead
            source.getSourceOfDamage().setFire(1);
            return false;
        }
        if (part != d.dragonPartHead) {
            amount = amount / 4.0F + Math.min(amount, 1.0F);
        }
        if (amount < 0.01F) {
            return false;
        }
        if (source.getEntity() instanceof EntityPlayer || source.isExplosion()) {
            float before = d.getHealth();
            attackDragon(d, source, amount);
            if (d.getHealth() <= 0.0F && !sitting(phase(d))) {
                d.setHealth(1.0F);
                setPhase(d, s, DYING);
            }
            if (sitting(phase(d))) {
                s.sittingDamage = (int) ((float) s.sittingDamage + (before - d.getHealth()));
                if ((float) s.sittingDamage > 0.25F * d.getMaxHealth()) {
                    s.sittingDamage = 0;
                    setPhase(d, s, TAKEOFF);
                }
            }
        }
        return true;
    }

    private static java.lang.reflect.Method attackSelf;

    /** func_82195_e (EntityLivingBase.attackEntityFrom for the dragon itself). */
    private static void attackDragon(EntityDragon d, DamageSource source, float amount) {
        try {
            if (attackSelf == null) {
                attackSelf = ReflectionHelper.findMethod(EntityDragon.class, d, new String[] {"func_82195_e"}, DamageSource.class, float.class);
            }
            attackSelf.invoke(d, source, amount);
        } catch (Exception e) {
            d.setHealth(d.getHealth() - amount);
        }
    }

    // ---- death (EntityDragon.onDeathUpdate of 1.12.2) ----

    /** Replaces onDeathUpdate: 200 ticks rising and turning, 500 experience (12000 the first time), then the portal. */
    public static boolean deathUpdate(EntityDragon d) {
        if (OFF) {
            return false;
        }
        d.deathTicks++;
        World w = d.worldObj;
        if (d.deathTicks >= 180 && d.deathTicks <= 200) {
            float fx = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            float fy = (d.getRNG().nextFloat() - 0.5F) * 4.0F;
            float fz = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            w.spawnParticle("hugeexplosion", d.posX + fx, d.posY + 2.0D + fy, d.posZ + fz, 0.0D, 0.0D, 0.0D);
        }
        boolean loot = w.getGameRules().getGameRuleBooleanValue("doMobLoot");
        boolean first = inEnd(d) && !End112.dragonKilledBefore(w);
        int xp = first ? 12000 : 500;
        if (!w.isRemote) {
            if (d.deathTicks > 150 && d.deathTicks % 5 == 0 && loot) {
                dropXp(d, MathHelper.floor_float((float) xp * 0.08F));
            }
            if (d.deathTicks == 1) {
                w.playBroadcastSound(1018, (int) d.posX, (int) d.posY, (int) d.posZ, 0);
            }
        }
        d.moveEntity(0.0D, 0.10000000149011612D, 0.0D);
        d.rotationYaw += 20.0F;
        d.renderYawOffset = d.rotationYaw;
        if (d.deathTicks == 200 && !w.isRemote) {
            if (loot) {
                dropXp(d, MathHelper.floor_float((float) xp * 0.2F));
            }
            if (inEnd(d)) {
                End112.finishFight(d, first);
            }
            d.setDead();
        }
        return true;
    }

    /** Experience dropped by dying dragons so far (tests: orbs may fall into the portal). */
    public static int xpDropped;

    private static void dropXp(EntityDragon d, int xp) {
        xpDropped += xp;
        while (xp > 0) {
            int orb = EntityXPOrb.getXPSplit(xp);
            xp -= orb;
            d.worldObj.spawnEntityInWorld(new EntityXPOrb(d.worldObj, d.posX, d.posY, d.posZ, orb));
        }
    }

    // ---- the path graph (EntityDragon.initPathPoints / findPath of 1.12.2) ----

    private static final int[] LINKS = {6146, 8197, 8202, 16404, 32808, 32848, 65696, 131392, 131712, 263424, 526848, 525313, 1581057,
        3166214, 2138120, 6373424, 4358208, 12910976, 9044480, 9706496, 15216640, 13688832, 11763712, 8257536};

    private static int[][] nodes(EntityDragon d, State s) {
        if (s.nodes == null) {
            int[][] n = new int[24][];
            for (int i = 0; i < 24; i++) {
                int extra = 5;
                int x;
                int z;
                if (i < 12) {
                    x = (int) (60.0F * MathHelper.cos(2.0F * (-(float) Math.PI + 0.2617994F * (float) i)));
                    z = (int) (60.0F * MathHelper.sin(2.0F * (-(float) Math.PI + 0.2617994F * (float) i)));
                } else if (i < 20) {
                    int j = i - 12;
                    x = (int) (40.0F * MathHelper.cos(2.0F * (-(float) Math.PI + 0.3926991F * (float) j)));
                    z = (int) (40.0F * MathHelper.sin(2.0F * (-(float) Math.PI + 0.3926991F * (float) j)));
                    extra += 10;
                } else {
                    int j = i - 20;
                    x = (int) (20.0F * MathHelper.cos(2.0F * (-(float) Math.PI + 0.7853982F * (float) j)));
                    z = (int) (20.0F * MathHelper.sin(2.0F * (-(float) Math.PI + 0.7853982F * (float) j)));
                }
                int y = Math.max(63 + 10, d.worldObj.getTopSolidOrLiquidBlock(x, z) + extra);
                n[i] = new int[] {x, y, z};
            }
            s.nodes = n;
        }
        return s.nodes;
    }

    /** getNearestPpIdx: the closest node (only the inner rings once no crystal is left). */
    static int nearestNode(EntityDragon d, State s, double x, double y, double z) {
        int[][] n = nodes(d, s);
        int from = crystalsLeft(d, s) == 0 || crystalsLeft(d, s) < 0 ? 12 : 0;
        float best = 10000.0F;
        int index = 0;
        int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
        for (int i = from; i < 24; i++) {
            float dx = n[i][0] - bx, dy = n[i][1] - by, dz = n[i][2] - bz;
            float dist = dx * dx + dy * dy + dz * dz;
            if (dist < best) {
                best = dist;
                index = i;
            }
        }
        return index;
    }

    private static float dist(int[] a, int[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        return MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
    }

    /** findPath: A* over the node graph from one node to another, with an optional last point after it. */
    static int[][] findPath(EntityDragon d, State s, int start, int end, int[] last) {
        int[][] n = nodes(d, s);
        int from = crystalsLeft(d, s) == 0 || crystalsLeft(d, s) < 0 ? 12 : 0;
        final float[] cost = new float[24];
        final float[] total = new float[24];
        int[] parent = new int[24];
        boolean[] closed = new boolean[24];
        java.util.Arrays.fill(cost, Float.MAX_VALUE);
        java.util.Arrays.fill(parent, -1);
        PriorityQueue<Integer> open = new PriorityQueue<Integer>(24, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(total[a], total[b]);
            }
        });
        cost[start] = 0.0F;
        total[start] = dist(n[start], n[end]);
        open.add(start);
        int closest = start;
        while (!open.isEmpty()) {
            int cur = open.poll();
            if (closed[cur]) {
                continue;
            }
            if (cur == end) {
                return build(n, parent, end, last);
            }
            if (dist(n[cur], n[end]) < dist(n[closest], n[end])) {
                closest = cur;
            }
            closed[cur] = true;
            for (int j = from; j < 24; j++) {
                if ((LINKS[cur] & 1 << j) > 0 && !closed[j]) {
                    float c = cost[cur] + dist(n[cur], n[j]);
                    if (c < cost[j]) {
                        cost[j] = c;
                        parent[j] = cur;
                        total[j] = c + dist(n[j], n[end]);
                        open.add(j);
                    }
                }
            }
        }
        if (closest == start) {
            return null;
        }
        return build(n, parent, closest, last);
    }

    private static int[][] build(int[][] n, int[] parent, int end, int[] last) {
        List<int[]> out = new ArrayList<int[]>();
        for (int i = end; i >= 0; i = parent[i]) {
            out.add(0, n[i]);
        }
        if (last != null) {
            out.add(last);
        }
        return out.toArray(new int[out.size()][]);
    }

    private static boolean pathDone(State s) {
        return s.path == null || s.pathIndex >= s.path.length;
    }

    /** The next point of the path, 0 - 20 blocks higher at random (the phases' findNewTarget / navigateToNextPathNode). */
    private static void nextTarget(EntityDragon d, State s) {
        if (!pathDone(s)) {
            int[] p = s.path[s.pathIndex++];
            double y;
            do {
                y = p[1] + d.getRNG().nextFloat() * 20.0F;
            } while (y < p[1]);
            s.target = new double[] {p[0], y, p[2]};
        }
    }

    private static int ring(EntityDragon d, State s, int node, boolean anyCrystal) {
        if (anyCrystal) {
            node %= 12;
            if (node < 0) {
                node += 12;
            }
        } else {
            node -= 12;
            node &= 7;
            node += 12;
        }
        return node;
    }

    private static double targetDist2(EntityDragon d, State s) {
        return s.target == null ? 0.0D : sq(s.target[0] - d.posX) + sq(s.target[1] - d.posY) + sq(s.target[2] - d.posZ);
    }

    private static double sq(double v) {
        return v * v;
    }

    private static int[] podium(World w) {
        return new int[] {0, w.getHeightValue(0, 0), 0};
    }

    // ---- the phases, server side (IPhase.serverTick) ----

    private static void serverTick(EntityDragon d, State s) {
        World w = d.worldObj;
        switch (s.phase) {
            case HOLDING: {
                double t = targetDist2(d, s);
                if (t < 100.0D || t > 22500.0D || d.isCollidedHorizontally || d.isCollidedVertically) {
                    holdingNewTarget(d, s);
                }
                break;
            }
            case STRAFE:
                strafeTick(d, s);
                break;
            case LANDING_APPROACH: {
                double t = targetDist2(d, s);
                if (t < 100.0D || t > 22500.0D || d.isCollidedHorizontally || d.isCollidedVertically) {
                    if (pathDone(s)) {
                        int start = nearestNode(d, s, d.posX, d.posY, d.posZ);
                        int[] top = podium(w);
                        EntityPlayer p = nearestAttackable(w, top[0], top[1], top[2], 128.0D, 128.0D);
                        int end;
                        if (p != null) {
                            Vec3 v = Vec3.createVectorHelper(p.posX, 0.0D, p.posZ).normalize();
                            end = nearestNode(d, s, -v.xCoord * 40.0D, 105.0D, -v.zCoord * 40.0D);
                        } else {
                            end = nearestNode(d, s, 40.0D, top[1], 0.0D);
                        }
                        s.path = findPath(d, s, start, end, top);
                        s.pathIndex = 0;
                        if (s.path != null) {
                            s.pathIndex = 1;
                        }
                    }
                    nextTarget(d, s);
                    if (s.path != null && pathDone(s)) {
                        setPhase(d, s, LANDING);
                    }
                }
                break;
            }
            case LANDING: {
                if (s.target == null) {
                    int[] top = podium(w);
                    s.target = new double[] {top[0], top[1], top[2]};
                }
                if (targetDist2(d, s) < 1.0D) {
                    s.flameCount = 0;
                    setPhase(d, s, SITTING_SCANNING);
                }
                break;
            }
            case TAKEOFF: {
                if (!s.takeoffFirst && s.path != null) {
                    int[] top = podium(w);
                    if (d.getDistanceSq(top[0], top[1], top[2]) > 100.0D) {
                        setPhase(d, s, HOLDING);
                    }
                } else {
                    s.takeoffFirst = false;
                    int start = nearestNode(d, s, d.posX, d.posY, d.posZ);
                    Vec3 look = d.getLook(1.0F);
                    int end = nearestNode(d, s, -look.xCoord * 40.0D, 105.0D, -look.zCoord * 40.0D);
                    end = ring(d, s, end, crystalsLeft(d, s) > 0);
                    s.path = findPath(d, s, start, end, null);
                    if (s.path != null) {
                        s.pathIndex = 1;
                        nextTarget(d, s);
                    }
                }
                break;
            }
            case SITTING_FLAMING: {
                s.sitTicks++;
                if (s.sitTicks >= 200) {
                    setPhase(d, s, s.flameCount >= 4 ? TAKEOFF : SITTING_SCANNING);
                } else if (s.sitTicks == 10) {
                    breathe(d, s);
                }
                break;
            }
            case SITTING_SCANNING: {
                s.sitTicks++;
                EntityPlayer p = nearestAttackable(w, d.posX, d.posY, d.posZ, 20.0D, 10.0D);
                if (p != null) {
                    if (s.sitTicks > 25) {
                        setPhase(d, s, SITTING_ATTACKING);
                    } else {
                        Vec3 to = Vec3.createVectorHelper(p.posX - d.posX, 0.0D, p.posZ - d.posZ).normalize();
                        Vec3 face = Vec3.createVectorHelper(MathHelper.sin(d.rotationYaw * 0.017453292F), 0.0D, -MathHelper.cos(d.rotationYaw * 0.017453292F)).normalize();
                        float angle = (float) (Math.acos(face.dotProduct(to)) * 57.2957763671875D) + 0.5F;
                        if (angle < 0.0F || angle > 10.0F) {
                            double dx = p.posX - d.dragonPartHead.posX;
                            double dz = p.posZ - d.dragonPartHead.posZ;
                            double turn = MathHelper.clamp_double(MathHelper.wrapAngleTo180_double(180.0D - Math.atan2(dx, dz) * 57.2957763671875D - d.rotationYaw), -100.0D, 100.0D);
                            float spin = randomYaw(d) * 0.8F;
                            float dist = MathHelper.sqrt_double(dx * dx + dz * dz) + 1.0F;
                            float raw = dist;
                            if (dist > 40.0F) {
                                dist = 40.0F;
                            }
                            spin = (float) (spin + turn * (0.7F / dist / raw));
                            randomYaw(d, spin);
                            d.rotationYaw += spin;
                        }
                    }
                } else if (s.sitTicks >= 100) {
                    p = nearestAttackable(w, d.posX, d.posY, d.posZ, 150.0D, 150.0D);
                    setPhase(d, s, TAKEOFF);
                    if (p != null) {
                        setPhase(d, s, CHARGING);
                        s.target = new double[] {p.posX, p.posY, p.posZ};
                    }
                }
                break;
            }
            case SITTING_ATTACKING:
                if (s.sitTicks++ >= 40) {
                    setPhase(d, s, SITTING_FLAMING);
                }
                break;
            case CHARGING: {
                if (s.target == null) {
                    setPhase(d, s, HOLDING);
                } else if (s.chargeTimeout > 0 && s.chargeTimeout++ >= 10) {
                    setPhase(d, s, HOLDING);
                } else {
                    double t = targetDist2(d, s);
                    if (t < 100.0D || t > 22500.0D || d.isCollidedHorizontally || d.isCollidedVertically) {
                        s.chargeTimeout++;
                    }
                }
                break;
            }
            case DYING: {
                s.dyingTicks++;
                if (s.target == null) {
                    int y = w.getTopSolidOrLiquidBlock(0, 0);
                    s.target = new double[] {0.0D, y, 0.0D};
                }
                double t = targetDist2(d, s);
                d.setHealth(t >= 100.0D && t <= 22500.0D && !d.isCollidedHorizontally && !d.isCollidedVertically ? 1.0F : 0.0F);
                break;
            }
            case HOVER:
                if (s.target == null) {
                    s.target = new double[] {d.posX, d.posY, d.posZ};
                }
                break;
            default:
                break;
        }
    }

    private static void holdingNewTarget(EntityDragon d, State s) {
        World w = d.worldObj;
        if (s.path != null && pathDone(s)) {
            int[] top = podium(w);
            int left = crystalsLeft(d, s) < 0 ? 0 : crystalsLeft(d, s);
            if (d.getRNG().nextInt(left + 3) == 0) {
                setPhase(d, s, LANDING_APPROACH);
                return;
            }
            double range = 64.0D;
            EntityPlayer p = nearestAttackable(w, top[0], top[1], top[2], range, range);
            if (p != null) {
                range = p.getDistanceSq(top[0], top[1], top[2]) / 512.0D;
            }
            if (p != null && (d.getRNG().nextInt(MathHelper.abs_int((int) range) + 2) == 0 || d.getRNG().nextInt(left + 2) == 0)) {
                strafe(d, s, p);
                return;
            }
        }
        if (pathDone(s)) {
            int start = nearestNode(d, s, d.posX, d.posY, d.posZ);
            int end = start;
            if (d.getRNG().nextInt(8) == 0) {
                s.holdClockwise = !s.holdClockwise;
                end = start + 6;
            }
            end += s.holdClockwise ? 1 : -1;
            end = ring(d, s, end, crystalsLeft(d, s) >= 0);
            s.path = findPath(d, s, start, end, null);
            if (s.path != null) {
                s.pathIndex = 1;
            }
        }
        nextTarget(d, s);
    }

    /** A line about the dragon for the test log. */
    public static String debug(EntityDragon d) {
        State s = state(d);
        return String.format("phase=%s target=%s path=%s/%s hp=%.0f death=%d motion=%.2f,%.2f,%.2f crystals=%d chunk=%s",
            s.phase < 0 ? "-" : NAMES[s.phase], s.target == null ? "none" : String.format("%.0f,%.0f,%.0f", s.target[0], s.target[1], s.target[2]),
            s.path == null ? "none" : String.valueOf(s.pathIndex), s.path == null ? "-" : String.valueOf(s.path.length), d.getHealth(), d.deathTicks,
            d.motionX, d.motionY, d.motionZ, s.crystals,
            d.worldObj.checkChunksExist((int) d.posX - 32, 0, (int) d.posZ - 32, (int) d.posX + 32, 0, (int) d.posZ + 32));
    }

    /** Breath clouds so far (tests). */
    public static int breaths;

    /** Fireballs shot so far (tests). */
    public static int fireballs;

    /** The holding pattern's choice to strafe a player (tests start it directly). */
    public static void strafeAt(EntityDragon d, EntityLivingBase p) {
        strafe(d, state(d), p);
    }

    /** PhaseStrafePlayer.setTarget: a path to above the player. */
    private static void strafe(EntityDragon d, State s, EntityLivingBase p) {
        setPhase(d, s, STRAFE);
        s.strafeTarget = p;
        int start = nearestNode(d, s, d.posX, d.posY, d.posZ);
        int end = nearestNode(d, s, p.posX, p.posY, p.posZ);
        int px = MathHelper.floor_double(p.posX);
        int pz = MathHelper.floor_double(p.posZ);
        double dx = px - d.posX;
        double dz = pz - d.posZ;
        double over = Math.min(0.4000000059604645D + Math.sqrt(dx * dx + dz * dz) / 80.0D - 1.0D, 10.0D);
        s.path = findPath(d, s, start, end, new int[] {px, MathHelper.floor_double(p.posY + over), pz});
        if (s.path != null) {
            s.pathIndex = 1;
            nextTarget(d, s);
        }
    }

    private static void strafeTick(EntityDragon d, State s) {
        EntityLivingBase p = s.strafeTarget;
        if (p == null) {
            setPhase(d, s, HOLDING);
            return;
        }
        if (s.path != null && pathDone(s)) {
            double dx = p.posX - d.posX;
            double dz = p.posZ - d.posZ;
            double over = Math.min(0.4000000059604645D + Math.sqrt(dx * dx + dz * dz) / 80.0D - 1.0D, 10.0D);
            s.target = new double[] {p.posX, p.posY + over, p.posZ};
        }
        double t = targetDist2(d, s);
        if (t < 100.0D || t > 22500.0D) {
            if (pathDone(s)) {
                int start = nearestNode(d, s, d.posX, d.posY, d.posZ);
                int end = start;
                if (d.getRNG().nextInt(8) == 0) {
                    s.strafeClockwise = !s.strafeClockwise;
                    end = start + 6;
                }
                end += s.strafeClockwise ? 1 : -1;
                end = ring(d, s, end, crystalsLeft(d, s) > 0);
                s.path = findPath(d, s, start, end, null);
                if (s.path != null) {
                    s.pathIndex = 1;
                }
            }
            nextTarget(d, s);
        }
        if (p.getDistanceSqToEntity(d) < 4096.0D) {
            if (d.canEntityBeSeen(p)) {
                s.fireballCharge++;
                Vec3 to = Vec3.createVectorHelper(p.posX - d.posX, 0.0D, p.posZ - d.posZ).normalize();
                Vec3 face = Vec3.createVectorHelper(MathHelper.sin(d.rotationYaw * 0.017453292F), 0.0D, -MathHelper.cos(d.rotationYaw * 0.017453292F)).normalize();
                float angle = (float) (Math.acos(face.dotProduct(to)) * 57.2957763671875D) + 0.5F;
                if (s.fireballCharge >= 5 && angle >= 0.0F && angle < 10.0F) {
                    Vec3 look = d.getLook(1.0F);
                    double x = d.dragonPartHead.posX - look.xCoord;
                    double y = d.dragonPartHead.posY + d.dragonPartHead.height / 2.0F + 0.5D;
                    double z = d.dragonPartHead.posZ - look.zCoord;
                    d.worldObj.playSoundEffect(d.posX, d.posY, d.posZ, "mob.ghast.fireball", 10.0F,
                        (d.getRNG().nextFloat() - d.getRNG().nextFloat()) * 0.2F + 1.0F);
                    Brew112.DragonFireball ball = new Brew112.DragonFireball(d.worldObj, d, p.posX - x, p.posY + p.height / 2.0F - (y + d.dragonPartHead.height / 2.0F), p.posZ - z);
                    ball.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
                    d.worldObj.spawnEntityInWorld(ball);
                    fireballs++;
                    s.fireballCharge = 0;
                    if (s.path != null) {
                        s.pathIndex = s.path.length;
                    }
                    setPhase(d, s, HOLDING);
                }
            } else if (s.fireballCharge > 0) {
                s.fireballCharge--;
            }
        } else if (s.fireballCharge > 0) {
            s.fireballCharge--;
        }
    }

    /** PhaseSittingFlaming at tick 10: a cloud of dragon's breath (radius 5, 200 ticks, instant damage) under the head. */
    private static void breathe(EntityDragon d, State s) {
        Vec3 dir = Vec3.createVectorHelper(d.dragonPartHead.posX - d.posX, 0.0D, d.dragonPartHead.posZ - d.posZ).normalize();
        double x = d.dragonPartHead.posX + dir.xCoord * 5.0D / 2.0D;
        double z = d.dragonPartHead.posZ + dir.zCoord * 5.0D / 2.0D;
        double y = d.dragonPartHead.posY + d.dragonPartHead.height / 2.0F;
        while (d.worldObj.isAirBlock(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z)) && y > 0.0D) {
            y--;
        }
        y = MathHelper.floor_double(y) + 1;
        Brew112.Cloud cloud = new Brew112.Cloud(d.worldObj, x, y, z);
        cloud.owner = d;
        cloud.radius(5.0F);
        cloud.duration = 200;
        cloud.setBreath();
        cloud.effects.add(new PotionEffect(Potion.harm.id, 1, 0));
        d.worldObj.spawnEntityInWorld(cloud);
        s.breath = cloud;
        breaths++;
    }

    // ---- the phases, client side (IPhase.doClientRenderEffects) ----

    private static void clientTick(EntityDragon d, State s, int phase) {
        World w = d.worldObj;
        if (phase != s.phase) {
            s.phase = phase;
            s.sitTicks = 0;
            s.dyingTicks = 0;
        }
        if (phase == LANDING || phase == SITTING_FLAMING && ++s.sitTicks % 2 == 0 && s.sitTicks < 10) {
            Vec3 look = d.getLook(1.0F).normalize();
            look.rotateAroundY(-0.7853982F);
            double hx = d.dragonPartHead.posX;
            double hy = d.dragonPartHead.posY + d.dragonPartHead.height / 2.0F;
            double hz = d.dragonPartHead.posZ;
            for (int i = 0; i < 8; i++) {
                double x = hx + d.getRNG().nextGaussian() / 2.0D;
                double y = hy + d.getRNG().nextGaussian() / 2.0D;
                double z = hz + d.getRNG().nextGaussian() / 2.0D;
                if (phase == LANDING) {
                    w.spawnParticle("portal", x, y, z, -look.xCoord * 0.08D + d.motionX, -look.yCoord * 0.3D + d.motionY, -look.zCoord * 0.08D + d.motionZ);
                } else {
                    for (int k = 0; k < 6; k++) {
                        w.spawnParticle("portal", x, y, z, -look.xCoord * 0.08D * k, -look.yCoord * 0.6D, -look.zCoord * 0.08D * k);
                    }
                }
                look.rotateAroundY(0.19634955F);
            }
        } else if (phase == SITTING_ATTACKING) {
            w.playSound(d.posX, d.posY, d.posZ, "mob.enderdragon.growl", 2.5F, 0.8F + d.getRNG().nextFloat() * 0.3F, false);
        } else if (phase == DYING && s.dyingTicks++ % 10 == 0) {
            float fx = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            float fy = (d.getRNG().nextFloat() - 0.5F) * 4.0F;
            float fz = (d.getRNG().nextFloat() - 0.5F) * 8.0F;
            w.spawnParticle("hugeexplosion", d.posX + fx, d.posY + 2.0D + fy, d.posZ + fz, 0.0D, 0.0D, 0.0D);
        }
    }

    // ---- World.getNearestAttackablePlayer of 1.12.2 ----

    /** Not creative or spectating, within maxXZ across and (as 1.12.2 squares it) maxY^2 up or down. */
    static EntityPlayer nearestAttackable(World w, double x, double y, double z, double maxXZ, double maxY) {
        EntityPlayer best = null;
        double bestDist = -1.0D;
        for (Object o : w.playerEntities) {
            EntityPlayer p = (EntityPlayer) o;
            if (p.capabilities.disableDamage || !p.isEntityAlive() || Spectator112.is(p)) {
                continue;
            }
            double d = p.getDistanceSq(x, p.posY, z);
            double range = maxXZ;
            if (p.isSneaking()) {
                range *= 0.800000011920929D;
            }
            if (p.isInvisible()) {
                float f = p.getArmorVisibility();
                if (f < 0.1F) {
                    f = 0.1F;
                }
                range *= 0.7F * f;
            }
            if ((maxY < 0.0D || Math.abs(p.posY - y) < maxY * maxY) && (maxXZ < 0.0D || d < range * range) && (bestDist == -1.0D || d < bestDist)) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }
}
