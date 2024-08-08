# Implementations

XPipe delegates the serial handling to external tools.
There are multiple available tools XPipe can delegate to, each with their own advantages and disadvantages.
To use them, it is required that they are available on the host system.
Most options should be supported by all tools, but some more exotic options might not be.

Before connecting, XPipe will verify that the selected tool is installed and supports all configured options.
If that check is successful, the selected tool will launch.

