# Ek RDP seçenekleri

Bağlantınızı daha da özelleştirmek istiyorsanız, bunu .rdp dosyalarında olduğu gibi RDP özelliklerini sağlayarak yapabilirsiniz. Kullanılabilir özelliklerin tam listesi için [RDP belgelerine] (https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files) bakın.

Bu seçenekler `option:type:value` biçimindedir. Örneğin, masaüstü penceresinin boyutunu özelleştirmek için aşağıdaki yapılandırmayı iletebilirsiniz:
```
desktopwidth:i:*width*
desktopheight:i:*height*
```
