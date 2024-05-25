# Дополнительные опции RDP

Если ты хочешь еще больше настроить свое соединение, то можешь сделать это, предоставив свойства RDP так же, как они содержатся в файлах .rdp. Полный список доступных свойств смотри в [документации RDP](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Эти параметры имеют формат `option:type:value`. Так, например, чтобы настроить размер окна рабочего стола, ты можешь передать следующую конфигурацию:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
