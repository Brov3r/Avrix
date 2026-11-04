-- Detection of an active Avrix loader session.
--
-- Avrix writes avrix-active.marker at startup, refreshes its timestamp every
-- 30 seconds, and removes it on shutdown. A marker older than the heartbeat
-- timeout is treated as stale, so a crash does not leave the loader marked as
-- active forever.

--- Maximum age of the last marker heartbeat before the loader is considered inactive.
local MAX_MARKER_AGE_MS = 5 * 60 * 1000

--- Runtime state helper for the Avrix loader marker.
---@class AvrixRuntime
local AvrixRuntime = {}

--- Reads the timestamp stored in the Avrix marker file.
---@return number|nil timestamp Marker timestamp in milliseconds, or nil when unavailable.
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

    if not ok or line == nil then
        return nil
    end

    return tonumber(line)
end

--- Checks whether the Avrix loader marked the current session as active.
---
--- The marker is active while it is younger than the heartbeat timeout. A missing
--- or unreadable marker means the loader is not running.
---@return boolean active True when the Avrix loader is active for this session.
function AvrixRuntime.isActive()
    local markerTime = readMarkerTimestamp()
    if not markerTime then
        return false
    end

    local now = getTimestampMs and getTimestampMs() or (os.time() * 1000)
    return (now - markerTime) <= MAX_MARKER_AGE_MS
end

return AvrixRuntime
