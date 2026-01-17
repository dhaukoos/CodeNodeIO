#!/bin/zsh

# CodeNodeIO Gradle Wrapper for macOS
# Automatically finds Java and delegates to Gradle 8.5

# Auto-detect JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null) || export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
fi

if [ -z "$JAVA_HOME" ]; then
    echo "Error: Java not found. Please install Java 11+ or set JAVA_HOME."
    exit 1
fi

# Execute Gradle 8.5
exec "$(dirname "$0")/gradle-8.5/bin/gradle" "$@"

