## Elevation

Der Prozess der Elevation ist betriebssystemspezifisch.

### Linux & macOS

Jeder erhobene Befehl wird mit `sudo` ausgeführt. Das optionale `sudo` Passwort wird bei Bedarf über XPipe abgefragt.
Du kannst in den Einstellungen festlegen, ob du dein Passwort jedes Mal eingeben willst, wenn es gebraucht wird, oder ob du es für die aktuelle Sitzung zwischenspeichern willst.

### Windows

Unter Windows ist es nicht möglich, einen untergeordneten Prozess zu aktivieren, wenn der übergeordnete Prozess nicht auch aktiviert ist.
Wenn XPipe also nicht als Administrator ausgeführt wird, kannst du lokal keine Berechtigungserweiterung nutzen.
Bei Fernverbindungen muss das verbundene Benutzerkonto über Administratorrechte verfügen.