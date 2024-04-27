## X11 Forwarding

When this option is enabled, the SSH connection will be started with X11 forwarding set up. On Linux, this will usually work out of the box and does not require any setup. On macOS, you need an X11 server like [XQuartz](https://www.xquartz.org/) to be running on your local machine.

### X11 on Windows

XPipe allows you to use the WSL2 X11 capabilities for your SSH connection. The only thing you need for this is a [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) distribution installed on your local system. XPipe it will automatically choose a compatible installed distribution if possible, but you can also use another one in the settings menu.

This means that you don't need to install a separate X11 server on Windows. However, if you are using one anyway, XPipe will detect that and use the currently running X11 server.

### X11 connections as desktops

Any SSH connection that has X11 forwarding enabled can be used as a desktop host. This means that you can launch desktop applications and desktop environments through this connection. When any desktop application is launched, this connection will automatically be started in the background to start the X11 tunnel.
