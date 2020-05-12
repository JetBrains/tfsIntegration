[![official JetBrains project](http://jb.gg/badges/obsolete.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

IntelliJ Team Foundation Server Integration
==

The plugin provides IntelliJ integration with Microsoft Team Foundation Server.

Supported TFS versions: up to TFS 2015.
Only Server workspace type is supported.

The following features are available:
* A dedicated page under the Version Control node in the Settings/Preferences dialog.
* Ability to create and manage TFS workspaces.
* Ability to download the files from a TFS server according to the settings from a new or the existing workspace.
* Checkout from TFS Wizard.


### To build and run the plugin:
1. Clone the project and open in IDEA
2. Wait for Gradle project import.
3. Configure the required `ideaVersion` of IDE and `pluginSinceBuild/pluginUntilBuild` values in **`gradle.properties`**
4. Execute gradle **intellij/buildPlugin** task
5. Get the `idea-tfs-XXX.jar` from **`/build/distributions`**
6. Load the jar using *Settings/Preferences - Plugins*

For more details about available tasks see https://github.com/JetBrains/gradle-intellij-plugin/blob/master/README.md