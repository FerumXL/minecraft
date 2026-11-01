package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.OffhandInventory;

public final class OffhandPlayerRenderer {
    @SubscribeEvent
    public void renderOffhand(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        ItemStack stack = OffhandInventory.get(player).getStack();
        if (stack == null || (player == Minecraft.getMinecraft().thePlayer
                && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)) {
            return;
        }

        GL11.glPushMatrix();
        try {
            event.renderer.modelBipedMain.bipedLeftArm.postRender(0.0625F);
            GL11.glTranslatef(0.0625F, 0.4375F, 0.0625F);
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            RenderManager.instance.itemRenderer.renderItem(player, stack, 0, ItemRenderType.EQUIPPED);
        } finally {
            GL11.glPopMatrix();
        }
    }
}

