## Gerbang koneksi shell

Jika diaktifkan, XPipe pertama-tama akan membuka sambungan shell ke gateway dan dari sana membuka sambungan SSH ke host yang ditentukan. Perintah `ssh` harus tersedia dan berada di `PATH` pada gateway yang Anda pilih.

### Lompat server

Mekanisme ini mirip dengan jump server, tetapi tidak sama. Mekanisme ini sepenuhnya tidak bergantung pada protokol SSH, sehingga Anda dapat menggunakan koneksi shell apa pun sebagai gateway.

Jika Anda mencari server lompatan SSH yang tepat, mungkin juga dikombinasikan dengan penerusan agen, gunakan fungsionalitas koneksi SSH khusus dengan opsi konfigurasi `ProxyJump`.