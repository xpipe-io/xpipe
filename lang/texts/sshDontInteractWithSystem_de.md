## Shell-Typ-Erkennung

XPipe arbeitet, indem es den Shell-Typ der Verbindung erkennt und dann mit der aktiven Shell interagiert. Dieser Ansatz funktioniert jedoch nur, wenn der Shell-Typ bekannt ist und eine bestimmte Anzahl von Aktionen und Befehlen unterstützt. Alle gängigen Shells wie `bash`, `cmd`, `powershell` und andere werden unterstützt.

## Unbekannte Shell-Typen

Wenn du eine Verbindung zu einem System herstellst, auf dem keine bekannte Befehlsshell läuft, z.B. ein Router, Link oder ein IOT-Gerät, kann XPipe den Shell-Typ nicht erkennen und bricht nach einiger Zeit ab. Wenn du diese Option aktivierst, versucht XPipe nicht, den Shell-Typ zu erkennen und startet die Shell so, wie sie ist. Dadurch kannst du die Verbindung ohne Fehler öffnen, aber viele Funktionen, z. B. der Dateibrowser, Skripting, Unterverbindungen und mehr, werden für diese Verbindung nicht unterstützt.
