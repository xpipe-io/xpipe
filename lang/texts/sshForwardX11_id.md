## Penerusan X11

Bila opsi ini diaktifkan, sambungan SSH akan dimulai dengan pengaturan penerusan X11. Pada Linux, ini biasanya akan bekerja secara otomatis dan tidak memerlukan pengaturan apa pun. Pada macOS, Anda memerlukan server X11 seperti [XQuartz] (https://www.xquartz.org/) untuk dijalankan pada mesin lokal Anda.

### X11 di Windows

XPipe memungkinkan Anda untuk menggunakan kemampuan WSL2 X11 untuk koneksi SSH Anda. Satu-satunya yang Anda perlukan untuk ini adalah distribusi [WSL2] (https://learn.microsoft.com/en-us/windows/wsl/install) yang terinstal pada sistem lokal Anda. XPipe secara otomatis akan memilih distribusi yang terinstal yang kompatibel jika memungkinkan, tetapi Anda juga dapat menggunakan distribusi yang lain pada menu pengaturan.

Ini berarti Anda tidak perlu menginstall server X11 yang terpisah pada Windows. Namun, jika Anda tetap menggunakannya, XPipe akan mendeteksinya dan menggunakan server X11 yang sedang berjalan.

### Koneksi X11 sebagai desktop

Setiap koneksi SSH yang memiliki penerusan X11 yang diaktifkan dapat digunakan sebagai host desktop. Ini berarti Anda dapat meluncurkan aplikasi desktop dan lingkungan desktop melalui koneksi ini. Ketika aplikasi desktop apa pun diluncurkan, koneksi ini akan secara otomatis dimulai di latar belakang untuk memulai terowongan X11.
