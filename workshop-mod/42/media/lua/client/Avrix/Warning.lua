require "ISUI/ISModalDialog"

local MAX_MARKER_AGE_MS = 24 * 60 * 60 * 1000
local MENU_DELAY_TICKS = 30
local shown = false
local scheduled = false
local delayTicks = 0

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

local function showWarning()
    if shown or not isClient() or isAvrixActive() then
        shown = true
        return
    end

    shown = true

    local width = 480
    local height = 190
    local text = "Avrix is enabled, but the Avrix loader is NOT active.\\n\\n"
            .. "Launch Project Zomboid with the Avrix -pzexeconfig profile.\\n"
            .. "Otherwise Avrix plugins and mixins will not load."

    local dialog = ISModalDialog:new(
            (getCore():getScreenWidth() - width) / 2,
            (getCore():getScreenHeight() - height) / 2,
            width,
            height,
            text,
            false,
            nil,
            nil
    )

    dialog:initialise()
    dialog:addToUIManager()
    dialog:bringToTop()
    AvrixWarningDialog = dialog
end

local function deferredShow()
    if delayTicks > 0 then
        delayTicks = delayTicks - 1
        return
    end

    Events.OnTick.Remove(deferredShow)
    scheduled = false
    showWarning()
end

local function scheduleShow(delay)
    if shown or scheduled then
        return
    end

    scheduled = true
    delayTicks = delay or 0
    Events.OnTick.Add(deferredShow)
end

Events.OnMainMenuEnter.Add(function()
    scheduleShow(MENU_DELAY_TICKS)
end)

Events.OnGameStart.Add(function()
    scheduleShow(0)
end)
