<p align="center">
    <a href="https://xpipe.io" target="_blank" rel="noopener">
        <img src="https://github.com/xpipe-io/.github/raw/main/img/banner.png" alt="XPipe Banner" />
    </a>
</p>

<h1></h1>

## About

XPipe is a new type of shell connection hub and remote file manager that allows you to access your entire server infrastructure from your local machine. It works on top of your installed command-line programs and does not require any setup on your remote systems. So if you normally use CLI tools like `ssh`, `docker`, `kubectl`, etc. to connect to your servers, you can just use XPipe on top of that.

XPipe fully integrates with your tools such as your favourite text/code editors, terminals, shells, command-line tools and more. The platform is designed to be extensible, allowing anyone to add easily support for more tools or to implement custom functionality through a modular extension system.

It currently supports:
- [SSH](https://www.ssh.com/academy/ssh/protocol) connections, config files, and tunnels
- [Docker](https://www.docker.com/), [Podman](https://podman.io/), and [LXD](https://linuxcontainers.org/lxd/introduction/) container instances located on any host
- [Windows Subsystem for Linux](https://ubuntu.com/wsl), [Cygwin](https://www.cygwin.com/), and [MSYS2](https://www.msys2.org/) instances
- [Proxmox PVE](https://www.proxmox.com/en/proxmox-virtual-environment/overview) virtual machines and containers
- [Kubernetes](https://kubernetes.io/) clusters, pods, and containers
- [Powershell Remote Sessions](https://learn.microsoft.com/en-us/powershell/scripting/learn/remoting/running-remote-commands?view=powershell-7.3)
- Any other custom remote connection methods that work through the command-line

## Connection hub

- Easily connect to and access all kinds of remote connections in one place
- Organize all your connections in hierarchical categories so you can keep an overview hundreds of connections
- Create specific login environments on any system to instantly jump into a properly set up environment for every use case
- Quickly perform various commonly used actions like starting/stopping containers, establishing tunnels, and more
- Create desktop shortcuts that automatically open remote connections in your terminal  without having to open any GUI

![Connections](https://github.com/xpipe-io/.github/raw/main/img/hub_shadow.png)

## Powerful file management

- Interact with the file system of any remote system using a workflow optimized for professionals
- Quickly open a terminal session into any directory in your favourite terminal emulator
- Utilize your entire arsenal of locally installed programs to open and edit remote files
- Dynamically elevate sessions with sudo when required without having to restart the session
- Seamlessly transfer files from and to your system desktop environment
- Work and perform transfers on multiple systems at the same time with the built-in tabbed multitasking

![Browser](https://github.com/xpipe-io/.github/raw/main/img/browser_shadow.png)

## Terminal launcher

- Boots you into a shell session in your favourite terminal with one click. Automatically fills password prompts and more
- Comes with support for all commonly used terminal emulators across all operating systems
- Supports opening custom terminal emulators as well via a custom command-line spec
- Works with all command shells such as bash, zsh, cmd, PowerShell, and more, locally and remote
- Connects to a system while the terminal is still starting up, allowing for faster connections than otherwise possible

<br>
<p align="center">
  <img src="https://github.com/xpipe-io/.github/raw/main/img/terminal.gif" alt="Terminal launcher"/>
</p>
<br>

## Versatile scripting system

- Create reusable simple shell scripts, templates, and groups to run on connected remote systems
- Automatically make your scripts available in the PATH on any remote system without any setup
- Setup shell init environments for connections to fully customize your work environment for every purpose
- Open custom shells and custom remote connections by providing your own commands

![scripts](https://github.com/xpipe-io/.github/raw/main/img/scripts_shadow.png)

## Secure vault

- All data is stored exclusively on your local system in a cryptographically secure vault. You can also choose to increase security by using a custom master passphrase for further encryption
- XPipe is able to retrieve secrets automatically from your password manager via it's command-line interface.
- There are no servers involved, all your information stays on your systems. The XPipe application does not send any personal or sensitive information to outside services.
- Vault changes can be pushed and pulled from your own remote git repository by multiple team members across many systems

## Programmatic connection control via the API

- The XPipe API provides programmatic access to XPipe’s features via an HTTP interface
- Manage all your remote systems and access their file systems in your own favorite programming language
- Either call the API directly or with the help of the [python library](https://github.com/xpipe-io/xpipe-python-api)
- Full documentation can be either found in the application itself under the API tab or in the [OpenAPI](/openapi.yaml) spec file

# Downloads

Note that this is a desktop application that should be run on your local desktop workstation, not on any server or containers. It will be able to connect to your server infrastructure from there.

## Windows

Installers are the easiest way to get started and come with an optional automatic update functionality:

- [Windows .msi Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-windows-x86_64.msi)

You can also install XPipe by pasting the installation command into your terminal. This will perform the .msi setup automatically:

```
powershell -ExecutionPolicy Bypass -Command iwr "https://github.com/xpipe-io/xpipe/raw/master/get-xpipe.ps1" -OutFile "$env:TEMP\get-xpipe.ps1" ";"  "&" "$env:TEMP\get-xpipe.ps1"
```

If you don't like installers, you can also use a portable version that is packaged as an archive:

- [Windows .zip Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-windows-x86_64.zip)

Alternatively, you can also use the following package managers:
- [choco](https://community.chocolatey.org/packages/xpipe) to install it with `choco install xpipe`.
- [winget](https://github.com/microsoft/winget-cli) to install it with `winget install xpipe-io.xpipe --source winget`.

## Linux

You can install XPipe the fastest by pasting the installation command into your terminal. This will perform the setup automatically.
The script supports installation via `apt`, `dnf`, `yum`, `zypper`, `rpm`, and `pacman` on Linux:

```
bash <(curl -sL https://github.com/xpipe-io/xpipe/raw/master/get-xpipe.sh)
```

Of course, there are also other installation methods available.

### Debian-based distros

The following debian installers are available:

- [Linux .deb Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-x86_64.deb)
- [Linux .deb Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-arm64.deb)

Note that you should use apt to install the package with `sudo apt install <file>` as other package managers, for example dpkg,
are not able to resolve and install any dependency packages.

### RHEL-based distros

The following rpm installers are available:

- [Linux .rpm Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-x86_64.rpm)
- [Linux .rpm Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-linux-arm64.rpm)

The same applies here, you should use a package manager that supports resolving and installing required dependencies if needed.

### Arch

There is an official [AUR package](https://aur.archlinux.org/packages/xpipe) available that you can either install manually or via an AUR helper such as with `yay -S xpipe`.

### NixOS

There's an official [xpipe nixpkg](https://search.nixos.org/packages?channel=unstable&show=xpipe&from=0&size=50&sort=relevance&type=packages&query=xpipe) available that you can install with `nix-env -iA nixos.xpipe`. This one is however not always up to date.

There is also a custom repository that contains the latest up-to-date releases: https://github.com/xpipe-io/nixpkg.
You can install XPipe by following the instructions in the linked repository.

### Portable

In case you prefer to use an archive version that you can extract anywhere, you can use these:

- [Linux .tar.gz Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-x86_64.tar.gz)
- [Linux .tar.gz Portable (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-arm64.tar.gz)

Alternatively, there are also AppImages available:

- [Linux .AppImage Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-x86_64.AppImage)
- [Linux .AppImage Portable (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-linux-arm64.AppImage)

Note that the portable version assumes that you have some basic packages for graphical systems already installed
as it is not a perfect standalone version. It should however run on most systems.

## macOS

Installers are the easiest way to get started and come with an optional automatic update functionality:

- [MacOS .pkg Installer (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-macos-x86_64.pkg)
- [MacOS .pkg Installer (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-installer-macos-arm64.pkg)

You also can install XPipe by pasting the installation command into your terminal. This will perform the `.pkg` installation automatically:

```
bash <(curl -sL https://github.com/xpipe-io/xpipe/raw/master/get-xpipe.sh)
```

If you don't like installers, you can also use a portable version that is packaged as an archive:

- [MacOS .dmg Portable (x86-64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-macos-x86_64.dmg)
- [MacOS .dmg Portable (ARM 64)](https://github.com/xpipe-io/xpipe/releases/latest/download/xpipe-portable-macos-arm64.dmg)

Alternatively, you can also use [Homebrew](https://github.com/xpipe-io/homebrew-tap) to install XPipe with `brew install --cask xpipe-io/tap/xpipe`.

## Early access releases

Prior to major releases, there will be several Public Test Build (PTB) releases published at https://github.com/xpipe-io/xpipe-ptb to see whether everything is production ready and contain the latest new features.

In case you're interested in trying out the PTB versions, you can easily do so without any limitations. The regular releases and PTB releases are designed to not interfere with each other and can therefore be installed and used side by side.

# Further information

## Open source model

XPipe follows an open core model, which essentially means that the main application is open source while certain other components are not. This mainly concerns the features only available in the professional edition and the shell handling library implementation. Furthermore, some CI pipelines and tests that run on private servers are also not included in the open repository.

The distributed XPipe application consists out of two parts:
- The open-source core that you can find this repository. It is licensed under the [Apache License 2.0](/LICENSE.md).
- The closed-source extensions, mostly for professional edition features, which are not included in this repository

Additional features are available in the professional edition. For more details see https://xpipe.io/pricing.
If your enterprise puts great emphasis on having access to the full source code, there are also full source-available enterprise options available.

## More links

You have more questions? Then check out the [FAQ](https://xpipe.io/#faq).

For information about the security model of XPipe, see the [security page](https://xpipe.io/features#security).

For information about the privacy policy of XPipe, see the [privacy policy](https://docs.xpipe.io/privacy-policy).

In case you're interested in development, check out the [contributing page](/CONTRIBUTING.md).

[![Discord](https://discordapp.com/api/guilds/979695018782646285/widget.png?style=banner2)](https://discord.gg/8y89vS8cRb)
