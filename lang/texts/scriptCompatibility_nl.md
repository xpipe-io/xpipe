## Script compatibiliteit

Het shelltype bepaalt waar dit script kan worden uitgevoerd.
Naast een exacte overeenkomst, d.w.z. het uitvoeren van een `zsh` script in `zsh`, zal XPipe ook bredere compatibiliteitscontroles uitvoeren.

### Posix Shells

Elk script dat is gedeclareerd als een `sh` script kan worden uitgevoerd in elke posix-gerelateerde shell-omgeving zoals `bash` of `zsh`.
Als je van plan bent om een basisscript op veel verschillende systemen te draaien, dan is het gebruik van alleen `sh` syntax scripts de beste oplossing.

### PowerShell

Scripts die zijn gedeclareerd als normale `powershell` scripts kunnen ook worden uitgevoerd in `pwsh` omgevingen.
