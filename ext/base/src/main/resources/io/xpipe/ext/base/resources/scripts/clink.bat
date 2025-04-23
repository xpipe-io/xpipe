WHERE /q clink
IF %ERRORLEVEL%==0 (
    exit /b 0
)

SET "PATH=%PATH%;%TEMP%\xpipe\scriptdata\clink"
WHERE clink >NUL 2>NUL
IF %ERRORLEVEL%==0 (
    exit /b 0
)

echo ^
$downloader = New-Object System.Net.WebClient;^
$defaultCreds = [System.Net.CredentialCache]::DefaultCredentials;^
if ($defaultCreds) {^
    $downloader.Credentials = $defaultCreds^
}^
$downloader.DownloadFile("https://github.com/chrisant996/clink/releases/download/v1.7.13/clink.1.7.13.ac5d42.zip", "$env:TEMP\clink.zip");^
Expand-Archive -Force -LiteralPath "$env:TEMP\clink.zip" -DestinationPath "$env:TEMP\xpipe\scriptdata\clink"; | powershell -NoLogo >NUL

clink set clink.autoupdate off
