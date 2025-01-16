## Skriptkompatibilitet

Shell-typen styr var det här skriptet kan köras.
Bortsett från en exakt matchning, dvs. att köra ett `zsh`-skript i `zsh`, kommer XPipe också att inkludera bredare kompatibilitetskontroll.

### Posix skal

Alla skript som deklareras som ett `sh`-skript kan köras i alla posix-relaterade skalmiljöer, t.ex. `bash` eller `zsh`.
Om du tänker köra ett grundläggande skript på många olika system är den bästa lösningen att bara använda skript med `sh`-syntax.

### PowerShell

Skript som deklareras som normala `powershell`-skript kan också köras i `pwsh`-miljöer.
