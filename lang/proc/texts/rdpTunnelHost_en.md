## RDP Tunnel Host

You can choose to connect to a remote RDP host via an SSH tunnel. This gives you the ability to use the more advanced SSH authentication features with RDP out of the box.

When this option is used, the host address in the RDP file will be replaced by the chosen hostname of the SSH connection. Upon first connection, an SSH tunnel will be established and the RDP client will connect to the tunneled connection via localhost instead. 