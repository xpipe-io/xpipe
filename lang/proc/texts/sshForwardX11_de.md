## X11 Weiterleitung

Wenn diese Option aktiviert ist, wird die SSH-Verbindung mit eingerichteter X11-Weiterleitung gestartet. Unter Linux funktioniert das normalerweise sofort und muss nicht eingerichtet werden. Unter macOS brauchst du einen X11-Server wie [XQuartz](https://www.xquartz.org/), der auf deinem lokalen Rechner läuft.

### X11 unter Windows

Mit XPipe kannst du die X11-Fähigkeiten von WSL2 für deine SSH-Verbindung nutzen. Das Einzige, was du dafür brauchst, ist eine [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) Distribution, die auf deinem lokalen System installiert ist. XPipe wählt nach Möglichkeit automatisch eine kompatible installierte Distribution aus, du kannst aber auch eine andere im Einstellungsmenü verwenden.

Das bedeutet, dass du keinen separaten X11-Server unter Windows installieren musst. Wenn du jedoch ohnehin einen verwendest, erkennt XPipe dies und verwendet den aktuell laufenden X11-Server.
