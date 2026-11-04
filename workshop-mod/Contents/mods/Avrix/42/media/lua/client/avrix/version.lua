-- Appends the Avrix version tag to the game version text shown by the game.
--
-- The tag is only added while the Avrix loader marker reports an active
-- session, so a regular launch keeps the vanilla version text.

local Runtime = require "avrix/runtime"

require "ISUI/ISPanel"
require "ISUI/ISVersionWaterMark"
require "OptionScreens/MainScreen"

--- Original main menu constructor kept for the version tag wrapper.
local MainScreenOriginalNew = MainScreen.new
--- Original watermark constructor kept for the version tag wrapper.
local WaterMarkUIOriginalNew = WaterMarkUI.new

--- Cached Avrix mod version, or false when it could not be read.
local avrixVersion = nil

--- Writes a warning Avrix line.
---@param message string Message to write.
local function logWarn(message)
    print("WARN: [Avrix] " .. tostring(message))
end

--- Reads the Avrix mod version used in the version tag.
---@return string|nil version Mod version, or nil when it is unavailable.
local function readAvrixVersion()
    if avrixVersion ~= nil then
        return avrixVersion or nil
    end

    local version = nil
    if getModInfoByID ~= nil then
        local ok, modInfo = pcall(getModInfoByID, "Avrix")
        if ok and modInfo ~= nil then
            version = modInfo:getModVersion()
        end
    end

    if version == nil or version == "" then
        avrixVersion = false
        logWarn("failed to read the Avrix mod version for the version label")
        return nil
    end

    avrixVersion = version
    return version
end

--- Builds the suffix appended to the game version text.
---@return string suffix Text in the form ` (Avrix vX.X.X)`, or an empty string when the tag is unavailable.
local function buildSuffix()
    if not Runtime.isActive() then
        return ""
    end

    local version = readAvrixVersion()
    if version == nil then
        return ""
    end

    return " (Avrix v" .. version .. ")"
end

--- Appends the Avrix version tag to the main menu version text.
---@param inGame boolean Whether the screen is the in-game escape menu.
---@return MainScreen screen New main menu screen.
function MainScreen:new(inGame)
    local screen = MainScreenOriginalNew(self, inGame)
    screen.version = screen.version .. buildSuffix()
    return screen
end

--- Appends the Avrix version tag to the in-game version watermark.
---@param x number Screen X position of the watermark.
---@param y number Screen Y position of the watermark.
---@param width number Watermark width in pixels.
---@param height number Watermark height in pixels.
---@return WaterMarkUI panel New watermark panel.
function WaterMarkUI:new(x, y, width, height)
    local panel = WaterMarkUIOriginalNew(self, x, y, width, height)
    panel.version = panel.version .. buildSuffix()
    return panel
end
