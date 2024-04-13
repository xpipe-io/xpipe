### SSH-configuraties

XPipe laadt alle hosts en past alle instellingen toe die je in het geselecteerde bestand hebt geconfigureerd. Dus door een configuratieoptie op globale of hostspecifieke basis op te geven, wordt deze automatisch toegepast op de verbinding die door XPipe tot stand wordt gebracht.

Als je meer wilt weten over het gebruik van SSH-configuraties, kun je `man ssh_config` gebruiken of deze [gids] (https://www.ssh.com/academy/ssh/config) lezen.

### Identiteiten

Merk op dat je hier ook een `IdentityFile` optie kunt opgeven. Als hier een identiteit wordt opgegeven, wordt elke anders opgegeven identiteit verderop genegeerd.

### X11 doorsturen

Als hier opties voor X11 forwarding worden opgegeven, zal XPipe automatisch proberen X11 forwarding op Windows via WSL in te stellen.