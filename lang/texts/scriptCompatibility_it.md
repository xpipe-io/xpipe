## Compatibilità con gli script

Il tipo di shell controlla dove questo script può essere eseguito.
Oltre alla corrispondenza esatta, cioè all'esecuzione di uno script `zsh` in `zsh`, XPipe include anche un controllo di compatibilità più ampio.

### Gusci Posix

Qualsiasi script dichiarato come script `sh` è in grado di essere eseguito in qualsiasi ambiente shell posix come `bash` o `zsh`.
Se intendi eseguire uno script di base su molti sistemi diversi, allora utilizzare solo script con sintassi `sh` è la soluzione migliore.

### PowerShell

Gli script dichiarati come normali script `powershell` possono essere eseguiti anche in ambienti `pwsh`.
