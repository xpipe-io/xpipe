# Dodatkowe opcje RDP

Jeśli chcesz jeszcze bardziej dostosować swoje połączenie, możesz to zrobić, podając właściwości RDP w taki sam sposób, w jaki są one zawarte w plikach .rdp. Pełną listę dostępnych właściwości znajdziesz w [dokumentacji RDP](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Opcje te mają format `option:type:value`. Na przykład, aby dostosować rozmiar okna pulpitu, możesz przekazać następującą konfigurację:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
