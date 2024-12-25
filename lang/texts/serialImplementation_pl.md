# Implementacje

XPipe deleguje obsługę szeregową do zewnętrznych narzędzi.
Istnieje wiele dostępnych narzędzi, do których XPipe może delegować, każde z własnymi zaletami i wadami.
Aby z nich korzystać, wymagane jest, aby były one dostępne w systemie hosta.
Większość opcji powinna być obsługiwana przez wszystkie narzędzia, ale niektóre bardziej egzotyczne opcje mogą nie być.

Przed połączeniem XPipe sprawdzi, czy wybrane narzędzie jest zainstalowane i obsługuje wszystkie skonfigurowane opcje.
Jeśli sprawdzenie zakończy się pomyślnie, uruchomione zostanie wybrane narzędzie.

