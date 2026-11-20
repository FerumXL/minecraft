package ru.codex.offhand1710.client;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import ru.codex.offhand1710.OffhandInventory;

/** ElytraSound of 1.12.2: the wind loop of a gliding player, louder with speed, faded in after the first second. */
public final class ElytraSound extends MovingSound {
    private final EntityPlayer player;
    private int time;

    public ElytraSound(EntityPlayer player) {
        super(new ResourceLocation("mc112bridge", "item.elytra.flying"));
        this.player = player;
        this.repeat = true;
        this.field_147665_h = 0; // repeat delay
        this.volume = 0.1F;
    }

    @Override
    public void update() {
        time++;
        if (player.isDead || (time > 20 && !OffhandInventory.get(player).elytraFlying)) {
            donePlaying = true;
            return;
        }
        xPosF = (float) player.posX;
        yPosF = (float) player.posY;
        zPosF = (float) player.posZ;
        float speed = (float) (player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ);
        float half = speed / 2.0F;
        volume = (double) speed >= 0.01D ? Math.max(0.0F, Math.min(1.0F, half * half)) : 0.0F;
        if (time < 20) {
            volume = 0.0F;
        } else if (time < 40) {
            volume = (float) ((double) volume * ((double) (time - 20) / 20.0D));
        }
        field_147663_c = volume > 0.8F ? 1.0F + (volume - 0.8F) : 1.0F; // pitch
    }
}
