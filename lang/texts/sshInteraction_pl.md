## Interakcja systemu

XPipe próbuje wykryć rodzaj powłoki, do której się zalogował, aby sprawdzić, czy wszystko działa poprawnie i wyświetlić informacje o systemie. Działa to w przypadku zwykłych powłok poleceń, takich jak bash, ale zawodzi w przypadku niestandardowych i niestandardowych powłok logowania dla wielu systemów wbudowanych. Musisz wyłączyć to zachowanie, aby połączenia z tymi systemami zakończyły się powodzeniem.

Gdy ta interakcja jest wyłączona, nie będzie próbowała zidentyfikować żadnych informacji o systemie. Uniemożliwi to wykorzystanie systemu w przeglądarce plików lub jako systemu proxy/bramy dla innych połączeń. XPipe będzie wtedy zasadniczo działać tylko jako program uruchamiający połączenie.
