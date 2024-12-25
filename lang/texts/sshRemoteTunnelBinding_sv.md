## Bindning

Den bindningsinformation som du tillhandahåller skickas direkt till `ssh`-klienten enligt följande: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Som standard kommer fjärrkälladressen att bindas till loopback-gränssnittet. Du kan också använda jokertecken för adresser, t.ex. genom att ange adressen till `0.0.0.0` för att binda till alla nätverksgränssnitt som är tillgängliga via IPv4. Om du helt utelämnar adressen används jokertecknet `*`, som tillåter anslutningar på alla nätverksgränssnitt. Observera att vissa notationer för nätverksgränssnitt kanske inte stöds av alla operativsystem. Windows-servrar stöder t.ex. inte jokertecknet `*`.
