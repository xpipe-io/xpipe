## Sistem etkileşimi

XPipe, her şeyin doğru çalıştığını doğrulamak ve sistem bilgilerini görüntülemek için ne tür bir kabukta oturum açtığını tespit etmeye çalışır. Bu, bash gibi normal komut kabukları için işe yarar, ancak birçok gömülü sistem için standart olmayan ve özel oturum açma kabukları için başarısız olur. Bu sistemlere yapılan bağlantıların başarılı olması için bu davranışı devre dışı bırakmanız gerekir.

Bu etkileşim devre dışı bırakıldığında, herhangi bir sistem bilgisini tanımlamaya çalışmayacaktır. Bu, sistemin dosya tarayıcısında veya diğer bağlantılar için bir proxy/geçit sistemi olarak kullanılmasını önleyecektir. XPipe daha sonra esasen sadece bağlantı için bir başlatıcı olarak hareket edecektir.
