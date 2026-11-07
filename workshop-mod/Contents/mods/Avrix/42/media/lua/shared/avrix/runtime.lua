-- Detection of an active Avrix loader session.
--
-- Avrix writes one avrix-active.marker in the Lua cache at startup. On Windows the
-- file is kept open with DELETE_ON_CLOSE, so Lua checks its presence and the operating
-- system removes it when the loader process exits, including after a crash. On other
-- systems the marker stays readable and its timestamp is refreshed every 30 seconds.

--- Maximum age of the last marker heartbeat before the loader is considered inactive.
local MAX_MARKER_AGE_MS = 5 * 60 * 1000

--- Whether Project Zomboid runs on Windows.
local IS_WINDOWS = type(getFileSeparator) == "function" and getFileSeparator() == "\\"

--- Marker file shared with the loader.
local MARKER_NAME = "avrix-active.marker"

--- Runtime state helper for the Avrix loader marker.
---@class AvrixRuntime
local AvrixRuntime = {}

--- Reads the timestamp from a marker reader and closes it.
---@param reader any|nil Open marker reader.
---@return number|nil timestamp Marker timestamp in milliseconds, or nil when unavailable.
local function readTimestamp(reader)
    if not reader then
        return nil
    end

    local ok, line = pcall(function()
        return reader:readLine()
    end)
    pcall(function()
        reader:close()
    end)

    if not ok or line == nil then
        return nil
    end

    return tonumber(line)
end

--- Checks whether the shared marker exists.
---@return boolean exists True when the loader marker file is present.
local function markerExists()
    return type(cacheFileExists) == "function" and cacheFileExists(MARKER_NAME)
end

--- Checks whether the marker is held open by a live loader process.
---
--- On Windows a stale marker is readable, while an active marker is locked by the
--- loader and cannot be opened with getFileReader.
---@return boolean locked True when the marker cannot be opened for reading.
local function markerIsLocked()
    local reader = getFileReader(MARKER_NAME, false)
    if reader == nil then
        return true
    end
    pcall(function()
        reader:close()
    end)
    return false
end

--- Reads the shared cache marker written by the loader.
---@return number|nil timestamp Marker timestamp in milliseconds, or nil when unavailable.
local function readMarkerTimestamp()
    return readTimestamp(getFileReader(MARKER_NAME, false))
end

--- Checks whether the Avrix loader marked the current session as active.
---
--- The loader keeps the marker alive for the whole process, so entering a world,
--- joining a server, or returning to the main menu re-checks it after Project Zomboid
--- resets the Lua state.
---@return boolean active True when the Avrix loader is active for this session.
function AvrixRuntime.isActive()
    if IS_WINDOWS then
        return markerExists() and markerIsLocked()
    end

    local markerTime = readMarkerTimestamp()
    if not markerTime then
        return false
    end

    local now = getTimestampMs and getTimestampMs() or (os.time() * 1000)
    local age = now - markerTime
    return age >= 0 and age <= MAX_MARKER_AGE_MS
end

return AvrixRuntime
