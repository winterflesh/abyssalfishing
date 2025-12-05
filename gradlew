#!/bin/bash
# Simple gradle wrapper script

if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java -jar "gradle/wrapper/gradle-wrapper.jar" "$@"
else
    echo "Gradle wrapper not found. Please install Gradle manually."
    exit 1
fi