# RDP Remote-Anwendungen

Du kannst RDP-Verbindungen in XPipe nutzen, um entfernte Anwendungen und Skripte schnell zu starten, ohne einen vollständigen Desktop zu öffnen. Damit das funktioniert, musst du allerdings die Liste der zugelassenen Fernanwendungen auf deinem Server bearbeiten, da es sich um ein RDP-Verfahren handelt.

## RDP-Zulassungslisten

Ein RDP-Server verwendet das Konzept der Zulassungslisten, um den Start von Anwendungen zu steuern. Das bedeutet, dass der direkte Start von Fernanwendungen fehlschlägt, es sei denn, die Zulassungsliste ist deaktiviert oder es wurden explizit bestimmte Anwendungen zur Zulassungsliste hinzugefügt.

Du findest die Einstellungen für die Erlaubnisliste in der Registrierung deines Servers unter `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Alle Anwendungen zulassen

Du kannst die Zulassen-Liste deaktivieren, damit alle Remote-Anwendungen direkt von XPipe aus gestartet werden können. Dazu kannst du den folgenden Befehl auf deinem Server in der PowerShell ausführen: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Hinzufügen von erlaubten Anwendungen

Alternativ kannst du auch einzelne Remote-Anwendungen zu der Liste hinzufügen. Dann kannst du die aufgelisteten Anwendungen direkt von XPipe aus starten.

Erstelle unter dem Schlüssel `Anwendungen` der `TSAppAllowList` einen neuen Schlüssel mit einem beliebigen Namen. Die einzige Bedingung für den Namen ist, dass er innerhalb der Kinder des Schlüssels "Anwendungen" eindeutig ist. Dieser neue Schlüssel muss die folgenden Werte enthalten: `Name`, `Pfad` und `CommandLineSetting`. Du kannst dies in der PowerShell mit den folgenden Befehlen tun:

```
$appName="Notepad"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Wert "$appPath" -Force
New-ItemProperty -Pfad "$regKey\$appName" -Name "CommandLineSetting" -Wert "1" -PropertyType DWord -Force
```

Wenn du XPipe auch das Ausführen von Skripten und das Öffnen von Terminalsitzungen erlauben willst, musst du `C:\Windows\System32\cmd.exe` ebenfalls in die Erlaubnisliste aufnehmen. 

## Sicherheitsüberlegungen

Das macht deinen Server in keiner Weise unsicher, denn du kannst die gleichen Anwendungen immer manuell ausführen, wenn du eine RDP-Verbindung startest. Die Zulassen-Listen sollen eher verhindern, dass Clients ohne Benutzereingabe sofort jede Anwendung ausführen. Letzten Endes liegt es an dir, ob du XPipe in dieser Hinsicht vertraust. Du kannst diese Verbindung ganz einfach starten. Das ist nur dann sinnvoll, wenn du eine der erweiterten Desktop-Integrationsfunktionen von XPipe nutzen willst.
