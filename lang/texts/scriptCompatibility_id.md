## Kompatibilitas skrip

Jenis shell mengontrol di mana skrip ini dapat dijalankan.
Selain pencocokan yang sama persis, misalnya menjalankan skrip `zsh` di `zsh`, XPipe juga akan menyertakan pengecekan kompatibilitas yang lebih luas.

### Kerang Posix

Skrip apapun yang dideklarasikan sebagai skrip `sh` dapat dijalankan di lingkungan shell yang berhubungan dengan posix seperti `bash` atau `zsh`.
Jika Anda berniat untuk menjalankan skrip dasar pada banyak sistem yang berbeda, maka hanya menggunakan skrip sintaks `sh` adalah solusi terbaik untuk itu.

### PowerShell

Skrip yang dideklarasikan sebagai skrip `powershell` normal juga dapat dijalankan di lingkungan `pwsh`.
