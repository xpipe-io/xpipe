## RDP-tunnel host

Je kunt ervoor kiezen om verbinding te maken met een RDP host op afstand via een SSH tunnel. Dit geeft je de mogelijkheid om de meer geavanceerde SSH authenticatie mogelijkheden met RDP out of the box te gebruiken.

Wanneer deze optie wordt gebruikt, wordt het hostadres in het RDP bestand vervangen door de gekozen hostnaam van de SSH verbinding. Bij de eerste verbinding zal er een SSH tunnel worden gemaakt en de RDP client zal in plaats daarvan verbinding maken met de getunnelde verbinding via localhost. 