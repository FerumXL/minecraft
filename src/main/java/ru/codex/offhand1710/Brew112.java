package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Dragon's breath, lingering potions, effect clouds and tipped arrows (1.9). */
public final class Brew112 {
    public static Item dragonBreath;
    public static ItemLingering lingering;
    public static ItemTipped tipped;
    private static final Map<String, String> KEYS = new HashMap<String, String>();

    static {
        String[][] names = {{"potion.moveSpeed", "swiftness"}, {"potion.moveSlowdown", "slowness"}, {"potion.damageBoost", "strength"}, {"potion.weakness", "weakness"},
                {"potion.poison", "poison"}, {"potion.regeneration", "regeneration"}, {"potion.fireResistance", "fire_resistance"}, {"potion.nightVision", "night_vision"},
                {"potion.invisibility", "invisibility"}, {"potion.waterBreathing", "water_breathing"}, {"potion.heal", "healing"}, {"potion.harm", "harming"}, {"potion.jump", "leaping"}};
        for (String[] pair : names) {
            KEYS.put(pair[0], pair[1]);
        }
    }

    private Brew112() {
    }

    public static void register(Object mod) {
        dragonBreath = new Item() {
            @Override
            public boolean isPotionIngredient(ItemStack stack) {
                return true;
            }

            @Override
            public String getPotionEffect(ItemStack stack) {
                return "";
            }
        }.setUnlocalizedName(MC112Bridge.MOD_ID + ".dragon_breath").setTextureName(MC112Bridge.MOD_ID + ":dragon_breath").setCreativeTab(CreativeTabs.tabBrewing).setContainerItem(Items.glass_bottle);
        GameRegistry.registerItem(dragonBreath, "dragon_breath");
        lingering = new ItemLingering();
        GameRegistry.registerItem(lingering, "lingering_potion");
        tipped = new ItemTipped();
        GameRegistry.registerItem(tipped, "tipped_arrow");
        EntityRegistry.registerModEntity(Cloud.class, "AreaEffectCloud", 64, mod, 160, 2147483647, true);
        EntityRegistry.registerModEntity(ThrownLingering.class, "LingeringPotion", 65, mod, 64, 10, true);
        EntityRegistry.registerModEntity(TippedArrow.class, "TippedArrow", 66, mod, 64, 20, false);
        EntityRegistry.registerModEntity(DragonFireball.class, "DragonFireball", 67, mod, 64, 10, true);
        CraftingManager.getInstance().getRecipeList().add(new TippedRecipe());
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static String suffix(int damage) {
        List<?> effects = Items.potionitem.getEffects(damage);
        if (effects == null || effects.isEmpty()) {
            return "water";
        }
        String key = KEYS.get(((PotionEffect) effects.get(0)).getEffectName());
        return key == null ? "empty" : key;
    }

    private static void splashSubItems(Item self, List<ItemStack> list) {
        List<ItemStack> all = new ArrayList<ItemStack>();
        Items.potionitem.getSubItems(Items.potionitem, CreativeTabs.tabBrewing, all);
        for (ItemStack stack : all) {
            if (ItemPotion.isSplash(stack.getItemDamage())) {
                list.add(new ItemStack(self, 1, stack.getItemDamage()));
            }
        }
    }

    public static final class ItemLingering extends ItemPotion {
        private IIcon bottle;
        private IIcon overlay;

        ItemLingering() {
            setUnlocalizedName(MC112Bridge.MOD_ID + ".lingering_potion");
            setTextureName(MC112Bridge.MOD_ID + ":potion_bottle_lingering");
            setCreativeTab(CreativeTabs.tabBrewing);
        }

        /**
         * Not ItemPotion's: that one adds "_bottle_drinkable", "_bottle_splash" and "_overlay" to the texture name,
         * and there are no such files. Like models/item/bottle_lingering.json of 1.12.2: layer 0 potion_overlay
         * (tinted with the colour of the potion), layer 1 potion_bottle_lingering.
         */
        @Override
        public void registerIcons(IIconRegister register) {
            bottle = register.registerIcon(MC112Bridge.MOD_ID + ":potion_bottle_lingering");
            overlay = register.registerIcon(MC112Bridge.MOD_ID + ":potion_overlay");
            itemIcon = bottle;
        }

        @Override
        public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
            return pass == 0 ? overlay : bottle;
        }

        @Override
        public IIcon getIconFromDamage(int damage) {
            return bottle;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return StatCollector.translateToLocal("lingering_potion.effect." + suffix(stack.getItemDamage()));
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void getSubItems(Item item, CreativeTabs tab, List list) {
            splashSubItems(this, list);
        }

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            if (!world.isRemote) {
                world.spawnEntityInWorld(new ThrownLingering(world, player, stack));
            }
            return stack;
        }
    }

    public static final class ItemTipped extends Item {
        private IIcon base;
        private IIcon head;

        ItemTipped() {
            setHasSubtypes(true);
            setMaxDamage(0);
            setUnlocalizedName(MC112Bridge.MOD_ID + ".tipped_arrow");
            setTextureName(MC112Bridge.MOD_ID + ":tipped_arrow_base");
            setCreativeTab(CreativeTabs.tabCombat);
        }

        @Override
        public void registerIcons(IIconRegister register) {
            base = register.registerIcon(MC112Bridge.MOD_ID + ":tipped_arrow_base");
            head = register.registerIcon(MC112Bridge.MOD_ID + ":tipped_arrow_head");
            itemIcon = base;
        }

        @Override
        public boolean requiresMultipleRenderPasses() {
            return true;
        }

        @Override
        public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
            return pass == 0 ? head : base;
        }

        @Override
        public int getColorFromItemStack(ItemStack stack, int pass) {
            return pass == 0 ? Items.potionitem.getColorFromDamage(stack.getItemDamage()) : 16777215;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return StatCollector.translateToLocal("tipped_arrow.effect." + suffix(stack.getItemDamage()));
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void getSubItems(Item item, CreativeTabs tab, List list) {
            splashSubItems(this, list);
        }
    }

    /** RecipeTippedArrow: eight arrows around a lingering potion. */
    public static final class TippedRecipe implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting grid, World world) {
            if (grid.getSizeInventory() != 9) {
                return false;
            }
            for (int i = 0; i < 9; i++) {
                ItemStack s = grid.getStackInSlot(i);
                if (s == null || s.getItem() != (i == 4 ? lingering : Items.arrow)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            return new ItemStack(tipped, 8, grid.getStackInSlot(4).getItemDamage());
        }

        @Override
        public int getRecipeSize() {
            return 9;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    public static final class TippedArrow extends EntityArrow implements IEntityAdditionalSpawnData {
        public int potion;

        public TippedArrow(World world) {
            super(world);
        }

        TippedArrow(World world, EntityLivingBase shooter, float power, int potion) {
            super(world, shooter, power);
            this.potion = potion;
        }

        @Override
        public void writeSpawnData(ByteBuf b) {
            b.writeInt(potion);
        }

        @Override
        public void readSpawnData(ByteBuf b) {
            potion = b.readInt();
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Potion112", potion);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            potion = tag.getInteger("Potion112");
        }

        @Override
        public void onCollideWithPlayer(EntityPlayer player) {
            boolean grounded = ReflectionHelper.<Boolean, EntityArrow>getPrivateValue(EntityArrow.class, this, "inGround", "field_70254_i");
            if (worldObj.isRemote || !grounded || arrowShake > 0) {
                return;
            }
            boolean take = canBePickedUp == 1 || (canBePickedUp == 2 && player.capabilities.isCreativeMode);
            if (canBePickedUp == 1 && !player.inventory.addItemStackToInventory(new ItemStack(tipped, 1, potion))) {
                take = false;
            }
            if (take) {
                playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                player.onItemPickup(this, 1);
                setDead();
            }
        }
    }

    public static Entity newTipped(World world, EntityLivingBase shooter, float power, int potion) {
        return new TippedArrow(world, shooter, power, potion);
    }

    public static final class ThrownLingering extends EntityPotion implements IEntityAdditionalSpawnData {
        public ThrownLingering(World world) {
            super(world);
        }

        ThrownLingering(World world, EntityLivingBase thrower, ItemStack stack) {
            super(world, thrower, stack);
        }

        @Override
        public void writeSpawnData(ByteBuf b) {
            b.writeInt(getPotionDamage());
        }

        @Override
        public void readSpawnData(ByteBuf b) {
            setPotionDamage(b.readInt());
        }

        @Override
        protected void onImpact(MovingObjectPosition hit) {
            if (!worldObj.isRemote) {
                Cloud cloud = new Cloud(worldObj, posX, posY, posZ);
                cloud.owner = getThrower();
                cloud.setColor(Items.potionitem.getColorFromDamage(getPotionDamage()));
                cloud.radius(3.0F);
                cloud.radiusOnUse = -0.5F;
                cloud.waitTime = 10;
                cloud.radiusPerTick = -cloud.radius() / (float) cloud.duration;
                List<?> effects = Items.potionitem.getEffects(getPotionDamage());
                if (effects != null) {
                    for (Object o : effects) {
                        PotionEffect e = (PotionEffect) o;
                        cloud.effects.add(new PotionEffect(e.getPotionID(), e.getDuration() / 4, e.getAmplifier()));
                    }
                }
                worldObj.spawnEntityInWorld(cloud);
                worldObj.playAuxSFX(2002, (int) Math.round(posX), (int) Math.round(posY), (int) Math.round(posZ), getPotionDamage());
                setDead();
            }
        }
    }

    /** EntityAreaEffectCloud. */
    public static final class Cloud extends Entity {
        private static final int WATCH_RADIUS = 16;
        private static final int WATCH_COLOR = 17;
        private static final int WATCH_BREATH = 18;
        private static final int WATCH_WAITING = 19;
        final List<PotionEffect> effects = new ArrayList<PotionEffect>();
        private final Map<Entity, Integer> applied = new HashMap<Entity, Integer>();
        int duration = 600;
        int waitTime = 20;
        int reapplication = 20;
        float radiusOnUse;
        float radiusPerTick;
        EntityLivingBase owner;

        public Cloud(World world) {
            super(world);
            noClip = true;
            isImmuneToFire = true;
            radius(3.0F);
        }

        Cloud(World world, double x, double y, double z) {
            this(world);
            setPosition(x, y, z);
        }

        @Override
        protected void entityInit() {
            dataWatcher.addObject(WATCH_RADIUS, Float.valueOf(0.5F));
            dataWatcher.addObject(WATCH_COLOR, Integer.valueOf(0));
            dataWatcher.addObject(WATCH_BREATH, Byte.valueOf((byte) 0));
            dataWatcher.addObject(WATCH_WAITING, Byte.valueOf((byte) 0));
        }

        public float radius() {
            return dataWatcher.getWatchableObjectFloat(WATCH_RADIUS);
        }

        final void radius(float value) {
            double x = posX;
            double y = posY;
            double z = posZ;
            setSize(value * 2.0F, 0.5F);
            setPosition(x, y, z);
            if (!worldObj.isRemote) {
                dataWatcher.updateObject(WATCH_RADIUS, Float.valueOf(value));
            }
        }

        void setColor(int color) {
            dataWatcher.updateObject(WATCH_COLOR, Integer.valueOf(color));
        }

        void setBreath() {
            dataWatcher.updateObject(WATCH_BREATH, Byte.valueOf((byte) 1));
        }

        public boolean isBreath() {
            return dataWatcher.getWatchableObjectByte(WATCH_BREATH) != 0;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) {
                particles();
                return;
            }
            if (ticksExisted >= waitTime + duration) {
                setDead();
                return;
            }
            boolean waiting = ticksExisted < waitTime;
            dataWatcher.updateObject(WATCH_WAITING, Byte.valueOf((byte) (waiting ? 1 : 0)));
            if (waiting) {
                return;
            }
            float r = radius();
            if (radiusPerTick != 0.0F) {
                r += radiusPerTick;
                if (r < 0.5F) {
                    setDead();
                    return;
                }
                radius(r);
            }
            if (ticksExisted % 5 != 0) {
                return;
            }
            for (Iterator<Map.Entry<Entity, Integer>> it = applied.entrySet().iterator(); it.hasNext();) {
                if (ticksExisted >= it.next().getValue()) {
                    it.remove();
                }
            }
            if (effects.isEmpty()) {
                applied.clear();
                return;
            }
            for (Object o : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox)) {
                EntityLivingBase target = (EntityLivingBase) o;
                double dx = target.posX - posX;
                double dz = target.posZ - posZ;
                if (applied.containsKey(target) || dx * dx + dz * dz > (double) (r * r)) {
                    continue;
                }
                applied.put(target, Integer.valueOf(ticksExisted + reapplication));
                for (PotionEffect e : effects) {
                    Potion potion = Potion.potionTypes[e.getPotionID()];
                    if (potion.isInstant()) {
                        potion.affectEntity(owner, target, e.getAmplifier(), 0.5D);
                    } else {
                        target.addPotionEffect(new PotionEffect(e));
                    }
                }
                if (radiusOnUse != 0.0F) {
                    r += radiusOnUse;
                    if (r < 0.5F) {
                        setDead();
                        return;
                    }
                    radius(r);
                }
            }
        }

        private void particles() {
            boolean waiting = dataWatcher.getWatchableObjectByte(WATCH_WAITING) != 0;
            float r = radius();
            int color = dataWatcher.getWatchableObjectInt(WATCH_COLOR);
            double red = (double) (color >> 16 & 255) / 255.0D;
            double green = (double) (color >> 8 & 255) / 255.0D;
            double blue = (double) (color & 255) / 255.0D;
            int count = waiting ? 2 : (int) ((float) Math.PI * r * r);
            for (int i = 0; i < count; i++) {
                float angle = rand.nextFloat() * (float) Math.PI * 2.0F;
                float dist = MathHelper.sqrt_float(rand.nextFloat()) * (waiting ? 0.2F : r);
                double x = posX + (double) (MathHelper.cos(angle) * dist);
                double z = posZ + (double) (MathHelper.sin(angle) * dist);
                if (isBreath()) {
                    worldObj.spawnParticle("portal", x, posY, z, 0.0D, 0.01D, 0.0D);
                } else {
                    worldObj.spawnParticle("mobSpell", x, posY, z, red, green, blue);
                }
            }
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {
            tag.setFloat("Radius", radius());
            tag.setInteger("Duration", duration);
            tag.setInteger("Wait", waitTime);
            tag.setFloat("OnUse", radiusOnUse);
            tag.setFloat("PerTick", radiusPerTick);
            tag.setInteger("Age", ticksExisted);
            tag.setInteger("Color", dataWatcher.getWatchableObjectInt(WATCH_COLOR));
            tag.setBoolean("Breath", isBreath());
            tag.setIntArray("Effects", encode());
        }

        private int[] encode() {
            int[] out = new int[effects.size() * 3];
            for (int i = 0; i < effects.size(); i++) {
                out[i * 3] = effects.get(i).getPotionID();
                out[i * 3 + 1] = effects.get(i).getDuration();
                out[i * 3 + 2] = effects.get(i).getAmplifier();
            }
            return out;
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {
            radius(tag.getFloat("Radius"));
            duration = tag.getInteger("Duration");
            waitTime = tag.getInteger("Wait");
            radiusOnUse = tag.getFloat("OnUse");
            radiusPerTick = tag.getFloat("PerTick");
            ticksExisted = tag.getInteger("Age");
            setColor(tag.getInteger("Color"));
            if (tag.getBoolean("Breath")) {
                setBreath();
            }
            int[] raw = tag.getIntArray("Effects");
            for (int i = 0; i + 2 < raw.length; i += 3) {
                effects.add(new PotionEffect(raw[i], raw[i + 1], raw[i + 2]));
            }
        }
    }

    /** EntityDragonFireball. */
    public static final class DragonFireball extends EntityFireball {
        public DragonFireball(World world) {
            super(world);
            setSize(1.0F, 1.0F);
        }

        DragonFireball(World world, EntityLivingBase shooter, double x, double y, double z) {
            super(world, shooter, x, y, z);
            setSize(1.0F, 1.0F);
        }

        @Override
        protected void onImpact(MovingObjectPosition hit) {
            if (worldObj.isRemote || (hit.entityHit != null && hit.entityHit == shootingEntity)) {
                return;
            }
            Cloud cloud = new Cloud(worldObj, posX, posY, posZ);
            cloud.owner = shootingEntity;
            cloud.setBreath();
            cloud.radius(3.0F);
            cloud.duration = 600;
            cloud.radiusPerTick = (7.0F - cloud.radius()) / (float) cloud.duration;
            cloud.effects.add(new PotionEffect(Potion.harm.id, 1, 1));
            for (Object o : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(4.0D, 2.0D, 4.0D))) {
                EntityLivingBase target = (EntityLivingBase) o;
                if (getDistanceSqToEntity(target) < 16.0D) {
                    cloud.setPosition(target.posX, target.posY, target.posZ);
                    break;
                }
            }
            worldObj.playAuxSFX(2006, (int) posX, (int) posY, (int) posZ, 0);
            worldObj.spawnEntityInWorld(cloud);
            setDead();
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }

        @Override
        public boolean attackEntityFrom(net.minecraft.util.DamageSource source, float amount) {
            return false;
        }
    }

    /** Head of TileEntityBrewingStand.canBrew: dragon's breath with a splash potion can be brewed. */
    public static boolean canBrew(TileEntityBrewingStand stand) {
        ItemStack[] slots = ReflectionHelper.getPrivateValue(TileEntityBrewingStand.class, stand, "brewingItemStacks", "field_145945_j");
        if (slots[3] == null || slots[3].getItem() != dragonBreath) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (slots[i] != null && slots[i].getItem() == Items.potionitem && ItemPotion.isSplash(slots[i].getItemDamage())) {
                return true;
            }
        }
        return false;
    }

    /** Head of ItemGlassBottle.onItemRightClick: a bottle collects the breath of the dragon. Null means vanilla. */
    public static ItemStack bottle(ItemStack stack, World world, EntityPlayer player) {
        AxisAlignedBB box = player.boundingBox.expand(2.0D, 2.0D, 2.0D);
        for (Object o : world.getEntitiesWithinAABB(Cloud.class, box)) {
            Cloud cloud = (Cloud) o;
            if (cloud.isBreath()) {
                if (!world.isRemote) {
                    cloud.radius(cloud.radius() - 0.5F);
                }
                world.playSoundAtEntity(player, "random.bottle", 1.0F, 1.0F);
                ItemStack filled = new ItemStack(dragonBreath);
                if (player.capabilities.isCreativeMode) {
                    player.inventory.addItemStackToInventory(filled);
                    return stack;
                }
                stack.stackSize--;
                if (stack.stackSize <= 0) {
                    return filled;
                }
                if (!player.inventory.addItemStackToInventory(filled)) {
                    player.dropPlayerItemWithRandomChoice(filled, false);
                }
                return stack;
            }
        }
        return null;
    }

    public static final class Events {
        @SubscribeEvent
        public void onBrew(PotionBrewEvent.Pre event) {
            ItemStack ingredient = event.getItem(3);
            if (ingredient == null || ingredient.getItem() != dragonBreath) {
                return;
            }
            for (int i = 0; i < 3; i++) {
                ItemStack s = event.getItem(i);
                if (s != null && s.getItem() == Items.potionitem && ItemPotion.isSplash(s.getItemDamage())) {
                    event.setItem(i, new ItemStack(lingering, 1, s.getItemDamage()));
                }
            }
            event.setItem(3, ingredient.stackSize > 1 ? new ItemStack(dragonBreath, ingredient.stackSize - 1) : new ItemStack(Items.glass_bottle));
            event.setCanceled(true);
        }

        @SubscribeEvent
        public void onHurt(LivingHurtEvent event) {
            if (event.source.getSourceOfDamage() instanceof TippedArrow) {
                List<?> effects = Items.potionitem.getEffects(((TippedArrow) event.source.getSourceOfDamage()).potion);
                if (effects != null) {
                    for (Object o : effects) {
                        PotionEffect e = (PotionEffect) o;
                        Potion potion = Potion.potionTypes[e.getPotionID()];
                        if (potion.isInstant()) {
                            potion.affectEntity(null, event.entityLiving, e.getAmplifier(), 1.0D);
                        } else {
                            event.entityLiving.addPotionEffect(new PotionEffect(e.getPotionID(), e.getDuration() / 8, e.getAmplifier()));
                        }
                    }
                }
            }
        }

        /** loot_tables/entities/stray.json: a tipped arrow of slowness when killed by a player. */
        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (event.entityLiving instanceof EntityStray && event.recentlyHit && event.entityLiving.worldObj.rand.nextFloat() < 0.5F + (float) event.lootingLevel * 0.125F) {
                event.entityLiving.entityDropItem(new ItemStack(tipped, 1, 8202), 0.0F);
            }
        }

        /** Simplified from the strafing phase of 1.9: the 1.7.10 dragon has no phases, so it fires now and then. */
        @SubscribeEvent
        public void onDragon(LivingUpdateEvent event) {
            if (!(event.entityLiving instanceof EntityDragon) || event.entityLiving.worldObj.isRemote || event.entityLiving.worldObj.rand.nextInt(160) != 0) {
                return;
            }
            EntityPlayer target = event.entityLiving.worldObj.getClosestVulnerablePlayerToEntity(event.entityLiving, 80.0D);
            if (target != null) {
                EntityLivingBase dragon = event.entityLiving;
                event.entityLiving.worldObj.spawnEntityInWorld(new DragonFireball(dragon.worldObj, dragon, target.posX - dragon.posX, target.boundingBox.minY - dragon.posY, target.posZ - dragon.posZ));
            }
        }
    }
}
