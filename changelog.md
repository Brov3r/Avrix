# AvrixLoader v1.4.0

- Launch scripts updated
- Fixed the custom command manager (the @CommandChatReturn annotation was removed as unnecessary)
- Minor bugs fixed
- Added client side plugin example
- Added `.exe` executable files for Windows, allowing launch from Steam
- Added new logging system `TinyLog2`
- Added a GUI system based on `NanoVG`
- Added `onWidgetManagerInitialized`, `OnPreWidgetRender`, `OnPostWidgetRender` events
- Added default fonts: `Arial-Regular`, `Roboto-Regular`, `Montserrat-Regular`, `FontAwesome`
- Added basic
  widgets: `Panel`, `Button`, `Scrollbar`, `ScrollPanel`, `Window`, `Label`, `InputText`, `Checkbox`, `Slider`, `Popup`, `Modal`, `ComboBox`, `HorizontalBox`, `VerticalBox`, `RadioButton`
- Added `ResourceManager` and `ImageLoader`
- Event renamed: `onPostTickRenderThread` -> `onTickRenderThread`

# AvrixLoader v1.3.0

- Updated player ban and kick events
- Added new methods for banning and kicking players
- Added methods for removing items from players' inventories
- Added a method to add items to the player's inventory

# AvrixLoader v1.2.1

- Fixed copying configs from the plugin archive

# AvrixLoader v1.2.0

- Added `getPlayerAccessLevel` method
- Added `getCommandArgs` method for different prefixes
- Added `sendMessageToAdmins` method
- Added tools for working with Lua
- Added `setPlayerAccessLevel` method
- Added `getRoleName` method in `AccessLevel` class
- Fixed creation of folders when copying nested configs
- Fixed log output when creating a new config
- Updated plugin example

# AvrixLoader v1.1.1

- Fixed adding Jar plugins to classpath

# AvrixLoader v1.1.0

- Minor code refactoring
- Added the ability to get a map of registered commands
- Added command description annotation
- Added methods for getting values from command annotations
- Added new launch scripts
- Fixed plugin example
- Fixed copying configs from the plugin
- Fixed plugin dependency version checking
- Fixed the visibility area of the default ClassPoll

# AvrixLoader v1.0.0

- The first stable version!