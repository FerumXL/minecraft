package ru.codex.offhand1710;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * EntityBoat of 1.12.2 for every boat (the coremod sends EntityBoat.onUpdate, updateRiderPosition,
 * setPositionAndRotation2 and entityInit here). The server runs the physics of 1.12.2 with the rider's input
 * (C0CPacketInput: forward/back go, left/right turn the boat), the client glides to the server's positions in 10
 * steps. Water, under water, flowing water, land (block slipperiness: ice 0.98) and air as in 1.12.2; boats no
 * longer break on a shore, pick up mobs, throw off the rider after 3 s under water, paddle with sounds.
 * The yaw stays in the 1.7.10 sense (0 heads +x), so saved boats and the renderer keep theirs: heading = yaw - 90.
 * Off with -Dmc112.boats17=1.
 */
public final class Boat112 {
    public static final boolean OFF = System.getProperty("mc112.boats17") != null;
    /** Left and right paddle in a byte (bits 0 and 1), for the renderer. */
    public static final int WATCH_PADDLES = 21;
    static final int IN_WATER = 0;
    static final int UNDER_WATER = 1;
    static final int UNDER_FLOWING_WATER = 2;
    static final int ON_LAND = 3;
    static final int IN_AIR = 4;
    private static final float PADDLE_STEP = 0.3926991F;

    public static final class State {
        int status = IN_AIR;
        int prev = IN_AIR;
        float deltaRotation;
        float momentum;
        int outOfControl;
        double waterLevel;
        float glide;
        public final float[] paddle = new float[2];
        public final float[] prevPaddle = new float[2];
        int lerpSteps;
        double lx;
        double ly;
        double lz;
        double lyaw;
        double lpitch;
        /** Autotest: server ticks spent in each status. */
        public final int[] ticks = new int[5];
        /** Autotest: the highest speed (blocks/tick) and turn (degrees/tick) seen. */
        public double maxSpeed;
        public double maxTurn;
        /** Autotest: input in place of the rider's. */
        public Boolean[] input;
    }

    private static final SideMap<EntityBoat, State> STATES = new SideMap<EntityBoat, State>();
    /** Autotest: the last paddle sound. */
    public static volatile String lastSound = "";

    private Boat112() {
    }

    public static State state(EntityBoat b) {
        State s = STATES.get(b);
        if (s == null) {
            s = new State();
            STATES.put(b, s);
        }
        return s;
    }

    /** EntityBoat.entityInit: the paddle states. */
    public static void init(EntityBoat b) {
        if (!OFF) {
            b.getDataWatcher().addObject(WATCH_PADDLES, Byte.valueOf((byte) 0));
        }
    }

    public static boolean paddling(EntityBoat b, int side) {
        try {
            return (b.getDataWatcher().getWatchableObjectByte(WATCH_PADDLES) & (1 << side)) != 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static double heading(EntityBoat b) {
        return (b.rotationYaw - 90.0F) * Math.PI / 180.0D;
    }

    // ---- status (EntityBoat.getBoatStatus and the methods it calls) -----------------------------------------

    /** The surface of the water at a liquid block: full when the same liquid is above. */
    private static double surface(World w, int x, int y, int z) {
        Block above = w.getBlock(x, y + 1, z);
        if (above.getMaterial() == Material.water) {
            return y + 1.0D;
        }
        return y + 1.0D - BlockLiquid.getLiquidHeightPercent(w.getBlockMetadata(x, y, z));
    }

    /** checkInWater: the water level over the bottom of the boat; true when the bottom is under it. */
    private static boolean inWater(EntityBoat b, State s) {
        AxisAlignedBB bb = b.boundingBox;
        int x0 = MathHelper.floor_double(bb.minX);
        int x1 = MathHelper.ceiling_double_int(bb.maxX);
        int y0 = MathHelper.floor_double(bb.minY);
        int y1 = MathHelper.ceiling_double_int(bb.minY + 0.001D);
        int z0 = MathHelper.floor_double(bb.minZ);
        int z1 = MathHelper.ceiling_double_int(bb.maxZ);
        boolean in = false;
        s.waterLevel = Double.MIN_VALUE;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (b.worldObj.getBlock(x, y, z).getMaterial() == Material.water) {
                        double top = surface(b.worldObj, x, y, z);
                        s.waterLevel = Math.max(top, s.waterLevel);
                        in |= bb.minY < top;
                    }
                }
            }
        }
        return in;
    }

    /** getUnderwaterStatus: water over the top of the boat, flowing or still. */
    private static int under(EntityBoat b) {
        AxisAlignedBB bb = b.boundingBox;
        double top = bb.maxY + 0.001D;
        int x0 = MathHelper.floor_double(bb.minX);
        int x1 = MathHelper.ceiling_double_int(bb.maxX);
        int y0 = MathHelper.floor_double(bb.maxY);
        int y1 = MathHelper.ceiling_double_int(top);
        int z0 = MathHelper.floor_double(bb.minZ);
        int z1 = MathHelper.ceiling_double_int(bb.maxZ);
        boolean still = false;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (b.worldObj.getBlock(x, y, z).getMaterial() == Material.water && top < surface(b.worldObj, x, y, z)) {
                        if (b.worldObj.getBlockMetadata(x, y, z) != 0) {
                            return UNDER_FLOWING_WATER;
                        }
                        still = true;
                    }
                }
            }
        }
        return still ? UNDER_WATER : -1;
    }

    /** getBoatGlide: the mean slipperiness of the blocks the boat stands on (0 in the air). */
    private static float glide(EntityBoat b) {
        AxisAlignedBB bb = b.boundingBox;
        AxisAlignedBB under = AxisAlignedBB.getBoundingBox(bb.minX, bb.minY - 0.001D, bb.minZ, bb.maxX, bb.minY, bb.maxZ);
        int x0 = MathHelper.floor_double(under.minX) - 1;
        int x1 = MathHelper.ceiling_double_int(under.maxX) + 1;
        int y0 = MathHelper.floor_double(under.minY) - 1;
        int y1 = MathHelper.ceiling_double_int(under.maxY) + 1;
        int z0 = MathHelper.floor_double(under.minZ) - 1;
        int z1 = MathHelper.ceiling_double_int(under.maxZ) + 1;
        float sum = 0.0F;
        int n = 0;
        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                int edges = (x == x0 || x == x1 - 1 ? 1 : 0) + (z == z0 || z == z1 - 1 ? 1 : 0);
                if (edges == 2) {
                    continue;
                }
                for (int y = y0; y < y1; y++) {
                    if (edges > 0 && (y == y0 || y == y1 - 1)) {
                        continue;
                    }
                    Block block = b.worldObj.getBlock(x, y, z);
                    AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(b.worldObj, x, y, z);
                    if (box != null && box.intersectsWith(under)) {
                        sum += block.slipperiness;
                        n++;
                    }
                }
            }
        }
        return n == 0 ? 0.0F : sum / n;
    }

    private static int status(EntityBoat b, State s) {
        int u = under(b);
        if (u >= 0) {
            s.waterLevel = b.boundingBox.maxY;
            return u;
        }
        if (inWater(b, s)) {
            return IN_WATER;
        }
        float g = glide(b);
        if (g > 0.0F) {
            s.glide = g;
            return ON_LAND;
        }
        return IN_AIR;
    }

    /** getWaterLevelAbove: the surface of the water the boat has dropped into. */
    private static double waterAbove(EntityBoat b) {
        AxisAlignedBB bb = b.boundingBox;
        int x0 = MathHelper.floor_double(bb.minX);
        int x1 = MathHelper.ceiling_double_int(bb.maxX);
        int y = MathHelper.floor_double(bb.maxY);
        int z0 = MathHelper.floor_double(bb.minZ);
        int z1 = MathHelper.ceiling_double_int(bb.maxZ);
        for (int dy = 0; dy < 16; dy++) {
            double best = 0.0D;
            boolean any = false;
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    if (b.worldObj.getBlock(x, y + dy, z).getMaterial() == Material.water) {
                        double top = surface(b.worldObj, x, y + dy, z);
                        best = Math.max(best, top);
                        any |= top >= y + dy + 1.0D;
                    }
                }
            }
            if (!any) {
                return best > 0.0D ? best : y + dy;
            }
        }
        return y + 16;
    }

    // ---- update ---------------------------------------------------------------------------------------------

    /** EntityBoat.updateMotion. */
    private static void motion(EntityBoat b, State s) {
        double gravity = -0.04D;
        double buoyancy = 0.0D;
        s.momentum = 0.05F;
        if (s.prev == IN_AIR && s.status != IN_AIR && s.status != ON_LAND) {
            s.waterLevel = b.boundingBox.minY + b.height;
            b.setPosition(b.posX, waterAbove(b) - b.height + 0.101D + b.yOffset, b.posZ);
            b.motionY = 0.0D;
            s.status = IN_WATER;
            return;
        }
        if (s.status == IN_WATER) {
            buoyancy = (s.waterLevel - b.boundingBox.minY) / b.height;
            s.momentum = 0.9F;
        } else if (s.status == UNDER_FLOWING_WATER) {
            gravity = -7.0E-4D;
            s.momentum = 0.9F;
        } else if (s.status == UNDER_WATER) {
            buoyancy = 0.01D;
            s.momentum = 0.45F;
        } else if (s.status == IN_AIR) {
            s.momentum = 0.9F;
        } else if (s.status == ON_LAND) {
            s.momentum = s.glide;
        }
        b.motionX *= s.momentum;
        b.motionZ *= s.momentum;
        s.deltaRotation *= s.momentum;
        b.motionY += gravity;
        if (buoyancy > 0.0D) {
            b.motionY += buoyancy * 0.06153846016296973D;
            b.motionY *= 0.75D;
        }
    }

    /** EntityBoat.controlBoat: returns the paddles (bit 0 left, bit 1 right). */
    private static int control(EntityBoat b, State s, boolean left, boolean right, boolean forward, boolean back) {
        float f = 0.0F;
        if (left) {
            s.deltaRotation -= 1.0F;
        }
        if (right) {
            s.deltaRotation += 1.0F;
        }
        if (right != left && !forward && !back) {
            f += 0.005F;
        }
        b.rotationYaw += s.deltaRotation;
        if (forward) {
            f += 0.04F;
        }
        if (back) {
            f -= 0.005F;
        }
        double h = heading(b);
        b.motionX += Math.sin(-h) * f;
        b.motionZ += Math.cos(h) * f;
        return (right && !left || forward ? 1 : 0) | (left && !right || forward ? 2 : 0);
    }

    /** Replaces EntityBoat.onUpdate; false leaves it to 1.7.10. */
    public static boolean update(EntityBoat b) {
        if (OFF) {
            return false;
        }
        State s = state(b);
        if (b.worldObj.isRemote) {
            client(b, s);
            return true;
        }
        s.prev = s.status;
        s.status = status(b, s);
        s.ticks[s.status]++;
        s.outOfControl = s.status == UNDER_WATER || s.status == UNDER_FLOWING_WATER ? s.outOfControl + 1 : 0;
        if (s.outOfControl >= 60 && b.riddenByEntity != null) {
            b.riddenByEntity.mountEntity(null);
        }
        if (b.getTimeSinceHit() > 0) {
            b.setTimeSinceHit(b.getTimeSinceHit() - 1);
        }
        if (b.getDamageTaken() > 0.0F) {
            b.setDamageTaken(b.getDamageTaken() - 1.0F);
        }
        b.prevPosX = b.posX;
        b.prevPosY = b.posY;
        b.prevPosZ = b.posZ;
        b.onEntityUpdate();
        if (b.isDead) {
            return true;
        }
        motion(b, s);
        int paddles = 0;
        Entity rider = b.riddenByEntity;
        if (s.input != null || rider instanceof EntityPlayer) {
            boolean forward;
            boolean back;
            boolean left;
            boolean right;
            if (s.input != null) {
                forward = s.input[0];
                back = s.input[1];
                left = s.input[2];
                right = s.input[3];
            } else {
                EntityLivingBase r = (EntityLivingBase) rider;
                forward = r.moveForward > 0.0F;
                back = r.moveForward < 0.0F;
                left = r.moveStrafing > 0.0F;
                right = r.moveStrafing < 0.0F;
            }
            paddles = control(b, s, left, right, forward, back);
        }
        b.moveEntity(b.motionX, b.motionY, b.motionZ);
        double dx = b.posX - b.prevPosX;
        double dz = b.posZ - b.prevPosZ;
        s.maxSpeed = Math.max(s.maxSpeed, Math.sqrt(dx * dx + dz * dz));
        s.maxTurn = Math.max(s.maxTurn, Math.abs(s.deltaRotation));
        b.getDataWatcher().updateObject(WATCH_PADDLES, Byte.valueOf((byte) paddles));
        for (int i = 0; i < 2; i++) {
            s.prevPaddle[i] = s.paddle[i];
            if ((paddles & (1 << i)) != 0) {
                float p = (float) (s.paddle[i] % (Math.PI * 2.0D));
                String sound = s.status == ON_LAND ? "entity.boat.paddle_land" : s.status == IN_AIR ? null : "entity.boat.paddle_water";
                if (sound != null && p <= Math.PI / 4.0D && (p + PADDLE_STEP) % (Math.PI * 2.0D) >= Math.PI / 4.0D) {
                    double h = heading(b);
                    double lookX = -Math.sin(h);
                    double lookZ = Math.cos(h);
                    double ox = i == 1 ? -lookZ : lookZ;
                    double oz = i == 1 ? lookX : -lookX;
                    lastSound = sound;
                    b.worldObj.playSoundEffect(b.posX + ox, b.posY, b.posZ + oz, Sounds112.name(sound), 1.0F, 0.8F + 0.4F * b.worldObj.rand.nextFloat());
                }
                s.paddle[i] += PADDLE_STEP;
            } else {
                s.paddle[i] = 0.0F;
            }
        }
        // EntityBoat.onUpdate: a mob that bumps into a boat nobody steers gets in; everything else is pushed
        List<?> near = b.worldObj.getEntitiesWithinAABBExcludingEntity(b, b.boundingBox.expand(0.2D, -0.01D, 0.2D));
        boolean pickUp = !(rider instanceof EntityPlayer);
        for (Object o : near) {
            Entity e = (Entity) o;
            if (e == rider) {
                continue;
            }
            if (pickUp && b.riddenByEntity == null && e.ridingEntity == null && e.width < b.width && e instanceof EntityLivingBase
                && !(e instanceof EntityWaterMob) && !(e instanceof EntityPlayer)) {
                e.mountEntity(b);
            } else if (e.canBePushed()) {
                b.applyEntityCollision(e);
            }
        }
        if (b.riddenByEntity != null && b.riddenByEntity.isDead) {
            b.riddenByEntity = null;
        }
        return true;
    }

    /** The client: glide to the position the server sent (lerpSteps 10), and turn the paddles. */
    private static void client(EntityBoat b, State s) {
        b.prevPosX = b.posX;
        b.prevPosY = b.posY;
        b.prevPosZ = b.posZ;
        b.prevRotationYaw = b.rotationYaw;
        b.onEntityUpdate();
        if (s.lerpSteps > 0) {
            double x = b.posX + (s.lx - b.posX) / s.lerpSteps;
            double y = b.posY + (s.ly - b.posY) / s.lerpSteps;
            double z = b.posZ + (s.lz - b.posZ) / s.lerpSteps;
            double dy = MathHelper.wrapAngleTo180_double(s.lyaw - b.rotationYaw);
            b.rotationYaw = (float) (b.rotationYaw + dy / s.lerpSteps);
            b.rotationPitch = (float) (b.rotationPitch + (s.lpitch - b.rotationPitch) / s.lerpSteps);
            s.lerpSteps--;
            b.setPosition(x, y, z);
        }
        for (int i = 0; i < 2; i++) {
            s.prevPaddle[i] = s.paddle[i];
            s.paddle[i] = paddling(b, i) ? s.paddle[i] + PADDLE_STEP : 0.0F;
        }
    }

    /** EntityBoat.setPositionAndRotation2 (client): the target of the glide. */
    public static boolean lerpTo(EntityBoat b, double x, double y, double z, float yaw, float pitch, int inc) {
        if (OFF) {
            return false;
        }
        State s = state(b);
        s.lx = x;
        s.ly = y;
        s.lz = z;
        s.lyaw = yaw;
        s.lpitch = pitch;
        s.lerpSteps = 10;
        return true;
    }

    /**
     * EntityBoat.updatePassenger: one rider in the middle; the boat's turn turns the rider, who may look at most 105
     * degrees away from the boat's heading (applyYawToEntity).
     */
    public static boolean rider(EntityBoat b) {
        if (OFF || b.riddenByEntity == null) {
            return !OFF;
        }
        Entity r = b.riddenByEntity;
        r.setPosition(b.posX, b.posY + b.getMountedYOffset() + r.getYOffset(), b.posZ);
        float turn = MathHelper.wrapAngleTo180_float(b.rotationYaw - b.prevRotationYaw);
        boolean steer = !b.worldObj.isRemote ? !(r instanceof EntityPlayer) : r instanceof EntityPlayer && isLocal(r);
        if (steer) {
            r.rotationYaw += turn;
            float face = b.rotationYaw - 90.0F;
            if (r instanceof EntityLivingBase) {
                ((EntityLivingBase) r).renderYawOffset = face;
            }
            float f = MathHelper.wrapAngleTo180_float(r.rotationYaw - face);
            float g = MathHelper.clamp_float(f, -105.0F, 105.0F);
            r.prevRotationYaw += g - f;
            r.rotationYaw += g - f;
            if (r instanceof net.minecraft.entity.EntityLivingBase) {
                // Entity.setRotationYawHead is client-only in 1.7.10
                ((net.minecraft.entity.EntityLivingBase) r).rotationYawHead = r.rotationYaw;
            }
        }
        return true;
    }

    private static boolean isLocal(Entity r) {
        return MC112Bridge.proxy.isLocalPlayer(r);
    }

    /** For the renderer: the paddle phase between ticks. */
    public static float paddlePhase(EntityBoat b, int side, float partial) {
        State s = state(b);
        return s.paddle[side] == 0.0F ? 0.0F : s.prevPaddle[side] + (s.paddle[side] - s.prevPaddle[side]) * partial;
    }
}
