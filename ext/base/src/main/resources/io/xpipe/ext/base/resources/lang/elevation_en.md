## Elevation

The process of elevation is operating system specific.

### Linux & macOS

Any elevated command is executed with `sudo`. The optional `sudo` password is queried via XPipe when needed.
You have the ability to adjust the elevation behavior in the settings to control whether you want to enter your password every time it is needed or if you want to cache it for the current session.

### Windows

On Windows, it is not possible to elevate a child process if the parent process is not elevated as well.
Therefore, if XPipe is not run as an administrator, you will be unable to use any elevation locally.
For remote connections, the connected user account has to be given administrator privileges.