-- Avrix runtime check.
-- Avrix writes avrix-active.marker at startup and removes it on shutdown.
-- The marker is available in the mod's 42 directory and in the game working directory.
local MAX_MARKER_AGE_MS = 24 * 60 * 60 * 1000

local function readMarkerTimestamp()
    local reader = getModFileReader("Avrix", "avrix-active.marker", false)
    if not reader then
        return nil
    end

    local ok, line = pcall(function()
        return reader:readLine()
    end)
    pcall(function()
        reader:close()
    end)

    if not ok then
        return nil
    end

    return tonumber(line)
end

local function isAvrixActive()
    local markerTime = readMarkerTimestamp()
    if markerTime then
        local now = getTimestampMs and getTimestampMs() or (os.time() * 1000)
        return (now - markerTime) <= MAX_MARKER_AGE_MS
    end

    return fileExists("avrix-active.marker")
end

if isAvrixActive() then
    print("[Avrix] Loader detected: this game session is running through Avrix.")
else
    print("[Avrix] WARNING: the Avrix Workshop mod is enabled, but the Avrix loader is NOT active.")
    print("[Avrix] Launch Project Zomboid with the Avrix -pzexeconfig profile.")
end
