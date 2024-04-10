## Compatibilidade de scripts

O tipo de shell controla onde este script pode ser executado.
Além de uma correspondência exata, ou seja, executar um script `zsh` em `zsh`, o XPipe também incluirá uma verificação de compatibilidade mais ampla.

### Shells Posix

Qualquer script declarado como um script `sh` pode ser executado em qualquer ambiente shell relacionado ao posix, como `bash` ou `zsh`.
Se pretendes correr um script básico em muitos sistemas diferentes, então usar apenas scripts com sintaxe `sh` é a melhor solução para isso.

### PowerShell

Os scripts declarados como scripts `powershell` normais também podem ser executados em ambientes `pwsh`.
