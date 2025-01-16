## Przekierowanie X11

Gdy ta opcja jest włączona, połączenie SSH zostanie uruchomione z skonfigurowanym przekierowaniem X11. W systemie Linux zazwyczaj działa to po wyjęciu z pudełka i nie wymaga żadnej konfiguracji. W systemie macOS potrzebujesz serwera X11, takiego jak [XQuartz](https://www.xquartz.org/), który musi być uruchomiony na komputerze lokalnym.

### X11 w systemie Windows

XPipe umożliwia korzystanie z możliwości WSL2 X11 dla twojego połączenia SSH. Jedyne czego potrzebujesz to dystrybucja [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) zainstalowana w twoim lokalnym systemie. XPipe automatycznie wybierze kompatybilną zainstalowaną dystrybucję, jeśli to możliwe, ale możesz także użyć innej w menu ustawień.

Oznacza to, że nie musisz instalować oddzielnego serwera X11 w systemie Windows. Jeśli jednak i tak używasz takiego serwera, XPipe wykryje to i użyje aktualnie działającego serwera X11.

### Połączenia X11 jako pulpity

Każde połączenie SSH z włączonym przekierowaniem X11 może być używane jako host pulpitu. Oznacza to, że możesz uruchamiać aplikacje pulpitu i środowiska pulpitu za pośrednictwem tego połączenia. Po uruchomieniu dowolnej aplikacji pulpitu połączenie to zostanie automatycznie uruchomione w tle, aby uruchomić tunel X11.
