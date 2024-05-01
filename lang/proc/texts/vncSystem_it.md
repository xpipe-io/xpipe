## Sistema di destinazione VNC

Oltre alle normali funzioni di VNC, XPipe aggiunge ulteriori funzioni attraverso l'interazione con la shell del sistema di destinazione.

In alcuni casi l'host del server VNC, cioè il sistema remoto su cui viene eseguito il server VNC, potrebbe essere diverso dal sistema effettivo che stai controllando con VNC. Ad esempio, se un server VNC è gestito da un hypervisor VM come Proxmox, il server viene eseguito sull'host dell'hypervisor mentre il sistema di destinazione effettivo che stai controllando, ad esempio una VM, è il guest VM. Per assicurarti che, ad esempio, le operazioni sul file system vengano eseguite sul sistema corretto, puoi cambiare manualmente il sistema di destinazione se è diverso dall'host del server VNC.