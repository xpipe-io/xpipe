# Yderligere RDP-muligheder

Hvis du vil tilpasse din forbindelse yderligere, kan du gøre det ved at angive RDP-egenskaber på samme måde, som de er indeholdt i .rdp-filer. For en komplet liste over tilgængelige egenskaber, se https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

Disse indstillinger har formatet `indstilling:type:værdi`. Hvis du f.eks. vil tilpasse størrelsen på skrivebordsvinduet, kan du sende følgende konfiguration:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
