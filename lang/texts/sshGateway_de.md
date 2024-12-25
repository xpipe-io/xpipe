## Shell-Verbindungsgateways

Wenn diese Option aktiviert ist, öffnet XPipe zuerst eine Shell-Verbindung zum Gateway und von dort aus eine SSH-Verbindung zum angegebenen Host. Der `ssh`-Befehl muss verfügbar sein und sich im `PATH` des gewählten Gateways befinden.

### Server springen

Dieser Mechanismus ist den Jump Servern ähnlich, aber nicht gleichwertig. Er ist völlig unabhängig vom SSH-Protokoll, so dass du jede Shell-Verbindung als Gateway verwenden kannst.

Wenn du auf der Suche nach richtigen SSH-Sprungservern bist, vielleicht auch in Kombination mit einer Agentenweiterleitung, verwende die benutzerdefinierte SSH-Verbindungsfunktion mit der Konfigurationsoption `ProxyJump`.