#!/bin/sh

set -e

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper jar not found: $WRAPPER_JAR" >&2
  echo "Download it from the wrapperUrl in .mvn/wrapper/maven-wrapper.properties." >&2
  exit 1
fi

exec java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
