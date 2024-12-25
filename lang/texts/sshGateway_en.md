## Shell connection gateways

If enabled, XPipe first opens a shell connection to the gateway and from there opens a SSH connection to the specified host. The `ssh` command must be available and located in the `PATH` on your chosen gateway.

### Jump servers

This mechanism is similar to jump servers, but not equivalent. It is completely independent of the SSH protocol, so you can use any shell connection as a gateway.

If you are looking for proper SSH jump servers, maybe also in combination with agent forwarding, use the custom SSH connection functionality with the `ProxyJump` configuration option.