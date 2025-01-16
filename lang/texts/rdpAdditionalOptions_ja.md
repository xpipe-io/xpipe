# 追加のRDPオプション

接続をさらにカスタマイズしたい場合は、.rdpファイルに含まれるのと同じように、 RDPプロパティを指定することで実現できる。利用可能なプロパティの完全なリストについては、[RDP documentation](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files) を参照のこと。

これらのオプションは、`option:type:value`というフォーマットを持っている。したがって、たとえばデスクトップ・ウィンドウのサイズをカスタマイズするには、次のような設定を渡すことができる：
```
desktopwidth:i:*width*を指定する。
desktopheight:i:*高さ
```
