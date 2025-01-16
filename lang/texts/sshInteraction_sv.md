## System interaktion

XPipe försöker upptäcka vilken typ av skal den loggade in i för att verifiera att allt fungerade korrekt och för att visa systeminformation. Det fungerar för vanliga kommandoschell som bash, men misslyckas för icke-standardiserade och anpassade inloggningsskal för många inbyggda system. Du måste inaktivera detta beteende för att anslutningar till dessa system ska lyckas.

När den här interaktionen är inaktiverad försöker den inte identifiera någon systeminformation. Detta förhindrar att systemet används i filbläddraren eller som ett proxy/gateway-system för andra anslutningar. XPipe kommer då i huvudsak bara att fungera som en lansering för anslutningen.
