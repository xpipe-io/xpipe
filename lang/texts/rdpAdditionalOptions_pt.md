# Opções adicionais de RDP

Se quiseres personalizar ainda mais a tua ligação, podes fazê-lo fornecendo propriedades RDP da mesma forma que estão contidas nos ficheiros .rdp. Para obter uma lista completa das propriedades disponíveis, consulte a [documentação do RDP] (https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Essas opções têm o formato `opção:tipo:valor`. Assim, por exemplo, para personalizar o tamanho da janela da área de trabalho, podes passar a seguinte configuração:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
