## Script compatibility

The shell type controls where this script can be run.
Aside from an exact match, i.e. running a `zsh` script in `zsh`, XPipe will also include wider compatibility checking.

### Posix Shells

Any script declared as a `sh` script is able to run in any posix-related shell environment such as `bash` or `zsh`.
If you intend to run a basic script on many different systems, then using only `sh` syntax scripts is the best solution for that.

### PowerShell

Scripts declared as normal PowerShell scripts are also able to run in PowerShell Core environments.
