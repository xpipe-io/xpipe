### Konfiguracje SSH

XPipe ładuje wszystkie hosty i stosuje wszystkie ustawienia, które skonfigurowałeś w wybranym pliku. Tak więc określając opcję konfiguracji na podstawie globalnej lub specyficznej dla hosta, zostanie ona automatycznie zastosowana do połączenia ustanowionego przez XPipe.

Jeśli chcesz dowiedzieć się więcej o tym, jak korzystać z konfiguracji SSH, możesz użyć `man ssh_config` lub przeczytać ten [przewodnik](https://www.ssh.com/academy/ssh/config).

### Tożsamości

Zauważ, że możesz również określić opcję `IdentityFile` w tym miejscu. Jeśli jakakolwiek tożsamość zostanie określona w tym miejscu, każda inna tożsamość określona później zostanie zignorowana.

### Przekierowanie X11

Jeśli określisz tutaj jakiekolwiek opcje przekierowania X11, XPipe automatycznie spróbuje skonfigurować przekierowanie X11 w systemie Windows za pośrednictwem WSL.