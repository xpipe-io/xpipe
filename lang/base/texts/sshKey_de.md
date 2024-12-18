### Keine

Wenn du diese Option auswählst, liefert XPipe keine Identitäten. Damit werden auch alle externen Quellen wie Agenten deaktiviert.

### Identitätsdatei

Du kannst auch eine Identitätsdatei mit einer optionalen Passphrase angeben.
Diese Option ist das Äquivalent zu `ssh -i <file>`.

Beachte, dass dies der *private* Schlüssel sein sollte, nicht der öffentliche.
Wenn du das verwechselst, wird dir ssh nur kryptische Fehlermeldungen geben.

### SSH-Agent

Wenn deine Identitäten im SSH-Agenten gespeichert sind, kann das ssh-Programm sie verwenden, wenn der Agent gestartet wird.
XPipe startet den Agentenprozess automatisch, wenn er noch nicht läuft.

### GPG-Agent

Wenn deine Identitäten zum Beispiel auf einer Smartcard gespeichert sind, kannst du sie dem SSH-Client über den `gpg-agent` zur Verfügung stellen.
Diese Option aktiviert automatisch die SSH-Unterstützung des Agenten, falls sie noch nicht aktiviert ist, und startet den GPG-Agent-Daemon mit den richtigen Einstellungen neu.

### Yubikey PIV

Wenn deine Identitäten mit der PIV-Chipkartenfunktion des Yubikey gespeichert sind, kannst du sie mit
kannst du sie mit der YKCS11-Bibliothek von Yubico abrufen, die im Lieferumfang des Yubico PIV Tools enthalten ist.

Beachte, dass du eine aktuelle Version von OpenSSH benötigst, um diese Funktion nutzen zu können.

### Benutzerdefinierte PKCS#11-Bibliothek

Hiermit wird der OpenSSH-Client angewiesen, die angegebene Shared-Library-Datei zu laden, die die Authentifizierung übernimmt.

Beachte, dass du einen aktuellen Build von OpenSSH brauchst, um diese Funktion zu nutzen.

### Pageant (Windows)

Wenn du Pageant unter Windows verwendest, prüft XPipe zuerst, ob Pageant läuft.
Aufgrund der Natur von Pageant liegt es in deiner Verantwortung, dass es
da du jedes Mal alle Schlüssel, die du hinzufügen möchtest, manuell eingeben musst.
Wenn es läuft, übergibt XPipe die entsprechende benannte Pipe über
`-oIdentityAgent=...` an ssh weiter, du musst keine benutzerdefinierten Konfigurationsdateien einfügen.

### Pageant (Linux & macOS)

Wenn deine Identitäten im Pageant-Agent gespeichert sind, kann das ssh-Programm sie verwenden, wenn der Agent gestartet wird.
XPipe startet den Agentenprozess automatisch, wenn er noch nicht läuft.

### Andere externe Quelle

Diese Option erlaubt es jedem externen Identitätsanbieter, seine Schlüssel an den SSH-Client zu liefern. Du solltest diese Option nutzen, wenn du einen anderen Agenten oder Passwortmanager zur Verwaltung deiner SSH-Schlüssel verwendest.
