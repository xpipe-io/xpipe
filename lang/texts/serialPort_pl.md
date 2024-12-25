## Windows

W systemach Windows zazwyczaj odnosisz się do portów szeregowych poprzez `COM<index>`.
XPipe obsługuje również samo określenie indeksu bez prefiksu `COM`.
Aby zaadresować porty większe niż 9, musisz użyć formy ścieżki UNC z `\\.\COM<index>`.

Jeśli masz zainstalowaną dystrybucję WSL1, możesz również odwoływać się do portów szeregowych z poziomu dystrybucji WSL poprzez `/dev/ttyS<index>`.
Nie działa to już jednak z WSL2.
Jeśli masz system WSL1, możesz użyć go jako hosta dla tego połączenia szeregowego i użyć notacji tty, aby uzyskać do niego dostęp za pomocą XPipe.

## Linux

W systemach Linux możesz zazwyczaj uzyskać dostęp do portów szeregowych poprzez `/dev/ttyS<index>`.
Jeśli znasz identyfikator podłączonego urządzenia, ale nie chcesz śledzić portu szeregowego, możesz również odwołać się do nich poprzez `/dev/serial/by-id/<device id>`.
Możesz wyświetlić listę wszystkich dostępnych portów szeregowych z ich identyfikatorami, uruchamiając `ls /dev/serial/by-id/*`.

## macOS

W systemie macOS nazwy portów szeregowych mogą być praktycznie dowolne, ale zwykle mają postać `/dev/tty.<id>`, gdzie id jest wewnętrznym identyfikatorem urządzenia.
Uruchomienie `ls /dev/tty.*` powinno znaleźć dostępne porty szeregowe.
