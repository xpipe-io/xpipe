## Binden

De bindingsinformatie die je opgeeft wordt direct doorgegeven aan de `ssh` client als volgt: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Standaard zal het remote bronadres zich binden aan de loopback interface. Je kunt ook gebruik maken van wildcards voor adressen, bijvoorbeeld door het adres in te stellen op `0.0.0.0` om te binden aan alle netwerkinterfaces die toegankelijk zijn via IPv4. Als je het adres helemaal weglaat, wordt het jokerteken `*` gebruikt, dat verbindingen op alle netwerkinterfaces toestaat. Merk op dat sommige notaties voor netwerkinterfaces mogelijk niet op alle besturingssystemen worden ondersteund. Windows servers bijvoorbeeld ondersteunen het jokerteken `*` niet.
