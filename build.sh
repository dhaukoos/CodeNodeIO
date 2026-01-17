#!/bin/zsh

# CodeNodeIO Build Script
# Run this to build the entire project

echo "🏗️  CodeNodeIO Build Script"
echo "============================"
echo ""

cd /Users/danahaukoos/CodeNodeIO

echo "✅ Setting up environment..."
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null) || export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)

if [ -z "$JAVA_HOME" ]; then
    echo "❌ Error: Java not found"
    echo "Please install Java 11+ first:"
    echo "  brew install openjdk@21"
    exit 1
fi

echo "✅ Java Home: $JAVA_HOME"
echo "✅ Java Version:"
java -version
echo ""

echo "🏗️  Building CodeNodeIO..."
echo "This may take 2-5 minutes on first run..."
echo ""

chmod +x ./gradlew
./gradlew clean build

BUILD_STATUS=$?

echo ""
if [ $BUILD_STATUS -eq 0 ]; then
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "Next steps:"
    echo "  • Run tests: ./gradlew test"
    echo "  • Try Compose UI: ./gradlew graphEditor:run"
    echo "  • Launch IDE plugin: ./gradlew idePlugin:runIde"
else
    echo "❌ BUILD FAILED (exit code: $BUILD_STATUS)"
    echo "Please check the output above for errors."
fi

exit $BUILD_STATUS

