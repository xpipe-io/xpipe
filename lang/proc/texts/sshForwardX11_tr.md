## X11 Yönlendirme

Bu seçenek etkinleştirildiğinde, SSH bağlantısı X11 yönlendirme kurulumu ile başlatılacaktır. Linux'ta bu genellikle kutudan çıkar çıkmaz çalışır ve herhangi bir kurulum gerektirmez. MacOS'ta, yerel makinenizde [XQuartz](https://www.xquartz.org/) gibi bir X11 sunucusunun çalışıyor olması gerekir.

### Windows üzerinde X11

XPipe, SSH bağlantınız için WSL2 X11 yeteneklerini kullanmanıza izin verir. Bunun için ihtiyacınız olan tek şey yerel sisteminizde kurulu bir [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) dağıtımıdır. XPipe mümkünse otomatik olarak uyumlu bir dağıtım seçecektir, ancak ayarlar menüsünden başka bir dağıtım da kullanabilirsiniz.

Bu, Windows'a ayrı bir X11 sunucusu kurmanıza gerek olmadığı anlamına gelir. Ancak, yine de bir tane kullanıyorsanız, XPipe bunu algılayacak ve o anda çalışan X11 sunucusunu kullanacaktır.
