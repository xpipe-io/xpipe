## Execution types

There are two distinct execution phases when XPipe connects to a system.
The first connection is made in the background in a dumb terminal.
Only afterward will a separate connection be made in the actual terminal.

The file browser for example entirely uses the dumb background mode to handle its operations, so if you want your script environment to apply to the file browser session, it should run in the dumb mode.

### Blocking commands

Blocking commands that require user input can freeze the shell process when XPipe starts it up internally first in the background.
To avoid this, you should only call these blocking commands in the terminal mode.
