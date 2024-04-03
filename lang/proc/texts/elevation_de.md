## Elevation

Der Prozess der Berechtigungserweiterung ist betriebssystemspezifisch.

### Linux & macOS

Jeder erweiterte Befehl wird mit `sudo` ausgeführt. Das optionale `sudo` Passwort wird bei Bedarf über XPipe abgefragt. Du kannst in den Einstellungen festlegen, ob du dein Passwort jedes Mal eingeben willst, wenn es gebraucht wird, oder ob du es für die aktuelle Sitzung zwischenspeichern willst.

### Windows

Unter Windows ist es nicht möglich, die Berechtigungen eines untergeordneten Prozesses zu erhöhen, wenn der übergeordnete Prozess nicht ebenfalls mit erhöhten Berechtigungen ausgeführt wird. Wenn XPipe also nicht als Administrator ausgeführt wird, kannst du lokal keine Berechtigungserweiterung nutzen. Bei Fernverbindungen muss das verbundene Benutzerkonto mit Administratorrechten ausgestattet sein.