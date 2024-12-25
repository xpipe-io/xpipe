## X11 Vidarebefordran

När det här alternativet är aktiverat kommer SSH-anslutningen att startas med X11-vidarebefordran konfigurerad. På Linux fungerar detta vanligtvis direkt och kräver ingen installation. På macOS måste en X11-server som [XQuartz] (https://www.xquartz.org/) köras på din lokala maskin.

### X11 på Windows

XPipe låter dig använda WSL2 X11-funktionerna för din SSH-anslutning. Det enda du behöver för detta är en [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)-distribution installerad på ditt lokala system. XPipe kommer automatiskt att välja en kompatibel installerad distribution om möjligt, men du kan också använda en annan i inställningsmenyn.

Detta innebär att du inte behöver installera en separat X11-server på Windows. Men om du ändå använder en, kommer XPipe att upptäcka det och använda den X11-server som för närvarande körs.

### X11-anslutningar som skrivbord

Alla SSH-anslutningar som har X11-vidarebefordran aktiverad kan användas som en stationär värd. Detta innebär att du kan starta skrivbordsapplikationer och skrivbordsmiljöer via den här anslutningen. När ett skrivbordsprogram startas kommer den här anslutningen automatiskt att startas i bakgrunden för att starta X11-tunneln.
