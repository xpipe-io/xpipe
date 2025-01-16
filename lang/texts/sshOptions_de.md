## SSH-Konfigurationen

Hier kannst du alle SSH-Optionen angeben, die an die Verbindung übergeben werden sollen.
Während einige Optionen für einen erfolgreichen Verbindungsaufbau erforderlich sind, wie `HostName`,
sind viele andere Optionen rein optional.

Um einen Überblick über alle möglichen Optionen zu bekommen, kannst du [`man ssh_config`](https://linux.die.net/man/5/ssh_config) verwenden oder diesen [guide](https://www.ssh.com/academy/ssh/config) lesen.
Die genaue Anzahl der unterstützten Optionen hängt ausschließlich von deinem installierten SSH-Client ab.

### Formatierung

Der Inhalt hier entspricht einem Host-Abschnitt in einer SSH-Konfigurationsdatei.
Beachte, dass du den `Host`-Schlüssel nicht explizit definieren musst, da dies automatisch gemacht wird.

Wenn du mehr als einen Host-Abschnitt definieren willst, z. B. bei abhängigen Verbindungen wie einem Proxy-Jump-Host, der von einem anderen Config-Host abhängt, kannst du hier auch mehrere Host-Einträge definieren. XPipe wird dann den ersten Host-Eintrag starten.

Du musst keine Formatierung mit Leerzeichen oder Einrückung vornehmen, das ist für die Funktion nicht erforderlich.

Beachte, dass du darauf achten musst, alle Werte in Anführungszeichen zu setzen, wenn sie Leerzeichen enthalten, sonst werden sie falsch übergeben.

### Identitätsdateien

Beachte, dass du hier auch eine `IdentityFile` Option angeben kannst.
Wenn diese Option hier angegeben wird, werden alle anderen Optionen für die schlüsselbasierte Authentifizierung weiter unten ignoriert.

Wenn du dich für eine Identitätsdatei entscheidest, die im XPipe-Git-Vault verwaltet wird, kannst du das ebenfalls tun.
XPipe erkennt gemeinsam genutzte Identitätsdateien und passt den Dateipfad automatisch auf jedem System an, auf dem du den Git-Depot geklont hast.
