## Custom shell connections

Opens a shell using the custom command by executing the given command on the selected host system. This shell can either be local or remote.

Note that this functionality expects the shell to be of a standard type such as `cmd`, `bash`, etc. If you want to open any other types of shells and commands in a terminal, you can use the custom terminal command type instead. Using standard shells allows you to also open this connection in the file browser.

### Interactive prompts

The shell process might time out or hang in case there is an unexpected required
input prompt, like a password prompt. Therefore, you should always make sure that there are no interactive input prompts.

For example, a command like `ssh user@host` will work fine here as long there is no password required.

### Custom local shells

In many cases, it is useful to launch a shell with certain options that are usually disabled by default in order to make some scripts and commands work properly. For example:

-   [Delayed Expansion in
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell execution
    policies](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mode](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- And any other possible launch option for a shell of your choice

This can be achieved by creating custom shell commands with for example the following commands:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`