# Ek RDP seçenekleri

Bağlantınızı daha da özelleştirmek isterseniz, bunu RDP özelliklerini .rdp dosyalarında olduğu gibi sağlayarak yapabilirsiniz. Kullanılabilir özelliklerin tam listesi için bkz. https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files.

Bu seçenekler `option:type:value` biçimindedir. Örneğin, masaüstü penceresinin boyutunu özelleştirmek için aşağıdaki yapılandırmayı iletebilirsiniz:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
