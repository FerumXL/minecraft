package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;

/**
 * The dragon respawn of 1.9 - 1.12.2 (DragonFightManager.respawnDragon and DragonSpawnManager): four end crystals on
 * the sides of the exit portal put it out and start the ritual. The crystals beam to the sky, the pillars are blown
 * up and built again one by one with beaming, unbreakable crystals, and after the last one a new dragon comes; the
 * crystals of the ritual blow up. Breaking one of them stops it all and lights the portal again. Stages and timings
 * from the 1.12.2 bytecode. -Dmc112.respawn17 turns it off.
 */
public final class Fight112 {
    public static final boolean OFF = System.getProperty("mc112.respawn17") != null;

    public static final int NONE = -1, START = 0, PREPARING = 1, PILLARS = 2, DRAGON = 3;
    private static final int[] SKY = {0, 128, 0};

    public static int stage = NONE;
    public static int ticks;
    private static final List<End112.Crystal> RITUAL = new ArrayList<End112.Crystal>();
    private static World end;
    /** The dragon the last ritual brought (tests). */
    public static EntityDragon summoned;

    public Fight112() {
    }

    private static boolean dragonAlive(World w) {
        for (Object o : w.loadedEntityList) {
            if (o instanceof EntityDragon && !((Entity) o).isDead) {
                return true;
            }
        }
        return false;
    }

    /**
     * ItemEndCrystal of 1.12.2 calls DragonFightManager.respawnDragon after each placed crystal: once the fight is over
     * and there is a crystal on each side of the portal, the ritual begins.
     */
    public static void tryRespawn(World w) {
        if (OFF || w.isRemote || !(w.provider instanceof WorldProviderEnd) || stage != NONE || !End112.Gateways.get(w).killed || dragonAlive(w)) {
            return;
        }
        int surface = End112.portalSurface(w);
        List<End112.Crystal> found = new ArrayList<End112.Crystal>();
        int[][] sides = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int[] s : sides) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(s[0], surface + 1, s[1], s[0] + 1, surface + 2, s[1] + 1);
            List<?> here = w.getEntitiesWithinAABB(End112.Crystal.class, box);
            if (here.isEmpty()) {
                return;
            }
            for (Object o : here) {
                if (!found.contains(o)) {
                    found.add((End112.Crystal) o);
                }
            }
        }
        End112.setPortal(w, false);
        RITUAL.clear();
        RITUAL.addAll(found);
        end = w;
        stage = START;
        ticks = 0;
    }

    /** End112.Crystal.attackEntityFrom: a crystal of the ritual broken stops it (DragonFightManager.onCrystalDestroyed). */
    static void crystalDestroyed(End112.Crystal c) {
        if (stage != NONE && RITUAL.contains(c)) {
            stage = NONE;
            ticks = 0;
            resetPillars(c.worldObj);
            End112.setPortal(c.worldObj, true);
            RITUAL.clear();
        }
    }

    private static void beamAll(int[] target) {
        for (End112.Crystal c : RITUAL) {
            c.setBeam(target);
        }
    }

    private static void growl(World w) {
        w.playSoundEffect(SKY[0], SKY[1], SKY[2], "mob.enderdragon.growl", 64.0F, 0.8F + w.rand.nextFloat() * 0.3F);
    }

    private static List<End112.Crystal> pillarCrystals(World w) {
        List<End112.Crystal> out = new ArrayList<End112.Crystal>();
        for (Object o : w.loadedEntityList) {
            if (o instanceof End112.Crystal && !RITUAL.contains(o)) {
                Entity c = (Entity) o;
                double r = Math.sqrt(c.posX * c.posX + c.posZ * c.posZ);
                if (r > 20.0D && r < 60.0D) {
                    out.add((End112.Crystal) c);
                }
            }
        }
        return out;
    }

    /** DragonFightManager.resetSpikeCrystals: the pillar crystals become breakable and stop beaming. */
    private static void resetPillars(World w) {
        for (End112.Crystal c : pillarCrystals(w)) {
            c.invulnerable = false;
            c.setBeam(null);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world != end || stage == NONE || event.world.isRemote) {
            return;
        }
        World w = event.world;
        for (End112.Crystal c : RITUAL) {
            if (c.isDead) {
                crystalDestroyed(c);
                return;
            }
        }
        int t = ticks++;
        switch (stage) {
            case START:
                beamAll(SKY);
                next(PREPARING);
                break;
            case PREPARING:
                if (t < 100) {
                    if (t == 0 || t == 50 || t == 51 || t == 52 || t >= 95) {
                        growl(w);
                    }
                } else {
                    next(PILLARS);
                }
                break;
            case PILLARS: {
                boolean aim = t % 40 == 0;
                boolean rise = t % 40 == 39;
                if (aim || rise) {
                    int[][] spikes = End112.spikes(w.getSeed());
                    int i = t / 40;
                    if (i < spikes.length) {
                        int[] s = spikes[i];
                        if (aim) {
                            beamAll(new int[] {s[0], s[3] + 1, s[1]});
                        } else {
                            for (int x = s[0] - 10; x <= s[0] + 10; x++) {
                                for (int y = s[3] - 10; y <= s[3] + 10; y++) {
                                    for (int z = s[1] - 10; z <= s[1] + 10; z++) {
                                        w.setBlockToAir(x, y, z);
                                    }
                                }
                            }
                            w.createExplosion(null, s[0] + 0.5D, s[3], s[1] + 0.5D, 5.0F, true);
                            End112.rebuildSpike(w, s);
                            for (Object o : w.getEntitiesWithinAABB(End112.Crystal.class,
                                AxisAlignedBB.getBoundingBox(s[0] - 1, s[3] - 4, s[1] - 1, s[0] + 2, s[3] + 3, s[1] + 2))) {
                                End112.Crystal c = (End112.Crystal) o;
                                c.invulnerable = true;
                                c.setBeam(SKY);
                            }
                        }
                    } else if (aim) {
                        next(DRAGON);
                    }
                }
                break;
            }
            case DRAGON:
                if (t >= 100) {
                    finish(w);
                } else if (t >= 80) {
                    growl(w);
                } else if (t == 0) {
                    beamAll(SKY);
                } else if (t < 5) {
                    growl(w);
                }
                break;
            default:
                break;
        }
    }

    private static void next(int s) {
        stage = s;
        ticks = 0;
    }

    /** DragonSpawnManager END and DragonFightManager.createNewDragon: a new dragon circling at (0, 128, 0). */
    private static void finish(World w) {
        stage = NONE;
        ticks = 0;
        EntityDragon dragon = new EntityDragon(w);
        dragon.setLocationAndAngles(0.0D, 128.0D, 0.0D, w.rand.nextFloat() * 360.0F, 0.0F);
        Dragon112.setPhase(dragon, Dragon112.state(dragon), Dragon112.HOLDING);
        w.spawnEntityInWorld(dragon);
        summoned = dragon;
        resetPillars(w);
        for (End112.Crystal c : RITUAL) {
            c.setBeam(null);
            w.createExplosion(c, c.posX, c.posY, c.posZ, 6.0F, false);
            c.setDead();
        }
        RITUAL.clear();
    }
}
