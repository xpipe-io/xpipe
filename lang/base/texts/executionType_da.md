## Udførelsestyper

Der er to forskellige eksekveringstyper, når XPipe opretter forbindelse til et system.

### I baggrunden

Den første forbindelse til et system oprettes i baggrunden i en dumb terminal-session.

Blokeringskommandoer, der kræver brugerinput, kan fryse shell-processen, når XPipe starter den op internt i baggrunden. For at undgå dette bør du kun kalde disse blokerende kommandoer i terminaltilstand.

Filbrowseren bruger for eksempel udelukkende den dumme baggrundstilstand til at håndtere sine operationer, så hvis du vil have dit scriptmiljø til at gælde for filbrowsersessionen, skal det køre i den dumme tilstand.

### I terminalerne

Når den indledende dumb terminal-forbindelse er lykkedes, vil XPipe åbne en separat forbindelse i den faktiske terminal. Hvis du vil have scriptet til at køre, når du åbner forbindelsen i en terminal, skal du vælge terminaltilstand.
