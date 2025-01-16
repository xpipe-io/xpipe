## Windows

On Windows systems you typically refer to serial ports via `COM<index>`.
XPipe also supports just specifying the index without the `COM` prefix.
To address ports greater than 9, you have to use the UNC path form with `\\.\COM<index>`.

If you have a WSL1 distribution installed, you can also reference the serial ports from within the WSL distribution via `/dev/ttyS<index>`.
This it does not work with WSL2 anymore though.
If you have a WSL1 system, you can use this one as the host for this serial connection and use the tty notation to access it with XPipe.

## Linux

On Linux systems you can typically access the serial ports via `/dev/ttyS<index>`.
If you know the ID of the connected device but don't want to keep track of the serial port, you can also reference them via `/dev/serial/by-id/<device id>`.
You can list all available serial ports with their IDs by running `ls /dev/serial/by-id/*`.

## macOS

On macOS, the serial port names can be pretty much anything, but usually have the form of `/dev/tty.<id>` where the id the internal device identifier.
Running `ls /dev/tty.*` should find available serial ports.
