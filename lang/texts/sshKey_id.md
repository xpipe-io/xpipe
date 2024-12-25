### Tidak ada

Jika dipilih, XPipe tidak akan memberikan identitas apa pun. Ini juga menonaktifkan sumber eksternal seperti agen.

### File identitas

Anda juga dapat menentukan berkas identitas dengan kata sandi opsional.
Opsi ini setara dengan `ssh -i <file>`.

Perhatikan bahwa ini harus merupakan kunci *private*, bukan kunci publik.
Jika Anda mencampurnya, ssh hanya akan memberikan pesan kesalahan yang tidak jelas.

### SSH-Agent

Jika identitas Anda disimpan di dalam SSH-Agent, eksekutor ssh dapat menggunakannya jika agen dijalankan.
XPipe akan secara otomatis memulai proses agen jika belum berjalan.

### Agen GPG

Jika identitas Anda disimpan, misalnya pada smartcard, Anda dapat memilih untuk memberikannya pada klien SSH melalui `gpg-agent`.
Opsi ini akan secara otomatis mengaktifkan dukungan SSH pada agen jika belum diaktifkan dan memulai ulang daemon agen GPG dengan pengaturan yang benar.

### Yubikey PIV

Jika identitas Anda disimpan dengan fungsi kartu pintar PIV pada Yubikey, Anda dapat menariknya kembali
mereka dengan pustaka YKCS11 Yubico, yang dibundel dengan Alat PIV Yubico.

Perhatikan bahwa Anda memerlukan versi terbaru dari OpenSSH untuk menggunakan fitur ini.

### Perpustakaan PKCS #11 khusus

Ini akan menginstruksikan klien OpenSSH untuk memuat berkas pustaka bersama yang ditentukan, yang akan menangani autentikasi.

Perhatikan bahwa Anda memerlukan versi terbaru dari OpenSSH untuk menggunakan fitur ini.

### Kontes (Windows)

Jika Anda menggunakan pageant pada Windows, XPipe akan memeriksa apakah pageant berjalan terlebih dahulu.
Karena sifat dari pageant, Anda bertanggung jawab untuk menjalankannya
berjalan karena Anda harus menentukan secara manual semua kunci yang ingin Anda tambahkan setiap saat.
Jika sudah berjalan, XPipe akan melewatkan pipa bernama yang tepat melalui
`-oIdentityAgent=...` ke ssh, Anda tidak perlu menyertakan berkas konfigurasi khusus apa pun.

### Kontes (Linux & macOS)

Jika identitas Anda disimpan di dalam agen kontes, eksekutor ssh dapat menggunakannya jika agen dimulai.
XPipe akan secara otomatis memulai proses agen jika belum berjalan.

### Sumber eksternal lainnya

Opsi ini akan mengizinkan penyedia identitas eksternal yang berjalan untuk menyediakan kuncinya kepada klien SSH. Anda sebaiknya menggunakan opsi ini jika Anda menggunakan agen atau pengelola kata sandi lain untuk mengelola kunci SSH Anda.
