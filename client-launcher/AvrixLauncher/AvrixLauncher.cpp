#include <iostream>
#include <string>
#include <windows.h>
#include <vector>
#include <filesystem>

void showPopup(const std::wstring& message, const std::wstring& title) {
    MessageBoxW(NULL, message.c_str(), title.c_str(), MB_ICONERROR);
}

std::wstring getJavaVersion() {
    std::wstring javaVersion;
    FILE* pipe = _wpopen(L"java -fullversion 2>&1", L"r");
    if (!pipe) return L"";
    wchar_t buffer[128];
    while (fgetws(buffer, 128, pipe) != NULL) {
        javaVersion += buffer;
    }
    _pclose(pipe);

    size_t start = javaVersion.find(L'"');
    size_t end = javaVersion.find(L'"', start + 1);
    if (start != std::wstring::npos && end != std::wstring::npos) {
        javaVersion = javaVersion.substr(start + 1, end - start - 1);
    }
    return javaVersion;
}

bool isJavaVersionValid(const std::wstring& version) {
    int majorVersion = 0;
    try {
        majorVersion = std::stoi(version.substr(0, version.find(L'.')));
    }
    catch (...) {
        return false;
    }
    return majorVersion >= 17;
}

bool checkJavaInPath() {
    return _wsystem(L"java -version >nul 2>&1") == 0;
}

void setupVariables(bool is64bit, std::wstring& classPath, std::wstring& libraryPath, std::wstring& javaOptions) {
    classPath = L"./;istack-commons-runtime.jar;jassimp.jar;javacord-2.0.17-shaded.jar;javax.activation-api.jar;jaxb-api.jar;jaxb-runtime.jar;lwjgl.jar;lwjgl-glfw.jar;lwjgl-jemalloc.jar;lwjgl-opengl.jar;lwjgl_util.jar;sqlite-jdbc-3.27.2.1.jar;trove-3.0.3.jar;uncommons-maths-1.2.3.jar;commons-compress-1.18.jar;";
    libraryPath = L"./;";
    javaOptions = is64bit ? L"-XX:+UseZGC -Xmx3072m" : L"-XX:+UseG1GC -Xmx1200m";

    if (is64bit) {
        libraryPath += L"win64/";
        classPath += L"lwjgl-natives-windows.jar;lwjgl-glfw-natives-windows.jar;lwjgl-jemalloc-natives-windows.jar;lwjgl-opengl-natives-windows.jar;";
    }
    else {
        libraryPath += L"win32/";
        classPath += L"lwjgl-natives-windows-x86.jar;lwjgl-glfw-natives-windows-x86.jar;lwjgl-jemalloc-natives-windows-x86.jar;lwjgl-opengl-natives-windows-x86.jar;";
    }
}

bool checkFolders() {
    std::vector<std::wstring> requiredFolders = { L"zombie", L"se", L"fmod", L"javax" };
    for (const auto& folder : requiredFolders) {
        if (!std::filesystem::exists(folder)) {
            showPopup(L"The necessary folders were not found!\n\nMove core jar file and this launch file to the root folder of your game and try again.", L"Game files not found");
            return false;
        }
    }
    return true;
}

void launchApplication(const std::wstring& classPath, const std::wstring& libraryPath, const std::wstring& javaOptions, const std::wstring& args) {
    std::wstring jarFile;
    for (const auto& entry : std::filesystem::directory_iterator(".")) {
        if (entry.path().extension() == L".jar" && entry.path().stem().wstring().find(L"Avrix-Core-") == 0) {
            jarFile = entry.path().filename().wstring();
            break;
        }
    }

    if (jarFile.empty()) {
        showPopup(L"The wrapper Jar file 'Avrix-Core' was not found.\n\nPlace it next to the launcher!", L"Wrapper not found");
        return;
    }

    std::wstring fullClassPath = classPath + jarFile;
    std::wstring javaArg = L"-Djava.awt.headless=true -Davrix.mode=client -Dzomboid.steam=1 -Dzomboid.znetlog=1 " + javaOptions + L" -XX:-CreateCoredumpOnCrash -XX:-OmitStackTraceInFastThrow -Djava.library.path=" + libraryPath + L" -cp " + fullClassPath;
    std::wstring command = L"java -Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading " + javaArg + L" com.avrix.Launcher " + args;

    // Use CreateProcess to launch Java application without showing console
    STARTUPINFOW si = { sizeof(si) };
    PROCESS_INFORMATION pi;
    std::vector<wchar_t> cmd(command.begin(), command.end());
    cmd.push_back(0);

    if (!CreateProcessW(NULL, cmd.data(), NULL, NULL, FALSE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {
        showPopup(L"Failed to start Java application.", L"Error");
        return;
    }

    // Wait until the process exits
    WaitForSingleObject(pi.hProcess, INFINITE);

    // Close process and thread handles
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR pCmdLine, int nCmdShow) {
    HWND hwnd = GetConsoleWindow();
    ShowWindow(hwnd, SW_HIDE);

    if (!checkJavaInPath()) {
        showPopup(L"Java is not found in PATH. Please install JDK 17 or higher and add it to the PATH.\n\nDownload link: https://www.oracle.com/java/technologies/downloads/", L"JDK not found");
        return 1;
    }

    std::wstring javaVersion = getJavaVersion();
    if (javaVersion.empty()) {
        showPopup(L"Failed to determine Java version.\n\nCheck if JDK17+ is installed and the path is correct in the System PATH.", L"JDK not found");
        return 1;
    }
    if (!isJavaVersionValid(javaVersion)) {
        showPopup(L"Java 17 or higher is required. Current version is " + javaVersion + L"\n\nDownload link: https://www.oracle.com/java/technologies/downloads/", L"Incorrect JDK version");
        return 1;
    }

    SYSTEM_INFO sysInfo;
    GetNativeSystemInfo(&sysInfo);
    bool is64bit = (sysInfo.wProcessorArchitecture == PROCESSOR_ARCHITECTURE_AMD64);

    std::wstring classPath, libraryPath, javaOptions;
    setupVariables(is64bit, classPath, libraryPath, javaOptions);

    if (!checkFolders()) {
        return 1;
    }

    launchApplication(classPath, libraryPath, javaOptions, pCmdLine);

    return 0;
}
