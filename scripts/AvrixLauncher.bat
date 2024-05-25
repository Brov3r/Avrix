@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem Defining the OS architecture
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set "osArchitecture=x86" || set "osArchitecture=x64"
echo ======================= Avrix Launcher (Windows %osArchitecture%) =======================

rem Setting up variables
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

:checkingFolders
rem Check the availability of the necessary folders
echo [Avrix-Launcher] Checking for dependencies...
if not exist "java" goto serverNotFound
if not exist "natives" goto serverNotFound
if not exist "jre" goto serverNotFound
if not exist "jre64" goto serverNotFound
if not exist "java\zombie" goto serverNotFound

echo [Avrix-Launcher] All necessary folders have been found.
goto chooseMode

:serverNotFound
echo [Avrix-Launcher] The necessary folders were not found!
echo [Avrix-Launcher] Move the core (jar file) and this script to the root folder of your server and try again.
pause
exit /b

rem Selecting the startup mode
:chooseMode
set /p launchMode="[Avrix-Launcher] Launcher startup mode (0 - start the server; 1 - install the core; 2 - uninstall the core): "
if "%launchMode%"=="0" (
    set "launchOption=-launch"
) else if "%launchMode%"=="1" (
    set "launchOption=-install"
    goto launchApp
) else if "%launchMode%"=="2" (
    set "launchOption=-uninstall"
    goto launchApp
) else (
    echo [Avrix-Launcher] Invalid input. Please enter 0, 1 or 2.
    goto chooseMode
)

rem Choosing the Steam mode
:chooseSteam
set /p steamMode="[Avrix-Launcher] Steam mode (0 - without Steam; 1 - with Steam): "
if "%steamMode%"=="1" (
    set "steamOption=yes"
) else if "%steamMode%"=="0" (
    set "steamOption=no"
) else (
    echo [Avrix-Launcher] Invalid input. Please enter 0 or 1.
    goto chooseSteam
)

rem Launching the application
:launchApp
for %%f in (./Avrix-Core-*.jar) do set "jarFile=%%~nf" & set "classPath=!classPath!%%f"
set "javaArg=-Djava.awt.headless=true -Dzomboid.steam=%steamMode% -Dzomboid.znetlog=1 %javaOptions% -Djava.library.path=%libraryPath% -cp %classPath%"
echo [Avrix-Launcher] Core: %jarFile% ^| OS: %osArchitecture% ^| Steam version: %steamOption%
%javaExec% %javaArg% com.avrix.Main %launchOption% %1 %2

pause