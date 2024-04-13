## Interação do sistema

XPipe tenta detetar que tipo de shell ele entrou para verificar se tudo funcionou corretamente e para exibir informações do sistema. Isso funciona para shells de comando normais como bash, mas falha para shells de login não-padrão e personalizados para muitos sistemas embarcados. Tens de desativar este comportamento para que as ligações a estes sistemas sejam bem sucedidas.

Quando esta interação está desactivada, não tenta identificar qualquer informação do sistema. Isso impedirá que o sistema seja usado no navegador de arquivos ou como um sistema proxy/gateway para outras conexões. O XPipe irá então agir essencialmente apenas como um lançador para a conexão.
