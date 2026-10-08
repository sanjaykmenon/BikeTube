#!/usr/bin/env python3
"""Create a source-only BikeTube archive from an explicit public file list."""
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
FILES = ['.gitignore', 'README.md', 'LICENSE', 'NOTICE', 'CONTRIBUTING.md',
         'settings.gradle', 'build.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
         'app/build.gradle', 'gradle/wrapper/gradle-wrapper.jar',
         'gradle/wrapper/gradle-wrapper.properties', '.github/workflows/build.yml', '.github/ISSUE_TEMPLATE/bug_report.md']
TREES = ['app/src', 'docs', 'scripts']

def main():
    paths = [ROOT / name for name in FILES]
    for name in TREES:
        paths.extend(p for p in (ROOT / name).rglob('*') if p.is_file()
                     and '__pycache__' not in p.parts and p.suffix != '.pyc')
    for path in paths:
        if path.is_symlink() or ROOT not in path.resolve().parents:
            raise ValueError('Public files must remain inside the project: ' + str(path))
        if path.suffix.lower() in ('.apk', '.keystore', '.jks'):
            raise ValueError('Private or compiled file in public source: ' + str(path))
    output = ROOT / 'dist/BikeTube-source.zip'
    output.parent.mkdir(exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(set(paths)):
            archive.write(path, 'BikeTube/' + path.relative_to(ROOT).as_posix())
    print(output)

if __name__ == '__main__':
    main()
