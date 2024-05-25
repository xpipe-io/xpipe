# Opciones RDP adicionales

Si quieres personalizar aún más tu conexión, puedes hacerlo proporcionando propiedades RDP de la misma forma que están contenidas en los archivos .rdp. Para obtener una lista completa de las propiedades disponibles, consulta la [documentación sobre RDP](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files).

Estas opciones tienen el formato `opción:tipo:valor`. Así, por ejemplo, para personalizar el tamaño de la ventana del escritorio, puedes pasar la siguiente configuración:
```
ancho escritorio:i:*ancho*
altura escritorio:i:*altura*
```
