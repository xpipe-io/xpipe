# Typy wykonania

Możesz użyć skryptu w wielu różnych scenariuszach.

Po włączeniu skryptu za pomocą przycisku włączania, typy wykonania dyktują, co XPipe zrobi ze skryptem.

## Typ skryptu początkowego

Gdy skrypt jest oznaczony jako skrypt początkowy, można go wybrać w środowiskach powłoki, aby był uruchamiany podczas inicjowania.

Ponadto, jeśli skrypt jest włączony, będzie automatycznie uruchamiany w init we wszystkich kompatybilnych powłokach.

Na przykład, jeśli utworzysz prosty skrypt init z poleceniem
```
alias ll="ls -l"
alias la="ls -A"
alias l="ls -CF"
```
będziesz mieć dostęp do tych aliasów we wszystkich kompatybilnych sesjach powłoki, jeśli skrypt jest włączony.

## Typ skryptu do uruchomienia

Wykonywalny skrypt powłoki jest przeznaczony do wywołania dla określonego połączenia z koncentratora połączeń.
Gdy ten skrypt jest włączony, będzie on dostępny do wywołania z przycisku skryptów dla połączenia z kompatybilnym dialektem powłoki.

Na przykład, jeśli utworzysz prosty skrypt powłoki w dialekcie `sh` o nazwie `ps`, aby wyświetlić bieżącą listę procesów z
```
ps -A
```
możesz wywołać skrypt na dowolnym kompatybilnym połączeniu w menu skryptów.

## Typ skryptu pliku

Wreszcie, możesz także uruchomić niestandardowy skrypt z danymi wejściowymi pliku z interfejsu przeglądarki plików.
Gdy skrypt pliku jest włączony, pojawi się w przeglądarce plików, aby można go było uruchomić z danymi wejściowymi pliku.

Na przykład, jeśli utworzysz prosty skrypt pliku z atrybutem
```
diff "$1" "$2"
```
możesz uruchomić skrypt na wybranych plikach, jeśli skrypt jest włączony.
W tym przykładzie skrypt zostanie pomyślnie uruchomiony tylko wtedy, gdy wybierzesz dokładnie dwa pliki.
W przeciwnym razie polecenie diff nie powiedzie się.

## Typ skryptu sesji powłoki

Skrypt sesji jest przeznaczony do wywołania w sesji powłoki w twoim terminalu.
Po włączeniu, skrypt zostanie skopiowany do systemu docelowego i umieszczony w PATH we wszystkich kompatybilnych powłokach.
Pozwala to na wywołanie skryptu z dowolnego miejsca w sesji terminala.
Nazwa skryptu będzie pisana małymi literami, a spacje zostaną zastąpione podkreśleniami, co pozwoli Ci łatwo wywołać skrypt.

Na przykład, jeśli utworzysz prosty skrypt powłoki dla dialektów `sh` o nazwie `apti` z rozszerzeniem
```
sudo apt install "$1"
```
możesz wywołać skrypt na dowolnym kompatybilnym systemie za pomocą `apti.sh <pkg>` w sesji terminala, jeśli skrypt jest włączony.

## Wiele typów

Możesz także zaznaczyć wiele pól dla typów wykonania skryptu, jeśli mają być one używane w wielu scenariuszach.
