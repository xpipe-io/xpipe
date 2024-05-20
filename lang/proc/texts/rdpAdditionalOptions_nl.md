# Extra RDP-opties

Als je je verbinding verder wilt aanpassen, kun je dat doen door RDP eigenschappen op te geven zoals ze in .rdp bestanden staan. Voor een volledige lijst van beschikbare eigenschappen, zie https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

Deze opties hebben het formaat `optie:type:waarde`. Om bijvoorbeeld de grootte van het bureaubladvenster aan te passen, kun je de volgende configuratie doorgeven:
```
bureaubladbreedte:i:*breedte*
bureaubladhoogte:i:*hoogte*
```
