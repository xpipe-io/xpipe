# RDPリモートアプリケーション

XPipeでRDP接続を使うと、フルデスクトップを開かずにリモートアプリケーションやスクリプトを素早く起動できる。しかし、RDPの性質上、これを動作させるには、サーバー上のリモートアプリケーション許可リストを編集する必要がある。

## RDP許可リスト

RDPサーバーは、許可リストという概念を使ってアプリケーションの起動を処理する。このため、許可リストが無効になっているか、特定のアプリケーショ ンが明示的に許可リストに追加されていない限り、リモートアプリケーションを直接起動す ることはできない。

許可リストの設定は、サーバーのレジストリの`HKEY_LOCAL_MACHINESOFTWARE`にある。

### すべてのアプリケーションを許可する

許可リストを無効にして、XPipeからすべてのリモートアプリケーションを直接起動できるようにすることができる。そのためには、PowerShellでサーバー上で次のコマンドを実行する: `Set-ItemProperty -Path 'HKLM:￭SOFTWARE￭Microsoft￭Windows NT￭CurrentVersion￭Terminal Server￭TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### 許可されたアプリケーションを追加する

別の方法として、個々のリモートアプリケーションをリストに追加することもできる。これにより、リストにあるアプリケーションをXPipeから直接起動できるようになる。

`TSAppAllowList`の`Applications`キーの下に、任意の名前で新しいキーを作成する。名前の唯一の条件は、"Applications "キーの子キー内で一意であることである。`Name`、`Path`、`CommandLineSetting`である。PowerShellでは、以下のコマンドでこれを行うことができる：

```
appName="メモ帳"
AppPath="C:¥WindowsSystem¥notepad.exe"

$regKey="HKLM:￤SOFTWARE￤Microsoft￤Windows NT￤CurrentVersion￤Terminal Server￤TSAppAllowList￤Applications"
New-item -Path "$regKey$appName"
New-ItemProperty -Path "$regKey$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
<コード>`<コード

XPipeがスクリプトを実行したり、ターミナル・セッションを開いたりすることも許可したい場合は、`C:³³³³cmd.exe`も許可リストに追加する必要がある。 

## セキュリティに関する考慮事項

RDP接続を起動するときは、常に同じアプリケーションを手動で実行できるので、これによってサーバーが安全でなくなることはない。許可リストは、クライアントがユーザーの入力なしに即座にアプリケーションを実行することを防ぐためのものである。結局のところ、XPipeを信頼するかどうかはあなた次第だ。これは、XPipeの高度なデスクトップ統合機能を使用する場合にのみ役立つ。
