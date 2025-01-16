## VNC hedef sistemi

Normal VNC özelliklerine ek olarak XPipe, hedef sistemin sistem kabuğu ile etkileşim yoluyla ek özellikler de ekler.

Bazı durumlarda VNC sunucu ana bilgisayarı, yani VNC sunucusunun üzerinde çalıştığı uzak sistem, VNC ile kontrol ettiğiniz gerçek sistemden farklı olabilir. Örneğin, bir VNC sunucusu Proxmox gibi bir VM hipervizörü tarafından yönetiliyorsa, sunucu hipervizör ana bilgisayarında çalışırken, kontrol ettiğiniz asıl hedef sistem, örneğin bir VM, VM misafiridir. Örneğin dosya sistemi işlemlerinin doğru sisteme uygulandığından emin olmak için, VNC sunucu ana bilgisayarından farklıysa hedef sistemi manuel olarak değiştirebilirsiniz.