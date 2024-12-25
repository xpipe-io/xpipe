## Elevação

O processo de elevação é específico do sistema operativo.

### Linux e macOS

Qualquer comando elevado é executado com `sudo`. A senha opcional `sudo` é consultada via XPipe quando necessário.
Tens a capacidade de ajustar o comportamento de elevação nas definições para controlar se queres introduzir a tua palavra-passe sempre que for necessária ou se a queres guardar em cache para a sessão atual.

### Windows

No Windows, não é possível elevar um processo filho se o processo pai também não estiver elevado.
Portanto, se o XPipe não for executado como administrador, não poderás utilizar qualquer elevação localmente.
Para ligações remotas, a conta de utilizador ligada tem de ter privilégios de administrador.