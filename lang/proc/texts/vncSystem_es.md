## Sistema de destino VNC

Además de las funciones normales de VNC, XPipe también añade funciones adicionales mediante la interacción con el shell del sistema de destino.

En algunos casos, el host del servidor VNC, es decir, el sistema remoto en el que se ejecuta el servidor VNC, puede ser distinto del sistema real que estás controlando con VNC. Por ejemplo, si un servidor VNC está gestionado por un hipervisor VM como Proxmox, el servidor se ejecuta en el host del hipervisor, mientras que el sistema de destino real que estás controlando, por ejemplo una VM, es el invitado de la VM. Para asegurarte de que, por ejemplo, las operaciones del sistema de archivos se aplican en el sistema correcto, puedes cambiar manualmente el sistema de destino si difiere del anfitrión del servidor VNC.