## Yükseklik

İzinlerin yükseltilmesi süreci işletim sistemine özgüdür.

### Linux ve macOS

Herhangi bir yükseltilmiş komut `sudo` ile yürütülür. İsteğe bağlı `sudo` parolası gerektiğinde XPipe aracılığıyla sorgulanır. Parolanıza her ihtiyaç duyulduğunda girmek isteyip istemediğinizi veya mevcut oturum için önbelleğe almak isteyip istemediğinizi kontrol etmek için ayarlarda yükseltme davranışını ayarlama olanağına sahipsiniz.

### Windows

Windows'ta, üst süreç de yükseltilmiş izinlerle çalışmıyorsa, bir alt sürecin izinlerini yükseltmek mümkün değildir. Bu nedenle, XPipe yönetici olarak çalıştırılmazsa, yerel olarak herhangi bir yükseltme kullanamazsınız. Uzak bağlantılar için, bağlı kullanıcı hesabına yönetici ayrıcalıkları verilmelidir.