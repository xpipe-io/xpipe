## X11 Yönlendirme

Bu seçenek etkinle?tirildi?inde, SSH ba?lant?s? X11 yönlendirme kurulumu ile ba?lat?lacakt?r. Linux'ta bu genellikle kutudan ç?kar ç?kmaz çal???r ve herhangi bir kurulum gerektirmez. MacOS'ta, yerel makinenizde [XQuartz](https://www.xquartz.org/) gibi bir X11 sunucusunun çal???yor olmas? gerekir.

### Windows üzerinde X11

XPipe, SSH ba?lant?n?z için WSL2 X11 yeteneklerini kullanman?za izin verir. Bunun için ihtiyac?n?z olan tek ?ey yerel sisteminizde kurulu bir [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) da??t?m?d?r. XPipe mümkünse otomatik olarak uyumlu bir da??t?m seçecektir, ancak ayarlar menüsünden ba?ka bir da??t?m da kullanabilirsiniz.

Bu, Windows'a ayr? bir X11 sunucusu kurman?za gerek olmad??? anlam?na gelir. Ancak, yine de bir tane kullan?yorsan?z, XPipe bunu alg?layacak ve o anda çal??an X11 sunucusunu kullanacakt?r.
