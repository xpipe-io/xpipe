## SSH configurations

Here you can specify any SSH options that should be passed to the connection.
While some options are essentially required to successfully establish a connection, such as `HostName`,
many other options are purely optional.

To get an overview over all possible options, you can use [`man ssh_config`](https://linux.die.net/man/5/ssh_config) or read this [guide](https://www.ssh.com/academy/ssh/config).
The exact amount of supported options purely depends on your installed SSH client.

### Formatting

The content here is equivalent to one host section in an SSH config file.
Note that you don't have to explicitly define the `Host` entry, as that will be done automatically.

If you intend to define more than one host section, e.g. with dependent connections such as a proxy jump host that depends on another config host, you can define multiple host entries in here as well. XPipe will then launch the first host entry.

You don't have to perform any formatting with whitespace or indentation, this is not needed for it to function.

### Identities

Note that you can also specify an `IdentityFile` option in here.
If this option is specified in here, any otherwise specified key-based authentication option later down below will be ignored.
