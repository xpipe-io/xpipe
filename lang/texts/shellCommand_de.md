## Benutzerdefinierte Shell-Verbindungen

Öffnet eine Shell mit dem benutzerdefinierten Befehl, indem es den angegebenen Befehl auf dem ausgewählten Hostsystem ausführt. Diese Shell kann entweder lokal oder remote sein.

Beachte, dass diese Funktion erwartet, dass die Shell von einem Standardtyp wie `cmd`, `bash`, etc. ist. Wenn du andere Arten von Shells und Befehlen in einem Terminal öffnen willst, kannst du stattdessen den benutzerdefinierten Terminalbefehlstyp verwenden. Wenn du Standardshells verwendest, kannst du diese Verbindung auch im Dateibrowser öffnen.

### Interaktive Eingabeaufforderungen

Der Shell-Prozess kann eine Zeitüberschreitung verursachen oder sich aufhängen, wenn eine unerwartete
eingabeaufforderung, wie z. B. eine Passwortabfrage. Deshalb solltest du immer darauf achten, dass es keine interaktiven Eingabeaufforderungen gibt.

Ein Befehl wie `ssh user@host` funktioniert hier zum Beispiel problemlos, solange kein Passwort verlangt wird.

### Benutzerdefinierte lokale Shells

In vielen Fällen ist es nützlich, eine Shell mit bestimmten Optionen zu starten, die normalerweise standardmäßig deaktiviert sind, damit einige Skripte und Befehle richtig funktionieren. Zum Beispiel:

-   [Verzögerte Erweiterung in
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell-Ausführung
    richtlinien](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Modus](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Und jede andere mögliche Startoption für eine Shell deiner Wahl

Dies kannst du erreichen, indem du benutzerdefinierte Shell-Befehle erstellst, zum Beispiel mit den folgenden Befehlen:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`