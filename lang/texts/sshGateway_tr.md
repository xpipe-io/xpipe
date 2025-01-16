## Kabuk bağlantı ağ geçitleri

Etkinleştirilirse, XPipe önce ağ geçidine bir kabuk bağlantısı açar ve buradan belirtilen ana bilgisayara bir SSH bağlantısı açar. `ssh` komutunun kullanılabilir olması ve seçtiğiniz ağ geçidinde `PATH` içinde bulunması gerekir.

### Jump sunucuları

Bu mekanizma atlama sunucularına benzer, ancak eşdeğer değildir. SSH protokolünden tamamen bağımsızdır, bu nedenle herhangi bir kabuk bağlantısını ağ geçidi olarak kullanabilirsiniz.

Uygun SSH atlama sunucuları arıyorsanız, belki de ajan yönlendirme ile birlikte, `ProxyJump` yapılandırma seçeneği ile özel SSH bağlantı işlevselliğini kullanın.