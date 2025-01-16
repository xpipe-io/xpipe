## Kompatybilność skryptu

Typ powłoki kontroluje, gdzie ten skrypt może być uruchomiony.
Oprócz dokładnego dopasowania, tj. uruchamiania skryptu `zsh` w `zsh`, XPipe będzie również obejmować szersze sprawdzanie zgodności.

### Posix Shells

Każdy skrypt zadeklarowany jako `sh` może być uruchomiony w dowolnym środowisku powłoki związanym z systemem Posix, takim jak `bash` lub `zsh`.
Jeśli zamierzasz uruchamiać podstawowy skrypt na wielu różnych systemach, najlepszym rozwiązaniem jest używanie tylko skryptów o składni `sh`.

### PowerShell

Skrypty zadeklarowane jako zwykłe skrypty `powershell` mogą być również uruchamiane w środowiskach `pwsh`.
