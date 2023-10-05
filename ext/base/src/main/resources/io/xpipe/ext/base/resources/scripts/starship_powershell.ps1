if (-not $(Get-Command -ErrorAction SilentlyContinue starship)) {
    winget install starship

    # Update current process PATH environment variable
    $env:Path=([System.Environment]::GetEnvironmentVariable("Path", "Machine"), [System.Environment]::GetEnvironmentVariable("Path", "User")) -match '.' -join ';'
}

Invoke-Expression (&starship init powershell)