## X11 Forwarding

Если эта опция включена, SSH-соединение будет запускаться с настроенной переадресацией X11. В Linux это обычно работает из коробки и не требует настройки. На macOS тебе понадобится X11-сервер, например [XQuartz](https://www.xquartz.org/), который должен быть запущен на твоей локальной машине.

### X11 в Windows

XPipe позволяет тебе использовать возможности WSL2 X11 для SSH-соединения. Единственное, что тебе для этого нужно, - это установленный на локальной системе дистрибутив [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install). XPipe автоматически выберет совместимый установленный дистрибутив, если это возможно, но ты можешь использовать и другой в меню настроек.

Это означает, что тебе не нужно устанавливать отдельный X11-сервер в Windows. Однако если ты все равно его используешь, XPipe обнаружит это и будет использовать текущий запущенный X11-сервер.

### X11-соединения как рабочие столы

Любое SSH-соединение, в котором включена переадресация X11, можно использовать в качестве хоста рабочего стола. Это значит, что через такое соединение можно запускать десктопные приложения и окружения рабочего стола. При запуске любого десктопного приложения это соединение будет автоматически запущено в фоновом режиме, чтобы запустить X11-туннель.