# Implementierungen

XPipe delegiert die serielle Verarbeitung an externe Tools.
Es gibt mehrere Tools, an die XPipe delegieren kann, jedes mit seinen eigenen Vor- und Nachteilen.
Um sie zu nutzen, müssen sie auf dem Hostsystem verfügbar sein.
Die meisten Optionen sollten von allen Tools unterstützt werden, aber einige exotischere Optionen sind es vielleicht nicht.

Bevor eine Verbindung hergestellt wird, prüft XPipe, ob das ausgewählte Tool installiert ist und alle konfigurierten Optionen unterstützt.
Wenn diese Prüfung erfolgreich ist, wird das ausgewählte Tool gestartet.

