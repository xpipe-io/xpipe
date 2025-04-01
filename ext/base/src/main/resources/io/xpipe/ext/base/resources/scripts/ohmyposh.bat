WHERE /q winget && winget install JanDeDobbeleer.OhMyPosh -s winget || powershell -ExecutionPolicy Bypass -Command "Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://ohmyposh.dev/install.ps1'))"
SET "PATH=%PATH%;%USERPROFILE%\AppData\Local\Programs\oh-my-posh\bin"
MKDIR "%TEMP%\\xpipe\\scriptdata\\starship" >NUL 2>NUL
ECHO load(io.popen('oh-my-posh init cmd'):read("*a"))() > "%TEMP%\\xpipe\\scriptdata\\ohmyposh\\ohmyposh.lua"
clink inject --quiet --profile "%TEMP%\\xpipe\\scriptdata\\ohmyposh"
