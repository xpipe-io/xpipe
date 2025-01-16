### SSH-Konfigurationen

XPipe lädt alle Hosts und wendet alle Einstellungen an, die du in der ausgewählten Datei konfiguriert hast. Wenn du also eine Konfigurationsoption entweder auf globaler oder hostspezifischer Basis angibst, wird sie automatisch auf die von XPipe aufgebaute Verbindung angewendet.

Wenn du mehr über die Verwendung von SSH-Konfigurationen erfahren möchtest, kannst du `man ssh_config` verwenden oder diese [Anleitung](https://www.ssh.com/academy/ssh/config) lesen.

### Identitäten

Beachte, dass du hier auch eine `IdentityFile` Option angeben kannst. Wenn du hier eine Identität angibst, werden alle anderen Identitäten, die weiter unten angegeben werden, ignoriert.

### X11-Weiterleitung

Wenn hier eine Option für die X11-Weiterleitung angegeben wird, versucht XPipe automatisch, die X11-Weiterleitung unter Windows über WSL einzurichten.