## Execution types

There are two distinct execution types when XPipe connects to a system.

### Dumb terminals

The first connection to a system is made in the background in a dumb terminal.

Blocking commands that require user input can freeze the shell process when XPipe starts it up internally first in the background.
To avoid this, you should only call these blocking commands in the terminal mode.

The file browser for example entirely uses the dumb background mode to handle its operations, so if you want your script environment to apply to the file browser session, it should run in the dumb mode.

### Proper terminals

After a dumb terminal connection has succeeded, XPipe will open a separate connection in the actual terminal.
If you want the script to be run when you open the connection in a terminal, then choose the terminal mode.
