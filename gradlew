#!/bin/sh
DIR=$(cd "$(dirname "$0")" && pwd)
if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
    curl -L -o "$DIR/gradle/wrapper/gradle-wrapper.jar" \
        "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
fi
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
