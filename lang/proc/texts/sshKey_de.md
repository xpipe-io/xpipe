### Keine

Deaktiviert die `publickey`-Authentifizierung.

### SSH-Agent

Wenn deine Identitäten im SSH-Agenten gespeichert sind, kann das ssh-Programm sie verwenden, wenn der Agent gestartet ist.
XPipe startet den Agentenprozess automatisch, wenn er noch nicht läuft.

### Pageant (Windows)

Wenn du Pageant unter Windows verwendest, prüft XPipe zuerst, ob Pageant läuft.
Aufgrund der Natur von Pageant liegt es in deiner Verantwortung, dass es
da du jedes Mal alle Schlüssel, die du hinzufügen möchtest, manuell eingeben musst.
Wenn es läuft, übergibt XPipe die richtig benannte Pipe über
`-oIdentityAgent=...` an ssh weiter, du musst keine eigenen Konfigurationsdateien einbinden.

Beachte, dass es einige Implementierungsfehler im OpenSSH-Client gibt, die Probleme verursachen können
wenn dein Benutzername Leerzeichen enthält oder zu lang ist.

### Pageant (Linux & macOS)

Wenn deine Identitäten im Pageant-Agent gespeichert sind, kann das ssh-Programm sie verwenden, wenn der Agent gestartet wird.
XPipe startet den Agentenprozess automatisch, wenn er noch nicht läuft.

### Identitätsdatei

Du kannst auch eine Identitätsdatei mit einer optionalen Passphrase angeben.
Diese Option ist das Äquivalent zu `ssh -i <file>`.

Beachte, dass dies der *private* Schlüssel sein sollte, nicht der öffentliche.
Wenn du das verwechselst, wird dir ssh nur kryptische Fehlermeldungen geben.

### GPG Agent

Wenn deine Identitäten zum Beispiel auf einer Smartcard gespeichert sind, kannst du sie dem SSH-Client über den `gpg-agent` zur Verfügung stellen.
Diese Option aktiviert automatisch die SSH-Unterstützung des Agenten, falls sie noch nicht aktiviert ist, und startet den GPG-Agent-Daemon mit den richtigen Einstellungen neu.

### Yubikey PIV

Wenn deine Identitäten mit der PIV-Chipkartenfunktion des Yubikey gespeichert sind, kannst du sie mit
kannst du sie mit der YKCS11-Bibliothek von Yubico abrufen, die im Lieferumfang des Yubico PIV Tools enthalten ist.

Beachte, dass du eine aktuelle Version von OpenSSH benötigst, um diese Funktion nutzen zu können.

### Benutzerdefinierter Agent

Du kannst auch einen benutzerdefinierten Agenten verwenden, indem du hier entweder den Socket-Speicherort oder den benannten Pipe-Speicherort angibst.
Er wird dann über die Option `IdentityAgent` übergeben.

### Benutzerdefinierte PKCS#11-Bibliothek

Hiermit wird der OpenSSH-Client angewiesen, die angegebene Shared-Library-Datei zu laden, die die Authentifizierung übernimmt.

Beachte, dass du einen aktuellen Build von OpenSSH brauchst, um diese Funktion zu nutzen.
