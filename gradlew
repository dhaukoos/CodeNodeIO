#!/bin/bash

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Determine the absolute path to this script
SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
APP_HOME="${SCRIPT_PATH}"

# Define wrapper JAR location
GRADLE_WRAPPER="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER" ]; then
    echo "Error: gradle-wrapper.jar not found at ${GRADLE_WRAPPER}"
    exit 1
fi

# Determine Java to use
if [ -z "$JAVA_HOME" ]; then
    # Try to find Java 11+
    JAVA_HOME=$( /usr/libexec/java_home -v 11+ 2>/dev/null ) || \
    JAVA_HOME=$( /usr/libexec/java_home 2>/dev/null ) || \
    JAVA_HOME="/usr/libexec/java_home"
fi

JAVA="${JAVA_HOME}/bin/java"

if [ ! -x "$JAVA" ]; then
    echo "Error: JAVA_HOME is not properly set or Java executable not found"
    exit 1
fi

# Pass through all arguments to the Gradle wrapper
exec "$JAVA" -classpath "$GRADLE_WRAPPER" org.gradle.wrapper.GradleWrapperMain "$@"

