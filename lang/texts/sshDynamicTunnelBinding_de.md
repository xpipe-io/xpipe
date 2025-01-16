## Tunnelbindung

Die Bindungsinformationen, die du angibst, werden direkt an den `ssh`-Client wie folgt weitergegeben: `-D [Adresse:]Port`.

Standardmäßig wird die Adresse an die Loopback-Schnittstelle gebunden. Du kannst auch beliebige Platzhalter für die Adresse verwenden, z.B. die Adresse `0.0.0.0`, um an alle Netzwerkschnittstellen zu binden, die über IPv4 erreichbar sind. Wenn du die Adresse komplett weglässt, wird der Platzhalter `*` verwendet, der Verbindungen zu allen Netzwerkschnittstellen erlaubt. Beachte, dass manche Netzwerkschnittstellen-Notation nicht von allen Betriebssystemen unterstützt wird. Windows-Server zum Beispiel unterstützen den Platzhalter `*` nicht.
