### SSH-konfigurationer

XPipe laddar alla värdar och tillämpar alla inställningar som du har konfigurerat i den valda filen. Så genom att ange ett konfigurationsalternativ på antingen en global eller värdspecifik basis kommer det automatiskt att tillämpas på den anslutning som upprättas av XPipe.

Om du vill lära dig mer om hur du använder SSH-konfigurationer kan du använda `man ssh_config` eller läsa den här [guiden] (https://www.ssh.com/academy/ssh/config).

### Identiteter

Observera att du också kan ange ett `IdentityFile`-alternativ här. Om en identitet anges här kommer alla andra identiteter som anges längre ned att ignoreras.

### X11 vidarebefordran

Om några alternativ för X11-vidarebefordran anges här, kommer XPipe automatiskt att försöka ställa in X11-vidarebefordran på Windows via WSL.