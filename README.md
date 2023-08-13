<img src="https://github.com/xpipe-io/xpipe/assets/72509152/88d750f3-8469-4c51-bb64-5b264b0e9d47" alt="drawing" width="250"/>

### A brand-new shell connection hub and remote file manager

XPipe is a new type of shell connection hub and remote file manager that allows you to access your entire sever infrastructure from your local machine. It works on top of your installed command-line programs that you normally use to connect and does not require any setup on your remote systems.

XPipe fully integrates with your tools such as your favourite text/code editors, terminals, shells, command-line tools and more. The platform is designed to be extensible, allowing anyone to add easily support for more tools or to implement custom functionality through a modular extension system.

It currently supports:
- [Kubernetes](https://kubernetes.io/) clusters, pods, and containers
- [Docker](https://www.docker.com/), [Podman](https://podman.io/), and [LXD](https://linuxcontainers.org/lxd/introduction/) container instances located on any host
- [SSH](https://www.ssh.com/academy/ssh/protocol) connections, config file connections, and tunnels
- [Windows Subsystem for Linux](https://ubuntu.com/wsl), [Cygwin](https://www.cygwin.com/), and [MSYS2](https://www.msys2.org/) instances
- [Powershell Remote Sessions](https://learn.microsoft.com/en-us/powershell/scripting/learn/remoting/running-remote-commands?view=powershell-7.3)
- Any other custom remote connection methods that work through the command-line

Furthermore, you can also use any remote shell connection as a proxy when establishing new connections, allowing full flexibility to set up connection routes.

The project is still in a relatively early stage and will benefit massively from your feedback, issue reports, feature request, and more. There are also a lot more features to come in the future.

You have more questions? Then check out the new [FAQ](/FAQ.md).

## Connection Hub

- Easily connect to and access all kinds of remote connections in one place
- Securely stores all information exclusively on your computer and encrypts all secret information. See the [security page](/SECURITY.md) for more information
- Allows you to fully customize the init environment of the launched shell sessions with custom scripts
- Can create desktop shortcuts that automatically open remote connections in your terminal

![connections](https://github.com/xpipe-io/xpipe/assets/72509152/ef19aa85-1b66-45e0-a051-5a4658758626)

## Remote File Manager

- Interact with the file system of any remote system using a workflow optimized for professionals
- Quickly open a terminal into any directory
- Utilize your favourite local programs to open and edit remote files
- Has the same feature set for all supported connection types
- Dynamically elevate sessions with sudo when required

The feature set is the same for all supported connection types. It of course also supports browsing the file system on your local machine.

![browser](https://github.com/xpipe-io/xpipe/assets/72509152/5631fe50-58b4-4847-a5f4-ad3898a02a9f)

## Terminal Launcher

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells and connections, locally and remote.
- Supports command shells (e.g. bash, PowerShell, cmd, etc.) and some database shells (e.g. PostgreSQL Shell)
- Comes with support for all commonly used terminal emulators across all operating systems
- Supports launches from the GUI or directly from the command-line
- Solves all encoding issues on Windows systems as all Windows shells are launched in UTF8 mode by default

<br>
<p align="center">
  <img src="https://github.com/xpipe-io/xpipe/assets/72509152/f3d29909-acd7-4568-a625-0667d936ef2b" alt="Terminal launcher"/>
</p>
<br>

## Downloads

### Installers

Installers are the easiest way to get started and come with an optional automatic update functionality. The following installers are available:

- [Windows .msi Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-windows-x86_64.msi)
- [Linux .deb Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-x86_64.deb)
- [Linux .deb Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-arm64.deb)
- [Linux .rpm Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-x86_64.rpm)
- [Linux .rpm Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-arm64.rpm)
- [MacOS .pkg Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-macos-x86_64.pkg)
- [MacOS .pkg Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-macos-arm64.pkg)

### Portable

If you don't like installers, you can also use portable versions that are packaged as an archive. The following portable versions are available:

- [Windows .zip Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-windows-x86_64.zip)
- [Linux .tar.gz Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-x86_64.tar.gz)
- [Linux .tar.gz Portable (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-arm64.tar.gz)
- [MacOS .dmg Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-macos-x86_64.dmg)
- [MacOS .dmg Portable (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-macos-arm64.dmg)

### Install Script

You can also install XPipe by pasting the installation command into your terminal. This will perform the full setup automatically.

#####  Linux / MacOS

The script supports installation via `apt`, `rpm`, and `pacman` on Linux, plus a `.pkg` install on macOS:

```
bash <(curl -sL https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.sh)
```

##### Windows

```
powershell -ExecutionPolicy Bypass -Command iwr "https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.ps1" -OutFile "$env:TEMP\get-xpipe.ps1" ";"  "&" "$env:TEMP\get-xpipe.ps1"
```

## Further information

For information about the security model of XPipe, see the [security page](/SECURITY.md).

For information about the privacy policy of XPipe, see the [privacy page](/PRIVACY.md).

In case you're interested in development, check out the [development page](/DEVELOPMENT.md).

If you want to talk you can also join:

- The [XPipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [XPipe Slack Server](https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)

