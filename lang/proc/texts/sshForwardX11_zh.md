## X11 转发

启用该选项后，SSH 连接将通过 X11 转发设置启动。在 Linux 上，通常开箱即用，无需任何设置。在 macOS 上，你需要在本地计算机上运行类似 [XQuartz](https://www.xquartz.org/) 这样的 X11 服务器。

### Windows 上的 X11

XPipe 允许您使用 WSL2 X11 功能进行 SSH 连接。您唯一需要的是在本地系统上安装一个 [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) 发行版。如果可能，XPipe 会自动选择一个兼容的已安装发行版，但您也可以在设置菜单中使用另一个发行版。

这意味着您无需在 Windows 上安装单独的 X11 服务器。但是，如果您正在使用X11服务器，XPipe会检测到并使用当前运行的X11服务器。
