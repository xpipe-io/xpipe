## System interaction

XPipe tries to detect what kind of shell it logged into to verify that everything worked correctly and to display system information. That works for normal command shells like bash, but fails for non-standard and custom login shells for many embedded systems. You have to disable this behavior in order for connections to these systems to succeed.

When this interaction is disabled, it will not attempt to identify any system information. This will prevent the system to be used in the file browser or as a proxy/gateway system for other connections. XPipe will then essentially just act as a launcher for the connection.
