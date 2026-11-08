package ru.codex.offhand1710.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import ru.codex.offhand1710.OffhandInventory;

/**
 * Wraps the local player's movement input: while the offhand item is in use (eating, drinking, drawing a
 * bow) the player moves at 20% speed, which is what EntityPlayerSP.onLivingUpdate does for an item in use in
 * the main hand. Slower forward movement also stops sprinting, as in vanilla.
 */
public final class OffhandMovementInput extends MovementInput {
    private final MovementInput base;
    private final EntityPlayer player;

    public OffhandMovementInput(MovementInput base, EntityPlayer player) {
        this.base = base;
        this.player = player;
    }

    @Override
    public void updatePlayerMoveState() {
        base.updatePlayerMoveState();
        moveStrafe = base.moveStrafe;
        moveForward = base.moveForward;
        jump = base.jump;
        sneak = base.sneak;
        if (OffhandInventory.get(player).elytraFlying) {
            moveStrafe = 0.0F; // the glide is steered by looking only (EntityLivingBase.travel of 1.12.2)
            moveForward = 0.0F;
        } else if (OffhandInventory.get(player).isUsing() && !player.isRiding()) {
            moveStrafe *= 0.2F;
            moveForward *= 0.2F;
        }
    }
}
