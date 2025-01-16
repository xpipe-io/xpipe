## Bramy połączeń powłoki

Jeśli ta opcja jest włączona, XPipe najpierw otwiera połączenie powłoki z bramą, a następnie otwiera połączenie SSH z określonym hostem. Polecenie `ssh` musi być dostępne i znajdować się w `PATH` na wybranej bramie.

### Połącz serwery

Ten mechanizm jest podobny do serwerów skoków, ale nie równoważny. Jest całkowicie niezależny od protokołu SSH, więc możesz użyć dowolnego połączenia powłoki jako bramy.

Jeśli szukasz odpowiednich serwerów skoków SSH, być może także w połączeniu z przekierowaniem agenta, użyj niestandardowej funkcjonalności połączenia SSH z opcją konfiguracji `ProxyJump`.