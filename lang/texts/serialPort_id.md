## Windows

Pada sistem Windows, Anda biasanya merujuk ke port serial melalui `COM<index`.
XPipe juga mendukung hanya menentukan indeks tanpa awalan `COM`.
Untuk mengalamatkan port yang lebih besar dari 9, Anda harus menggunakan bentuk jalur UNC dengan `\\.\COM<index>`.

Jika Anda memiliki distribusi WSL1 yang terinstal, Anda juga dapat mereferensikan port serial dari dalam distribusi WSL melalui `/dev/ttyS<index>`.
Ini tidak berfungsi dengan WSL2 lagi.
Jika Anda memiliki sistem WSL1, Anda dapat menggunakan sistem ini sebagai host untuk koneksi serial dan menggunakan notasi tty untuk mengaksesnya dengan XPipe.

## Linux

Pada sistem Linux, Anda biasanya dapat mengakses port serial melalui `/dev/ttyS<index>`.
Jika Anda mengetahui ID dari perangkat yang tersambung namun tidak ingin melacak port serial, Anda juga dapat mereferensikannya melalui `/dev/serial/by-id/<device id>`.
Anda dapat membuat daftar semua port serial yang tersedia dengan ID-nya dengan menjalankan `ls /dev/serial/by-id/*`.

## macOS

Pada macOS, nama port serial bisa apa saja, tetapi biasanya berbentuk `/dev/tty.<id>` di mana id adalah pengenal perangkat internal.
Menjalankan `ls /dev/tty.*` akan menemukan port serial yang tersedia.
