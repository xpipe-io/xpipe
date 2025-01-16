## Bindning

Den bindningsinformation som du tillhandahåller skickas direkt till `ssh`-klienten enligt följande: `-L [origin_address:]origin_port:remote_address:remote_port`.

Som standard kommer origin att binda till loopback-gränssnittet om inget annat anges. Du kan också använda jokertecken för adresser, t.ex. genom att ange adressen till `0.0.0.0` för att binda till alla nätverksgränssnitt som är tillgängliga via IPv4. Om du helt utelämnar adressen används jokertecknet `*`, som tillåter anslutningar på alla nätverksgränssnitt. Observera att vissa notationer för nätverksgränssnitt kanske inte stöds av alla operativsystem. Windows-servrar stöder t.ex. inte jokertecknet `*`.
