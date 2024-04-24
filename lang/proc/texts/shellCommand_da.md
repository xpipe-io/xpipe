## Brugerdefinerede shell-forbindelser

Åbner en shell ved hjælp af den brugerdefinerede kommando ved at udføre den givne kommando på det valgte værtssystem. Denne shell kan enten være lokal eller fjern.

Bemærk, at denne funktionalitet forventer, at shellen er af en standardtype som `cmd`, `bash` osv. Hvis du vil åbne andre typer shells og kommandoer i en terminal, kan du bruge den brugerdefinerede terminalkommandotype i stedet. Hvis du bruger standard-shells, kan du også åbne denne forbindelse i filbrowseren.

### Interaktive prompts

Shell-processen kan gå i stå eller hænge, hvis der kommer en uventet prompt med påkrævet
inputprompt, som f.eks. en adgangskodeprompt. Derfor bør du altid sørge for, at der ikke er nogen interaktive inputprompter.

For eksempel vil en kommando som `ssh user@host` fungere fint her, så længe der ikke kræves en adgangskode.

### Brugerdefinerede lokale shells

I mange tilfælde er det nyttigt at starte en shell med visse indstillinger, der normalt er deaktiveret som standard, for at få nogle scripts og kommandoer til at fungere korrekt. For eksempel:

-   [Forsinket ekspansion i
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell-udførelse
    politikker](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Og enhver anden mulig startmulighed for en shell efter eget valg

Dette kan opnås ved at oprette brugerdefinerede shell-kommandoer med f.eks. følgende kommandoer:

-   <kode>cmd /v</kode>
-   <kode>powershell -ExecutionMode Bypass</kode>
-   `bash --posix`