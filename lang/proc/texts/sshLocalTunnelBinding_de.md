## Bindung

Die Bindungsinformationen, die du angibst, werden direkt an den `ssh`-Client wie folgt übergeben: `-L [origin_address:]origin_port:remote_address:remote_port`.

Standardmäßig wird der Ursprung an die Loopback-Schnittstelle gebunden, wenn nicht anders angegeben. Du kannst auch beliebige Adressplatzhalter verwenden, z.B. indem du die Adresse auf `0.0.0.0` setzt, um an alle Netzwerkschnittstellen zu binden, die über IPv4 erreichbar sind. Wenn du die Adresse komplett weglässt, wird der Platzhalter `*` verwendet, der Verbindungen zu allen Netzwerkschnittstellen erlaubt. Beachte, dass manche Netzwerkschnittstellen-Notation nicht von allen Betriebssystemen unterstützt wird. Windows-Server zum Beispiel unterstützen den Platzhalter `*` nicht.
