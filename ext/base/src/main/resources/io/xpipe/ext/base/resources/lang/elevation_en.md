## Elevation

The process of elevation is operating system specific.

### Linux & macOS

Any elevated command is executed with `sudo`.
If this is done on your local system, the optional `sudo` password is queried via XPipe itself.
For remote systems, the `sudo` password is determined from the connection information, i.e. the used login password.

### Windows

On Windows, it is not possible to elevate a child process if the parent process is not elevated as well.
Therefore, if XPipe is not run as an administrator, you will be unable to use any elevation locally.
For remote connections, the connected user account has to be given administrator privileges.