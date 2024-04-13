## X11 doorsturen

Als deze optie is ingeschakeld, wordt de SSH verbinding gestart met X11 forwarding ingesteld. Op Linux werkt dit meestal out of the box en hoeft het niet ingesteld te worden. Op macOS heb je een X11 server zoals [XQuartz](https://www.xquartz.org/) nodig op je lokale machine.

### X11 op Windows

Met XPipe kun je de WSL2 X11 mogelijkheden gebruiken voor je SSH verbinding. Het enige wat je hiervoor nodig hebt is een [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) distributie geïnstalleerd op je lokale systeem. XPipe zal automatisch een compatibele geïnstalleerde distributie kiezen als dat mogelijk is, maar je kunt ook een andere gebruiken in het instellingenmenu.

Dit betekent dat je geen aparte X11 server op Windows hoeft te installeren. Maar als je er toch een gebruikt, zal XPipe dat detecteren en de huidige X11 server gebruiken.
