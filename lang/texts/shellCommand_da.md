## Brugerdefinerede shell-forbindelser

Åbner en shell ved hjælp af den brugerdefinerede kommando ved at udføre den givne kommando på det valgte værtssystem. Denne shell kan enten være lokal eller ekstern.

Bemærk, at denne funktion forventer, at skallen er af en standardtype som `cmd`, `bash` osv. Hvis du vil åbne andre typer skaller og kommandoer i en terminal, kan du i stedet bruge kommandotypen custom terminal. Hvis du bruger standardskaller, kan du også åbne denne forbindelse i filbrowseren.

### Interaktive prompter

Shell-processen kan få timeout eller hænge, hvis der kommer en uventet, påkrævet
input-prompt, som f.eks. en password-prompt. Derfor bør du altid sørge for, at der ikke er nogen interaktive inputprompter.

For eksempel vil en kommando som `ssh user@host` fungere fint her, så længe der ikke kræves en adgangskode.

### Brugerdefinerede lokale skaller

I mange tilfælde er det nyttigt at starte en shell med visse indstillinger, som normalt er deaktiveret som standard, for at få nogle scripts og kommandoer til at fungere korrekt. For eksempel:

-   [Forsinket udvidelse i
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell-udførelse
    politikker](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Og enhver anden mulig startmulighed for en shell efter eget valg

Dette kan opnås ved at oprette brugerdefinerede shell-kommandoer med f.eks. følgende kommandoer:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`