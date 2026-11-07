"""Refresh shipped license summaries from resolved Maven artifacts and locked Cargo metadata.
Run the companion Gradle inventory task first; see README.md. No license is guessed.
"""
import argparse
import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'
OUTPUTS = {
    'core': ROOT / 'src/main/resources',
    'uie': ROOT / 'addons/ui-enhancements/src/main/resources',
    'controller': ROOT / 'addons/ui-enhancements-controller/src/main/resources',
    'typst': ROOT / 'addons/typst-renderer/src/main/resources',
}

def pom_info(group, name, version, depth=0):
    paths = list((CACHE / group / name / version).glob('*/*.pom'))
    if not paths or depth > 8:
        return [], ''
    node = ET.parse(paths[0]).getroot()
    licenses = [n.findtext('m:name', namespaces=NS) for n in node.findall('m:licenses/m:license', NS)]
    url = node.findtext('m:url', default='', namespaces=NS)
    parent = node.find('m:parent', NS)
    if not licenses and parent is not None:
        licenses, parent_url = pom_info(*(parent.findtext('m:' + key, namespaces=NS)
                                         for key in ('groupId', 'artifactId', 'version')), depth + 1)
        url = url or parent_url
    return licenses, url

NORMALIZE = {
    'GNU Lesser General Public License v3.0': 'LGPL-3.0',
    'The Apache License, Version 2.0': 'Apache-2.0',
    'The Apache Software License, Version 2.0': 'Apache-2.0',
    'Apache License, Version 2.0': 'Apache-2.0',
    'MIT License': 'MIT', 'The MIT License': 'MIT',
    'Mozilla Public License, Version 2.0': 'MPL-2.0',
    'GPLv2 with Classpath Exception': 'GPL-2.0-or-later WITH Classpath exception; font terms also apply',
}
# Explicit corrections backed by pinned upstream source/license or source-jar notices.
OVERRIDES = {
    'jazzy': ('LGPL-2.1-or-later', 'https://repo.maven.apache.org/maven2/net/sf/jazzy/jazzy/0.5.2-rtext-1.4.1-2/jazzy-0.5.2-rtext-1.4.1-2-sources.jar'),
    'jaudiotagger': ('LGPL-2.1-or-later', 'https://repo.maven.apache.org/maven2/net/jthink/jaudiotagger/3.0.1/jaudiotagger-3.0.1-sources.jar'),
    'gson': ('Apache-2.0', 'https://github.com/google/gson/blob/gson-parent-2.12.1/LICENSE'),
    'jieba-analysis': ('Apache-2.0', 'https://github.com/huaban/jieba-analysis/blob/master/LICENSE'),
    'h2': ('MPL-2.0 OR EPL-1.0', 'https://github.com/h2database/h2database/blob/version-2.3.232/LICENSE.txt'),
    'error_prone_annotations': ('Apache-2.0', 'https://github.com/google/error-prone/blob/v2.36.0/COPYING'),
    'kotlin-stdlib': ('Apache-2.0', 'https://github.com/JetBrains/kotlin/blob/v2.3.20/license/LICENSE.txt'),
    'annotations': ('Apache-2.0', 'https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt'),
    'commons-io': ('Apache-2.0', 'https://commons.apache.org/proper/commons-io/'),
    'commons-logging': ('Apache-2.0', 'https://commons.apache.org/proper/commons-logging/'),
    'commons-codec': ('Apache-2.0', 'https://commons.apache.org/proper/commons-codec/'),
    'json': ('Public domain', 'https://github.com/stleary/JSON-java/blob/20240303/LICENSE'),
    'sdl-api': ('MPL-2.0', 'https://github.com/isXander/controlify-sdl'),
    'sdl-backend-ffm': ('MPL-2.0', 'https://github.com/isXander/controlify-sdl'),
}

def entry(section, name, version, license, source, evidence):
    assert all((section, name, license, source, evidence)), name
    return dict(section=section, name=name, version=version, license=license,
                source=source, evidence=evidence)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('maven_inventory', type=Path)
    args = parser.parse_args()
    catalogs = json.loads((Path(__file__).parent / 'curated.json').read_text(encoding='utf-8'))
    for row in json.loads(args.maven_inventory.read_text(encoding='utf-8-sig')):
        if row['version'] == 'unspecified' or row['group'].startswith(('neofontrender', 'org.tiqian.nfr')):
            continue
        module, section = {
            ':': ('core', '10_core'), ':addons:ui-enhancements': ('uie', '30_uie'),
            ':engine:audio': ('uie', '40_audio'), ':addons:ui-enhancements-controller': ('controller', '50_controller')
        }[row['project']]
        licenses, url = pom_info(row['group'], row['name'], row['version'])
        license = ' / '.join(NORMALIZE.get(x, x) for x in licenses)
        evidence = 'Maven POM: ' + ':'.join(row[k] for k in ('group', 'name', 'version'))
        if row['name'] in OVERRIDES:
            license, url = OVERRIDES[row['name']]
            evidence += '; upstream license/source or bundled META-INF/LICENSE'
        if not url or '${' in url:
            url = 'https://repo.maven.apache.org/maven2/' + row['group'].replace('.', '/') + '/' + row['name'] + '/' + row['version'] + '/'
        catalogs[module].append(entry(section, row['name'], row['version'], license, url, evidence))
    for module, section, manifest in [
        ('core', '20_cosmic', 'native/cosmic-text/Cargo.toml'),
        ('typst', '70_typst_native', 'engine/typst-render/native/Cargo.toml')
    ]:
        data = json.loads(subprocess.check_output(['cargo', 'metadata', '--format-version', '1',
            '--locked', '--offline', '--manifest-path', str(ROOT / manifest)], cwd=ROOT))
        # Include normal/build dependencies and platform alternatives, but not dev-only packages.
        nodes = {n['id']: n for n in data['resolve']['nodes']}
        seen, pending = set(), [data['resolve']['root']]
        while pending:
            package = pending.pop()
            if package in seen: continue
            seen.add(package)
            for dependency in nodes[package]['deps']:
                if any(kind['kind'] != 'dev' for kind in dependency['dep_kinds']):
                    pending.append(dependency['pkg'])
        for package in data['packages']:
            if package['id'] not in seen or package['id'] == data['resolve']['root']: continue
            source = package.get('repository') or package.get('homepage') or 'https://crates.io/crates/' + package['name']
            catalogs[module].append(entry(section, package['name'], package['version'], package['license'],
                source, manifest + ' / Cargo.lock; resolved package Cargo.toml license field; includes build/platform dependencies'))
    for module, rows in catalogs.items():
        unique = {(r['section'], r['name'], r['version']): r for r in rows}
        rows = sorted(unique.values(), key=lambda r: (r['section'], r['name'], r['version']))
        output = OUTPUTS[module] / 'META-INF/neofontrender/third-party-licenses.json'
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(module, len(rows), 'entries')

if __name__ == '__main__': main()
