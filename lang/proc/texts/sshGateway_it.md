## Gateway di connessione alla shell

Se abilitato, XPipe apre prima una connessione shell al gateway e da lì apre una connessione SSH all'host specificato. Il comando `ssh` deve essere disponibile e presente nel `PATH` del gateway scelto.

### Salta i server

Questo meccanismo è simile ai server di salto, ma non è equivalente. È completamente indipendente dal protocollo SSH, quindi puoi utilizzare qualsiasi connessione shell come gateway.

Se stai cercando dei veri e propri server di salto SSH, magari anche in combinazione con l'inoltro di agenti, utilizza la funzionalità di connessione SSH personalizzata con l'opzione di configurazione `ProxyJump`.