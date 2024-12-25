## Tunnelbinding

De bindingsinformatie die je opgeeft wordt direct doorgegeven aan de `ssh` client als volgt: `-D [adres:]poort`.

Standaard zal het adres zich binden aan de loopback interface. Je kunt ook gebruik maken van wildcards voor adressen, bijvoorbeeld door het adres in te stellen op `0.0.0.0` om te binden aan alle netwerkinterfaces die toegankelijk zijn via IPv4. Als je het adres helemaal weglaat, wordt het jokerteken `*` gebruikt, dat verbindingen op alle netwerkinterfaces toestaat. Merk op dat sommige notaties voor netwerkinterfaces mogelijk niet op alle besturingssystemen worden ondersteund. Windows servers bijvoorbeeld ondersteunen het jokerteken `*` niet.
