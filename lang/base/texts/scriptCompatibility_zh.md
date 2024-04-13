## 脚本兼容性

shell 类型控制着脚本的运行位置。
除了完全匹配（即在 `zsh` 中运行 `zsh` 脚本）外，XPipe 还将进行更广泛的兼容性检查。

### Posix Shells

任何声明为 `sh` 脚本的脚本都可以在任何与 Posix 相关的 shell 环境（如 `bash` 或 `zsh` 中运行。
如果您打算在许多不同的系统上运行一个基本脚本，那么只使用 `sh` 语法的脚本是最好的解决方案。

#### PowerShell

声明为普通 `powershell` 脚本的脚本也可以在 `pwsh` 环境中运行。
