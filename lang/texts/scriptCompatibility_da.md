## Script-kompatibilitet

Shell-typen styrer, hvor dette script kan køres.
Bortset fra et nøjagtigt match, dvs. at køre et `zsh`-script i `zsh`, vil XPipe også inkludere en bredere kompatibilitetskontrol.

### Posix-skaller

Ethvert script, der er erklæret som et `sh`-script, kan køre i ethvert posix-relateret shell-miljø såsom `bash` eller `zsh`.
Hvis du har til hensigt at køre et grundlæggende script på mange forskellige systemer, er det den bedste løsning kun at bruge `sh`-syntaks-scripts.

### PowerShell

Scripts, der er erklæret som normale `powershell`-scripts, kan også køre i `pwsh`-miljøer.
