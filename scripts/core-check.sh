#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
kotlinc core-domain/src/main/kotlin/com/hanziflow/core/*.kt scripts/CoreSmoke.kt -include-runtime -d "$tmp/check.jar"
java -jar "$tmp/check.jar"
