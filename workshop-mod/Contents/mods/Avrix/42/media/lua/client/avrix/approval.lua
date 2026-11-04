require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISScrollingListBox"

local Runtime = require "avrix/runtime"
local UI = require "avrix/ui"

--- Frames to wait before showing the panel on the main menu.
local MENU_DELAY_FRAMES = 30
--- Panel width in pixels.
local PANEL_WIDTH = 760
--- Panel height in pixels.
local PANEL_HEIGHT = 620
--- Space between the panel edge and its content.
local UI_MARGIN = 16
--- Height reserved for the title, subtitle, and hint.
local HEADER_HEIGHT = 112
--- Height reserved for the status text and buttons.
local FOOTER_HEIGHT = 90
--- Height of one plugin row in the list.
local ROW_HEIGHT = 86
--- File that remembers the last applied hash for each plugin.
local SHA_HISTORY_FILE = "avrix/plugin-sha-history.txt"
--- Height of a panel button.
local BUTTON_HEIGHT = 32
--- Badge color for a newly discovered plugin.
local STATE_NEW_COLOR = {r = 0.45, g = 0.85, b = 0.55, a = 1}
--- Badge color for a plugin whose jar changed.
local STATE_CHANGED_COLOR = {r = 1.0, g = 0.75, b = 0.3, a = 1}
--- Badge color for a plugin with no decision yet.
local DECISION_NONE_COLOR = {r = 0.55, g = 0.58, b = 0.64, a = 1}
--- Badge color for a plugin the player allows.
local DECISION_ALLOW_COLOR = {r = 0.45, g = 0.9, b = 0.55, a = 1}
--- Badge color for a plugin the player denies.
local DECISION_DENY_COLOR = {r = 1.0, g = 0.42, b = 0.45, a = 1}

--- Whether the approval panel has already been scheduled or shown.
local shown = false
--- Whether a deferred show is currently waiting on the render event.
local scheduled = false
--- Frames remaining before the deferred show runs.
local delayFrames = 0

--- Writes an informational Avrix line.
---@param message string Message to write.
local function logInfo(message)
    print("INFO: [Avrix] " .. tostring(message))
end

--- Writes an error Avrix line.
---@param message string Message to write.
local function logError(message)
    print("ERROR: [Avrix] " .. tostring(message))
end

--- Splits one tab-separated line into fields.
---@param line string Line read from the pending-plugin file.
---@return table fields Field values in file order.
local function splitTabs(line)
    local parts = {}
    local start = 1
    while true do
        local tab = string.find(line, "\t", start, true)
        if not tab then
            table.insert(parts, string.sub(line, start))
            break
        end
        table.insert(parts, string.sub(line, start, tab - 1))
        start = tab + 1
    end
    return parts
end

--- Builds the key used to remember one plugin hash.
---@param modId string Source mod identifier.
---@param pluginId string Plugin identifier.
---@return string key Combined key for the SHA history map.
local function shaKey(modId, pluginId)
    return (modId or "") .. "\t" .. (pluginId or "")
end

--- Builds the author, version, and Workshop line shown under the jar path.
---@param author string|nil Plugin author name from mod metadata.
---@param version string|nil Source mod version.
---@param workshopId string|nil Steam Workshop item id.
---@return string|nil text Combined metadata line, or nil when all values are missing.
local function formatMeta(author, version, workshopId)
    local parts = {}
    if author ~= nil and author ~= "" then
        table.insert(parts, UI.translate("UI_Avrix_Author", "Author") .. ": " .. author)
    end
    if version ~= nil and version ~= "" then
        table.insert(parts, UI.translate("UI_Avrix_Version", "Version") .. ": " .. version)
    end
    if workshopId ~= nil and workshopId ~= "" then
        table.insert(parts, UI.translate("UI_Avrix_Workshop", "Workshop") .. ": " .. workshopId)
    end
    if #parts == 0 then
        return nil
    end
    return table.concat(parts, "   |   ")
end

--- Shortens a SHA-256 value for the row display.
---@param sha string|nil Full SHA-256 value.
---@return string|nil short First 16 characters, or nil when the value is empty.
local function shortSha(sha)
    if sha == nil or sha == "" then
        return nil
    end
    return string.sub(sha, 1, 16)
end

--- Builds the old/new hash line shown below the metadata line.
---@param oldSha string|nil Previously applied hash.
---@param newSha string|nil Current hash.
---@return string|nil text Combined hash line, or nil when no hash is available.
local function formatSha(oldSha, newSha)
    local parts = {}
    local shortOld = shortSha(oldSha)
    local shortNew = shortSha(newSha)
    if shortOld ~= nil and shortOld ~= shortNew then
        table.insert(parts, UI.translate("UI_Avrix_ShaOld", "Old SHA") .. ": " .. shortOld)
    end
    if shortNew ~= nil then
        table.insert(parts, UI.translate("UI_Avrix_ShaNew", "New SHA") .. ": " .. shortNew)
    end
    if #parts == 0 then
        return nil
    end
    return table.concat(parts, "   |   ")
end

--- Returns the badge text for one plugin decision.
---@param decision string|nil Either "ALLOW", "DENY", or nil when undecided.
---@return string text Localized decision text.
local function decisionText(decision)
    if decision == "ALLOW" then
        return UI.translate("UI_Avrix_Allowed", "Allow")
    end
    if decision == "DENY" then
        return UI.translate("UI_Avrix_Denied", "Deny")
    end
    return UI.translate("UI_Avrix_Undecided", "Skip")
end

--- Returns the badge color for one plugin decision.
---@param decision string|nil Either "ALLOW", "DENY", or nil when undecided.
---@return table color Badge color.
local function decisionColor(decision)
    if decision == "ALLOW" then
        return DECISION_ALLOW_COLOR
    end
    if decision == "DENY" then
        return DECISION_DENY_COLOR
    end
    return DECISION_NONE_COLOR
end

--- Reads the last applied hash for every plugin.
---@return table history Map of mod/plugin keys to SHA-256 values.
local function readShaHistory()
    local reader = getFileReader(SHA_HISTORY_FILE, false)
    if not reader then
        return {}
    end

    local history = {}
    local ok, err = pcall(function()
        while true do
            local line = reader:readLine()
            if not line then
                break
            end
            local parts = splitTabs(line)
            if parts[1] == "SHA" and #parts >= 4 then
                history[shaKey(parts[2], parts[3])] = parts[4]
            end
        end
    end)
    pcall(function()
        reader:close()
    end)

    if not ok then
        logError("failed to read SHA history: " .. tostring(err))
    end

    return history
end

--- Stores the newly applied hashes in the SHA history file.
---@param history table Map of mod/plugin keys to SHA-256 values.
---@param entries table Plugin entries whose current hashes were applied.
local function saveShaHistory(history, entries)
    for _, entry in ipairs(entries) do
        if entry.sha256 ~= nil and entry.sha256 ~= "" then
            history[shaKey(entry.modId, entry.pluginId)] = entry.sha256
        end
    end

    local writer = getFileWriter(SHA_HISTORY_FILE, true, false)
    if not writer then
        logError("failed to open SHA history file for writing")
        return
    end

    local ok, err = pcall(function()
        writer:writeln("VERSION\t1")
        for key, sha in pairs(history) do
            local separator = string.find(key, "\t", 1, true)
            if separator ~= nil then
                writer:writeln("SHA\t" .. string.sub(key, 1, separator - 1) .. "\t"
                        .. string.sub(key, separator + 1) .. "\t" .. sha)
            end
        end
        writer:close()
    end)
    if not ok then
        pcall(function()
            writer:close()
        end)
        logError("failed to write SHA history: " .. tostring(err))
    end
end

--- Reads plugin entries that are waiting for a user decision.
---@return table entries Pending plugin entry records.
local function readPendingEntries()
    local reader = getFileReader("avrix/plugin-pending.txt", false)
    if not reader then
        return {}
    end

    local entries = {}
    local ok, err = pcall(function()
        while true do
            local line = reader:readLine()
            if not line then
                break
            end

            local parts = splitTabs(line)
            if parts[1] == "ENTRY" and #parts >= 6 then
                table.insert(entries, {
                    modId      = parts[2] or "",
                    pluginId   = parts[3] or "",
                    state      = parts[4] or "NEW",
                    sha256     = parts[5] or "",
                    jarPath    = parts[6] or "",
                    pluginName = parts[7] or parts[3] or "",
                })
            end
        end
    end)
    pcall(function()
        reader:close()
    end)

    if not ok then
        logError("failed to read pending plugins: " .. tostring(err))
    end

    return entries
end

--- Scrolling list that draws one plugin entry on two lines.
---@class AvrixApprovalList
local AvrixApprovalList = ISScrollingListBox:derive("AvrixApprovalList")

--- Creates the plugin list.
---@param x number Screen X position of the list.
---@param y number Screen Y position of the list.
---@param width number List width in pixels.
---@param height number List height in pixels.
---@return AvrixApprovalList list New list instance.
function AvrixApprovalList:new(x, y, width, height)
    local o = ISScrollingListBox:new(x, y, width, height)
    setmetatable(o, self)
    self.__index = self

    o.itemheight = ROW_HEIGHT
    o.backgroundColor = {r = 0.02, g = 0.02, b = 0.03, a = 0.35}
    o.borderColor = {r = 0.3, g = 0.32, b = 0.36, a = 0.8}
    o.drawBorder = true
    o.selected = -1

    return o
end

--- Draws one plugin entry with its name, state, decision, and jar path.
---@param y number Vertical offset of the row inside the list.
---@param item table List item that wraps one plugin entry.
---@param alt boolean Whether this row is an alternating row.
---@return number nextY Vertical offset of the next row.
function AvrixApprovalList:doDrawItem(y, item, alt)
    local entry = item.item
    local height = item.height or ROW_HEIGHT
    local badgeColor = decisionColor(entry.decision)
    local text = item.decisionText or decisionText(entry.decision)

    if alt then
        self:drawRect(0, y, self:getWidth(), height, 0.035, 1, 1, 1)
    end
    if self.selected == item.index then
        self:drawRect(0, y, self:getWidth(), height, 0.2, 0.35, 0.45, 0.7)
    elseif self.mouseoverselected == item.index and not self:isMouseOverScrollBar() then
        self:drawRect(0, y, self:getWidth(), height, 0.07, 1, 1, 1)
    end
    self:drawRect(0, y, 3, height, 1, badgeColor.r, badgeColor.g, badgeColor.b)

    local name = entry.pluginName or entry.pluginId or ""
    local nameWidth = item.nameWidth or getTextManager():MeasureStringX(UIFont.Medium, name)
    self:drawText(name, 14, y + 3, 0.96, 0.96, 0.96, 1, UIFont.Medium)

    local badgeY = y + 3
    local badgeHeight = 18
    local state = entry.state or "NEW"
    local stateColor = state == "CHANGED" and STATE_CHANGED_COLOR or STATE_NEW_COLOR
    local stateText = item.stateText
            or (state == "CHANGED" and UI.translate("UI_Avrix_StateChanged", "CHANGED")
            or UI.translate("UI_Avrix_StateNew", "NEW"))
    local stateWidth = getTextManager():MeasureStringX(UIFont.Small, stateText) + 12
    local stateX = 14 + nameWidth + 8

    self:drawRect(stateX, badgeY, stateWidth, badgeHeight, 0.3, stateColor.r, stateColor.g, stateColor.b)
    self:drawRectBorder(stateX, badgeY, stateWidth, badgeHeight, 1, stateColor.r, stateColor.g, stateColor.b)
    self:drawTextCentre(stateText, stateX + stateWidth / 2, badgeY + 2,
        stateColor.r, stateColor.g, stateColor.b, 1, UIFont.Small)

    local decisionWidth = getTextManager():MeasureStringX(UIFont.Small, text) + 12
    local decisionX = stateX + stateWidth + 6
    self:drawRect(decisionX, badgeY, decisionWidth, badgeHeight, 0.3,
        badgeColor.r, badgeColor.g, badgeColor.b)
    self:drawRectBorder(decisionX, badgeY, decisionWidth, badgeHeight, 1,
        badgeColor.r, badgeColor.g, badgeColor.b)
    self:drawTextCentre(text, decisionX + decisionWidth / 2, badgeY + 2,
        badgeColor.r, badgeColor.g, badgeColor.b, 1, UIFont.Small)

    local path = item.displayPath or entry.jarPath or ""
    self:drawText(path, 14, y + 28, 0.55, 0.57, 0.62, 1, UIFont.Small)

    if item.metaText ~= nil then
        self:drawText(item.metaText, 14, y + 47, 0.45, 0.48, 0.54, 1, UIFont.Small)
    end
    if item.shaText ~= nil then
        self:drawText(item.shaText, 14, y + 66, 0.38, 0.42, 0.5, 1, UIFont.Small)
    end

    return y + height
end

--- Cycles skip → allow → deny for the clicked plugin row.
---@param x number Mouse X position inside the list.
---@param y number Mouse Y position inside the list.
function AvrixApprovalList:onMouseDown(x, y)
    if self:isMouseOverScrollBar() then
        return
    end

    local row = self:rowAt(x, y)
    local item = self.items[row]
    if item == nil then
        return
    end

    local entry = item.item
    if entry.decision == nil then
        entry.decision = "ALLOW"
    elseif entry.decision == "ALLOW" then
        entry.decision = "DENY"
    else
        entry.decision = nil
    end
    item.decisionText = decisionText(entry.decision)
    self.selected = row
    getSoundManager():playUISound("UISelectListItem")
end

--- Panel that asks the player to approve or deny pending Java plugins.
---@class AvrixApprovalPanel
---@field entries table Pending plugin entry records shown by the panel.
---@field statusText string|nil Result message shown above the buttons.
---@field statusColor table|nil Color used for the status message.
---@field shaHistory table Map of mod/plugin keys to previously applied hashes.
---@field list AvrixApprovalList|nil Scrollable plugin list.
---@field allowButton ISButton|nil Button that sets every pending plugin to allow.
---@field denyButton ISButton|nil Button that sets every pending plugin to deny.
---@field applyButton ISButton|nil Button that writes all selected decisions.
---@field laterButton ISButton|nil Button that hides the panel until the next session.
local AvrixApprovalPanel = ISPanel:derive("AvrixApprovalPanel")

--- Creates a new approval panel.
---@param x number Screen X position of the panel.
---@param y number Screen Y position of the panel.
---@param width number Panel width in pixels.
---@param height number Panel height in pixels.
---@param entries table Pending plugin entry records to display.
---@return AvrixApprovalPanel panel New panel instance.
function AvrixApprovalPanel:new(x, y, width, height, entries)
    local o = ISPanel:new(x, y, width, height)
    setmetatable(o, self)
    self.__index = self

    o.entries = entries
    o.statusText = nil
    o.statusColor = nil
    o.background = true
    o.backgroundColor = {r = 0.055, g = 0.058, b = 0.07, a = 0.96}
    o.borderColor = {r = 0.32, g = 0.34, b = 0.4, a = 1}
    o.moveWithMouse = false

    return o
end

--- Creates the plugin list and the panel buttons.
function AvrixApprovalPanel:initialise()
    ISPanel.initialise(self)

    local listHeight = self.height - HEADER_HEIGHT - FOOTER_HEIGHT
    self.list = AvrixApprovalList:new(UI_MARGIN, HEADER_HEIGHT, self.width - UI_MARGIN * 2, listHeight)
    self.list:initialise()
    self.list:instantiate()
    self:addChild(self.list)
    self.shaHistory = readShaHistory()

    for _, entry in ipairs(self.entries) do
        local item = self.list:addItem(entry.pluginName or entry.pluginId or "", entry)
        local path = entry.jarPath or ""
        if #path > 78 then
            path = "..." .. string.sub(path, -75)
        end
        item.displayPath = path
        item.nameWidth = getTextManager():MeasureStringX(UIFont.Medium, entry.pluginName or entry.pluginId or "")
        item.stateText = entry.state == "CHANGED" and UI.translate("UI_Avrix_StateChanged", "CHANGED")
                or UI.translate("UI_Avrix_StateNew", "NEW")
        item.decisionText = decisionText(entry.decision)

        local author = entry.author
        local version = entry.modVersion
        local workshopId = entry.workshopId
        if getModInfoByID ~= nil then
            local ok, modInfo = pcall(getModInfoByID, entry.modId)
            if ok and modInfo ~= nil then
                if author == nil or author == "" then
                    author = modInfo:getAuthor()
                end
                if version == nil or version == "" then
                    version = modInfo:getModVersion()
                end
                if workshopId == nil or workshopId == "" then
                    workshopId = modInfo:getWorkshopID()
                end
            end
        end
        item.metaText = formatMeta(author, version, workshopId)

        local oldSha = nil
        if entry.state == "CHANGED" then
            oldSha = entry.oldSha or self.shaHistory[shaKey(entry.modId, entry.pluginId)]
        end
        if oldSha == entry.sha256 then
            oldSha = nil
        end
        item.shaText = formatSha(oldSha, entry.sha256)
    end

    local buttonY = self.height - 50
    local buttonWidth = 160

    self.allowButton = ISButton:new(UI_MARGIN, buttonY, buttonWidth, BUTTON_HEIGHT,
        UI.translate("UI_Avrix_AllowAll", "Allow all"), self, AvrixApprovalPanel.onAllow)
    UI.styleButton(self.allowButton,
        {r = 0.14, g = 0.3, b = 0.17, a = 1},
        {r = 0.2, g = 0.43, b = 0.24, a = 1},
        {r = 0.35, g = 0.7, b = 0.42, a = 1})
    self.allowButton:initialise()
    self:addChild(self.allowButton)

    self.denyButton = ISButton:new(UI_MARGIN + buttonWidth + 10, buttonY, buttonWidth, BUTTON_HEIGHT,
        UI.translate("UI_Avrix_DenyAll", "Deny all"), self, AvrixApprovalPanel.onDeny)
    UI.styleButton(self.denyButton,
        {r = 0.34, g = 0.13, b = 0.14, a = 1},
        {r = 0.48, g = 0.19, b = 0.2, a = 1},
        {r = 0.78, g = 0.33, b = 0.35, a = 1})
    self.denyButton:initialise()
    self:addChild(self.denyButton)

    self.applyButton = ISButton:new(UI_MARGIN + (buttonWidth + 10) * 2, buttonY, buttonWidth, BUTTON_HEIGHT,
        UI.translate("UI_Avrix_Apply", "Apply"), self, AvrixApprovalPanel.onApply)
    UI.styleButton(self.applyButton,
        {r = 0.13, g = 0.22, b = 0.34, a = 1},
        {r = 0.19, g = 0.32, b = 0.48, a = 1},
        {r = 0.4, g = 0.6, b = 0.9, a = 1})
    self.applyButton:initialise()
    self:addChild(self.applyButton)

    self.laterButton = ISButton:new(self.width - UI_MARGIN - 140, buttonY, 140, BUTTON_HEIGHT,
        UI.translate("UI_Avrix_Later", "Later"), self, AvrixApprovalPanel.onLater)
    UI.styleButton(self.laterButton,
        {r = 0.15, g = 0.16, b = 0.18, a = 1},
        {r = 0.23, g = 0.24, b = 0.27, a = 1},
        {r = 0.42, g = 0.44, b = 0.5, a = 1})
    self.laterButton:initialise()
    self:addChild(self.laterButton)
end

--- Draws the panel header and the current status message.
function AvrixApprovalPanel:prerender()
    ISPanel.prerender(self)

    self:drawRect(0, 0, self.width, 3, 1, 0.85, 0.55, 0.18)
    self:drawTextCentre(UI.translate("UI_Avrix_ApprovalTitle", "Avrix Java Plugin Approval"),
        self.width / 2, 20, 1, 1, 1, 1, UIFont.Large)
    self:drawTextCentre(
        UI.translate("UI_Avrix_ApprovalSubtitle", "Java plugins run with full system access. Review each plugin, then apply."),
        self.width / 2, 50, 0.62, 0.64, 0.7, 1, UIFont.Small)
    self:drawTextCentre(
        UI.translate("UI_Avrix_ApprovalHint", "Click a plugin to cycle: skip, allow, deny."),
        self.width / 2, 70, 0.78, 0.65, 0.38, 1, UIFont.Small)
    self:drawRect(UI_MARGIN, 94, self.width - UI_MARGIN * 2, 1, 0.4, 0.35, 0.38, 0.45)

    if self.statusText then
        local color = self.statusColor or {r = 0.75, g = 0.78, b = 0.82, a = 1}
        self:drawText(self.statusText, UI_MARGIN, self.height - FOOTER_HEIGHT + 4,
            color.r, color.g, color.b, color.a, UIFont.Small)
    end
end

--- Saves the selected decisions and leaves undecided plugins pending.
---@return boolean saved True when the decision file and SHA history were written.
function AvrixApprovalPanel:writeDecisions()
    local decided = {}
    for _, entry in ipairs(self.entries) do
        if entry.decision ~= nil then
            table.insert(decided, entry)
        end
    end

    if #decided == 0 then
        self.statusText = UI.translate("UI_Avrix_NoDecisions", "No decisions selected. Nothing was saved.")
        self.statusColor = {r = 0.85, g = 0.72, b = 0.35, a = 1}
        return false
    end

    local writer = getFileWriter("avrix/plugin-decisions.txt", true, false)
    if not writer then
        self.statusText = UI.translate("UI_Avrix_SaveFailed", "Failed to save decision. Check the console log.")
        self.statusColor = {r = 1, g = 0.45, b = 0.45, a = 1}
        logError("failed to open plugin decision file for writing")
        return false
    end

    local ok, err = pcall(function()
        writer:writeln("VERSION\t1")
        for _, entry in ipairs(decided) do
            local allow = entry.decision == "ALLOW"
            writer:writeln("DECISION\t" .. entry.modId .. "\t" .. entry.sha256 .. "\t" .. tostring(allow))
        end
        writer:close()
    end)
    if not ok then
        pcall(function()
            writer:close()
        end)
        self.statusText = UI.translate("UI_Avrix_SaveFailed", "Failed to save decision. Check the console log.")
        self.statusColor = {r = 1, g = 0.45, b = 0.45, a = 1}
        logError("failed to write plugin decisions: " .. tostring(err))
        return false
    end

    saveShaHistory(self.shaHistory, decided)

    self.statusText = UI.translate("UI_Avrix_DecisionSaved", "Decision saved. Restart Project Zomboid to apply changes.")
    self.statusColor = {r = 0.55, g = 1, b = 0.62, a = 1}
    self.allowButton.enable = false
    self.denyButton.enable = false
    self.applyButton.enable = false
    logInfo("plugin decisions saved")
    return true
end

--- Sets every plugin decision to allow or deny.
---@param decision string Either "ALLOW" or "DENY".
function AvrixApprovalPanel:setAllDecisions(decision)
    for _, item in ipairs(self.list.items) do
        item.item.decision = decision
        item.decisionText = decisionText(decision)
    end
    self.statusText = nil
    self.statusColor = nil
end

--- Sets every pending plugin to allow.
function AvrixApprovalPanel:onAllow()
    self:setAllDecisions("ALLOW")
end

--- Sets every pending plugin to deny.
function AvrixApprovalPanel:onDeny()
    self:setAllDecisions("DENY")
end

--- Writes the selected decisions and closes the panel when saving succeeds.
function AvrixApprovalPanel:onApply()
    if self:writeDecisions() then
        self:removeFromUIManager()
    end
end

--- Hides the panel without writing a decision.
function AvrixApprovalPanel:onLater()
    self:removeFromUIManager()
end

--- Shows the approval panel when pending plugins exist.
local function showApproval()
    if shown or not Runtime.isActive() then
        return
    end

    local entries = readPendingEntries()
    if #entries == 0 then
        shown = true
        return
    end

    shown = true

    local panel = AvrixApprovalPanel:new(
            (getCore():getScreenWidth() - PANEL_WIDTH) / 2,
            (getCore():getScreenHeight() - PANEL_HEIGHT) / 2,
            PANEL_WIDTH,
            PANEL_HEIGHT,
            entries
    )

    panel:initialise()
    panel:addToUIManager()
    panel:setAlwaysOnTop(true)
    panel:bringToTop()
    logInfo("plugin approval panel shown")
end

--- Decrements the delay and shows the panel when the delay expires.
local function deferredShow()
    if delayFrames > 0 then
        delayFrames = delayFrames - 1
        return
    end

    Events.OnRenderTick.Remove(deferredShow)
    scheduled = false
    showApproval()
end

--- Schedules the approval panel after an optional frame delay.
---@param delay number|nil Frames to wait before showing the panel.
local function scheduleShow(delay)
    if shown or scheduled then
        return
    end

    scheduled = true
    delayFrames = delay or 0
    Events.OnRenderTick.Add(deferredShow)
end

--- Schedules the approval panel when the main menu appears.
local function onMainMenuEnter()
    scheduleShow(MENU_DELAY_FRAMES)
end

--- Schedules the approval panel when a game session starts.
local function onGameStart()
    scheduleShow(0)
end

Events.OnMainMenuEnter.Add(onMainMenuEnter)
Events.OnGameStart.Add(onGameStart)
