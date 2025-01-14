WHERE clink >NUL 2>NUL
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
$downloader.DownloadFile("https://github.com/chrisant996/clink/releases/download/v1.7.7/clink.1.7.7.521fa7.zip", "$env:TEMP\clink.zip");^
Expand-Archive -Force -LiteralPath "$env:TEMP\clink.zip" -DestinationPath "$env:TEMP\xpipe\scriptdata\clink"; | powershell -NoLogo >NUL
