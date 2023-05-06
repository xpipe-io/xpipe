# Frequently asked questions

## What is so new about this?

Compared to other existing tools, the fundamental approach of how to
connect and how to communicate with the remote system differs.
Other tools utilize the established protocol-based approach, i.e. connect and communicate with a
server via a certain protocol like SSH, SFTP, and many more.
X-Pipe utilizes a shell-based approach that works on top of command-line programs.

Let's use the example of SSH.
Protocol-based programs come with an included SSH library that allows them to interact with a remote system via SSH.
This requires an SSH server implementation to be running on the remote system.
X-Pipe does not ship with any sort of SSH library or similar.
Instead, X-Pipe creates a new process using your local `ssh` executable, which is usually the OpenSSH client.
I.e. it launches the process `ssh user@host` in the background and communicates
with the opened remote shell through the stdout, stderr, stdin of the process.
From there, it detects what kind of server and environment,
e.g. shell type, os, etc. you have logged into with that shell connection,
and adjusts how it talks to the remote system from there.
It effectively delegates everything protocol and connection related to your external programs.

As a result of this approach, you can do stuff with X-Pipe that you can't do with other tools.
One example would be connecting and accessing files on a
docker container as there's no real protocol to formally connect here by default.
X-Pipe can simply execute `docker exec -i <name> sh` to open a shell into the container
and handle this shell exactly the same way as any other shell connection.

More broadly, X-Pipe can work on any shell connection, regardless of how it is established.
From its perspective, there's no visible difference between a
remote ssh connection and a shell to a local docker container.

## Does it run on my system?

The desktop application should run on any reasonably up-to-date
Windows/Linux/macOS system that has been released in the last ten years.

## What else do I need to use this?

As mentioned previously, X-Pipe itself does not ship with any sort of libraries for connection handling
and instead delegates this to your existing command-line tools.
For this approach to work however, you need to have the required tools installed.

For example, if you want to connect to a remote system via SSH with X-Pipe,
you need to have an `ssh` client installed and added to your PATH.
The exact vendor and version of this `ssh` command-line
tool doesn't matter as long as the standard options are supported.

If a required program is attempted to be used but can not be found, X-Pipe will notify you.

## Is this secure / Can I entrust my sensitive information to this?

Due to its nature, X-Pipe has to handle a lot of sensitive information like passwords, keys, and more.
As security plays a very important role here, there exists a dedicated [security page](/SECURITY.md)
that should contain all relevant information for you to make your decision.

## How does X-Pipe handle privacy?

X-Pipe does not collect any sort of data like usage or tracking data.
The only case in which some sort of data is collected is when you choose to
use the built-in error reporter to submit a report.
This report data is limited to general system and error information, no sensitive information is submitted.
For those people who like to read legalese, there's the [privacy policy](/PRIVACY.md).

## How does X-Pipe handle updates?

Especially in its early development stage, it can be pretty important to frequently distribute new releases.
How exactly the update process is handled depends on your distribution:

- Installers (msi/deb/rpm/pkg): They come with the ability to automatically check for
  updates, download them, and install them if you provide your confirmation.
- Portable versions (zip/tar.gz/dmg): They can check for updates and will notify you if one is available but
  lack the ability to install them. You therefore have to download and install them manually.
- Package managers: They can check for updates and will notify you if one is available
  by allowing you to copy and paste the applicable package manager command in your terminal.

Note that you can choose to disable this functionality entirely in the settings menu.

## Why are there no GitHub actions workflows in this repository?

There are several test workflows run in a private environment as they use private test connections
such as remote server connections and database connections.
Other private workflows are responsible for packaging, signing, and distributing the releases.
So you can assume that the code is tested and the release is automated!

## What is the best way to reach out to the developers and other users?

There are several to reach out, so you can choose whatever you like best:

- [X-Pipe Discord Server](https://discord.gg/8y89vS8cRb)
- [X-Pipe Slack Server](https://join.slack.com/t/x-pipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)
- [X-Pipe Issue Tracker](https://github.com/xpipe-io/xpipe/issues)

## I want to be the first to test use new features. How can I do that?

Most new releases are first published as a pre-release only.
By enabling the setting to download pre-releases in the settings menu when looking for updates,
you can be the first to use a new version.

