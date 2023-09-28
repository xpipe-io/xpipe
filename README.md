<img src="https://github.com/xpipe-io/xpipe/assets/72509152/88d750f3-8469-4c51-bb64-5b264b0e9d47" alt="drawing" width="250"/>

### A brand-new shell connection hub and remote file manager

XPipe is a new type of shell connection hub and remote file manager that allows you to access your entire sever infrastructure from your local machine. It works on top of your installed command-line programs that you normally use to connect and does not require any setup on your remote systems.

XPipe fully integrates with your tools such as your favourite text/code editors, terminals, shells, command-line tools and more. The platform is designed to be extensible, allowing anyone to add easily support for more tools or to implement custom functionality through a modular extension system.

It currently supports:
- [Kubernetes](https://kubernetes.io/) clusters, pods, and containers
- [Docker](https://www.docker.com/), [Podman](https://podman.io/), and [LXD](https://linuxcontainers.org/lxd/introduction/) container instances located on any host
- [SSH](https://www.ssh.com/academy/ssh/protocol) connections, config files, and tunnels
- [Windows Subsystem for Linux](https://ubuntu.com/wsl), [Cygwin](https://www.cygwin.com/), and [MSYS2](https://www.msys2.org/) instances
- [Powershell Remote Sessions](https://learn.microsoft.com/en-us/powershell/scripting/learn/remoting/running-remote-commands?view=powershell-7.3)
- Any other custom remote connection methods that work through the command-line

## Connection Hub

- Easily connect to and access all kinds of remote connections in one place
- Securely stores all information exclusively on your computer and encrypts all secret information. See the [security page](/SECURITY.md) for more information
- Allows you to create specific login environments on any system to instantly jump into proper environment for every use case of yours
- Can create desktop shortcuts that automatically open remote connections in your terminal
- Group all your connections into hierarchical categories

![connections](https://github.com/xpipe-io/xpipe/assets/72509152/ef19aa85-1b66-45e0-a051-5a4658758626)

## Remote File Manager

- Interact with the file system of any remote system using a workflow optimized for professionals
- Quickly open a terminal into any directory
- Utilize your favourite local programs to open and edit remote files
- Dynamically elevate sessions with sudo when required

The feature set is the same for all supported connection types. It also supports browsing the file system on your local machine.

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

Note that this is a desktop application that should be run on your local desktop workstation, not on any server or containers. It will be able to connect to your server infrastructure with ease from your local machine.

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

### Command-line

You can also install XPipe by pasting the installation command into your terminal. This will perform the full setup automatically.

#####  Linux / MacOS

The script supports installation via `apt`, `rpm`, and `pacman` on Linux, plus a `.pkg` install on macOS:

```
bash <(curl -sL https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.sh)
```

Alternatively on arch, the [xpipe AUR package](https://aur.archlinux.org/packages/xpipe) can also be installed via `yay -S xpipe`.

##### Windows

This script will automatically install the `.msi` for you.

```
powershell -ExecutionPolicy Bypass -Command iwr "https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.ps1" -OutFile "$env:TEMP\get-xpipe.ps1" ";"  "&" "$env:TEMP\get-xpipe.ps1"
```

If you like chocolatey, you can also install the [xpipe choco package](https://community.chocolatey.org/packages/xpipe/1.6.0):

```
choco install xpipe
```

## Commercial usage

Recently I decided to try to develop XPipe full-time. To finance this, there now is a professional tier intended for commercial users.
The free community tier comes with the full feature set and no restrictions on anything as long you are using it for non-commercial purposes. For commercial usage, I would like to ask you to purchase an [XPipe professional license](https://buy.xpipe.io/checkout/buy/dbcd37b8-be94-40a5-8c1c-af61979e6537). You can try out XPipe as much as you want in a non-commercial setting or start a free trial if you want to test it in your commercial environments prior to purchasing a license.

## Open source model

XPipe utilizes an open core model, which essentially means that the main application is open source while certain other components are not. Select parts are not open source yet, but may be added to this repository in the future.

This mainly concerns the features only available in the professional tier and the shell handling library implementation and extensions for configuring and handling shell connections. Furthermore, some tests and especially test environments and that run on private servers are also not included in this repository. Finally, scripts and workflows to create and publish installers and packages are also not included to prevent attackers from easily impersonating the XPipe application.

## Further information

You have more questions? Then check out the [FAQ](/FAQ.md).

For information about the security model of XPipe, see the [security page](/SECURITY.md).

For information about the privacy policy of XPipe, see the [privacy page](/PRIVACY.md).

In case you're interested in development, check out the [development page](/DEVELOPMENT.md).

If you want to talk you can also join:

- The [XPipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [XPipe Slack Server](https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)

