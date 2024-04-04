## Skript-Kompatibilität

Der Shell-Typ bestimmt, wo das Skript ausgeführt werden kann.
Abgesehen von einer exakten Übereinstimmung, d.h. der Ausführung eines `zsh`-Skripts in `zsh`, führt XPipe auch eine breitere Kompatibilitätsprüfung durch.

### Posix-Shells

Jedes Skript, das als `sh`-Skript deklariert ist, kann in jeder Posix-Shell-Umgebung wie `bash` oder `zsh` ausgeführt werden.
Wenn du ein grundlegendes Skript auf vielen verschiedenen Systemen ausführen willst, ist die Verwendung von Skripten mit `sh`-Syntax die beste Lösung dafür.

### PowerShell

Skripte, die als normale `powershell`-Skripte deklariert sind, können auch in `pwsh`-Umgebungen ausgeführt werden.
