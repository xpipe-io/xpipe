## Binding

Informacje o powiązaniu, które podajesz, są przekazywane bezpośrednio do klienta `ssh` w następujący sposób: `-L [origin_address:]origin_port:remote_address:remote_port`.

Domyślnie origin będzie powiązany z interfejsem loopback, jeśli nie określono inaczej. Możesz także użyć dowolnych symboli wieloznacznych adresu, np. ustawiając adres na `0.0.0.0` w celu powiązania ze wszystkimi interfejsami sieciowymi dostępnymi przez IPv4. Jeśli całkowicie pominiesz adres, zostanie użyty symbol wieloznaczny `*`, który zezwala na połączenia na wszystkich interfejsach sieciowych. Zauważ, że niektóre notacje interfejsów sieciowych mogą nie być obsługiwane we wszystkich systemach operacyjnych. Na przykład serwery Windows nie obsługują symboli wieloznacznych `*`.
