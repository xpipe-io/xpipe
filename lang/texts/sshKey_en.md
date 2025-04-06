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

### Password manager agent

If you are using a password manager with an SSH agent functionality, you can choose to use it here. XPipe will verify it doesn't conflict with any other agent configuration. XPipe however can't start this agent by itself, you have to ensure that it is running.

### GPG Agent

If your identities are stored for example on a smartcard, you can choose to provide them to the SSH client via the `gpg-agent`.
This option will automatically enable SSH support of the agent if not enabled yet and restart the GPG agent daemon with the correct settings.

### Pageant (Windows)

In case you are using pageant on Windows, XPipe will check whether pageant is running first.
Due to the nature of pageant, it is your responsibility to have it
running as you manually have to specify all keys you would like to add every time.
If it is running, XPipe will pass the proper named pipe via
`-oIdentityAgent=...` to ssh, you don't have to include any custom config files.

### Pageant (Linux & macOS)

In case your identities are stored in the pageant agent, the ssh executable can use them if the agent is started.
XPipe will automatically start the agent process if it is not running yet.

### Yubikey PIV

If your identities are stored with the PIV smart card function of the Yubikey, you can retreive
them with Yubico's YKCS11 library, which comes bundled with Yubico PIV Tool.

Note that you need an up-to-date build of OpenSSH in order to use this feature.

### Custom PKCS#11 library

This will instruct the OpenSSH client to load the specified shared library file, which will handle the authentication.

Note that you need an up-to-date build of OpenSSH in order to use this feature.

### Other external source

This option will permit any external running identity provider to supply its keys to the SSH client. You should use this option if you are using any other agent or password manager to manage your SSH keys.
