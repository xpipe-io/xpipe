## Windows

Nei sistemi Windows di solito ci si riferisce alle porte seriali tramite `COM<index>`.
XPipe supporta anche la semplice indicazione dell'indice senza il prefisso `COM`.
Per indirizzare le porte superiori a 9, devi utilizzare la forma UNC path con `\\.\COM<index>`.

Se hai installato una distribuzione WSL1, puoi anche fare riferimento alle porte seriali dall'interno della distribuzione WSL tramite `/dev/ttyS<index>`.
Questo però non funziona più con WSL2.
Se hai un sistema WSL1, puoi usarlo come host per questa connessione seriale e utilizzare la notazione tty per accedervi con XPipe.

## Linux

Sui sistemi Linux puoi accedere alle porte seriali tramite `/dev/ttyS<index>`.
Se conosci l'ID del dispositivo collegato ma non vuoi tenere traccia della porta seriale, puoi anche fare riferimento ad esso tramite `/dev/serial/by-id/<device id>`.
Puoi elencare tutte le porte seriali disponibili con i relativi ID eseguendo `ls /dev/serial/by-id/*`.

## macOS

Su macOS, i nomi delle porte seriali possono essere praticamente qualsiasi cosa, ma di solito hanno la forma di `/dev/tty.<id>` dove l'id è l'identificatore interno del dispositivo.
L'esecuzione di `ls /dev/tty.*` dovrebbe trovare le porte seriali disponibili.
