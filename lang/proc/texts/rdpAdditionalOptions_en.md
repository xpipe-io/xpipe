# Additional RDP options

If you want to further customize your connection, you can do that by providing RDP properties the same way as they are contained in .rdp files. For a full list of available properties, see https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

These options have the format `option:type:value`. So for example, to customize the size of the desktop window, you can pass the following configuration:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
