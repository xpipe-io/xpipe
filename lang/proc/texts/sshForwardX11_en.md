## X11 Forwarding

When this option is enabled, the SSH connection will be started with X11 forwarding set up. On Linux, this will usually work out of the box and does not require any setup. On macOS, you need an X11 server like [XQuartz](https://www.xquartz.org/) to be running on your local machine.

### X11 on Windows

XPipe allows you to use the WSL2 X11 capabilities for your SSH connection. The only thing you need for this is a [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) distribution installed on your local system. XPipe it will automatically choose a compatible installed distribution if possible, but you can also use another one in the settings menu.

This means that you don't need to install a separate X11 server on Windows. However, if you are using one anyway, XPipe will detect that and use the currently running X11 server.
