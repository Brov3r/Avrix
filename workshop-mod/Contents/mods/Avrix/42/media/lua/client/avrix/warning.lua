require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISRichTextPanel"

local Runtime = require "avrix/runtime"
local UI = require "avrix/ui"

--- Frames to wait before showing the warning on the main menu.
local MENU_DELAY_FRAMES = 30
--- Panel width in pixels.
local PANEL_WIDTH = 640
--- Panel height in pixels.
local PANEL_HEIGHT = 440
--- Space between the panel edge and its content.
local UI_MARGIN = 16
--- Height reserved for the title.
local HEADER_HEIGHT = 78
--- Height of the highlighted inactive-loader banner.
local BANNER_HEIGHT = 34
--- Vertical offset where the body text starts.
local TEXT_TOP = HEADER_HEIGHT + 50
--- Height reserved for the OK button.
local FOOTER_HEIGHT = 82
--- Height of the OK button.
local BUTTON_HEIGHT = 32
--- Width of the OK button.
local BUTTON_WIDTH = 150

--- Whether the warning has already been scheduled or shown.
local shown = false
--- Whether a deferred show is currently waiting on the render event.
local scheduled = false
--- Frames remaining before the deferred show runs.
local delayFrames = 0

--- Writes a warning Avrix line.
---@param message string Message to write.
local function logWarn(message)
    print("WARN: [Avrix] " .. tostring(message))
end

--- Panel that explains why Avrix plugins are not loading.
---@class AvrixWarningPanel
---@field message string Warning text shown in the scrolling area.
local AvrixWarningPanel = ISPanel:derive("AvrixWarningPanel")

--- Creates a new warning panel.
---@param x number Screen X position of the panel.
---@param y number Screen Y position of the panel.
---@param width number Panel width in pixels.
---@param height number Panel height in pixels.
---@param message string Warning text to display.
---@return AvrixWarningPanel panel New panel instance.
function AvrixWarningPanel:new(x, y, width, height, message)
    local o = ISPanel:new(x, y, width, height)
    setmetatable(o, self)
    self.__index = self

    o.message = message
    o.background = true
    o.backgroundColor = {r = 0.055, g = 0.058, b = 0.07, a = 0.96}
    o.borderColor = {r = 0.32, g = 0.34, b = 0.4, a = 1}
    o.moveWithMouse = false

    return o
end

--- Creates the scrollable text area and the OK button.
function AvrixWarningPanel:initialise()
    ISPanel.initialise(self)

    local textHeight = self.height - TEXT_TOP - FOOTER_HEIGHT
    local richText = ISRichTextPanel:new(UI_MARGIN, TEXT_TOP, self.width - UI_MARGIN * 2, textHeight)
    richText.background = false
    richText.autosetheight = false
    richText.clip = true
    richText.defaultFont = UIFont.Medium
    richText.marginLeft = 20
    richText.marginRight = 20
    richText.marginTop = 6
    richText.marginBottom = 10
    richText:addScrollBars()
    richText:setText(self.message)
    richText:paginate()
    for index = 1, #richText.lines do
        richText.orient[index] = "centre"
    end
    self:addChild(richText)

    local okButton = ISButton:new((self.width - BUTTON_WIDTH) / 2, self.height - 50,
        BUTTON_WIDTH, BUTTON_HEIGHT, getText("UI_Ok"), self, AvrixWarningPanel.onOK)
    UI.styleButton(okButton,
        {r = 0.17, g = 0.17, b = 0.19, a = 1},
        {r = 0.26, g = 0.26, b = 0.29, a = 1},
        {r = 0.85, g = 0.55, b = 0.2, a = 1})
    okButton:initialise()
    self:addChild(okButton)
end

--- Draws the panel title.
function AvrixWarningPanel:prerender()
    ISPanel.prerender(self)

    self:drawRect(0, 0, self.width, 3, 1, 0.85, 0.55, 0.18)
    self:drawTextCentre(UI.translate("UI_Avrix_WarningTitle", "Avrix Loader Warning"),
        self.width / 2, 18, 1, 1, 1, 1, UIFont.Large)
    self:drawRect(UI_MARGIN, 64, self.width - UI_MARGIN * 2, 1, 0.4, 0.35, 0.38, 0.45)

    local bannerY = HEADER_HEIGHT + 4
    self:drawRect(UI_MARGIN, bannerY, self.width - UI_MARGIN * 2, BANNER_HEIGHT,
        0.18, 0.9, 0.25, 0.25)
    self:drawRectBorder(UI_MARGIN, bannerY, self.width - UI_MARGIN * 2, BANNER_HEIGHT,
        0.9, 0.9, 0.25, 0.25)
    self:drawTextCentre(
        UI.translate("UI_Avrix_WarningInactive", "AVRIX LOADER IS NOT ACTIVE"),
        self.width / 2, bannerY + 8, 1, 0.45, 0.45, 1, UIFont.Medium)
end

--- Closes the warning panel.
function AvrixWarningPanel:onOK()
    self:removeFromUIManager()
end

--- Shows a warning when the mod is enabled without an active Avrix loader.
local function showWarning()
    if shown or Runtime.isActive() then
        shown = true
        return
    end

    shown = true

    local text = UI.translate("UI_Avrix_WarningLine1",
                "Avrix is enabled, but the game was started without the Avrix loader.")
            .. "\n\n"
            .. UI.translate("UI_Avrix_WarningLine2", "Close Project Zomboid.")
            .. "\n"
            .. UI.translate("UI_Avrix_WarningLine3",
                "In the Project Zomboid launch options, add the -pzexeconfig line shown in the mod description on the Steam Workshop.")
            .. "\n\n"
            .. UI.translate("UI_Avrix_WarningLine4",
                "Until the loader is active, plugins and mixins will not load.")

    local panel = AvrixWarningPanel:new(
            (getCore():getScreenWidth() - PANEL_WIDTH) / 2,
            (getCore():getScreenHeight() - PANEL_HEIGHT) / 2,
            PANEL_WIDTH,
            PANEL_HEIGHT,
            text
    )

    panel:initialise()
    panel:addToUIManager()
    panel:setAlwaysOnTop(true)
    panel:bringToTop()
    logWarn("loader warning shown")
end

--- Decrements the delay and shows the warning when the delay expires.
local function deferredShow()
    if delayFrames > 0 then
        delayFrames = delayFrames - 1
        return
    end

    Events.OnRenderTick.Remove(deferredShow)
    scheduled = false
    showWarning()
end

--- Schedules the warning after an optional frame delay.
---@param delay number|nil Frames to wait before showing the warning.
local function scheduleShow(delay)
    if shown or scheduled then
        return
    end

    scheduled = true
    delayFrames = delay or 0
    Events.OnRenderTick.Add(deferredShow)
end

--- Schedules the warning when the main menu appears.
local function onMainMenuEnter()
    scheduleShow(MENU_DELAY_FRAMES)
end

--- Schedules the warning when a game session starts.
local function onGameStart()
    scheduleShow(0)
end

Events.OnMainMenuEnter.Add(onMainMenuEnter)
Events.OnGameStart.Add(onGameStart)
