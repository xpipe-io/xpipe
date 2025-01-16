### Konfigurasi SSH

XPipe memuat semua host dan menerapkan semua pengaturan yang telah Anda konfigurasikan dalam file yang dipilih. Jadi, dengan menentukan opsi konfigurasi pada basis global atau khusus host, secara otomatis akan diterapkan pada koneksi yang dibuat oleh XPipe.

Jika Anda ingin mempelajari lebih lanjut tentang cara menggunakan konfigurasi SSH, Anda dapat menggunakan `man ssh_config` atau membaca [panduan] ini (https://www.ssh.com/academy/ssh/config).

identitas ###

Perhatikan bahwa Anda juga dapat menentukan opsi `IdentityFile` di sini. Jika ada identitas yang ditentukan di sini, identitas yang ditentukan di bawahnya akan diabaikan.

### Penerusan X11

Jika ada opsi untuk penerusan X11 yang ditentukan di sini, XPipe akan secara otomatis mencoba mengatur penerusan X11 pada Windows melalui WSL.