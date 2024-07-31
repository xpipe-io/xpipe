## Windows

Windows sistemlerinde seri portlara genellikle `COM<index>` ile başvurursunuz.
XPipe ayrıca `COM` öneki olmadan sadece dizinin belirtilmesini de destekler.
9'dan büyük portları adreslemek için, `\\.\COM<index>` ile UNC yol formunu kullanmanız gerekir.

Eğer bir WSL1 dağıtımı yüklüyse, seri portlara WSL dağıtımı içinden `/dev/ttyS<index>` ile de başvurabilirsiniz.
Ancak bu artık WSL2 ile çalışmamaktadır.
Eğer bir WSL1 sisteminiz varsa, bunu seri bağlantı için ana bilgisayar olarak kullanabilir ve XPipe ile erişmek için tty gösterimini kullanabilirsiniz.

## Linux

Linux sistemlerinde seri portlara genellikle `/dev/ttyS<index>` üzerinden erişebilirsiniz.
Eğer bağlı cihazın ID'sini biliyorsanız ancak seri portu takip etmek istemiyorsanız, `/dev/serial/by-id/<device id>` üzerinden de referans verebilirsiniz.
`ls /dev/serial/by-id/*` komutunu çalıştırarak mevcut tüm seri bağlantı noktalarını kimlikleriyle birlikte listeleyebilirsiniz.

## macOS

MacOS'ta seri bağlantı noktası adları hemen hemen her şey olabilir, ancak genellikle `/dev/tty.<id>` biçimindedir; burada id dahili aygıt tanımlayıcısıdır.
`ls /dev/tty.*` çalıştırıldığında mevcut seri portlar bulunacaktır.
