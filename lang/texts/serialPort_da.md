## Windows

På Windows-systemer henviser man typisk til serielle porte via `COM<index>`.
XPipe understøtter også blot at angive indekset uden præfikset `COM`.
For at adressere porte større end 9 skal du bruge UNC-stiformen med `\\.\COM<index>`.

Hvis du har installeret en WSL1-distribution, kan du også henvise til de serielle porte inde fra WSL-distributionen via `/dev/ttyS<index>`.
Dette virker dog ikke længere med WSL2.
Hvis du har et WSL1-system, kan du bruge det som vært for den serielle forbindelse og bruge tty-notationen til at få adgang til det med XPipe.

## Linux

På Linux-systemer kan du typisk få adgang til de serielle porte via `/dev/ttyS<index>`.
Hvis du kender ID'et på den tilsluttede enhed, men ikke ønsker at holde styr på den serielle port, kan du også henvise til dem via `/dev/serial/by-id/<device id>`.
Du kan få en liste over alle tilgængelige serielle porte med deres ID'er ved at køre `ls /dev/serial/by-id/*`.

## macOS

På macOS kan de serielle portnavne være stort set hvad som helst, men har normalt formen `/dev/tty.<id>`, hvor id er den interne enhedsidentifikator.
Ved at køre `ls /dev/tty.*` kan man finde tilgængelige serielle porte.
