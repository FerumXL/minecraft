package ru.codex.offhand1710.client;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderPlayerEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.ItemShield;
import ru.codex.offhand1710.OffhandInventory;

/**
 * Draws the offhand item on the player's left arm.
 *
 * In 1.7.10 the orientation and size of a held item is not part of ItemRenderer.renderItem: vanilla
 * RenderPlayer.renderSpecials applies a different transform per item kind (3D block, bow, tool, other)
 * before calling it. This class draws the left arm as the mirror image of that right-hand code (M * T,
 * M mirroring the X axis), item included, like the left hand of 1.12.2. Mirroring the item back separately
 * is wrong: items are drawn in a 0..1 square, so the extra flip moves them off the hand and turns them.
 */
public final class OffhandPlayerRenderer {
    /**
     * Arm pose: a player holding something in the offhand keeps the left arm raised like the right arm
     * holding an item (ModelBiped.heldItemLeft, the same pose 1.12.2 uses). The models are shared by all
     * players, so the value is set for every player before drawing.
     */
    @SubscribeEvent
    public void setArmPose(RenderPlayerEvent.Pre event) {
        OffhandInventory offhand = OffhandInventory.get(event.entityPlayer);
        ItemStack stack = offhand.getStack();
        int held = stack != null ? 1 : 0;
        if (stack != null && offhand.isUsing() && stack.getItemUseAction() == EnumAction.block) {
            held = 3; // blocking pose, as heldItemRight = 3 for a blocking main hand
        }
        event.renderer.modelBipedMain.heldItemLeft = held;
        event.renderer.modelArmorChestplate.heldItemLeft = held;
        event.renderer.modelArmor.heldItemLeft = held;
        // Drawing a bow in the offhand: the aiming pose. RenderPlayer only ever sets aimedBow to true for the
        // main hand and resets it after drawing, so setting it here lasts for this player only.
        if (stack != null && offhand.isUsing() && stack.getItemUseAction() == EnumAction.bow) {
            event.renderer.modelBipedMain.aimedBow = true;
            event.renderer.modelArmorChestplate.aimedBow = true;
            event.renderer.modelArmor.aimedBow = true;
        }
    }

    /**
     * Puts the 1.12.2 pose additions (shield block turn, left arm swing) into this RenderPlayer's models, once,
     * and only where they are still plain ModelBiped (a mod that replaced them keeps its own). Lowest priority
     * and skipped for cancelled events, so the pose is only active when the player is really drawn.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beginPose(RenderPlayerEvent.Pre event) {
        RenderPlayer renderer = event.renderer;
        if (renderer.modelBipedMain.getClass() == ModelBiped.class) {
            ModelBiped112 model = new ModelBiped112(0.0F);
            try {
                if (ReflectionHelper.getPrivateValue(RendererLivingEntity.class, renderer, "field_77045_g",
                        "mainModel") == renderer.modelBipedMain) {
                    ReflectionHelper.setPrivateValue(RendererLivingEntity.class, renderer, model, "field_77045_g",
                            "mainModel");
                    renderer.modelBipedMain = model;
                }
            } catch (RuntimeException e) {
                FMLLog.warning("[mc112bridge] player model not replaced: %s", e);
            }
        }
        if (renderer.modelArmorChestplate.getClass() == ModelBiped.class) {
            renderer.modelArmorChestplate = new ModelBiped112(1.0F);
        }
        if (renderer.modelArmor.getClass() == ModelBiped.class) {
            renderer.modelArmor = new ModelBiped112(0.5F);
        }
        setArmPose(event); // again, in case the models were just replaced
        ModelBiped112.begin(event.entityPlayer, event.partialRenderTick);
    }

    @SubscribeEvent
    public void endPose(RenderPlayerEvent.Post event) {
        ModelBiped112.end();
    }

    @SubscribeEvent
    public void renderOffhand(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        ItemStack stack = OffhandInventory.get(player).getStack();
        // No first-person check: the world never draws the local player's body in first person, and the
        // inventory preview of the player must show the offhand item too.
        if (stack == null || stack.getItem() == null) {
            return;
        }

        GL11.glPushMatrix();
        try {
            event.renderer.modelBipedMain.bipedLeftArm.postRender(0.0625F);
            if (stack.getItem() instanceof ItemShield) {
                ShieldRenderer.renderThirdPerson(true, ShieldRenderer.isOffhandBlocking(player, stack));
                return;
            }
            // Exact mirror of the vanilla right hand, item included (as the 1.12.2 left hand). Mirroring turns
            // the faces around, so the front face winding is flipped while the item is drawn.
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
            applyHeldTransform(stack);

            Item item = stack.getItem();
            ItemInUseOverride inUse = ItemInUseOverride.apply(player, stack);
            try {
                if (item.requiresMultipleRenderPasses()) {
                    for (int pass = 0; pass < item.getRenderPasses(stack.getItemDamage()); pass++) {
                        renderPass(player, stack, pass);
                    }
                } else {
                    renderPass(player, stack, 0);
                }
            } finally {
                if (inUse != null) {
                    inUse.restore(player);
                }
            }
        } finally {
            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void renderPass(EntityPlayer player, ItemStack stack, int pass) {
        int color = stack.getItem().getColorFromItemStack(stack, pass);
        GL11.glColor4f((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F,
                (float) (color & 255) / 255.0F, 1.0F);
        RenderManager.instance.itemRenderer.renderItem(player, stack, pass, ItemRenderType.EQUIPPED);
    }

    /** Same branches and numbers as vanilla RenderPlayer.renderSpecials (1.7.10) for the right hand. */
    private static void applyHeldTransform(ItemStack stack) {
        Item item = stack.getItem();
        IItemRenderer custom = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED);
        boolean customBlock3d = custom != null
                && custom.shouldUseRenderHelper(ItemRenderType.EQUIPPED, stack, ItemRendererHelper.BLOCK_3D);
        boolean block3d = item instanceof ItemBlock
                && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(item).getRenderType());

        float scale;
        if (customBlock3d || block3d) {
            scale = 0.5F;
            GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
            scale *= 0.75F;
            GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(-scale, -scale, scale);
        } else if (item == Items.bow) {
            scale = 0.625F;
            GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
            GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(scale, -scale, scale);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        } else if (item.isFull3D()) {
            scale = 0.625F;
            if (item.shouldRotateAroundWhenRendering()) {
                GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                GL11.glTranslatef(0.0F, -0.125F, 0.0F);
            }
            GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
            GL11.glScalef(scale, -scale, scale);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        } else {
            scale = 0.375F;
            GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
            GL11.glScalef(scale, scale, scale);
            GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
        }
    }
}
