## Skrypt początkowy

Opcjonalne polecenia uruchamiane po wykonaniu plików init i profili powłoki.

Możesz traktować to jako normalny skrypt powłoki, tj. korzystać z całej składni obsługiwanej przez powłokę w skryptach. Wszystkie polecenia, które wykonujesz, są pobierane przez powłokę i modyfikują środowisko. Jeśli więc na przykład ustawisz zmienną, będziesz mieć do niej dostęp w tej sesji powłoki.

### Blokowanie poleceń

Zwróć uwagę, że polecenia blokujące, które wymagają danych wejściowych użytkownika, mogą zamrozić proces powłoki, gdy XPipe uruchomi go wewnętrznie jako pierwszy w tle. Aby tego uniknąć, wywołuj te polecenia blokujące tylko wtedy, gdy zmienna `TERM` nie jest ustawiona na `dumb`. XPipe automatycznie ustawia zmienną `TERM=dumb` podczas przygotowywania sesji powłoki w tle, a następnie ustawia `TERM=xterm-256color` podczas faktycznego otwierania terminala.