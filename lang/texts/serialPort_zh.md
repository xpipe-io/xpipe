## 窗口

在 Windows 系统中，您通常通过 `COM<index>` 引用串行端口。
XPipe 也支持只指定索引而不使用 `COM` 前缀。
要寻址大于 9 的端口，您必须使用 UNC 路径形式，即 `\.\COM<index>`。

如果安装了 WSL1 发行版，也可以在 WSL 发行版中通过 `/dev/ttyS<index>` 引用串行端口。
不过，这种方法在 WSL2 中不再适用。
如果您有 WSL1 系统，可以将其作为串行连接的主机，并使用 tty 符号通过 XPipe 访问。

## Linux

在 Linux 系统中，通常可以通过 `/dev/ttyS<index>` 访问串口。
如果您知道所连接设备的 ID，但不想跟踪串行端口，也可以通过 `/dev/serial/by-id/<device id>` 引用它们。
运行 `ls /dev/serial/by-id/*` 可以列出所有可用串行端口及其 ID。

## macOS

在 macOS 上，串行端口名称几乎可以是任何名称，但通常采用 `/dev/tty.<id>` 的形式，其中 id 是内部设备标识符。
运行 `ls /dev/tty.*` 可以找到可用的串行端口。
