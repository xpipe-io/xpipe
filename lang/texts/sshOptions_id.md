## Konfigurasi SSH

Di sini Anda dapat menentukan opsi SSH apa pun yang harus diteruskan ke koneksi.
Meskipun beberapa opsi pada dasarnya diperlukan untuk membuat sambungan dengan sukses, seperti `HostName`,
banyak pilihan lain yang hanya bersifat opsional.

Untuk mendapatkan gambaran umum tentang semua opsi yang mungkin, Anda dapat menggunakan [`man ssh_config`] (https://linux.die.net/man/5/ssh_config) atau membaca [panduan] ini (https://www.ssh.com/academy/ssh/config).
Jumlah pasti opsi yang didukung sepenuhnya bergantung pada klien SSH yang Anda instal.

pemformatan ###

Konten di sini setara dengan satu bagian hos dalam berkas konfigurasi SSH.
Perhatikan bahwa Anda tidak perlu secara eksplisit mendefinisikan kunci `Host`, karena akan dilakukan secara otomatis.

Jika Anda bermaksud mendefinisikan lebih dari satu bagian host, misalnya dengan koneksi dependen seperti host lompatan proksi yang bergantung pada host konfigurasi lain, Anda dapat mendefinisikan beberapa entri host di sini juga. XPipe kemudian akan meluncurkan entri host pertama.

Anda tidak perlu melakukan pemformatan apa pun dengan spasi atau lekukan, ini tidak diperlukan agar dapat berfungsi.

Perhatikan bahwa Anda harus berhati-hati dalam mengutip nilai apa pun jika mengandung spasi, jika tidak, nilai tersebut akan diteruskan dengan tidak benar.

### File identitas

Perhatikan bahwa Anda juga dapat menentukan opsi `IdentityFile` di sini.
Jika opsi ini ditentukan di sini, opsi autentikasi berbasis kunci yang ditentukan di bawahnya akan diabaikan.

Jika Anda memilih untuk merujuk ke berkas identitas yang dikelola di brankas git XPipe, Anda juga dapat melakukannya.
XPipe akan mendeteksi berkas identitas bersama dan secara otomatis menyesuaikan jalur berkas pada setiap sistem tempat Anda mengkloning git vault.
