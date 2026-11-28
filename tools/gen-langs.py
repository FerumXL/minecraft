#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Make the language files of the mod for every language of Minecraft 1.12.2.

The mod keeps its strings in assets/mc112bridge/lang/en_US.lang and ru_RU.lang
(kept in git). For the other languages this script takes the translations from
your own Minecraft 1.12.2 assets: for each key of en_US.lang it looks for the
same key in the 1.12.2 English file, or else for a 1.12.2 key with the same
English text, and writes that key's translation. Keys it cannot match stay out,
so the game shows the English text for them (1.7.10 falls back to en_US).

The files it writes belong to Mojang's translations and are not stored in git
(.gitignore lists assets/mc112bridge/lang/ except en_US.lang and ru_RU.lang).

Usage:
    python tools/gen-langs.py [path-to-.minecraft]
"""
import io
import json
import os
import sys
import zipfile

KEEP = {'en_US', 'ru_RU'}


def default_minecraft():
    appdata = os.environ.get('APPDATA')
    if appdata:
        return os.path.join(appdata, '.minecraft')
    return os.path.join(os.path.expanduser('~'), '.minecraft')


def parse(text):
    out = {}
    for line in text.splitlines():
        if not line or line.startswith('#') or '=' not in line:
            continue
        k, v = line.split('=', 1)
        out[k] = v
    return out


def code17(code112):
    """de_de -> de_DE: the names 1.7.10 gives its language files."""
    if '_' not in code112:
        return code112
    a, b = code112.split('_', 1)
    return a + '_' + b.upper()


def main():
    mc = sys.argv[1] if len(sys.argv) > 1 else default_minecraft()
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    lang_dir = os.path.join(repo, 'src', 'main', 'resources', 'assets', 'mc112bridge', 'lang')
    jar_path = os.path.join(mc, 'versions', '1.12.2', '1.12.2.jar')
    index_path = os.path.join(mc, 'assets', 'indexes', '1.12.json')
    if not os.path.isfile(jar_path) or not os.path.isfile(index_path):
        sys.exit('Minecraft 1.12.2 (versions/1.12.2/1.12.2.jar and assets/indexes/1.12.json) not found in ' + mc)
    en112 = parse(zipfile.ZipFile(jar_path).read('assets/minecraft/lang/en_us.lang').decode('utf-8'))
    by_text = {}
    for k, v in en112.items():
        by_text.setdefault(v, k)
    with io.open(os.path.join(lang_dir, 'en_US.lang'), encoding='utf-8') as f:
        ours = parse(f.read())
    source = {}
    for k, v in ours.items():
        if k in en112:
            source[k] = k
        elif v in by_text:
            source[k] = by_text[v]
    with io.open(index_path, encoding='utf-8') as f:
        objects = json.load(f)['objects']
    written = 0
    for name, obj in sorted(objects.items()):
        if not (name.startswith('minecraft/lang/') and name.endswith('.lang')):
            continue
        code = code17(name[len('minecraft/lang/'):-len('.lang')])
        if code in KEEP or code == 'en_US':
            continue
        h = obj['hash']
        path = os.path.join(mc, 'assets', 'objects', h[:2], h)
        if not os.path.isfile(path):
            continue
        with io.open(path, encoding='utf-8') as f:
            theirs = parse(f.read())
        lines = []
        for k in sorted(source):
            v = theirs.get(source[k])
            if v is not None:
                lines.append(k + '=' + v)
        if lines:
            with io.open(os.path.join(lang_dir, code + '.lang'), 'w', encoding='utf-8', newline='\n') as out:
                out.write('\n'.join(lines) + '\n')
            written += 1
    print('mod keys %d, matched to 1.12.2 keys %d, languages written %d' % (len(ours), len(source), written))


if __name__ == '__main__':
    main()
