<img src="https://user-images.githubusercontent.com/72509152/213873342-7638e830-8a95-4b5d-ad3e-5a9a0b4bf538.png" alt="drawing" width="250"/>

### A flexible connection manager and remote file explorer

X-Pipe is a brand-new type of connection manager and remote file explorer that works by only interacting with command-line
tools on local and remote shell connections.
This approach makes it much more flexible as it doesn't have to deal with file system APIs or remote file handling
protocols at all.

It currently supports:
- Containers located on any host, e.g. [docker](https://www.docker.com/) or [LXD](https://linuxcontainers.org/lxd/introduction/) container instances
- [SSH](https://www.ssh.com/academy/ssh/protocol) connections
- [Windows Subsystem for Linux](https://ubuntu.com/wsl) instances located on any host
- [Powershell Remote Sessions](https://learn.microsoft.com/en-us/powershell/scripting/learn/remoting/running-remote-commands?view=powershell-7.3)
- Any other custom remote connection methods that works through the command-line
- Arbitrary types of proxies to establish connections

Furthermore, X-Pipe integrates with your existing tools and workflows
by outsourcing as many tasks as possible to your favourite
text/code editors, terminals, shells, command-line tools and more.
The platform is designed to be extensible, allowing anyone
to implement custom functionality through extensions.

## Getting Started

Head over to the [releases page](https://github.com/xpipe-io/xpipe/releases/latest) and try it out.

## Features

### Remote file explorer

- Access the file system of any remote system
- Quickly open a terminal into any directory
- Run commands from the explorer interface
- Utilize your favourite local programs to open and edit remote files

![Remote file explorer](https://user-images.githubusercontent.com/72509152/230100929-4476f76c-ea81-43d9-ac4a-b3b02df2334e.png)

### Connection manager

- Easily create and manage all kinds of remote connections at one location
- Securely stores all information exclusively on your computer and encrypts all secret information. See
  the [security page](/SECURITY.md) for more info
- Create desktop shortcuts to automatically open your connections in your terminal

![Connection manager](https://user-images.githubusercontent.com/72509152/230098966-000596ca-8167-4cb8-8ada-f6b3a7d482e2.png)

### Instant launch for remote shells and commands

- Automatically login into a shell in your favourite terminal with one click (no need to fill password prompts, etc.)
- Works for all kinds of shells. This includes command shells (e.g. bash, PowerShell, cmd, etc.) and database shells (
  e.g. PostgreSQL Shell)
- Comes with integrations for all commonly used terminals for all operating systems
- Allows you to customize the launched shell's init environment
- Launches from the GUI or command-line

## Further information

In case you're interested in development, check out the [development page](/DEVELOPMENT.md).

For information about the security model of X-Pipe, see the [security page](/SECURITY.md).

If you want to talk you can also join:

- The [X-Pipe Discord Server](https://discord.gg/8y89vS8cRb)
- The [X-Pipe Slack Server](https://join.slack.com/t/x-pipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)
