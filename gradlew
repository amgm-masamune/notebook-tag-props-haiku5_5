#!/bin/sh
# Minimal Gradle Wrapper launcher (POSIX sh). Delegates to gradle-wrapper.jar.

# Resolve the real script location (follow symlinks).
app_path=$0
while [ -h "$app_path" ]; do
    ls=$(ls -ld -- "$app_path")
    link=${ls#*' -> '}
    case $link in
        /*) app_path=$link ;;
        *) app_path=$(dirname "$app_path")/$link ;;
    esac
done
APP_HOME=$(cd "$(dirname "$app_path")" > /dev/null && pwd -P) || exit

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
    command -v java > /dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        exit 1
    }
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVACMD" -Xmx64m -Xms64m $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
