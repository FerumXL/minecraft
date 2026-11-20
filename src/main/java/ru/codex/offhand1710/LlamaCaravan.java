package ru.codex.offhand1710;

import java.util.List;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.Vec3;

/**
 * EntityAILlamaFollowCaravan of 1.12.2: a llama that is not on a lead joins the nearest caravan tail within 9 blocks
 * (or a llama on a lead without a follower), at least 2 blocks away, when the head of that chain is on a lead within
 * 8 links; it then walks to 2 blocks behind the llama in front at 2.1 x speed, faster (x1.2, up to 3) when it falls
 * more than 26 blocks behind, and leaves after 40 ticks of that.
 */
public final class LlamaCaravan extends EntityAIBase {
    private final MoreMobs.Llama llama;
    private double speed;
    private int distCheckCounter;

    public LlamaCaravan(MoreMobs.Llama llama, double speed) {
        this.llama = llama;
        this.speed = speed;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (llama.getLeashed() || llama.inCaravan()) {
            return false;
        }
        List<?> list = llama.worldObj.getEntitiesWithinAABB(MoreMobs.Llama.class, llama.boundingBox.expand(9.0D, 4.0D, 9.0D));
        MoreMobs.Llama best = null;
        double d = Double.MAX_VALUE;
        for (Object o : list) {
            MoreMobs.Llama l = (MoreMobs.Llama) o;
            if (l != llama && l.inCaravan() && !l.hasCaravanTail()) {
                double e = llama.getDistanceSqToEntity(l);
                if (e <= d) {
                    d = e;
                    best = l;
                }
            }
        }
        if (best == null) {
            for (Object o : list) {
                MoreMobs.Llama l = (MoreMobs.Llama) o;
                if (l != llama && l.getLeashed() && !l.hasCaravanTail()) {
                    double e = llama.getDistanceSqToEntity(l);
                    if (e <= d) {
                        d = e;
                        best = l;
                    }
                }
            }
        }
        if (best == null || d < 4.0D || !best.getLeashed() && !firstIsLeashed(best, 1)) {
            return false;
        }
        llama.caravanHead = best;
        best.caravanTail = llama;
        return true;
    }

    @Override
    public boolean continueExecuting() {
        if (llama.inCaravan() && llama.caravanHead.isEntityAlive() && firstIsLeashed(llama, 0)) {
            double d = llama.getDistanceSqToEntity(llama.caravanHead);
            if (d > 676.0D) {
                if (speed <= 3.0D) {
                    speed *= 1.2D;
                    distCheckCounter = 40;
                    return true;
                }
                if (distCheckCounter == 0) {
                    return false;
                }
            }
            if (distCheckCounter > 0) {
                distCheckCounter--;
            }
            return true;
        }
        return false;
    }

    @Override
    public void resetTask() {
        llama.leaveCaravan();
        speed = 2.1D;
    }

    @Override
    public void updateTask() {
        if (llama.inCaravan()) {
            MoreMobs.Llama head = llama.caravanHead;
            double d = (double) llama.getDistanceToEntity(head);
            Vec3 v = Vec3.createVectorHelper(head.posX - llama.posX, head.posY - llama.posY, head.posZ - llama.posZ).normalize();
            double s = Math.max(d - 2.0D, 0.0D);
            llama.getNavigator().tryMoveToXYZ(llama.posX + v.xCoord * s, llama.posY + v.yCoord * s, llama.posZ + v.zCoord * s, speed);
        }
    }

    private static boolean firstIsLeashed(MoreMobs.Llama l, int depth) {
        if (depth > 8 || !l.inCaravan()) {
            return false;
        }
        return l.caravanHead.getLeashed() || firstIsLeashed(l.caravanHead, depth + 1);
    }
}
