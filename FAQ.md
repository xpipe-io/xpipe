# Frequently asked questions

## What is so new and different about this?

Compared to other existing tools, the fundamental approach of how to
connect and how to communicate with remote systems differs.
Other tools utilize the established protocol-based approach, i.e. connect and communicate with a
server via a certain protocol like SSH, SFTP, and many more using a bundled library for that purpose.
XPipe instead utilizes a shell-based approach that works on top of command-line programs.
It exclusively interacts with your installed command-line programs via their stdout, stderr,
and stdin to handle local and remote shell connections.
This approach makes it much more flexible as it doesn't have to deal with any file system APIs, remote file handling protocols, or libraries at all as that part is delegated to your existing programs.

Let's use the example of SSH.
Protocol-based programs come with an included SSH library that allows them to interact with a remote system via SSH.
XPipe does not ship with any sort of SSH library or similar.
Instead, XPipe starts a new process using your local `ssh` executable, which is usually the OpenSSH client.
I.e. it launches the process `ssh user@host` in the background and communicates
with the opened remote shell through the stdout, stderr, stdin of the process.
From there, it detects what kind of server and environment,
e.g. shell type, os, etc. you have logged into with that shell connection,
and adjusts how it talks to the remote system from there.
It effectively delegates everything protocol and connection related to your external programs.

As a result of this approach, you can do stuff with XPipe that you can't do with other tools.
One example would be connecting and accessing files on a docker container as there's no real protocol to formally connect here by default.
XPipe can simply execute `docker exec -i <name> sh` to open a shell into the container
and handle the file management through this opened shell by sending commands like `ls`, `touch`, and more.

More broadly, XPipe can work on any shell connection, regardless of how it is established.
From its perspective, there's no visible difference between a
remote ssh connection, a shell in a docker container, or your local system shell.

If you are more interested in the implementation details,
you can read the [introduction article](https://foojay.io/today/presenting-xpipe/) on foojay.io.

## Does it run on my system?

The desktop application should run on any reasonably up-to-date Windows/Linux/macOS system that has been released in the last ten years.

In case you are running this on a very slow system, there is also a performance mode available in the settings menu to reduce the visual fidelity and make the application more responsive.

## Is this secure / Can I entrust my sensitive information to this?

Due to its nature, XPipe has to handle a lot of sensitive information like passwords, keys, and more. As security plays a very important role here, there exists a dedicated [security details page](/SECURITY.md) that should contain all relevant information for you to make your decision.

In short, all is transferred in an encrypted manner to other programs. You can choose whether you want to store sensitive information within XPipe or source it from other sources such as password managers. If you store that sensitive data in XPipe, it is also stored encrypted on your local machine. You can also set a custom master password to improve the encryption security of your data further.

## How does XPipe handle privacy?

XPipe does not collect any personal data.
The only case in which some sort of data is collected is when the built-in error reporter is used to submit a report.
This report data is limited to general system and error information, no sensitive information is submitted.
For those people who like to read legalese, there's the [privacy policy](/PRIVACY.md).

## Do I have to pay to use this effectively?

I recently decided to develop XPipe full time and hope to finance this by providing plans for professional and commercial users.
The commercialization model is designed to be very generous for personal users. If you don't use XPipe for commercial purposes, you can use it basically without any limitations for free. If you intend to use it for commercial purposes or want to support the development, you can check out the [available tiers](https://buy.xpipe.io/checkout/buy/dbcd37b8-be94-40a5-8c1c-af61979e6537).

## Which release type should I choose?

You are able to essentially get the same feature set regardless which way you choose. There are a few small exceptions, such as desktop environment integrations for your operating system that are only available with installers, however these features are not crucial to XPipe.

Especially in its early development stage, it can be pretty important to frequently distribute new releases. How exactly the update process is handled depends on your distribution:

- Installers (msi/deb/rpm/pkg): They come with the ability to automatically check for
  updates, download them, and install them if you provide your confirmation.
- Portable versions (zip/tar.gz/dmg): They can check for updates and will notify you if one is available but
  lack the ability to install them. You therefore have to download and extract them manually.
- Package managers: They can check for updates and will notify you if one is available
  by allowing you to copy and paste the applicable package manager command in your terminal.

Note that you can choose to disable this update check functionality entirely in the settings menu.

## Does it matter which type of release I choose initially?

Not really, they all share the same configuration data locations. You can switch between different release types, e.g. from the portable version to an installer without any issues if you just want to try it out without installing.

There also exists a separate PTB (Public Test Build) release that is meant for testing out new features early on. You can find them at https://github.com/xpipe-io/xpipe-ptb if you're interested. The regular releases and PTB releases are designed to not interfere with each other and can therefore be installed and used side by side.

## How can I save/export my configuration data?

If you want to export or share the whole connection list, you can find all the data at ~/.xpipe/storage. You can also change that directory in the settings menu.

A simple solution is to change the storage directory to be in a cloud directory like OneDrive or Dropbox so it automatically synchronizes the data across all systems.

The professional version also comes with a feature to synchronize your storage with a remote git repository that you can host yourself wherever you like. This comes with the advantage of a commit history for individual connections and the ability to share this repository data with other team members using the access management of your git platform.

## Can I contribute to this project?

Yes, check out the [contribution page](/CONTRIBUTING.md) for details.

## Why are there no GitHub actions workflows in this repository?

There are several test workflows run in a private environment as they use private test connections such as remote server connections and database connections. Other private workflows are responsible for packaging, signing, and distributing the releases and are also kept private due to them handling a lot of passwords and API keys. So you can assume that the code is tested and the release is automated!

## What is the best way to reach out to the developers and other users?

You can always open a GitHub issue in this repository in case you encounter a problem. There are also several other ways to reach out, so you can choose whatever you like best:

- [XPipe Discord Server](https://discord.gg/8y89vS8cRb)
- [XPipe Slack Server](https://join.slack.com/t/XPipe/shared_invite/zt-1awjq0t5j-5i4UjNJfNe1VN4b_auu6Cg)

## What is XPipe not?

XPipe is not:

- a backup tool: It is not designed to copy large masses of files across systems reliably.
- a system management tool: While it allows you to access any remote system, it does not come with a fancy management dashboard and overview for your server infrastructure.
- a terminal emulator: XPipe is designed around integrating with your own favorite terminal and will allow you to launch any preconfigured shell connection in it. It does not come with any integrated terminal functionality itself.
- a separate protocol handling implementation: XPipe does not come with its own libraries to handle protocols, so it is not able to connect via SSH without a locally installed SSH client like OpenSSH
- an RDP/VNC client: It does not support these protocols (yet)

## What will definitely not be implemented?

While the general development direction is still very open, there are a few things that definitely won't be implemented:

- A mobile version, an app store version, and a flatpak version: The concept of integrating with your local CLI tools is incompatible with most sandboxes.
