<img src="https://user-images.githubusercontent.com/72509152/213873342-7638e830-8a95-4b5d-ad3e-5a9a0b4bf538.png" alt="drawing" width="250"/>

### The remote file browser for professionals

XPipe is a brand-new type of remote file browser that works by only interacting with your installed command-line tools on local and remote shell connections. This approach makes it much more flexible as it doesn't have to deal with file system APIs or remote file handling protocols at all.

It currently supports:
- Containers located on any host such as [docker](https://www.docker.com/) or [LXD](https://linuxcontainers.org/lxd/introduction/) container instances
- [SSH](https://www.ssh.com/academy/ssh/protocol) connections
- [Windows Subsystem for Linux](https://ubuntu.com/wsl) instances
- [Powershell Remote Sessions](https://learn.microsoft.com/en-us/powershell/scripting/learn/remoting/running-remote-commands?view=powershell-7.3)
- [Kubernetes](https://kubernetes.io/) clusters and their contained pods and containers
- Any other custom remote connection methods that works through the command-line
- Arbitrary types of proxies to establish connections

Furthermore, XPipe integrates with your existing tools and workflows by delegating all tasks to your favourite text/code editors, terminals, shells, command-line tools and more. The platform is designed to be extensible, allowing anyone to implement custom functionality through extensions.

You have more questions? Then check out the new [FAQ](/FAQ.md).

## Getting Started

Head over to the [releases page](https://github.com/xpipe-io/xpipe/releases/latest) and try it out.

## Features

### Flexible remote file browser

- Interact with the file system of any remote system using a workflow optimized for professionals
- Quickly open a terminal into any directory
- Utilize your favourite local programs to open and edit remote files

![Remote file explorer](https://user-images.githubusercontent.com/72509152/230100929-4476f76c-ea81-43d9-ac4a-b3b02df2334e.png)

### Simple connection management

- Easily create and manage all kinds of remote connections
- Securely stores all information exclusively on your computer and encrypts all secret information. See
  the [security page](/SECURITY.md) for more information
- Create custom desktop shortcuts to automatically open specific remote connections in your terminal

![Connection manager](https://user-images.githubusercontent.com/72509152/230098966-000596ca-8167-4cb8-8ada-f6b3a7d482e2.png)

### Instant launch for remote shells and commands

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells. This includes command shells (e.g. bash, PowerShell, cmd, etc.) and some database shells (e.g. PostgreSQL Shell)
- Comes with support for all commonly used terminals across all operating systems
- Allows you to customize the launched shell's init environment
- Supports launches from the GUI or command-line

## Further information

For information about the security model of XPipe, see the [security page](/SECURITY.md).

For information about the privacy policy of XPipe, see the [privacy page](/PRIVACY.md).

In case you're interested in development, check out the [development page](/DEVELOPMENT.md).

If you want to talk you can also join:

- The [XPipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [XPipe Slack Server](https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)
