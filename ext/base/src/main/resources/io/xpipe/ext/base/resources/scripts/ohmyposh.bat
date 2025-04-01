WHERE /q oh-my-posh
IF NOT %ERRORLEVEL%==0 (
    IF NOT EXIST "%USERPROFILE%\AppData\Local\Programs\oh-my-posh\bin\oh-my-posh.exe" (
        WHERE /q winget
        IF NOT %ERRORLEVEL%==0 (
            winget install JanDeDobbeleer.OhMyPosh -s winget
        ) ELSE (
            powershell -ExecutionPolicy Bypass -Command "Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://ohmyposh.dev/install.ps1'))"
        )
    )
    SET "PATH=%PATH%;%USERPROFILE%\AppData\Local\Programs\oh-my-posh\bin"
)

MKDIR "%TEMP%\\xpipe\\scriptdata\\ohmyposh" >NUL 2>NUL
ECHO load(io.popen('oh-my-posh init cmd'):read("*a"))() > "%TEMP%\\xpipe\\scriptdata\\ohmyposh\\ohmyposh.lua"
clink inject --quiet --profile "%TEMP%\\xpipe\\scriptdata\\ohmyposh"
