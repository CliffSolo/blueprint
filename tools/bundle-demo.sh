#!/usr/bin/env bash
# Copies the demo course into the APK so the app works offline on first launch.
# Usage: tools/bundle-demo.sh [path-to-course-repo]   (default: ../blueprint-4-stroke-engine)
set -euo pipefail
src="${1:-$(dirname "$0")/../../blueprint-4-stroke-engine}"
dest="$(dirname "$0")/../app/src/main/assets/courses/4-stroke-engine"
rm -rf "$dest" && mkdir -p "$dest"
cp -r "$src/course.json" "$src/lessons" "$src/labs" "$dest/"
echo "Bundled $(find "$dest" -type f | wc -l) files into $dest"
