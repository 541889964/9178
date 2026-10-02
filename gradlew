#!/bin/sh
DIR=$(cd "$(dirname "$0")" && pwd)
if [ ! -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
    for u in \
      "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
      "https://gh-proxy.com/https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
      "https://ghproxy.net/https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"; do
        curl -L -s -o "$DIR/gradle/wrapper/gradle-wrapper.jar" "$u" && [ -s "$DIR/gradle/wrapper/gradle-wrapper.jar" ] && break
    done
fi
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
