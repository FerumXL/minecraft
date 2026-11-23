"""Writes scenes.txt: the same commands for vanilla 1.12.2 and for 1.7.10 with the mod.

Directives (one per line):
  cmd <command>       a command as the player (no slash)
  wait <ticks>        client ticks
  shot <name>         a screenshot
  hud on|off          F1
  view 0|1|2          first / third person
  gui inventory|close
"""
import os

L = []


def cmd(c):
    L.append('cmd ' + c)


def wait(t):
    L.append('wait %d' % t)


def shot(n):
    L.append('shot ' + n)


def row(x0, y, z, blocks, step=1):
    x = x0
    for b in blocks:
        cmd('setblock %d %d %d %s' % (x, y, z, b))
        x += step


# ---- setup ----
for c in ['gamerule sendCommandFeedback false', 'gamerule doDaylightCycle false', 'gamerule doMobSpawning false',
          'gamerule doWeatherCycle false', 'gamerule doFireTick false', 'weather clear', 'time set 6000',
          'difficulty 0', 'gamemode 1', 'tp @p 100.5 4 90.5 0 0']:
    cmd(c)
wait(100)
L.append('gui inventory')
wait(20)
L.append('gui close')
wait(20)

# ---- full blocks ----
full1 = ['minecraft:stone 0', 'minecraft:stone 1', 'minecraft:stone 2', 'minecraft:stone 3', 'minecraft:stone 4',
         'minecraft:stone 5', 'minecraft:stone 6', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone',
         'minecraft:stonebrick 0', 'minecraft:stonebrick 1', 'minecraft:stonebrick 2', 'minecraft:stonebrick 3',
         'minecraft:brick_block', 'minecraft:sandstone 0', 'minecraft:sandstone 1', 'minecraft:sandstone 2',
         'minecraft:red_sandstone 0', 'minecraft:red_sandstone 1', 'minecraft:red_sandstone 2']
full2 = ['minecraft:planks %d' % i for i in range(6)] + ['minecraft:log 0', 'minecraft:log 1', 'minecraft:log 2',
         'minecraft:log 3', 'minecraft:log2 0', 'minecraft:log2 1', 'minecraft:prismarine 0', 'minecraft:prismarine 1',
         'minecraft:prismarine 2', 'minecraft:sea_lantern', 'minecraft:purpur_block', 'minecraft:purpur_pillar',
         'minecraft:end_bricks', 'minecraft:end_stone']
full3 = ['minecraft:concrete %d' % i for i in range(16)] + ['minecraft:magma', 'minecraft:bone_block',
         'minecraft:nether_wart_block', 'minecraft:red_nether_brick']
full4 = ['minecraft:concrete_powder %d' % i for i in range(16)] + ['minecraft:slime', 'minecraft:observer',
         'minecraft:sponge 1', 'minecraft:hay_block']
full5 = ['minecraft:%s_glazed_terracotta' % c for c in ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime',
         'pink', 'gray', 'silver', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']] + [
         'minecraft:stained_hardened_clay 1', 'minecraft:wool 1', 'minecraft:quartz_block 1', 'minecraft:quartz_block 2']
z = 100
for i, blocks in enumerate([full1, full2, full3, full4, full5]):
    row(100, 4 + i, z, blocks)
cmd('tp @p 110.0 6.6 90.0 0 8')
wait(80)
L.append('hud off')
shot('blocks_full')
L.append('hud on')

# ---- shapes (a separate area) ----
z = 130
cmd('fill 98 4 %d 125 12 %d minecraft:air' % (z - 3, z + 3))
woods = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak']
row(100, 4, z, ['minecraft:%s_stairs' % w for w in woods] + ['minecraft:stone_stairs', 'minecraft:stone_brick_stairs',
                'minecraft:purpur_stairs', 'minecraft:quartz_stairs', 'minecraft:red_sandstone_stairs'], 2)
fences = ['minecraft:fence', 'minecraft:spruce_fence', 'minecraft:birch_fence', 'minecraft:jungle_fence',
          'minecraft:acacia_fence', 'minecraft:dark_oak_fence', 'minecraft:nether_brick_fence',
          'minecraft:cobblestone_wall 0', 'minecraft:cobblestone_wall 1', 'minecraft:iron_bars', 'minecraft:glass_pane',
          'minecraft:stained_glass_pane 4']
row(100, 6, z, fences, 2)
row(100, 8, z, ['minecraft:stone_slab %d' % i for i in range(8)] + ['minecraft:wooden_slab %d' % i for i in range(6)]
    + ['minecraft:purpur_slab', 'minecraft:stone_slab2'], 1)
row(100, 10, z, ['minecraft:end_rod', 'minecraft:chorus_flower', 'minecraft:white_shulker_box', 'minecraft:red_shulker_box',
                 'minecraft:chest', 'minecraft:ender_chest', 'minecraft:enchanting_table', 'minecraft:anvil',
                 'minecraft:cauldron', 'minecraft:brewing_stand', 'minecraft:hopper', 'minecraft:beacon',
                 'minecraft:standing_banner', 'minecraft:flower_pot', 'minecraft:skull 1', 'minecraft:daylight_detector'], 1)
cmd('tp @p 112.0 7.6 120.0 0 10')
wait(80)
L.append('hud off')
shot('blocks_shapes')
L.append('hud on')

# ---- plants and liquids ----
z = 160
cmd('fill 98 3 %d 125 3 %d minecraft:grass' % (z - 3, z + 3))
row(100, 4, z, ['minecraft:red_flower %d' % i for i in range(9)] + ['minecraft:yellow_flower', 'minecraft:tallgrass 1',
                'minecraft:tallgrass 2', 'minecraft:deadbush', 'minecraft:sapling 0', 'minecraft:sapling 4',
                'minecraft:brown_mushroom', 'minecraft:red_mushroom'], 1)
row(100, 4, z + 2, ['minecraft:double_plant %d' % i for i in range(6)], 2)
cmd('fill 112 3 %d 116 3 %d minecraft:water' % (z + 2, z + 4))
cmd('fill 118 3 %d 122 3 %d minecraft:lava' % (z + 2, z + 4))
cmd('tp @p 110.0 6.0 %d.0 0 25' % (z - 7))
wait(100)
L.append('hud off')
shot('plants_liquids')
L.append('hud on')

# ---- sky and fog at noon and at dusk, the HUD on ----
cmd('tp @p 100.5 4 200.5 0 -5')
wait(100)
shot('sky_noon_hud')
cmd('time set 12500')
wait(60)
L.append('hud off')
shot('sky_dusk')
L.append('hud on')
cmd('time set 18000')
wait(60)
L.append('hud off')
shot('sky_night')
L.append('hud on')
cmd('time set 6000')

# ---- mobs in cells ----
z = 230
cmd('fill 98 3 %d 140 8 %d minecraft:air' % (z - 2, z + 4))
cmd('fill 98 3 %d 140 3 %d minecraft:grass' % (z - 2, z + 4))
mobs = ['zombie', 'skeleton', 'creeper', 'spider', 'enderman', 'pig', 'cow', 'sheep', 'chicken', 'wolf', 'villager',
        'husk', 'stray', 'polar_bear', 'llama', 'parrot', 'vindicator', 'evoker', 'iron_golem', 'blaze']
x = 100
for m in mobs:
    cmd('summon minecraft:%s %d.5 4 %d.5 {NoAI:1b,Rotation:[180f,0f]}' % (m, x, z + 2))
    x += 2
cmd('tp @p 119.0 6.0 %d.0 0 12' % (z - 12))
wait(100)
L.append('hud off')
shot('mobs')
L.append('hud on')
cmd('kill @e[type=!player]')

# ---- hand and inventory ----
cmd('tp @p 100.5 4 260.5 0 0')
wait(60)
cmd('replaceitem entity @p slot.weapon.mainhand minecraft:diamond_sword')
wait(40)
shot('hand_sword')
cmd('replaceitem entity @p slot.weapon.mainhand minecraft:grass')
wait(40)
shot('hand_block')
cmd('replaceitem entity @p slot.weapon.mainhand minecraft:bow')
L.append('view 2')
wait(40)
shot('third_person')
L.append('view 0')
cmd('gamemode 0')
cmd('clear')
cmd('replaceitem entity @p slot.hotbar.0 minecraft:diamond_sword')
cmd('replaceitem entity @p slot.hotbar.1 minecraft:bow')
cmd('replaceitem entity @p slot.hotbar.2 minecraft:cooked_beef 32')
cmd('replaceitem entity @p slot.armor.chest minecraft:diamond_chestplate')
wait(40)
shot('survival_hud')
L.append('gui inventory')
wait(30)
shot('inventory')
L.append('gui close')
cmd('gamemode 1')
wait(20)
L.append('gui inventory')
wait(30)
shot('creative')
L.append('gui close')
wait(20)

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'scenes.txt')
open(out, 'w', encoding='utf8').write('\n'.join(L) + '\n')
print(out, len(L))
