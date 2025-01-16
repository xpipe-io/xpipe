## Elevasi

Proses elevasi adalah sistem operasi yang spesifik.

### Linux & macOS

Perintah yang ditinggikan dieksekusi dengan `sudo`. Kata sandi opsional `sudo` ditanyakan melalui XPipe bila diperlukan.
Anda memiliki kemampuan untuk menyesuaikan perilaku elevasi dalam pengaturan untuk mengontrol apakah Anda ingin memasukkan kata sandi setiap kali diperlukan atau jika Anda ingin menyimpannya untuk sesi saat ini.

### Windows

Pada Windows, tidak mungkin untuk meninggikan proses anak jika proses induknya tidak ditinggikan juga.
Oleh karena itu, jika XPipe tidak dijalankan sebagai administrator, Anda tidak akan dapat menggunakan elevasi secara lokal.
Untuk koneksi jarak jauh, akun pengguna yang terhubung harus diberikan hak administrator.