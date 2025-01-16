## Gateways för Shell-anslutning

Om det är aktiverat öppnar XPipe först en shell-anslutning till gatewayen och därifrån öppnas en SSH-anslutning till den angivna värden. Kommandot `ssh` måste vara tillgängligt och finnas i `PATH` på din valda gateway.

### Hoppa över servrar

Den här mekanismen liknar jump-servrar, men är inte likvärdig. Den är helt oberoende av SSH-protokollet, så du kan använda vilken shell-anslutning som helst som gateway.

Om du är ute efter riktiga SSH jump-servrar, kanske även i kombination med agent forwarding, ska du använda funktionen för anpassade SSH-anslutningar med konfigurationsalternativet `ProxyJump`.