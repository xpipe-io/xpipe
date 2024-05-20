# Zusätzliche RDP-Optionen

Wenn du deine Verbindung noch weiter anpassen möchtest, kannst du das tun, indem du RDP-Eigenschaften so bereitstellst, wie sie in .rdp-Dateien enthalten sind. Eine vollständige Liste der verfügbaren Eigenschaften findest du unter https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

Diese Optionen haben das Format `Option:Typ:Wert`. Um zum Beispiel die Größe des Desktop-Fensters anzupassen, kannst du die folgende Konfiguration übergeben:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
