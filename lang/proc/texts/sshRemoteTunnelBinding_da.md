## Binding

De bindingsoplysninger, du angiver, sendes direkte til `ssh`-klienten som følger: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Som standard bindes fjernkildeadressen til loopback-grænsefladen. Du kan også bruge adressejokertegn, f.eks. indstille adressen til `0.0.0.0` for at binde til alle netværksgrænseflader, der er tilgængelige via IPv4. Når du helt udelader adressen, vil jokertegnet `*`, som tillader forbindelser på alle netværksgrænseflader, blive brugt. Bemærk, at nogle notationer for netværksgrænseflader måske ikke understøttes af alle operativsystemer. Windows-servere understøtter f.eks. ikke jokertegnet `*`.
