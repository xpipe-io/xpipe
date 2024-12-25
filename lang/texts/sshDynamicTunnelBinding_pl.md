## Wiązanie tunelu

Informacje o powiązaniu, które podajesz, są przekazywane bezpośrednio do klienta `ssh` w następujący sposób: `-D [adres:]port`.

Domyślnie adres będzie powiązany z interfejsem loopback. Możesz także użyć dowolnych symboli wieloznacznych adresu, np. ustawiając adres na `0.0.0.0` w celu powiązania ze wszystkimi interfejsami sieciowymi dostępnymi przez IPv4. Jeśli całkowicie pominiesz adres, użyty zostanie symbol wieloznaczny `*`, który zezwala na połączenia na wszystkich interfejsach sieciowych. Zwróć uwagę, że niektóre notacje interfejsów sieciowych mogą nie być obsługiwane we wszystkich systemach operacyjnych. Na przykład serwery Windows nie obsługują symboli wieloznacznych `*`.
