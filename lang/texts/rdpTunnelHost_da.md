## RDP Tunnel Host

Du kan oprette forbindelse til en ekstern RDP-vært via en SSH-tunnel. Dette giver dig mulighed for at bruge de mere avancerede SSH-godkendelsesfunktioner med RDP out of the box.

Ved første forbindelse etableres en SSH-tunnel, og RDP-klienten opretter forbindelse til tunnelforbindelsen via localhost i stedet. Den vil bruge legitimationsoplysningerne for SSH-forbindelsesbrugeren til RDP-godkendelse.