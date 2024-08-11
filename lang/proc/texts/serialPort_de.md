## Windows

Auf Windows-Systemen bezeichnest du serielle Schnittstellen normalerweise mit `COM<index>`.
XPipe unterstützt auch die bloße Angabe des Index ohne das Präfix `COM`.
Um Ports größer als 9 anzusprechen, musst du die UNC-Pfadform mit `\.\COM<index>` verwenden.

Wenn du eine WSL1-Distribution installiert hast, kannst du die seriellen Schnittstellen auch aus der WSL-Distribution heraus über `/dev/ttyS<index>` ansprechen.
Das funktioniert allerdings nicht mehr mit WSL2.
Wenn du ein WSL1-System hast, kannst du dieses als Host für diese serielle Verbindung verwenden und die tty-Notation nutzen, um mit XPipe darauf zuzugreifen.

## Linux

Auf Linux-Systemen kannst du normalerweise über `/dev/ttyS<index>` auf die seriellen Schnittstellen zugreifen.
Wenn du die ID des angeschlossenen Geräts kennst, dir aber die serielle Schnittstelle nicht merken willst, kannst du sie auch über `/dev/serial/by-id/<device id>` ansprechen.
Du kannst alle verfügbaren seriellen Schnittstellen mit ihren IDs auflisten, indem du `ls /dev/serial/by-id/*` ausführst.

## macOS

Unter macOS können die Namen der seriellen Schnittstellen so ziemlich alles sein, aber normalerweise haben sie die Form `/dev/tty.<id>`, wobei id die interne Gerätekennung ist.
Wenn du `ls /dev/tty.*` ausführst, solltest du die verfügbaren seriellen Schnittstellen finden.
