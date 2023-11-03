<img src="https://github.com/xpipe-io/xpipe/assets/72509152/88d750f3-8469-4c51-bb64-5b264b0e9d47" alt="drawing" width="250"/>

XPipe is a new type of shell connection hub and remote file manager that allows you to access your entire server infrastructure from your local machine. It works on top of your installed command-line programs that you normally use to connect and does not require any setup on your remote systems.

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
- Securely stores all information exclusively on your computer and encrypts all secret information
- Allows you to create specific login environments on any system to instantly jump into a properly set up environment for every use case
- Can create desktop shortcuts that automatically open remote connections in your terminal
- Group all your connections into hierarchical categories

![connections](https://github.com/xpipe-io/xpipe/assets/72509152/3a690fe3-29b8-43fc-a1d1-1dee9be71d4d)

## Remote File Manager

- Interact with the file system of any remote system using a workflow optimized for professionals
- Quickly open a terminal session into any directory in your favourite terminal emulator
- Utilize your favourite local programs to open and edit remote files
- Dynamically elevate sessions with sudo when required without having to restart the session
- Integrates with your local desktop environment for a seamless transfer of local files

![browser](https://github.com/xpipe-io/xpipe/assets/72509152/60d70293-c513-4f12-b242-30610ce5ab5d)

## Versatile scripting system

- Create reusable simple shell scripts, templates, and groups to run on connected remote systems
- Automatically make your scripts available in the PATH on any remote system without any setup
- Setup shell init environments for connections to fully customize your work environment for every purpose
- Open custom shells and custom remote connections by providing your own commands

![browser](https://github.com/xpipe-io/xpipe/assets/72509152/2d473f7b-ae1d-4dd1-86a3-02658b094da5)

## Terminal Launcher

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells and connections, locally and remote.
- Supports command shells (e.g. bash, PowerShell, cmd, etc.) and some database shells (e.g. PostgreSQL Shell)
- Comes with support for all commonly used terminal emulators across all operating systems
- Supports launches from the GUI or directly from the command-line
- Solves all encoding issues on Windows systems as all Windows shells are launched in UTF8 mode by default

<br>
<p align="center">
  <img src="https://github.com/xpipe-io/xpipe/assets/72509152/02351317-f25d-4af3-8116-bc3b4fb92312" alt="Terminal launcher"/>
</p>
<br>

# Downloads

Note that this is a desktop application that should be run on your local desktop workstation, not on any server or containers. It will be able to connect to your server infrastructure from there.

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

#### Windows

This script will automatically install the `.msi` for you.

```
powershell -ExecutionPolicy Bypass -Command iwr "https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.ps1" -OutFile "$env:TEMP\get-xpipe.ps1" ";"  "&" "$env:TEMP\get-xpipe.ps1"
```

####  Linux / MacOS

The script supports installation via `apt`, `rpm`, and `pacman` on Linux, plus a `.pkg` install on macOS:

```
bash <(curl -sL https://raw.githubusercontent.com/xpipe-io/xpipe/master/get-xpipe.sh)
```

### Package managers

Alternatively, you can also use your favorite package manager (if supported):

- [choco](https://community.chocolatey.org/packages/xpipe): `choco install xpipe`
- [AUR package](https://aur.archlinux.org/packages/xpipe): `yay -S xpipe`
- [Homebrew](https://github.com/xpipe-io/homebrew-tap): `brew install --cask xpipe-io/tap/xpipe`

## Open source model

XPipe utilizes an open core model, which essentially means that the main application is open source while certain other components are not. Select parts are not open source yet, but may be added to this repository in the future.

This mainly concerns the features only available in the professional tier and the shell handling library implementation. Furthermore, some tests and especially test environments and that run on private servers are also not included in this repository.

## Further information

You have more questions? Then check out the [FAQ](https://xpipe.io/faq).

For information about the security model of XPipe, see the [security page](/SECURITY.md).

For information about the privacy policy of XPipe, see the [privacy page](/PRIVACY.md).

In case you're interested in development, check out the [contributing page](/CONTRIBUTING.md).

If you want to talk you can also join:

- The [XPipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [XPipe Slack Server](https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)

