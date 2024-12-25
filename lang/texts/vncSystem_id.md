## Sistem target VNC

Selain fitur VNC normal, XPipe juga menambahkan fitur tambahan melalui interaksi dengan shell sistem dari sistem target.

Dalam beberapa kasus, hos server VNC, yaitu sistem jarak jauh tempat server VNC berjalan, mungkin berbeda dengan sistem aktual yang Anda kendalikan dengan VNC. Sebagai contoh, jika server VNC ditangani oleh hypervisor VM seperti Proxmox, server berjalan pada host hypervisor sementara sistem target aktual yang Anda kendalikan, misalnya VM, adalah tamu VM. Untuk memastikan bahwa misalnya operasi sistem berkas diterapkan pada sistem yang benar, Anda bisa mengubah sistem target secara manual jika berbeda dengan hos server VNC.