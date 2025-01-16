## Pengikatan terowongan

Informasi pengikatan yang Anda berikan diteruskan langsung ke klien `ssh` sebagai berikut: `-D [alamat:]port`.

Secara default, alamat akan mengikat antarmuka loopback. Anda juga dapat menggunakan alamat wildcard, misalnya mengatur alamat ke `0.0.0.0` untuk mengikat semua antarmuka jaringan yang dapat diakses melalui IPv4. Bila Anda benar-benar menghilangkan alamat, wildcard `*`, yang memungkinkan koneksi pada semua antarmuka jaringan, akan digunakan. Perhatikan bahwa beberapa notasi antarmuka jaringan mungkin tidak didukung pada semua sistem operasi. Server Windows misalnya tidak mendukung wildcard `*`.
