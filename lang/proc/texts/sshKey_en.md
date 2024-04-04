### None

Disables `publickey` authentication.

### SSH-Agent

In case your identities are stored in the SSH-Agent, the ssh executable can use them if the agent is started.
XPipe will automatically start the agent process if it is not running yet.

### Pageant (Windows)

In case you are using pageant on Windows, XPipe will check whether pageant is running first.
Due to the nature of pageant, it is your responsibility to have it
running as you manually have to specify all keys you would like to add every time.
If it is running, XPipe will pass the proper named pipe via
`-oIdentityAgent=...` to ssh, you don't have to include any custom config files.

Note that there are some implementation bugs in the OpenSSH client that can cause issues
if your username contains spaces or is too long, so try to use the latest version.

### Pageant (Linux & macOS)

In case your identities are stored in the pageant agent, the ssh executable can use them if the agent is started.
XPipe will automatically start the agent process if it is not running yet.

### Identity file

You can also specify an identity file with an optional passphrase.
This option is the equivalent of `ssh -i <file>`.

Note that this should be the *private* key, not the public one.
If you mix that up, ssh will only give you cryptic error messages.

### GPG Agent

If your identities are stored for example on a smartcard, you can choose to provide them to the SSH client via the `gpg-agent`.
This option will automatically enable SSH support of the agent if not enabled yet and restart the GPG agent daemon with the correct settings.

### Yubikey PIV

If your identities are stored with the PIV smart card function of the Yubikey, you can retreive
them with Yubico's YKCS11 library, which comes bundled with Yubico PIV Tool.

Note that you need an up-to-date build of OpenSSH in order to use this feature.

### Custom agent

You can also use a custom agent by providing either the socket location or named pipe location here.
This will pass it via the `IdentityAgent` option.

### Custom PKCS#11 library

This will instruct the OpenSSH client to load the specified shared library file, which will handle the authentication.

Note that you need an up-to-date build of OpenSSH in order to use this feature.
