## Ausführungsarten

Es gibt zwei verschiedene Ausführungsarten, wenn XPipe eine Verbindung zu einem System herstellt.

### Im Hintergrund

Die erste Verbindung zu einem System wird im Hintergrund in einer stummen Terminalsitzung hergestellt.

Blockierende Befehle, die Benutzereingaben erfordern, können den Shell-Prozess einfrieren, wenn XPipe ihn intern zuerst im Hintergrund startet. Um dies zu vermeiden, solltest du diese blockierenden Befehle nur im Terminalmodus aufrufen.

Der Dateibrowser z. B. verwendet für seine Operationen ausschließlich den dummen Hintergrundmodus. Wenn du also möchtest, dass deine Skriptumgebung für die Dateibrowser-Sitzung gilt, sollte sie im dummen Modus ausgeführt werden.

### In den Terminals

Nachdem die anfängliche Dumb-Terminal-Verbindung erfolgreich war, öffnet XPipe eine separate Verbindung im eigentlichen Terminal. Wenn du möchtest, dass das Skript ausgeführt wird, wenn du die Verbindung in einem Terminal öffnest, dann wähle den Terminalmodus.
