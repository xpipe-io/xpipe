## VNC målsystem

Utöver de vanliga VNC-funktionerna lägger XPipe till ytterligare funktioner genom interaktion med målsystemets systemskal.

I vissa fall kan VNC-serverns värd, dvs. fjärrsystemet där VNC-servern körs, skilja sig från det faktiska system som du kontrollerar med VNC. Om en VNC-server t.ex. hanteras av en VM-hypervisor som Proxmox körs servern på hypervisorvärden medan det faktiska målsystemet som du styr, t.ex. en VM, är VM-gästen. För att se till att t.ex. filsystemoperationer tillämpas på rätt system kan du manuellt ändra målsystemet om det skiljer sig från VNC-serverns värd.