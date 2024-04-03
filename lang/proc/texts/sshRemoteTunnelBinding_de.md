## Bindung

Die Bindungsinformationen, die du angibst, werden direkt an den `ssh`-Client wie folgt weitergegeben: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Standardmäßig wird die entfernte Quelladresse an die Loopback-Schnittstelle gebunden. Du kannst auch beliebige Adressplatzhalter verwenden, z.B. die Adresse `0.0.0.0`, um an alle über IPv4 erreichbaren Netzwerkschnittstellen zu binden. Wenn du die Adresse komplett weglässt, wird der Platzhalter `*` verwendet, der Verbindungen zu allen Netzwerkschnittstellen erlaubt. Beachte, dass manche Netzwerkschnittstellen-Notation nicht von allen Betriebssystemen unterstützt wird. Windows-Server zum Beispiel unterstützen den Platzhalter `*` nicht.
