## Höjd

Processen för upphöjning är specifik för operativsystemet.

### Linux & macOS

Alla förhöjda kommandon körs med `sudo`. Det valfria lösenordet `sudo` frågas via XPipe vid behov.
Du har möjlighet att justera upphöjningsbeteendet i inställningarna för att kontrollera om du vill ange ditt lösenord varje gång det behövs eller om du vill cacha det för den aktuella sessionen.

### Windows

I Windows är det inte möjligt att höja en underordnad process om inte den överordnade processen också är höjd.
Därför, om XPipe inte körs som administratör, kommer du inte att kunna använda någon förhöjning lokalt.
För fjärranslutningar måste det anslutna användarkontot ges administratörsbehörighet.