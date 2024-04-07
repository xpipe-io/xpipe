## SSH-Konfigurationen

Hier kannst du alle SSH-Optionen angeben, die an die Verbindung übergeben werden sollen.
Während einige Optionen für einen erfolgreichen Verbindungsaufbau unbedingt erforderlich sind, wie z.B. `HostName`,
sind viele andere Optionen rein optional.

Um einen Überblick über alle möglichen Optionen zu bekommen, kannst du [`man ssh_config`](https://linux.die.net/man/5/ssh_config) verwenden oder diesen [guide](https://www.ssh.com/academy/ssh/config) lesen.
Die genaue Anzahl der unterstützten Optionen hängt ausschließlich von deinem installierten SSH-Client ab.

### Formatierung

Der Inhalt hier entspricht einem Host-Abschnitt in einer SSH-Konfigurationsdatei.
Beachte, dass du den `Host`-Eintrag nicht explizit definieren musst, denn das wird automatisch erledigt.

Wenn du mehr als einen Host-Abschnitt definieren willst, z. B. bei abhängigen Verbindungen wie einem Proxy-Jump-Host, der von einem anderen Config-Host abhängt, kannst du auch mehrere Host-Einträge definieren. Es wird dann der erste Host als Verbindung genutzt.

Du musst keine Formatierung mit Leerzeichen oder Einrückung vornehmen, das ist für die Funktion nicht erforderlich.

### Identitäten

Beachte, dass du hier auch eine `IdentityFile` Option angeben kannst.
Wenn diese Option hier angegeben wird, werden alle anderen Optionen für die schlüsselbasierte Authentifizierung weiter unten ignoriert.
