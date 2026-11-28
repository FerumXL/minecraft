package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Auto-jump of 1.11 - 1.12.2 (EntityPlayerSP.updateAutoJump, option "Auto-Jump", on by default): walking into a step
 * between half a block and the jump height while looking roughly that way jumps on the next tick. Checked after every
 * client tick from the movement of that tick. -Dmc112.autojump17 turns it off.
 */
public final class AutoJump112 {
    public static final boolean OFF = System.getProperty("mc112.autojump17") != null;
    /** EntityPlayerSP.autoJumpTime: the jump is pressed by OffhandMovementInput on the next tick. */
    private static int autoJumpTime;
    private double lastX;
    private double lastZ;
    private boolean known;

    /** Called by the movement input after reading the keys. */
    public static boolean consume() {
        if (autoJumpTime > 0) {
            --autoJumpTime;
            return true;
        }
        return false;
    }

    public static boolean enabled() {
        return !OFF && Client122.autoJump;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (p == null) {
            known = false;
            return;
        }
        if (known) {
            update(p, (float) (p.posX - lastX), (float) (p.posZ - lastZ));
        }
        lastX = p.posX;
        lastZ = p.posZ;
        known = true;
    }

    /** EntityPlayerSP.updateAutoJump of 1.12.2. */
    static void update(EntityPlayerSP p, float dx, float dz) {
        if (!enabled() || autoJumpTime > 0 || !p.onGround || p.isSneaking() || p.isRiding() || p.capabilities.isFlying || p.movementInput == null) {
            return;
        }
        float inX = p.movementInput.moveStrafe;
        float inY = p.movementInput.moveForward;
        if (inX == 0.0F && inY == 0.0F) {
            return;
        }
        double minY = p.boundingBox.minY;
        Vec3 from = Vec3.createVectorHelper(p.posX, minY, p.posZ);
        Vec3 to = Vec3.createVectorHelper(p.posX + dx, minY, p.posZ + dz);
        double mx = dx;
        double mz = dz;
        float speed = p.getAIMoveSpeed();
        float len2 = (float) (mx * mx + mz * mz);
        if (len2 <= 0.001F) {
            float f2 = speed * inX;
            float f3 = speed * inY;
            float sin = MathHelper.sin(p.rotationYaw * 0.017453292F);
            float cos = MathHelper.cos(p.rotationYaw * 0.017453292F);
            mx = f2 * cos - f3 * sin;
            mz = f3 * cos + f2 * sin;
            len2 = (float) (mx * mx + mz * mz);
            if (len2 <= 0.001F) {
                return;
            }
        }
        float inv = 1.0F / MathHelper.sqrt_float(len2);
        double ux = mx * inv;
        double uz = mz * inv;
        // Entity.getForward: the look vector
        float fc = MathHelper.cos(-p.rotationYaw * 0.017453292F - (float) Math.PI);
        float fs = MathHelper.sin(-p.rotationYaw * 0.017453292F - (float) Math.PI);
        float pc = -MathHelper.cos(-p.rotationPitch * 0.017453292F);
        double lookX = fs * pc;
        double lookZ = fc * pc;
        if (lookX * ux + lookZ * uz < -0.15D) {
            return;
        }
        int hx = MathHelper.floor_double(p.posX);
        int hy = MathHelper.floor_double(p.boundingBox.maxY);
        int hz = MathHelper.floor_double(p.posZ);
        if (box(p, hx, hy, hz) != null || box(p, hx, hy + 1, hz) != null) {
            return;
        }
        hy += 1;
        float jump = 1.2F;
        if (p.isPotionActive(Potion.jump)) {
            jump += (float) (p.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.75F;
        }
        float reach = Math.max(speed * 7.0F, 1.0F / inv);
        Vec3 end = Vec3.createVectorHelper(to.xCoord + ux * reach, minY, to.zCoord + uz * reach);
        float w = p.width;
        float h = p.height;
        AxisAlignedBB sweep = AxisAlignedBB.getBoundingBox(Math.min(from.xCoord, end.xCoord), minY, Math.min(from.zCoord, end.zCoord),
            Math.max(from.xCoord, end.xCoord), minY + h, Math.max(from.zCoord, end.zCoord)).expand(w, 0.0D, w);
        double sy = minY + 0.5099999904632568D;
        // two rays along the sides of the body: (from, end) moved by half the width across the direction
        double cx = -uz * w * 0.5D;
        double cz = ux * w * 0.5D;
        double[][] rays = {
            {from.xCoord - cx, sy, from.zCoord - cz, end.xCoord - cx, sy, end.zCoord - cz},
            {from.xCoord + cx, sy, from.zCoord + cz, end.xCoord + cx, sy, end.zCoord + cz}};
        List<?> boxes = p.worldObj.getCollidingBoundingBoxes(p, sweep);
        float top = Float.MIN_VALUE;
        outer:
        for (Object o : boxes) {
            AxisAlignedBB b = (AxisAlignedBB) o;
            if (!hits(b, rays[0]) && !hits(b, rays[1])) {
                continue;
            }
            top = (float) b.maxY;
            int bx = MathHelper.floor_double((b.minX + b.maxX) * 0.5D);
            int by = MathHelper.floor_double((b.minY + b.maxY) * 0.5D);
            int bz = MathHelper.floor_double((b.minZ + b.maxZ) * 0.5D);
            int headY = hy;
            for (int i = 1; (float) i < jump; ++i) {
                AxisAlignedBB above = box(p, bx, by + i, bz);
                if (above != null) {
                    top = (float) above.maxY;
                    if ((double) top - minY > (double) jump) {
                        return;
                    }
                }
                if (i > 1) {
                    ++headY;
                    if (box(p, hx, headY, hz) != null) {
                        return;
                    }
                }
            }
            break outer;
        }
        if (top != Float.MIN_VALUE) {
            float step = (float) ((double) top - minY);
            if (step > 0.5F && step <= jump) {
                autoJumpTime = 1;
            }
        }
    }

    private static AxisAlignedBB box(EntityPlayerSP p, int x, int y, int z) {
        Block b = p.worldObj.getBlock(x, y, z);
        return b.getCollisionBoundingBoxFromPool(p.worldObj, x, y, z);
    }

    /** AxisAlignedBB.intersects(Vec3d, Vec3d) of 1.12.2: the box against the box spanned by the segment. */
    private static boolean hits(AxisAlignedBB b, double[] r) {
        return b.intersectsWith(AxisAlignedBB.getBoundingBox(Math.min(r[0], r[3]), Math.min(r[1], r[4]), Math.min(r[2], r[5]),
            Math.max(r[0], r[3]), Math.max(r[1], r[4]), Math.max(r[2], r[5])));
    }

    /** For the autotest: whether a jump is waiting. */
    public static int pending() {
        return autoJumpTime;
    }
}
