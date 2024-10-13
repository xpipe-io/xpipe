# VM SSH identities

If your VM guest user requires key-based authentication for SSH, you can enable this here.

Note that it is assumed that your VM is not exposed to the public, so the VM host system is used as an SSH gateway.
As a result, any identity option is specified relative to the VM host system and not your local machine.
Any key you specify here is interpreted as a file on the VM host.
If you are using any agent, is expected that the agent is running on the VM host system not on your local machine.

### None

If selected, XPipe will not supply any identities. This also disables any external sources like agents.

### Identity file

You can also specify an identity file with an optional passphrase.
This option is the equivalent of `ssh -i <file>`.

Note that this should be the *private* key, not the public one.
If you mix that up, ssh will only give you cryptic error messages.

### SSH-Agent

In case your identities are stored in the SSH-Agent, the ssh executable can use them if the agent is started.
XPipe will automatically start the agent process if it is not running yet.

If you don't have the agent set up on the VM host system, is recommended that you enable SSH agent forwarding for the original SSH connection to the VM host.
You can do that by creating a custom SSH connection with the `ForwardAgent` option enabled.

### GPG Agent

If your identities are stored for example on a smartcard, you can choose to provide them to the SSH client via the `gpg-agent`.
This option will automatically enable SSH support of the agent if not enabled yet and restart the GPG agent daemon with the correct settings.

### Yubikey PIV

If your identities are stored with the PIV smart card function of the Yubikey, you can retreive
them with Yubico's YKCS11 library, which comes bundled with Yubico PIV Tool.

Note that you need an up-to-date build of OpenSSH in order to use this feature.

### Custom PKCS#11 library

This will instruct the OpenSSH client to load the specified shared library file, which will handle the authentication.

Note that you need an up-to-date build of OpenSSH in order to use this feature.

### Pageant (Windows)

In case you are using pageant on Windows, XPipe will check whether pageant is running first.
Due to the nature of pageant, it is your responsibility to have it
running as you manually have to specify all keys you would like to add every time.
If it is running, XPipe will pass the proper named pipe via
`-oIdentityAgent=...` to ssh, you don't have to include any custom config files.

### Pageant (Linux & macOS)

In case your identities are stored in the pageant agent, the ssh executable can use them if the agent is started.
XPipe will automatically start the agent process if it is not running yet.

### Other external source

This option will permit any external running identity provider to supply its keys to the SSH client. You should use this option if you are using any other agent or password manager to manage your SSH keys.
