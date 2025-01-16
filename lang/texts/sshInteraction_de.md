## Systeminteraktion

XPipe versucht zu erkennen, in welche Art von Shell es sich eingeloggt hat, um zu überprüfen, ob alles richtig funktioniert hat und um Systeminformationen anzuzeigen. Das funktioniert bei normalen Befehlsshells wie der Bash, schlägt aber bei nicht standardmäßigen und benutzerdefinierten Anmeldeshells für viele eingebettete Systeme fehl. Du musst dieses Verhalten deaktivieren, damit Verbindungen zu diesen Systemen erfolgreich sind.

Wenn diese Interaktion deaktiviert ist, wird nicht versucht, irgendwelche Systeminformationen zu ermitteln. Dadurch wird verhindert, dass das System im Dateibrowser oder als Proxy-/Gateway-System für andere Verbindungen verwendet werden kann. XPipe fungiert dann im Wesentlichen nur noch als Startprogramm für die Verbindung.
