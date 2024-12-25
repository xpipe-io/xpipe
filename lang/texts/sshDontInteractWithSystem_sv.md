## Detektering av skaltyp

XPipe fungerar genom att detektera anslutningens shelltyp och sedan interagera med det aktiva skalet. Detta tillvägagångssätt fungerar dock bara när shell-typen är känd och stöder en viss mängd åtgärder och kommandon. Alla vanliga skal som `bash`, `cmd`, `powershell`, med flera, stöds.

## Okända typer av skal

Om du ansluter till ett system som inte kör ett känt kommandoschell, t.ex. en router, länk eller någon IOT-enhet, kommer XPipe inte att kunna upptäcka shell-typen och felar efter en tid. Genom att aktivera det här alternativet kommer XPipe inte att försöka identifiera skaltypen och starta skalet som det är. Detta gör att du kan öppna anslutningen utan fel men många funktioner, t.ex. filbläddraren, skript, underanslutningar och mer, kommer inte att stödjas för den här anslutningen.
