## Sistema de destino VNC

Além dos recursos normais do VNC, o XPipe também adiciona recursos adicionais por meio da interação com o shell do sistema de destino.

Em alguns casos, o host do servidor VNC, ou seja, o sistema remoto onde o servidor VNC é executado, pode ser diferente do sistema real que estás a controlar com o VNC. Por exemplo, se um servidor VNC for gerido por um hipervisor de VM como o Proxmox, o servidor é executado no anfitrião do hipervisor, enquanto o sistema de destino real que estás a controlar, por exemplo uma VM, é o convidado da VM. Para garantir que, por exemplo, as operações do sistema de arquivos sejam aplicadas no sistema correto, é possível alterar manualmente o sistema de destino se ele for diferente do host do servidor VNC.