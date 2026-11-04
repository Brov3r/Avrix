-- Avrix runtime check.
--
-- The check uses the shared Runtime module so the client UI and this startup
-- message agree on what counts as an active loader session.

local Runtime = require "avrix/runtime"

--- Writes an informational Avrix line.
---@param message string Message to write.
local function logInfo(message)
    print("INFO: [Avrix] " .. tostring(message))
end

--- Writes a warning Avrix line.
---@param message string Message to write.
local function logWarn(message)
    print("WARN: [Avrix] " .. tostring(message))
end

if Runtime.isActive() then
    logInfo("Loader detected: this game session is running through Avrix.")
else
    logWarn("The Avrix Workshop mod is enabled, but the Avrix loader is NOT active.")
    logWarn("Launch Project Zomboid with the Avrix -pzexeconfig profile.")
end
