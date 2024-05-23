@echo off
setlocal enabledelayedexpansion

echo ======================= Avrix Launcher =======================

rem Determine the OS bitness
:determineOS
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set "osArchitecture=x86" || set "osArchitecture=x64"

echo [Avrix-Launcher] Current OS: %osArchitecture%

rem Prompt the user to choose the Steam version
:chooseSteam
set /p steamVersion="[Avrix-Launcher] Use Steam version? (0 - no, 1 - yes): "
if "%steamVersion%"=="1" (
    set "steamFlag=1"
    set "useSteamText=yes"
) else if "%steamVersion%"=="0" (
    set "steamFlag=0"
    set "useSteamText=no"
) else (
    echo [Avrix-Launcher] Invalid input. Please enter 0 or 1.
    goto chooseSteam
)

rem Set the appropriate variables based on the OS bitness
set "classPath=java/;java/istack-commons-runtime.jar;java/jassimp.jar;java/javacord-2.0.17-shaded.jar;java/javax.activation-api.jar;java/jaxb-api.jar;java/jaxb-runtime.jar;java/lwjgl.jar;java/lwjgl-glfw.jar;java/lwjgl-jemalloc.jar;java/lwjgl-opengl.jar;java/lwjgl_util.jar;java/sqlite-jdbc-3.27.2.1.jar;java/trove-3.0.3.jar;java/uncommons-maths-1.2.3.jar;java/commons-compress-1.18.jar;"

if "%osArchitecture%"=="x64" (
    set "javaExec=.\jre64\bin\java.exe"
    set "libraryPath=natives/;natives/win64/;."
    set "javaOptions=-XX:+UseZGC -Xms16g -Xmx16g"
    set "classPath=!classPath!java/lwjgl-natives-windows.jar;java/lwjgl-glfw-natives-windows.jar;java/lwjgl-jemalloc-natives-windows.jar;java/lwjgl-opengl-natives-windows.jar;"
) else (
    set "javaExec=.\jre\bin\java.exe"
    set "libraryPath=natives/;natives/win32/;./"
    set "javaOptions=-XX:+UseG1GC -Xms768m -Xmx768m"
    set "classPath=!classPath!java/lwjgl-natives-windows-x86.jar;java/lwjgl-glfw-natives-windows-x86.jar;java/lwjgl-jemalloc-natives-windows-x86.jar;java/lwjgl-opengl-natives-windows-x86.jar;"
)

rem Find the jar file matching the pattern
for %%f in (./Avrix-Core-*.jar) do (
    set "jarFile=%%~nf"
    set "classPath=!classPath!%%f"
)

set "javaArg=-Djava.awt.headless=true -Dzomboid.steam=%steamFlag% -Dzomboid.znetlog=1 %javaOptions% -Djava.library.path=%libraryPath% -cp %classPath%"

rem Launch the Java application

echo [Avrix-Launcher] Core: %jarFile% ^| OS: %osArchitecture% ^| Steam version: %useSteamText%
echo [Avrix-Launcher] Loading a server...

%javaExec% %javaArg% com.avrix.Main %1 %2

pause