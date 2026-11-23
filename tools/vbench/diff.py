"""Compares the screenshots of vanilla 1.12.2 (shots112/screenshots) with the mod (shotsmod, shotsmodof).

For each scene: the mean absolute difference (0-255 over RGB) and the share of pixels whose largest channel differs by
more than 40, plus a picture: 1.12.2 | mod | the difference (brighter = more different, scaled x3).
"""
import json
import os
import sys

from PIL import Image, ImageChops, ImageDraw

V = os.path.dirname(os.path.abspath(__file__))
REF = os.path.join(V, 'shots112', 'screenshots')
OUT = os.path.join(V, 'report')
os.makedirs(OUT, exist_ok=True)


def metrics(a, b):
    a = a.convert('RGB')
    b = b.convert('RGB').resize(a.size)
    d = ImageChops.difference(a, b)
    px = list(d.getdata())
    total = len(px)
    mean = sum(sum(p) for p in px) / (3.0 * total)
    big = sum(1 for p in px if max(p) > 40) / float(total)
    return d, mean, big


def sheet(name, ref, mod, d, tag):
    w, h = ref.size
    img = Image.new('RGB', (w * 3, h + 18), (20, 20, 20))
    img.paste(ref.convert('RGB'), (0, 18))
    img.paste(mod.convert('RGB').resize((w, h)), (w, 18))
    img.paste(d.point(lambda v: min(255, v * 3)), (w * 2, 18))
    dr = ImageDraw.Draw(img)
    dr.text((4, 3), '1.12.2', fill=(255, 255, 255))
    dr.text((w + 4, 3), 'mod (%s)' % tag, fill=(255, 255, 255))
    dr.text((2 * w + 4, 3), 'difference x3', fill=(255, 255, 255))
    path = os.path.join(OUT, '%s-%s.png' % (name, tag))
    img.save(path)
    return path


rows = []
for f in sorted(os.listdir(REF)):
    if not f.endswith('.png'):
        continue
    name = f[2:-4]
    ref = Image.open(os.path.join(REF, f))
    row = {'scene': name}
    for tag in ('mod', 'modof'):
        p = os.path.join(V, 'shots' + tag, f)
        if not os.path.exists(p):
            continue
        mod = Image.open(p)
        d, mean, big = metrics(ref, mod)
        row[tag] = {'mean': round(mean, 2), 'big': round(big * 100, 1), 'sheet': os.path.basename(sheet(name, ref, mod, d, tag))}
    rows.append(row)
json.dump(rows, open(os.path.join(OUT, 'metrics.json'), 'w'), indent=1)
for r in rows:
    m = r.get('mod', {})
    o = r.get('modof', {})
    print('%-16s mod: mean %5.1f, >40: %5.1f%%   optifine: mean %5.1f, >40: %5.1f%%' % (
        r['scene'], m.get('mean', -1), m.get('big', -1), o.get('mean', -1), o.get('big', -1)))
