## Kabuk ba?lant? a? geçitleri

Etkinle?tirilirse, XPipe önce a? geçidine bir kabuk ba?lant?s? açar ve buradan belirtilen ana bilgisayara bir SSH ba?lant?s? açar. `ssh` komutunun kullan?labilir olmas? ve seçti?iniz a? geçidinde `PATH` içinde bulunmas? gerekir.

### Jump sunucular?

Bu mekanizma atlama sunucular?na benzer, ancak e?de?er de?ildir. SSH protokolünden tamamen ba??ms?zd?r, bu nedenle herhangi bir kabuk ba?lant?s?n? a? geçidi olarak kullanabilirsiniz.

Uygun SSH atlama sunucular? ar?yorsan?z, belki de ajan yönlendirme ile birlikte, `ProxyJump` yap?land?rma seçene?i ile özel SSH ba?lant? i?levselli?ini kullan?n.