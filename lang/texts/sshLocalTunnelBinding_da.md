## Binding

De bindingsoplysninger, du angiver, sendes direkte til `ssh`-klienten som følger: `-L [origin_address:]origin_port:remote_address:remote_port`.

Som standard vil origin binde til loopback-grænsefladen, hvis ikke andet er angivet. Du kan også gøre brug af adressejokertegn, f.eks. indstille adressen til `0.0.0.0` for at binde til alle netværksgrænseflader, der er tilgængelige via IPv4. Når du helt udelader adressen, vil jokertegnet `*`, som tillader forbindelser på alle netværksgrænseflader, blive brugt. Bemærk, at nogle notationer for netværksgrænseflader måske ikke understøttes af alle operativsystemer. Windows-servere understøtter f.eks. ikke jokertegnet `*`.
