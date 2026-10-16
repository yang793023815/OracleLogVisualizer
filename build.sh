#!/usr/bin/env bash
# Build OracleLogVisualizer-cn.jar from src/ (needs Java 11+)
set -euo pipefail
cd "$(dirname "$0")"

JAVAC=""
for c in /opt/homebrew/opt/openjdk@21/bin/javac /opt/homebrew/opt/openjdk@17/bin/javac javac; do
  if command -v "$c" >/dev/null 2>&1; then JAVAC="$c"; break; fi
done
[ -n "$JAVAC" ] || { echo "javac (Java 11+) not found" >&2; exit 1; }
JAR="$(dirname "$(command -v "$JAVAC" || echo "$JAVAC")")/jar"
[ -x "$JAR" ] || JAR="jar"

echo "Using $JAVAC"
BUILD=$(mktemp -d)
trap 'rm -rf "$BUILD"' EXIT

"$JAVAC" -encoding UTF-8 -d "$BUILD" src/com/weejar/oralog/*.java
"$JAR" --create --file OracleLogVisualizer-cn.jar --manifest manifest.txt -C "$BUILD" com
echo "Built OracleLogVisualizer-cn.jar"
