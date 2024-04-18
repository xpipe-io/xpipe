## RDP desktop integration

You can use this RDP connection in XPipe to quickly launch applications and scripts. However, due to the nature of RDP, you would have to edit the remote application allow list on your server for this to work. You can also choose not to do this and just use XPipe to launch your RDP client without using any advanced desktop integration features.

### RDP allow lists

An RDP server uses the concept of allow lists to handle application launches. This essentially means that unless the allow list is disabled or specific applications have been explicitly added the allow list, launching any remote applications directly will fail.

You can find the allow list settings in the registry of your server at `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

#### Disabling the allow list

You can disable the allow list concept to allow all remote applications to be started directly from XPipe. For this, you can run the following command on your server: `Set-ItemProperty -Path 'HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

#### Adding allowed applications

Alternatively, you can also add individual remote applications to the list. This will then allow you to launch the listed applications directly from XPipe.

Under the `Applications` key of `TSAppAllowList`, create a new key with some arbitrary name. The only requirement for the name is that it is unique within the children of the “Applications” key. This new key, must have two string values in it: `Name` and `Path`. `Name` is the name by which we will refer to the application later when configuring the client, and `Path` is the path to the application on the server:

```
New-Item -Path 'HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications' -Force
New-Item -Path 'HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications\<MyApplication>' -Force
Set-ItemProperty -Path 'HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "Name" -Value "<MyApplication>"
Set-ItemProperty -Path 'HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "Path" -Value "<absolute path of executable>"
```

If you want to allow XPipe to also run scripts and open terminal sessions, you have to add `cmd.exe` to the allow list as well. 

### Security considerations

This does not make your server insecure in any way, as you can always run the same applications manually when launching an RDP connection. Allow lists are more intended to prevent clients from instantly running any application without user input. At the end of the day, it is up to you whether you trust XPipe to do this. You can launch this connection just fine out of the box, this is only useful if you want to use any of the advanced desktop integration features in XPipe.
