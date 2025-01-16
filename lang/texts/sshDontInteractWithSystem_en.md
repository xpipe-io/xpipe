## Shell type detection

XPipe works by detecting the shell type of the connection and then interacting with the active shell. This approach only works however when the shell type is known and supports a certain amount of actions and commands. All common shells like `bash`, `cmd`, `powershell`, and more, are supported.

## Unknown shell types

If you are connecting to a system that does not run a known command shell, e.g. a router, link, or some IOT device, XPipe will be unable to detect the shell type and error out after some time. By enabling this option, XPipe will not attempt to identify the shell type and launch the shell as-is. This allows you to open the connection without errors but many features, e.g. the file browser, scripting, subconnections, and more, will not be supported for this connection.
