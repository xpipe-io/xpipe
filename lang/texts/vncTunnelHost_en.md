## VNC Tunnel Host

You can connect to a remote VNC host via an SSH tunnel. This gives you the ability to use the more advanced SSH authentication features with VNC out of the box. This is important as VNC is fundamentally insecure and unencrypted protocol. Tunneling provides the needed layer of security.

You also don't have to worry about exposing the VNC port on your remote system as it only needs to be reachable via SSH.