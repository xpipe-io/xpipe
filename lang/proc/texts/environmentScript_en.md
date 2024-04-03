## Init script

The optional commands to run after the shell's init files and profiles have been executed.

You can treat this as a normal shell script, i.e. make use of all the syntax that the shell supports in scripts. All commands you execute are sourced by the shell and modify the environment. So if you for example set a variable, you will have access to this variable in this shell session.

### Blocking commands

Note that blocking commands that require user input can freeze the shell process when XPipe starts it up internally first in the background. To avoid this, only call these blocking commands if the variable `TERM` is not set to `dumb`. XPipe automatically sets the variable `TERM=dumb` when it is preparing the shell session in the background and then sets `TERM=xterm-256color` when actually opening the terminal.