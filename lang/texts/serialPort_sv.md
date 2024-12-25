## Windows

På Windows-system hänvisar du vanligtvis till serieportar via `COM<index>`.
XPipe stöder också att man bara anger indexet utan prefixet `COM`.
Om du vill adressera portar som är större än 9 måste du använda UNC-sökvägsformen med `\\.\COM<index>`.

Om du har en WSL1-distribution installerad kan du också referera till serieportarna inifrån WSL-distributionen via `/dev/ttyS<index>`.
Detta fungerar dock inte med WSL2 längre.
Om du har ett WSL1-system kan du använda detta som värd för den här seriella anslutningen och använda tty-notationen för att komma åt den med XPipe.

## Linux

På Linux-system kan du vanligtvis komma åt de seriella portarna via `/dev/ttyS<index>`.
Om du känner till ID för den anslutna enheten men inte vill hålla reda på serieporten kan du också referera till dem via `/dev/serial/by-id/<device id>`.
Du kan lista alla tillgängliga serieportar med deras ID genom att köra `ls /dev/serial/by-id/*`.

## macOS

På macOS kan serieportnamnen vara i stort sett vad som helst, men har vanligtvis formen `/dev/tty.<id>` där id är den interna enhetsidentifieraren.
Om du kör `ls /dev/tty.*` bör du hitta tillgängliga serieportar.
