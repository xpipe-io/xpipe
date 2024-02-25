WHERE starship >NUL 2>NUL
IF NOT %ERRORLEVEL%==0 (
    winget install starship
    SET "PATH=%PATH%;C:\\Program Files\\starship\\bin"
)

MKDIR "%TEMP%\\xpipe\\scriptdata\\starship" >NUL 2>NUL
echo load(io.popen('starship init cmd'):read("*a"))() > "%TEMP%\\xpipe\\scriptdata\\starship\\starship.lua"
clink inject --quiet --profile "%TEMP%\\xpipe\\scriptdata\\starship"