# 其他 RDP 选项

如果你想进一步自定义连接，可以通过提供 RDP 属性来实现，就像 .rdp 文件中包含的属性一样。有关可用属性的完整列表，请参阅 https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files。

这些选项的格式为 `option:type:value`。例如，要自定义桌面窗口的大小，可以传递以下配置：
```
桌面宽度：i:*宽度*
桌面高度：i:*高度*
```
