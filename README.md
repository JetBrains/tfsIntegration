[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

IntelliJ Team Foundation Server Integration
==

The plugin provides IntelliJ integration with Microsoft Team Foundation Server.

Supported TFS versions: up to TFS 2015.

The following features are available:
* Dedicated page under the Version Control node in the Settings/Preferences dialog.
* Ability to create and manage TFS workspaces.>
* Ability to download the files from a TFS server according to the settings from a new or the existing workspace.
* Checkout from TFS Wizard.


###To build and run the plugin:
1. Clone the project and open in IDEA (tfsintegration.iml should be used)
2. Configure IntelliJ Platform Plugin SDK called **IntelliJ IDEA SDK** pointing to the existing IDEA installation using Project Settings
3. Run using provided **IDEA** run configuration
4. After applying hte needed changes use *Build - Prepare Plugin Module for deployment* to generate the jar
5. Load the jar using *Settings/Preferences - Plugins*