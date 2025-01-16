## Temporäre Container

Hiermit wird ein temporärer Container mit dem angegebenen Image gestartet, der automatisch entfernt wird, sobald er gestoppt wird. Der Container läuft auch dann weiter, wenn im Container-Image kein Befehl angegeben ist, der ausgeführt werden soll.

Das kann nützlich sein, wenn du schnell eine bestimmte Umgebung mit einem bestimmten Container-Image einrichten willst. Du kannst den Container dann wie gewohnt in XPipe betreten, deine Operationen durchführen und den Container stoppen, sobald er nicht mehr benötigt wird. Er wird dann automatisch entfernt.