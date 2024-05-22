@echo off
setlocal enabledelayedexpansion

rem Prompt the user to choose the Steam version
:chooseSteam
set /p steamVersion="[Avrix-Launcher] Use Steam version? (0 - no, 1 - yes): "
if "%steamVersion%"=="1" (
    set "steamFlag=1"
) else if "%steamVersion%"=="0" (
    set "steamFlag=0"
) else (
    echo [Avrix-Launcher] Invalid input. Please enter 0 or 1.
    goto chooseSteam
)

rem Prompt the user to choose the OS version
:chooseOS
set /p os="[Avrix-Launcher] Use x86 or x64 OS version? (0 - x86, 1 - x64): "
if "%os%"=="1" (
    set "javaExec=.\jre64\bin\java.exe"
    set "libraryPath=natives/;natives/win64/;."
    set "javaOptions=-XX:+UseZGC -Xms16g -Xmx16g"
) else if "%os%"=="0" (
    set "javaExec=.\jre\bin\java.exe"
    set "libraryPath=natives/;natives/win32/;./"
    set "javaOptions=-XX:+UseG1GC -Xms768m -Xmx768m"
) else (
    echo [Avrix-Launcher] Invalid input. Please enter 0 or 1.
    goto chooseOS
)

rem Initialize the classPath variable based on user input
set "classPath=java/;java/istack-commons-runtime.jar;java/jassimp.jar;java/javacord-2.0.17-shaded.jar;java/javax.activation-api.jar;java/jaxb-api.jar;java/jaxb-runtime.jar;java/lwjgl.jar;java/lwjgl-glfw.jar;java/lwjgl-jemalloc.jar;java/lwjgl-opengl.jar;java/lwjgl_util.jar;java/sqlite-jdbc-3.27.2.1.jar;java/trove-3.0.3.jar;java/uncommons-maths-1.2.3.jar;java/commons-compress-1.18.jar;"

if "%os%"=="1" (
    set "classPath=!classPath!java/lwjgl-natives-windows.jar;java/lwjgl-glfw-natives-windows.jar;java/lwjgl-jemalloc-natives-windows.jar;java/lwjgl-opengl-natives-windows.jar;"
) else (
    set "classPath=!classPath!java/lwjgl-natives-windows-x86.jar;java/lwjgl-glfw-natives-windows-x86.jar;java/lwjgl-jemalloc-natives-windows-x86.jar;java/lwjgl-opengl-natives-windows-x86.jar;"
)

rem Find the jar file matching the pattern
for %%f in (./Avrix-Core-*.jar) do (
    set "jarFile=%%~nf"
    set "classPath=!classPath!%%f"
)

set "javaArg=-Djava.awt.headless=true -Dzomboid.steam=%steamFlag% -Dzomboid.znetlog=1 %javaOptions% -Djava.library.path=%libraryPath% -cp %classPath%"

rem Launch the Java application

echo [Avrix-Launcher] Starting the server with %jarFile%..
%javaExec% %javaArg% com.avrix.Main %1 %2

pause