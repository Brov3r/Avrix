@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ======================= Avrix Launcher =======================

rem Defining the OS architecture
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set "osArchitecture=x86" || set "osArchitecture=x64"

rem Check if Java is in PATH
java -version >nul 2>&1

rem Check JDK
if %errorlevel% neq 0 (
    echo [Avrix-Launcher] Java is not found in PATH. Please install JDK 17 or higher and add it to the PATH.
    echo [Avrix-Launcher] Download link: https://www.oracle.com/java/technologies/downloads/.
    pause
    exit /b
)
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jdkVersion=%%j.%%k.%%l-%%m"

if not defined jdkVersion (
    echo [Avrix-Launcher] Failed to determine Java version.
    echo [Avrix-Launcher] Check if JDK17+ is installed and the path is correct in the System PATH.
    pause
    exit /b
)

if %jdkVersion% lss 17 (
    echo [Avrix-Launcher] Java 17 or higher is required. Current version is %jdkVersion%.
    echo [Avrix-Launcher] Download link: https://www.oracle.com/java/technologies/downloads/.
    pause
    exit /b
)

rem Setting up variables
set "classPath=./;istack-commons-runtime.jar;jassimp.jar;javacord-2.0.17-shaded.jar;javax.activation-api.jar;jaxb-api.jar;jaxb-runtime.jar;lwjgl.jar;lwjgl-glfw.jar;lwjgl-jemalloc.jar;lwjgl-opengl.jar;lwjgl_util.jar;sqlite-jdbc-3.27.2.1.jar;trove-3.0.3.jar;uncommons-maths-1.2.3.jar;commons-compress-1.18.jar;"
if "%osArchitecture%"=="x64" (
    set "libraryPath=./;win64/"
    set "javaOptions=-XX:+UseZGC -Xmx16g"
    set "classPath=!classPath!lwjgl-natives-windows.jar;lwjgl-glfw-natives-windows.jar;lwjgl-jemalloc-natives-windows.jar;lwjgl-opengl-natives-windows.jar;"
) else (
    set "libraryPath=./;win32/"
    set "javaOptions=-XX:+UseG1GC -Xmx1200m"
    set "classPath=!classPath!lwjgl-natives-windows-x86.jar;lwjgl-glfw-natives-windows-x86.jar;lwjgl-jemalloc-natives-windows-x86.jar;lwjgl-opengl-natives-windows-x86.jar;"
)

:checkingFolders
rem Check the availability of the necessary folders
echo [Avrix-Launcher] Checking the server directory...
if not exist "zombie" goto serverNotFound
if not exist "se" goto serverNotFound
if not exist "fmod" goto serverNotFound
if not exist "javax" goto serverNotFound

echo [Avrix-Launcher] The server directory has been confirmed.
goto chooseSteam

:serverNotFound
echo [Avrix-Launcher] The necessary folders were not found!
echo [Avrix-Launcher] Move core jar file and this script to the root folder of your server and try again.
pause
exit /b

rem Choosing the Steam mode
:chooseSteam
set /p steamMode="[Avrix-Launcher] Use Steam mode? (0 - without Steam; 1 - with Steam): "
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

if "%jarFile%"=="" (
    echo [Avrix-Launcher] The wrapper Jar file was not found! Place it next to the launcher!
    pause
    exit /b
)

set "javaArg=-Djava.awt.headless=true -Davrix.mode=server -Dzomboid.steam=%steamMode% -Dzomboid.znetlog=1 %javaOptions% -Djava.library.path=%libraryPath% -cp %classPath%"
echo [Avrix-Launcher] Core: %jarFile% ^| OS: Win %osArchitecture% ^| JDK: %jdkVersion% ^| Steam mode: %steamOption%

java -Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading %javaArg% com.avrix.Launcher %1 %2

pause