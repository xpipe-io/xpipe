## VNC-Zielsystem

Zusätzlich zu den normalen VNC-Funktionen fügt XPipe durch die Interaktion mit der System-Shell des Zielsystems weitere Funktionen hinzu.

In einigen Fällen kann sich der VNC-Server-Host, d.h. das entfernte System, auf dem der VNC-Server läuft, von dem eigentlichen System unterscheiden, das du mit VNC steuerst. Wenn ein VNC-Server zum Beispiel von einem VM-Hypervisor wie Proxmox verwaltet wird, läuft der Server auf dem Hypervisor-Host, während das eigentliche Zielsystem, das du steuerst, zum Beispiel eine VM, der VM-Gast ist. Um sicherzustellen, dass zum Beispiel Dateisystemoperationen auf dem richtigen System ausgeführt werden, kannst du das Zielsystem manuell ändern, wenn es sich vom VNC-Server-Host unterscheidet.