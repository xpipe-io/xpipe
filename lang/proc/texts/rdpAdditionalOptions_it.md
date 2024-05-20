# Opzioni RDP aggiuntive

Se vuoi personalizzare ulteriormente la tua connessione, puoi farlo fornendo le proprietà RDP nello stesso modo in cui sono contenute nei file .rdp. Per un elenco completo delle proprietà disponibili, consulta https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

Queste opzioni hanno il formato `opzione:tipo:valore`. Ad esempio, per personalizzare le dimensioni della finestra del desktop, puoi passare la seguente configurazione:
```
larghezza del desktop:i:*larghezza*
desktopheight:i:*height*
```
