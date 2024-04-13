## SSH configuraties

Hier kun je SSH opties opgeven die aan de verbinding moeten worden doorgegeven.
Hoewel sommige opties in principe nodig zijn om succesvol een verbinding op te zetten, zoals `HostName`,
zijn veel andere opties puur optioneel.

Om een overzicht te krijgen van alle mogelijke opties, kun je [`man ssh_config`](https://linux.die.net/man/5/ssh_config) gebruiken of deze [guide](https://www.ssh.com/academy/ssh/config) lezen.
Het exacte aantal ondersteunde opties hangt puur af van je geïnstalleerde SSH-client.

### Opmaak

De inhoud hier komt overeen met een hostsectie in een SSH config bestand.
Merk op dat je de `Host` sleutel niet expliciet hoeft te definiëren, want dat wordt automatisch gedaan.

Als je van plan bent om meer dan één hostsectie te definiëren, bijvoorbeeld met afhankelijke verbindingen zoals een proxy jump host die afhankelijk is van een andere config host, dan kun je hier ook meerdere hostregels definiëren. XPipe zal dan de eerste host starten.

Je hoeft geen opmaak met spaties of inspringen uit te voeren, dit is niet nodig om het te laten werken.

Merk op dat je moet zorgen voor het citeren van waarden als ze spaties bevatten, anders worden ze verkeerd doorgegeven.

### Identiteitsbestanden

Merk op dat je hier ook een `IdentityFile` optie kunt opgeven.
Als deze optie hier wordt opgegeven, wordt elke anders opgegeven sleutelgebaseerde authenticatieoptie verderop genegeerd.

Als je ervoor kiest om te verwijzen naar een identiteitsbestand dat wordt beheerd in de XPipe git vault, dan kun je dat ook doen.
XPipe zal gedeelde identiteitsbestanden detecteren en automatisch het bestandspad aanpassen op elk systeem waarop je de git vault hebt gekloond.
