## VNC doelsysteem

Naast de normale VNC functies voegt XPipe ook extra functies toe door interactie met de systeemshell van het doelsysteem.

In een paar gevallen kan de VNC server host, d.w.z. het externe systeem waar de VNC server op draait, anders zijn dan het systeem dat je bestuurt met VNC. Als een VNC-server bijvoorbeeld wordt beheerd door een VM-hypervisor zoals Proxmox, dan draait de server op de hypervisor-host, terwijl het eigenlijke doelsysteem dat je bestuurt, bijvoorbeeld een VM, de VM-gast is. Om er zeker van te zijn dat bijvoorbeeld bestandssysteembewerkingen op het juiste systeem worden toegepast, kun je het doelsysteem handmatig wijzigen als het verschilt van de VNC server host.