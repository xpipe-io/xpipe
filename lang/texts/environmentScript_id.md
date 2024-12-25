## Skrip inisialisasi

Perintah opsional untuk dijalankan setelah file init dan profil shell dijalankan.

Anda dapat memperlakukannya sebagai skrip shell biasa, yaitu menggunakan semua sintaks yang didukung oleh shell dalam skrip. Semua perintah yang Anda jalankan bersumber dari shell dan memodifikasi lingkungan. Jadi, jika Anda, misalnya, mengatur sebuah variabel, Anda akan memiliki akses ke variabel ini dalam sesi shell ini.

### Memblokir perintah-perintah

Perhatikan bahwa memblokir perintah yang membutuhkan input pengguna dapat membekukan proses shell ketika XPipe memulainya secara internal terlebih dahulu di latar belakang. Untuk menghindari hal ini, panggil perintah pemblokiran ini hanya jika variabel `TERM` tidak disetel ke `dumb`. XPipe secara otomatis menetapkan variabel `TERM=dumb` ketika menyiapkan sesi shell di latar belakang dan kemudian menetapkan `TERM=xterm-256color` ketika benar-benar membuka terminal.