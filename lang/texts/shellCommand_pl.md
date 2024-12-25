## Niestandardowe połączenia powłoki

Otwiera powłokę przy użyciu niestandardowego polecenia, wykonując podane polecenie w wybranym systemie hosta. Powłoka ta może być lokalna lub zdalna.

Zwróć uwagę, że ta funkcja oczekuje, że powłoka będzie standardowego typu, takiego jak `cmd`, `bash` itp. Jeśli chcesz otworzyć inne typy powłok i poleceń w terminalu, możesz zamiast tego użyć niestandardowego typu polecenia terminala. Korzystając ze standardowych powłok, możesz również otworzyć to połączenie w przeglądarce plików.

### Interaktywne podpowiedzi

Proces powłoki może przekroczyć limit czasu lub zawiesić się w przypadku nieoczekiwanego
wymagany monit wejściowy, taki jak monit o hasło. Dlatego zawsze powinieneś upewnić się, że nie ma żadnych interaktywnych monitów.

Na przykład polecenie takie jak `ssh user@host` będzie działać dobrze, o ile nie jest wymagane hasło.

### Niestandardowe powłoki lokalne

W wielu przypadkach przydatne jest uruchomienie powłoki z pewnymi opcjami, które są zwykle domyślnie wyłączone, aby niektóre skrypty i polecenia działały poprawnie. Na przykład:

-   [Delayed Expansion in
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Wykonywanie Powershell
    zasady](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- I każdą inną możliwą opcję uruchamiania dla wybranej powłoki

Można to osiągnąć, tworząc niestandardowe polecenia powłoki, na przykład za pomocą następujących poleceń:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`