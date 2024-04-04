## Skriptabhängigkeiten

Die Skripte und Skriptgruppen, die zuerst ausgeführt werden sollen. Wenn eine ganze Gruppe zu einer Abhängigkeit gemacht wird, werden alle Skripte in dieser Gruppe als Abhängigkeiten betrachtet.

Der aufgelöste Abhängigkeitsgraph von Skripten wird abgeflacht, gefiltert und eindeutig gemacht. D.h. es werden nur kompatible Skripte ausgeführt und wenn ein Skript mehrmals ausgeführt werden würde, wird es nur beim ersten Mal ausgeführt.
