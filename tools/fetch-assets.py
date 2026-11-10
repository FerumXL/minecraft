#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Restore the Minecraft 1.12.2 files listed in tools/mojang-assets.txt.

These files belong to Mojang and are not stored in this repository. The script
copies them from your own Minecraft installation into src/main/resources.

Usage:
    python tools/fetch-assets.py [path-to-.minecraft]

Default path: %APPDATA%/.minecraft on Windows, ~/.minecraft elsewhere.
Minecraft 1.12.2 must be installed (versions/1.12.2/1.12.2.jar).
"""
import hashlib
import io
import os
import sys
import zipfile


def default_minecraft():
    appdata = os.environ.get('APPDATA')
    if appdata:
        return os.path.join(appdata, '.minecraft')
    return os.path.join(os.path.expanduser('~'), '.minecraft')


def sha1(data):
    return hashlib.sha1(data).hexdigest()


def main():
    mc = sys.argv[1] if len(sys.argv) > 1 else default_minecraft()
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    manifest = os.path.join(repo, 'tools', 'mojang-assets.txt')
    jar_path = os.path.join(mc, 'versions', '1.12.2', '1.12.2.jar')
    if not os.path.isfile(jar_path):
        sys.exit('1.12.2.jar not found under versions/1.12.2 of the given .minecraft folder')
    jar = zipfile.ZipFile(jar_path)

    copied = same = failed = 0
    with io.open(manifest, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            if not line or line.startswith('#'):
                continue
            path, digest, source = line.split('\t')
            kind, ref = source.split(':', 1)
            target = os.path.join(repo, *path.split('/'))
            if os.path.isfile(target):
                with open(target, 'rb') as t:
                    if sha1(t.read()) == digest:
                        same += 1
                        continue
            try:
                if kind in ('jar', 'jar-eol'):
                    data = jar.read(ref)
                    if kind == 'jar-eol' and sha1(data) != digest:
                        lf = data.replace(b'\r\n', b'\n')
                        data = lf if sha1(lf) == digest else lf.replace(b'\n', b'\r\n')
                elif kind == 'asset':
                    with open(os.path.join(mc, 'assets', 'objects', ref[:2], ref), 'rb') as a:
                        data = a.read()
                else:
                    raise ValueError('unknown source ' + source)
                if sha1(data) != digest:
                    raise ValueError('sha1 mismatch')
            except Exception as e:
                failed += 1
                print('FAIL %s (%s): %s' % (path, source, e))
                continue
            folder = os.path.dirname(target)
            if not os.path.isdir(folder):
                os.makedirs(folder)
            with open(target, 'wb') as t:
                t.write(data)
            copied += 1

    print('copied %d, already present %d, failed %d' % (copied, same, failed))
    if failed:
        sys.exit(1)


if __name__ == '__main__':
    main()
