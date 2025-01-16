## Wykrywanie typu powłoki

XPipe działa poprzez wykrywanie typu powłoki połączenia, a następnie interakcję z aktywną powłoką. Podejście to działa jednak tylko wtedy, gdy typ powłoki jest znany i obsługuje określoną liczbę akcji i poleceń. Obsługiwane są wszystkie popularne powłoki, takie jak `bash`, `cmd`, `powershell` i inne.

## Nieznane typy powłok

Jeśli łączysz się z systemem, który nie uruchamia znanej powłoki poleceń, np. routerem, łączem lub jakimś urządzeniem IOT, XPipe nie będzie w stanie wykryć typu powłoki i po pewnym czasie wystąpi błąd. Po włączeniu tej opcji, XPipe nie będzie próbował zidentyfikować typu powłoki i uruchomi powłokę tak jak jest. Dzięki temu możesz otworzyć połączenie bez błędów, ale wiele funkcji, np. przeglądarka plików, skrypty, połączenia podrzędne i inne, nie będą obsługiwane dla tego połączenia.
