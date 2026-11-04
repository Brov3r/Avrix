-- Shared presentation helpers for Avrix client panels.

--- Shared visual helpers used by the Avrix approval and warning panels.
---@class AvrixUI
local AvrixUI = {}

--- Returns the localized text for a UI translation key.
---@param key string Translation key declared in `Translate/<LANG>/UI.json`.
---@param fallback string English text used when the key is missing.
---@return string text Localized text, or `fallback` when the key is absent.
function AvrixUI.translate(key, fallback)
    if getTextOrNull == nil then
        return fallback
    end
    return getTextOrNull(key) or fallback
end

--- Applies the shared dark-button style used by Avrix panels.
---@param button ISButton Button to style.
---@param background table Normal background color.
---@param hover table Mouse-over background color.
---@param border table Border color.
function AvrixUI.styleButton(button, background, hover, border)
    button.backgroundColor = background
    button.backgroundColorMouseOver = hover
    button.borderColor = border
    button.textColor = {r = 1, g = 1, b = 1, a = 1}
end

return AvrixUI
