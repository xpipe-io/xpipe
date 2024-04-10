## Compatibilidad con guiones

El tipo de shell controla dónde se puede ejecutar este script.
Aparte de una coincidencia exacta, es decir, ejecutar un script `zsh` en `zsh`, XPipe también incluirá una comprobación de compatibilidad más amplia.

### Shell Posix

Cualquier script declarado como script `sh` puede ejecutarse en cualquier entorno shell relacionado con posix, como `bash` o `zsh`.
Si pretendes ejecutar un script básico en muchos sistemas distintos, la mejor solución es utilizar sólo scripts de sintaxis `sh`.

### PowerShell

Los scripts declarados como scripts `powershell` normales también pueden ejecutarse en entornos `pwsh`.
