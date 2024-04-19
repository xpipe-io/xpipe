# Integração do ambiente de trabalho RDP

Podes utilizar esta ligação RDP no XPipe para lançar rapidamente aplicações e scripts. No entanto, devido à natureza do RDP, tens de editar a lista de permissões de aplicações remotas no teu servidor para que isto funcione. Além disso, esta opção permite a partilha de unidades para executar os teus scripts no teu servidor remoto.

Também podes optar por não fazer isto e utilizar apenas o XPipe para lançar o cliente RDP sem utilizar quaisquer funcionalidades avançadas de integração do ambiente de trabalho.

## Listas de permissões RDP

Um servidor RDP usa o conceito de listas de permissão para lidar com lançamentos de aplicativos. Isso significa essencialmente que, a menos que a lista de permissões esteja desativada ou que aplicativos específicos tenham sido explicitamente adicionados à lista de permissões, o lançamento de qualquer aplicativo remoto diretamente falhará.

Podes encontrar as definições da lista de permissões no registo do teu servidor em `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Permitir todas as aplicações

Podes desativar a lista de permissões para permitir que todas as aplicações remotas sejam iniciadas diretamente a partir do XPipe. Para tal, podes executar o seguinte comando no teu servidor em PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Adicionar aplicações permitidas

Em alternativa, podes também adicionar aplicações remotas individuais à lista. Isto permitir-te-á iniciar as aplicações listadas diretamente a partir do XPipe.

Sob a chave `Applications` de `TSAppAllowList`, cria uma nova chave com um nome arbitrário. O único requisito para o nome é que ele seja exclusivo dentro dos filhos da chave "Applications". Essa nova chave deve ter os seguintes valores: `Name`, `Path` e `CommandLineSetting`. Podes fazer isto no PowerShell com os seguintes comandos:

```
$appName="Bloco de Notas"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
Novo item -Path "$regKey\$appName"
Novo-ItemProperty -Path "$regKey\$appName" -Name "Nome" -Value "$appName" -Force
Novo-ItemProperty -Path "$regKey\$appName" -Nome "Caminho" -Valor "$appPath" -Force
Novo-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
<código>`</código>

Se quiseres permitir que o XPipe também execute scripts e abra sessões de terminal, tens de adicionar `C:\Windows\System32\cmd.exe` à lista de permissões também.

## Considerações de segurança

Isto não torna o teu servidor inseguro de forma alguma, uma vez que podes sempre executar as mesmas aplicações manualmente quando inicias uma ligação RDP. As listas de permissão são mais destinadas a impedir que os clientes executem instantaneamente qualquer aplicativo sem a entrada do usuário. No final do dia, cabe-te a ti decidir se confias no XPipe para fazer isto. Podes iniciar esta ligação sem problemas, isto só é útil se quiseres utilizar qualquer uma das funcionalidades avançadas de integração de ambiente de trabalho no XPipe.
