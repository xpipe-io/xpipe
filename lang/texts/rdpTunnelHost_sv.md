## RDP Tunnel Värd

Du kan ansluta till en fjärransluten RDP-värd via en SSH-tunnel. Detta ger dig möjlighet att använda de mer avancerade SSH-autentiseringsfunktionerna med RDP direkt från start.

Vid den första anslutningen upprättas en SSH-tunnel och RDP-klienten ansluter till den tunnlade anslutningen via localhost istället. Den kommer att använda autentiseringsuppgifterna för SSH-anslutningens användare för RDP-autentisering.