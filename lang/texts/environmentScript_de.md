## Init-Skript

Die optionalen Befehle, die ausgeführt werden, nachdem die Init-Dateien und -Profile der Shell ausgeführt worden sind.

Du kannst dies wie ein normales Shell-Skript behandeln, d.h. du kannst die gesamte Syntax verwenden, die die Shell in Skripten unterstützt. Alle Befehle, die du ausführst, werden von der Shell übernommen und verändern die Umgebung. Wenn du also zum Beispiel eine Variable setzt, hast du in dieser Shell-Sitzung Zugriff auf diese Variable.

### Blockierende Befehle

Beachte, dass blockierende Befehle, die Benutzereingaben erfordern, den Shell-Prozess einfrieren können, wenn XPipe ihn zuerst intern im Hintergrund startet. Um dies zu vermeiden, rufe diese blockierenden Befehle nur auf, wenn die Variable `TERM` nicht auf `dumb` gesetzt ist. XPipe setzt die Variable `TERM=dumb` automatisch, wenn es die Shell-Sitzung im Hintergrund vorbereitet und setzt dann `TERM=xterm-256color`, wenn es das Terminal tatsächlich öffnet.