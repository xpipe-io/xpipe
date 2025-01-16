# Ytterligare RDP-alternativ

Om du vill anpassa din anslutning ytterligare kan du göra det genom att tillhandahålla RDP-egenskaper på samma sätt som de finns i .rdp-filer. En fullständig lista över tillgängliga egenskaper finns i [RDP-dokumentationen] (https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Dessa alternativ har formatet `alternativ:typ:värde`. Om du t.ex. vill anpassa storleken på skrivbordsfönstret kan du skicka följande konfiguration:
```
skrivbordsbredd:i:*bredd*
skrivbordshöjd:i:*höjd*
```
