# XPipe Git Vault

XPipe kann alle deine Verbindungsdaten mit deinem eigenen Git Remote Repository synchronisieren. Du kannst mit diesem Repository in allen XPipe-Anwendungsinstanzen auf die gleiche Weise synchronisieren, d.h. jede Änderung, die du in einer Instanz vornimmst, wird in das Repository übernommen.

Als Erstes musst du ein Remote-Repository mit einem Git-Anbieter deiner Wahl erstellen. Dieses Repository muss privat sein.
Dann kannst du die URL einfach kopieren und in die XPipe-Einstellungen für das Remote-Repository einfügen.

Außerdem brauchst du einen lokal installierten `git`-Client auf deinem lokalen Rechner. Du kannst versuchen, `git` in einem lokalen Terminal auszuführen, um das zu überprüfen.
Wenn du keinen hast, kannst du [https://git-scm.com](https://git-scm.com/) besuchen, um Git zu installieren.

## Authentifizierung gegenüber dem entfernten Repository

Es gibt mehrere Möglichkeiten, sich zu authentifizieren. Die meisten Repositories verwenden HTTPS, bei dem du einen Benutzernamen und ein Passwort angeben musst.
Einige Anbieter unterstützen auch das SSH-Protokoll, das auch von XPipe unterstützt wird.
Wenn du SSH für Git verwendest, weißt du wahrscheinlich, wie man es konfiguriert, daher wird in diesem Abschnitt nur auf HTTPS eingegangen.

Du musst dein Git CLI so einrichten, dass es sich bei deinem entfernten Git-Repository über HTTPS authentifizieren kann. Es gibt mehrere Möglichkeiten, das zu tun.
Du kannst überprüfen, ob dies bereits geschehen ist, indem du XPipe neu startest, sobald ein entferntes Repository konfiguriert ist.
Wenn XPipe dich nach deinen Anmeldedaten fragt, musst du sie einrichten.

Viele spezielle Tools wie dieses [GitHub CLI] (https://cli.github.com/) erledigen alles automatisch für dich, wenn es installiert ist.
Einige neuere Git-Client-Versionen können sich auch über spezielle Webdienste authentifizieren, bei denen du dich einfach in deinem Browser bei deinem Konto anmelden musst.

Es gibt auch manuelle Möglichkeiten, sich mit einem Benutzernamen und einem Token zu authentifizieren.
Heutzutage verlangen die meisten Anbieter für die Authentifizierung über die Kommandozeile ein Personal Access Token (PAT) anstelle eines herkömmlichen Passworts.
Gängige (PAT) Seiten findest du hier:
- **GitHub**: [Persönliche Zugangstoken (klassisch)](https://github.com/settings/tokens)
- **GitLab**: [Persönliche Zugangstoken](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html)
- **BitBucket**: [Persönliches Zugriffstoken](https://support.atlassian.com/bitbucket-cloud/docs/access-tokens/)
- **Gitea**: `Einstellungen -> Anwendungen -> Abschnitt Zugriffstoken verwalten`
Setze die Token-Berechtigung für das Repository auf Lesen und Schreiben. Die übrigen Token-Berechtigungen können auf Lesen gesetzt werden.
Auch wenn dein Git-Client dich zur Eingabe eines Passworts auffordert, solltest du dein Token eingeben, es sei denn, dein Anbieter verwendet noch Passwörter.
- Die meisten Anbieter unterstützen keine Passwörter mehr.

Wenn du deine Anmeldedaten nicht jedes Mal eingeben willst, kannst du dafür einen beliebigen Git-Anmeldemanager verwenden.
Weitere Informationen findest du zum Beispiel unter:
- [https://git-scm.com/doc/credential-helpers](https://git-scm.com/doc/credential-helpers)
- [https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git](https://docs.github.com/en/get-started/getting-started-with-git/caching-your-github-credentials-in-git)

Einige moderne Git-Clients kümmern sich auch automatisch um die Speicherung der Anmeldeinformationen.

Wenn alles klappt, sollte XPipe einen Commit an dein entferntes Repository senden.

## Kategorien zum Repository hinzufügen

Standardmäßig sind keine Verbindungskategorien für die Synchronisierung eingestellt, damit du explizit festlegen kannst, welche Verbindungen übertragen werden sollen.
Zu Beginn ist dein entferntes Projektarchiv also leer.

Um die Verbindungen einer Kategorie in dein Git-Repository zu übertragen
musst du auf das Zahnradsymbol klicken (wenn du den Mauszeiger über die Kategorie bewegst)
in deinem `Reiter Verbindungen` unter der Kategorieübersicht auf der linken Seite.
Klicke dann auf `Zum Git-Repository hinzufügen`, um die Kategorie und die Verbindungen mit deinem Git-Repository zu synchronisieren.
Dadurch werden alle synchronisierbaren Verbindungen zum Git-Repository hinzugefügt.

## Lokale Verbindungen werden nicht synchronisiert

Alle Verbindungen, die sich unter dem lokalen Rechner befinden, können nicht synchronisiert werden, da sie sich auf Verbindungen und Daten beziehen, die nur auf dem lokalen System verfügbar sind.

Bestimmte Verbindungen, die auf einer lokalen Datei basieren, z. B. SSH-Konfigurationen, können über Git geteilt werden, wenn die zugrundeliegenden Daten, in diesem Fall die Datei, ebenfalls zum Git-Repository hinzugefügt wurden.

## Dateien zu git hinzufügen

Wenn alles eingerichtet ist, hast du die Möglichkeit, zusätzliche Dateien wie SSH-Schlüssel zu git hinzuzufügen.
Neben jeder Datei befindet sich ein Git-Button, mit dem die Datei zum Git-Repository hinzugefügt wird.
Auch diese Dateien werden verschlüsselt, wenn sie veröffentlicht werden.
