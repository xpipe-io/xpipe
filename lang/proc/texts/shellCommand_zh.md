## 自定义 shell 连接

在选定的主机系统上执行给定命令，使用自定义命令打开一个 shell。此 shell 可以是本地的，也可以是远程的。

请注意，此功能希望 shell 是标准类型的，如 `cmd`, `bash` 等。如果你想在终端中打开任何其他类型的 shell 和命令，可以使用自定义终端命令类型来代替。使用标准 shell 还可以在文件浏览器中打开此连接。

### 交互式提示

如果出现意外的输入提示（如密码提示），shell 进程可能会超时或挂起。
输入提示（如密码提示）时，shell 进程可能会超时或挂起。因此，应始终确保没有交互式输入提示。

例如，只要不需要密码，`ssh user@host` 这样的命令就可以正常运行。

### 自定义本地 shell

在许多情况下，为了使某些脚本和命令正常运行，在启动 shell 时使用某些通常默认禁用的选项是非常有用的。例如

-   在
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell 执行
    策略](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    模式](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- 以及您选择的 shell 的任何其他可能的启动选项

这可以通过创建自定义 shell 命令来实现，例如使用以下命令：

-   <代码>cmd /v</code
-   <代码>powershell -ExecutionMode Bypass</代码
-   <code>bash --posix</code