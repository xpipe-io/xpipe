## SSH-konfigurationer

Här kan du ange alla SSH-alternativ som ska skickas till anslutningen.
Vissa alternativ är i princip nödvändiga för att en anslutning ska kunna upprättas, t.ex. `HostName`,
är många andra alternativ helt och hållet valfria.

För att få en överblick över alla möjliga alternativ kan du använda [`man ssh_config`](https://linux.die.net/man/5/ssh_config) eller läsa den här [guiden](https://www.ssh.com/academy/ssh/config).
Det exakta antalet alternativ som stöds beror helt på vilken SSH-klient du har installerat.

### Formatering

Innehållet här motsvarar ett host-avsnitt i en SSH-konfigurationsfil.
Observera att du inte behöver definiera `Host`-nyckeln explicit, eftersom det görs automatiskt.

Om du tänker definiera mer än ett värdavsnitt, t.ex. med beroende anslutningar som en proxyhoppvärd som beror på en annan konfigurationsvärd, kan du också definiera flera värdposter här. XPipe kommer då att starta den första värdposten.

Du behöver inte utföra någon formatering med blanksteg eller indrag, det behövs inte för att det ska fungera.

Observera att du måste ta hand om att citera några värden om de innehåller mellanslag, annars kommer de att skickas felaktigt.

### Identitetsfiler

Observera att du också kan ange ett `IdentityFile`-alternativ här.
Om detta alternativ anges här kommer alla andra angivna nyckelbaserade autentiseringsalternativ längre ned att ignoreras.

Om du väljer att hänvisa till en identitetsfil som hanteras i XPipe git-valvet kan du också göra det.
XPipe kommer att upptäcka delade identitetsfiler och automatiskt anpassa filsökvägen på varje system du klonade git-valvet på.
