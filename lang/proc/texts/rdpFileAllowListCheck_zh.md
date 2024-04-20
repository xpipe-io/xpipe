# RDP 远程应用程序

您可以在 XPipe 中使用 RDP 连接，在不打开完整桌面的情况下快速启动远程应用程序和脚本。不过，由于 RDP 的特性，您必须编辑服务器上的远程应用程序允许列表，才能实现这一功能。

## RDP 允许列表

RDP 服务器使用允许列表的概念来处理应用程序的启动。这基本上意味着，除非允许列表被禁用或特定应用程序已明确添加到允许列表中，否则直接启动任何远程应用程序都将失败。

您可以在服务器注册表中的 `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList 中找到允许列表设置`。

### 允许所有应用程序

您可以禁用允许列表，允许直接从 XPipe 启动所有远程应用程序。为此，您可以在 PowerShell 中的服务器上运行以下命令：`Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`。

### 添加允许的应用程序

或者，你也可以将单个远程应用程序添加到列表中。这样，您就可以直接从 XPipe 启动列出的应用程序。

在 `TSAppAllowList` 的 `Applications` 键下，创建一个具有任意名称的新键。对名称的唯一要求是它在 "Applications "键的子键中是唯一的。这个新键必须包含以下值：`名称`、`路径`和`命令行设置`。您可以使用以下命令在 PowerShell 中完成此操作：

<code>`</code
$appName="记事本"
$appPath="C:\Windows\System32\notepad.exe"。

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
<代码>`</代码

如果您还想允许 XPipe 运行脚本和打开终端会话，则必须在允许列表中添加 `C:\Windows\System32\cmd.exe` 。 

### 安全注意事项

这并不会使你的服务器变得不安全，因为你可以在启动 RDP 连接时手动运行相同的应用程序。允许列表更多是为了防止客户端在没有用户输入的情况下立即运行任何应用程序。说到底，这取决于您是否相信 XPipe 能做到这一点。您可以直接启动该连接，只有当您想使用 XPipe 中的任何高级桌面集成功能时，这才有用。
