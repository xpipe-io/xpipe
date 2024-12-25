## Windows

Op Windows systemen verwijs je meestal naar seriële poorten via `COM<index>`.
XPipe ondersteunt ook het opgeven van de index zonder het `COM` voorvoegsel.
Om poorten groter dan 9 te adresseren, moet je de UNC pad vorm gebruiken met `\.\COM<index>`.

Als je een WSL1 distributie hebt geïnstalleerd, kun je de seriële poorten ook vanuit de WSL distributie benaderen via `/dev/ttyS<index>`.
Dit werkt echter niet meer met WSL2.
Als je een WSL1 systeem hebt, kun je deze gebruiken als host voor deze seriële verbinding en de tty notatie gebruiken om deze te benaderen met XPipe.

## Linux

Op Linux systemen heb je meestal toegang tot de seriële poorten via `/dev/ttyS<index>`.
Als je de ID van het aangesloten apparaat weet, maar de seriële poort niet wilt bijhouden, kun je ze ook benaderen via `/dev/serial/by-id/<device id>`.
Je kunt een lijst maken van alle beschikbare seriële poorten met hun ID's door `ls /dev/serial/by-id/*` uit te voeren.

## macOS

Op macOS kunnen de namen van de seriële poorten van alles zijn, maar meestal hebben ze de vorm `/dev/tty.<id>` waarbij de id de interne apparaat-ID is.
Het uitvoeren van `ls /dev/tty.*` zou beschikbare seriële poorten moeten vinden.
