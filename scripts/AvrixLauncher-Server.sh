#!/bin/bash
set -e

echo ======================= Avrix Launcher =======================

# OS Architecture Definition
ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ]; then
    osArchitecture="x64"
else
    osArchitecture="x86"
fi

# Checking java PATH
if ! java -version &> /dev/null; then
    echo "[Avrix-Launcher] Java not found in PATH. Install JDK 17 or higher and add it to PATH."
    echo "[Avrix-Launcher] Download link: https://www.oracle.com/java/technologies/downloads/."
    exit 1
fi

# Checking the JDK version
jdkVersion=$(java -fullversion 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ -z "$jdkVersion" || $(echo "$jdkVersion < 17" | bc) -eq 1 ]]; then
    echo "[Avrix-Launcher] Requires Java 17 or higher. Current version: $jdkVersion."
    echo "[Avrix-Launcher] Download link: https://www.oracle.com/java/technologies/downloads/."
    exit 1
fi

# Setting Variables
classPath="java/:java/istack-commons-runtime.jar:java/jassimp.jar:java/javacord-2.0.17-shaded.jar:java/javax.activation-api.jar:java/jaxb-api.jar:java/jaxb-runtime.jar:java/lwjgl.jar:java/lwjgl-glfw.jar:java/lwjgl-jemalloc.jar:java/lwjgl-opengl.jar:java/lwjgl_util.jar:java/sqlite-jdbc-3.27.2.1.jar:java/trove-3.0.3.jar:java/uncommons-maths-1.2.3.jar:java/commons-compress-1.18.jar"

if [ "$osArchitecture" = "x64" ]; then
    libraryPath="natives/:natives/linux64/:."
    javaOptions="-XX:+UseZGC -Xms16g -Xmx16g"
    classPath="$classPath:java/lwjgl-natives-linux.jar:java/lwjgl-glfw-natives-linux.jar:java/lwjgl-jemalloc-natives-linux.jar:java/lwjgl-opengl-natives-linux.jar"
else
    libraryPath="natives/:natives/linux32/:./"
    javaOptions="-XX:+UseG1GC -Xms768m -Xmx768m"
    classPath="$classPath:java/lwjgl-natives-linux-x86.jar:java/lwjgl-glfw-natives-linux-x86.jar:java/lwjgl-jemalloc-natives-linux-x86.jar:java/lwjgl-opengl-natives-linux-x86.jar"
fi

# Checking the presence of the necessary directories
echo "[Avrix-Launcher] Checking the server directory..."
required_dirs=("java" "natives" "java/zombie")
for dir in "${required_dirs[@]}"; do
    if [ ! -d "$dir" ]; then
        echo "[Avrix-Launcher] Required directories not found!"
        echo "[Avrix-Launcher] Move the main jar file and this script to the server root folder and try again."
        exit 1
    fi
done

echo "[Avrix-Launcher] The server directory is confirmed."

# Selecting Steam Mode
read -p "[Avrix-Launcher] Use Steam mode (0 - without Steam; 1 - with Steam): " steamMode
if [ "$steamMode" = "1" ]; then
    steamOption="yes"
elif [ "$steamMode" = "0" ]; then
    steamOption="no"
else
    echo "[Avrix-Launcher] Invalid input. Enter 0 or 1."
    exit 1
fi

# Launching the application
jarFile=$(ls ./Avrix-Core-*.jar 2> /dev/null | head -n 1)
if [ -z "$jarFile" ]; then
    echo "[Avrix-Launcher] The wrapper Jar file was not found! Place it next to the launcher!"
    exit 1
fi

javaArg="-Djava.awt.headless=true -Davrix.mode=server -Dzomboid.steam=$steamMode -Dzomboid.znetlog=1 $javaOptions -Djava.library.path=$libraryPath -cp $classPath"
echo "[Avrix-Launcher] Core: $(basename "$jarFile") | OS: Linux $osArchitecture | JDK: $jdkVersion | Steam mode: $steamOption"
java -Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading $javaArg com.avrix.Launcher "$@"