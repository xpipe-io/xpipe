### SSH configs

XPipe loads all hosts and applies all settings that you have configured in the selected file. So by specifying a configuration option on either a global or host-specific basis, it will automatically be applied to the connection established by XPipe.

If you want to learn more about how to use SSH configs, you can use `man ssh_config` or read this [guide](https://www.ssh.com/academy/ssh/config).

### Identities

Note that you can also specify an `IdentityFile` option in here. If any identity is specified in here, any otherwise specified identity later down below will be ignored.