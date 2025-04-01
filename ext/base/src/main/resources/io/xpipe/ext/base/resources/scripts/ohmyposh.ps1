if (Get-Command "winget" -ErrorAction SilentlyContinue) {
    winget install JanDeDobbeleer.OhMyPosh -s winget
} else {
    Set-ExecutionPolicy Bypass -Scope Process -Force; Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://ohmyposh.dev/install.ps1'))
}
$env:Path += ";$env:USERPROFILE\AppData\Local\Programs\oh-my-posh\bin"
& ([ScriptBlock]::Create((oh-my-posh init $(oh-my-posh get shell) --config "$env:POSH_THEMES_PATH\jandedobbeleer.omp.json" --print) -join "`n"))
