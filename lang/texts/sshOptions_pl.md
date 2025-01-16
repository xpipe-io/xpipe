## Konfiguracje SSH

Tutaj możesz określić dowolne opcje SSH, które powinny zostać przekazane do połączenia.
Podczas gdy niektóre opcje są zasadniczo wymagane do pomyślnego nawiązania połączenia, takie jak `HostName`,
wiele innych opcji jest czysto opcjonalnych.

Aby uzyskać przegląd wszystkich możliwych opcji, możesz użyć [`man ssh_config`](https://linux.die.net/man/5/ssh_config) lub przeczytać ten [przewodnik](https://www.ssh.com/academy/ssh/config).
Dokładna liczba obsługiwanych opcji zależy wyłącznie od zainstalowanego klienta SSH.

### Formatowanie

Zawartość tutaj jest równoważna jednej sekcji hosta w pliku konfiguracyjnym SSH.
Zauważ, że nie musisz jawnie definiować klucza `Host`, ponieważ zostanie to zrobione automatycznie.

Jeśli zamierzasz zdefiniować więcej niż jedną sekcję hosta, np. z zależnymi połączeniami, takimi jak host skoku proxy, który zależy od innego hosta konfiguracji, możesz również zdefiniować wiele wpisów hosta w tym miejscu. XPipe uruchomi wówczas pierwszy wpis hosta.

Nie musisz wykonywać żadnego formatowania za pomocą białych znaków lub wcięć, nie jest to konieczne do działania.

Zwróć uwagę, że musisz zadbać o cytowanie wszelkich wartości, jeśli zawierają spacje, w przeciwnym razie zostaną przekazane nieprawidłowo.

### Pliki tożsamości

Zauważ, że możesz również określić opcję `IdentityFile` w tym miejscu.
Jeśli ta opcja zostanie określona w tym miejscu, wszelkie inne opcje uwierzytelniania oparte na kluczach określone poniżej zostaną zignorowane.

Jeśli zdecydujesz się odwołać do pliku tożsamości, który jest zarządzany w skarbcu git XPipe, możesz to również zrobić.
XPipe wykryje współdzielone pliki tożsamości i automatycznie dostosuje ścieżkę pliku w każdym systemie, w którym sklonowałeś skarbiec git.
