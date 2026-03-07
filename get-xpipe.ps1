<#
    .SYNOPSIS
    Downloads and installs XPipe on the local machine.

    .DESCRIPTION
    Retrieves the XPipe msi for the latest or a specified version, and
    downloads and installs the application to the local machine.
#>
[CmdletBinding(DefaultParameterSetName = 'Default')]
param(
    # Specifies a target version of XPipe to install. By default, the latest
    # stable version is installed.
    [Parameter(Mandatory = $false)]
    [string]
    $XPipeVersion = $xpipeVersion,

    # If set, will download releases from the staging repository instead.
    [Parameter(Mandatory = $false)]
    [switch]
    $UseStageDownloads = $useStageDownloads
)

#region Functions

function Get-Downloader {
    <#
    .SYNOPSIS
    Gets a System.Net.WebClient that respects relevant proxies to be used for
    downloading data.

    .DESCRIPTION
    Retrieves a WebClient object that is pre-configured according to specified
    environment variables for any proxy and authentication for the proxy.
    Proxy information may be omitted if the target URL is considered to be
    bypassed by the proxy (originates from the local network.)

    .PARAMETER Url
    Target URL that the WebClient will be querying. This URL is not queried by
    the function, it is only a reference to determine if a proxy is needed.

    .EXAMPLE
    Get-Downloader -Url $fileUrl

    Verifies whether any proxy configuration is needed, and/or whether $fileUrl
    is a URL that would need to bypass the proxy, and then outputs the
    already-configured WebClient object.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [string]
        $Url
    )

    $downloader = New-Object System.Net.WebClient

    $defaultCreds = [System.Net.CredentialCache]::DefaultCredentials
    if ($defaultCreds) {
        $downloader.Credentials = $defaultCreds
    }

    $downloader
}

function Request-File {
    <#
    .SYNOPSIS
    Downloads a file from a given URL.

    .DESCRIPTION
    Downloads a target file from a URL to the specified local path.
    Any existing proxy that may be in use will be utilised.

    .PARAMETER Url
    URL of the file to download from the remote host.

    .PARAMETER File
    Local path for the file to be downloaded to.

    Downloads the file to the path specified in $targetFile.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $false)]
        [string]
        $Url,

        [Parameter(Mandatory = $false)]
        [string]
        $File
    )

    Write-Host "Downloading $url to $file"
    (Get-Downloader $url).DownloadFile($url, $file)
}


function Uninstall {
    [CmdletBinding()]
    param()

    # Quick heuristic to see whether is can be possibly installed
    if (-not ((Test-Path "$env:LOCALAPPDATA\$ProductName" -PathType Container) -or (Test-Path "$env:ProgramFiles\$ProductName" -PathType Container))) {
        return
    }

    Write-Host "Looking for previous $ProductName installations ..."

    $cim = Get-CimInstance Win32_Product | Where {$_.Name -eq "$ProductName" } | Select-Object -First 1
    if ($cim) {
        $message = @(
            "Uninstalling existing $ProductName $($cim.Version) installation ..."
        ) -join [Environment]::NewLine
        Write-Host $message
        $cimResult = Invoke-CimMethod -InputObject $cim -Name Uninstall
        if ($cimResult.ReturnValue) {
            Write-Host "Uninstallation failed: Code $($cimResult.ReturnValue)"
            exit
        }
    }
}

#endregion Functions

#region Pre-check

if ($UseStageDownloads) {
    $XPipeRepoUrl = "https://github.com/xpipe-io/xpipe-ptb"
    $ProductName = "XPipe PTB"
} else {
    $XPipeRepoUrl = "https://github.com/xpipe-io/xpipe"
    $ProductName = "XPipe"
}

if ($XPipeVersion) {
    $XPipeDownloadUrl = "$XPipeRepoUrl/releases/download/$XPipeVersion"
} else {
    $XPipeDownloadUrl = "$XPipeRepoUrl/releases/latest/download"
}

Uninstall

#endregion Pre-check

#region Setup

$RawArch = [System.Runtime.InteropServices.RuntimeInformation,mscorlib]::OSArchitecture.ToString().ToLower();
$Arch = If ($RawArch -eq "x64") {"x86_64"} Else {"arm64"}
$XPipeDownloadUrl = "$XPipeDownloadUrl/xpipe-installer-windows-$($Arch).msi"

if (-not $env:TEMP) {
    $env:TEMP = Join-Path $env:SystemDrive -ChildPath 'temp'
}

$tempDir = $env:TEMP

if (-not (Test-Path $tempDir -PathType Container)) {
    $null = New-Item -Path $tempDir -ItemType Directory
}

#endregion Setup

#region Download

$file = Join-Path $tempDir "xpipe-installer.msi"
Write-Host "Getting $ProductName from $XPipeRepoUrl."
Request-File -Url $XPipeDownloadUrl -File $file

#endregion Download

#region Install XPipe

Write-Host "Installing $ProductName ..."

# Wait for completion
# The file variable can contain spaces, so we have to accommodate for that
Start-Process -FilePath "msiexec" -Wait -ArgumentList "/i", "`"$file`"", "/quiet"

# Update current process PATH environment variable
$env:Path=(
    [System.Environment]::GetEnvironmentVariable("Path", "Machine"),
    [System.Environment]::GetEnvironmentVariable("Path", "User")
) -match '.' -join ';'

Write-Host
Write-Host "$ProductName has been successfully installed. You should be able to find it in your applications."
Write-Host

# Use absolute path as we can't assume that the user has selected to put XPipe into the Path
& "$env:LOCALAPPDATA\$ProductName\bin\xpipe.exe" open

#endregion Install XPipe

# SIG # Begin signature block
# MII2LQYJKoZIhvcNAQcCoII2HjCCNhoCAQExDzANBglghkgBZQMEAgEFADB5Bgor
# BgEEAYI3AgEEoGswaTA0BgorBgEEAYI3AgEeMCYCAwEAAAQQH8w7YFlLCE63JNLG
# KX7zUQIBAAIBAAIBAAIBAAIBADAxMA0GCWCGSAFlAwQCAQUABCAwATRG6rHgZRP3
# qY8+6nQ4zP5otf3TGbo74AaDJY/XZKCCFHQwggVyMIIDWqADAgECAhB2U/6sdUZI
# k/Xl10pIOk74MA0GCSqGSIb3DQEBDAUAMFMxCzAJBgNVBAYTAkJFMRkwFwYDVQQK
# ExBHbG9iYWxTaWduIG52LXNhMSkwJwYDVQQDEyBHbG9iYWxTaWduIENvZGUgU2ln
# bmluZyBSb290IFI0NTAeFw0yMDAzMTgwMDAwMDBaFw00NTAzMTgwMDAwMDBaMFMx
# CzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52LXNhMSkwJwYDVQQD
# EyBHbG9iYWxTaWduIENvZGUgU2lnbmluZyBSb290IFI0NTCCAiIwDQYJKoZIhvcN
# AQEBBQADggIPADCCAgoCggIBALYtxTDdeuirkD0DcrA6S5kWYbLl/6VnHTcc5X7s
# k4OqhPWjQ5uYRYq4Y1ddmwCIBCXp+GiSS4LYS8lKA/Oof2qPimEnvaFE0P31PyLC
# o0+RjbMFsiiCkV37WYgFC5cGwpj4LKczJO5QOkHM8KCwex1N0qhYOJbp3/kbkbuL
# ECzSx0Mdogl0oYCve+YzCgxZa4689Ktal3t/rlX7hPCA/oRM1+K6vcR1oW+9YRB0
# RLKYB+J0q/9o3GwmPukf5eAEh60w0wyNA3xVuBZwXCR4ICXrZ2eIq7pONJhrcBHe
# OMrUvqHAnOHfHgIB2DvhZ0OEts/8dLcvhKO/ugk3PWdssUVcGWGrQYP1rB3rdw1G
# R3POv72Vle2dK4gQ/vpY6KdX4bPPqFrpByWbEsSegHI9k9yMlN87ROYmgPzSwwPw
# jAzSRdYu54+YnuYE7kJuZ35CFnFi5wT5YMZkobacgSFOK8ZtaJSGxpl0c2cxepHy
# 1Ix5bnymu35Gb03FhRIrz5oiRAiohTfOB2FXBhcSJMDEMXOhmDVXR34QOkXZLaRR
# kJipoAc3xGUaqhxrFnf3p5fsPxkwmW8x++pAsufSxPrJ0PBQdnRZ+o1tFzK++Ol+
# A/Tnh3Wa1EqRLIUDEwIrQoDyiWo2z8hMoM6e+MuNrRan097VmxinxpI68YJj8S4O
# JGTfAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB0G
# A1UdDgQWBBQfAL9GgAr8eDm3pbRD2VZQu86WOzANBgkqhkiG9w0BAQwFAAOCAgEA
# Xiu6dJc0RF92SChAhJPuAW7pobPWgCXme+S8CZE9D/x2rdfUMCC7j2DQkdYc8pzv
# eBorlDICwSSWUlIC0PPR/PKbOW6Z4R+OQ0F9mh5byV2ahPwm5ofzdHImraQb2T07
# alKgPAkeLx57szO0Rcf3rLGvk2Ctdq64shV464Nq6//bRqsk5e4C+pAfWcAvXda3
# XaRcELdyU/hBTsz6eBolSsr+hWJDYcO0N6qB0vTWOg+9jVl+MEfeK2vnIVAzX9Rn
# m9S4Z588J5kD/4VDjnMSyiDN6GHVsWbcF9Y5bQ/bzyM3oYKJThxrP9agzaoHnT5C
# JqrXDO76R78aUn7RdYHTyYpiF21PiKAhoCY+r23ZYjAf6Zgorm6N1Y5McmaTgI0q
# 41XHYGeQQlZcIlEPs9xOOe5N3dkdeBBUO27Ql28DtR6yI3PGErKaZND8lYUkqP/f
# obDckUCu3wkzq7ndkrfxzJF0O2nrZ5cbkL/nx6BvcbtXv7ePWu16QGoWzYCELS/h
# AtQklEOzFfwMKxv9cW/8y7x1Fzpeg9LJsy8b1ZyNf1T+fn7kVqOHp53hWVKUQY9t
# W76GlZr/GnbdQNJRSnC0HzNjI3c/7CceWeQIh+00gkoPP/6gHcH1Z3NFhnj0qinp
# J4fGGdvGExTDOUmHTaCX4GUT9Z13Vunas1jHOvLAzYIwggboMIIE0KADAgECAhB3
# vQ4Ft1kLth1HYVMeP3XtMA0GCSqGSIb3DQEBCwUAMFMxCzAJBgNVBAYTAkJFMRkw
# FwYDVQQKExBHbG9iYWxTaWduIG52LXNhMSkwJwYDVQQDEyBHbG9iYWxTaWduIENv
# ZGUgU2lnbmluZyBSb290IFI0NTAeFw0yMDA3MjgwMDAwMDBaFw0zMDA3MjgwMDAw
# MDBaMFwxCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52LXNhMTIw
# MAYDVQQDEylHbG9iYWxTaWduIEdDQyBSNDUgRVYgQ29kZVNpZ25pbmcgQ0EgMjAy
# MDCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAMsg75ceuQEyQ6BbqYoj
# /SBerjgSi8os1P9B2BpV1BlTt/2jF+d6OVzA984Ro/ml7QH6tbqT76+T3PjisxlM
# g7BKRFAEeIQQaqTWlpCOgfh8qy+1o1cz0lh7lA5tD6WRJiqzg09ysYp7ZJLQ8LRV
# X5YLEeWatSyyEc8lG31RK5gfSaNf+BOeNbgDAtqkEy+FSu/EL3AOwdTMMxLsvUCV
# 0xHK5s2zBZzIU+tS13hMUQGSgt4T8weOdLqEgJ/SpBUO6K/r94n233Hw0b6nskEz
# IHXMsdXtHQcZxOsmd/KrbReTSam35sOQnMa47MzJe5pexcUkk2NvfhCLYc+YVaMk
# oog28vmfvpMusgafJsAMAVYS4bKKnw4e3JiLLs/a4ok0ph8moKiueG3soYgVPMLq
# 7rfYrWGlr3A2onmO3A1zwPHkLKuU7FgGOTZI1jta6CLOdA6vLPEV2tG0leis1Ult
# 5a/dm2tjIF2OfjuyQ9hiOpTlzbSYszcZJBJyc6sEsAnchebUIgTvQCodLm3HadNu
# twFsDeCXpxbmJouI9wNEhl9iZ0y1pzeoVdwDNoxuz202JvEOj7A9ccDhMqeC5LYy
# AjIwfLWTyCH9PIjmaWP47nXJi8Kr77o6/elev7YR8b7wPcoyPm593g9+m5XEEofn
# GrhO7izB36Fl6CSDySrC/blTAgMBAAGjggGtMIIBqTAOBgNVHQ8BAf8EBAMCAYYw
# EwYDVR0lBAwwCgYIKwYBBQUHAwMwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHQ4E
# FgQUJZ3Q/FkJhmPF7POxEztXHAOSNhEwHwYDVR0jBBgwFoAUHwC/RoAK/Hg5t6W0
# Q9lWULvOljswgZMGCCsGAQUFBwEBBIGGMIGDMDkGCCsGAQUFBzABhi1odHRwOi8v
# b2NzcC5nbG9iYWxzaWduLmNvbS9jb2Rlc2lnbmluZ3Jvb3RyNDUwRgYIKwYBBQUH
# MAKGOmh0dHA6Ly9zZWN1cmUuZ2xvYmFsc2lnbi5jb20vY2FjZXJ0L2NvZGVzaWdu
# aW5ncm9vdHI0NS5jcnQwQQYDVR0fBDowODA2oDSgMoYwaHR0cDovL2NybC5nbG9i
# YWxzaWduLmNvbS9jb2Rlc2lnbmluZ3Jvb3RyNDUuY3JsMFUGA1UdIAROMEwwQQYJ
# KwYBBAGgMgECMDQwMgYIKwYBBQUHAgEWJmh0dHBzOi8vd3d3Lmdsb2JhbHNpZ24u
# Y29tL3JlcG9zaXRvcnkvMAcGBWeBDAEDMA0GCSqGSIb3DQEBCwUAA4ICAQAldaAJ
# yTm6t6E5iS8Yn6vW6x1L6JR8DQdomxyd73G2F2prAk+zP4ZFh8xlm0zjWAYCImbV
# YQLFY4/UovG2XiULd5bpzXFAM4gp7O7zom28TbU+BkvJczPKCBQtPUzosLp1pnQt
# pFg6bBNJ+KUVChSWhbFqaDQlQq+WVvQQ+iR98StywRbha+vmqZjHPlr00Bid/XSX
# hndGKj0jfShziq7vKxuav2xTpxSePIdxwF6OyPvTKpIz6ldNXgdeysEYrIEtGiH6
# bs+XYXvfcXo6ymP31TBENzL+u0OF3Lr8psozGSt3bdvLBfB+X3Uuora/Nao2Y8nO
# ZNm9/Lws80lWAMgSK8YnuzevV+/Ezx4pxPTiLc4qYc9X7fUKQOL1GNYe6ZAvytOH
# X5OKSBoRHeU3hZ8uZmKaXoFOlaxVV0PcU4slfjxhD4oLuvU/pteO9wRWXiG7n9dq
# cYC/lt5yA9jYIivzJxZPOOhRQAyuku++PX33gMZMNleElaeEFUgwDlInCI2Oor0i
# xxnJpsoOqHo222q6YV8RJJWk4o5o7hmpSZle0LQ0vdb5QMcQlzFSOTUpEYck08T7
# qWPLd0jV+mL8JOAEek7Q5G7ezp44UCb0IXFl1wkl1MkHAHq4x/N36MXU4lXQ0x72
# f1LiSY25EXIMiEQmM2YBRN/kMw4h3mKJSAfa9TCCCA4wggX2oAMCAQICDDjATBs0
# gFlMPflq4jANBgkqhkiG9w0BAQsFADBcMQswCQYDVQQGEwJCRTEZMBcGA1UEChMQ
# R2xvYmFsU2lnbiBudi1zYTEyMDAGA1UEAxMpR2xvYmFsU2lnbiBHQ0MgUjQ1IEVW
# IENvZGVTaWduaW5nIENBIDIwMjAwHhcNMjUxMTE0MTAxOTEyWhcNMjYxMTE1MTAx
# OTEyWjCCAVcxHTAbBgNVBA8MFFByaXZhdGUgT3JnYW5pemF0aW9uMRMwEQYDVQQF
# EwpIUkIgNzkxMzAxMRMwEQYLKwYBBAGCNzwCAQMTAkRFMSMwIQYLKwYBBAGCNzwC
# AQITEkJhZGVuLVd1ZXJ0dGVtYmVyZzEaMBgGCysGAQQBgjc8AgEBEwlTdHV0dGdh
# cnQxCzAJBgNVBAYTAkRFMRswGQYDVQQIDBJCYWRlbi1Xw7xydHRlbWJlcmcxFDAS
# BgNVBAcTC0x1ZHdpZ3NidXJnMRowGAYDVQQJExFSZWljaGVydHNoYWxkZSA4MTEn
# MCUGA1UECgweWFBpcGUgVUcgKGhhZnR1bmdzYmVzY2hyw6Rua3QpMScwJQYDVQQD
# DB5YUGlwZSBVRyAoaGFmdHVuZ3NiZXNjaHLDpG5rdCkxHTAbBgkqhkiG9w0BCQEW
# DmhlbGxvQHhwaXBlLmlvMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA
# iWiV2em3EWZvvhWONaxbhUUl+xkrjris0DlgHJKjIsHRtlqdI9g+K3Bg1cEdgNp9
# ISYfp6dgBsdceg5tgVfOxuCpmxU04dfV1B+En5hh6LSvaMa2dkaGmMHzuI5KUC9k
# BCbZ9j8wlkSV3SCXPXFP5uGNgKF5O0RcUmoGUuz0zTWThcyhcZZ+8wDueqj72mlL
# Hu/2CipSFmyDrTVr02j4Dr3MA+bSXYWCQKFDji8V3SCBOjLXZFvdZPYRcz6I4GZy
# XE1QfvAzzsI6aKIQpquXwWEzkgBcGaDG8yV+cTCisXDT5dVBX69FV8lVnlvzoVJE
# XPZnWaXOcOzFXNDoRSmc51/REOgvmUmlD/EYPXF6dEn67ZZVnSyLvKyTRCLx+LUi
# KfqCTUDJXuZog3wpnkoBHClyrBinHjxHApbx4TxVPLpOnlWuPfbDKoNIEigXFyeb
# x6FHSABQn02p4r+6YpQK1DSnMtRvxL4XmllI1h1d3cmyTU4HfdlTaoXsAlaTzbTP
# kO9M7Mu045fexFupD/lpk7jv0zFvBxYsBkPuGubfIpaX5Q6wybGMED3XbTnKykN9
# tD8ncWJgKcpu5eBrmZotzGkb8ngHMCOooS+H3lONf9G4Oub/oIRiLApUl83+VJ8k
# xZ7B9W4DOpEo6HsbfzO1eXV+Wiy4UJ+WPvPoPFHQ+XsCAwEAAaOCAdEwggHNMA4G
# A1UdDwEB/wQEAwIHgDCBnwYIKwYBBQUHAQEEgZIwgY8wTAYIKwYBBQUHMAKGQGh0
# dHA6Ly9zZWN1cmUuZ2xvYmFsc2lnbi5jb20vY2FjZXJ0L2dzZ2NjcjQ1ZXZjb2Rl
# c2lnbmNhMjAyMC5jcnQwPwYIKwYBBQUHMAGGM2h0dHA6Ly9vY3NwLmdsb2JhbHNp
# Z24uY29tL2dzZ2NjcjQ1ZXZjb2Rlc2lnbmNhMjAyMDBVBgNVHSAETjBMMEEGCSsG
# AQQBoDIBAjA0MDIGCCsGAQUFBwIBFiZodHRwczovL3d3dy5nbG9iYWxzaWduLmNv
# bS9yZXBvc2l0b3J5LzAHBgVngQwBAzAJBgNVHRMEAjAAMEcGA1UdHwRAMD4wPKA6
# oDiGNmh0dHA6Ly9jcmwuZ2xvYmFsc2lnbi5jb20vZ3NnY2NyNDVldmNvZGVzaWdu
# Y2EyMDIwLmNybDAZBgNVHREEEjAQgQ5oZWxsb0B4cGlwZS5pbzATBgNVHSUEDDAK
# BggrBgEFBQcDAzAfBgNVHSMEGDAWgBQlndD8WQmGY8Xs87ETO1ccA5I2ETAdBgNV
# HQ4EFgQU3cIQIJuJcgAVvx1aKUUPxC698swwDQYJKoZIhvcNAQELBQADggIBAD9t
# ixHvBK/ulMMa2S63NYZQs1C/yvdzXBS/0vmMUeuJcy4yhL3dSgJE9qVO5GG2PDqV
# yQ36WIck0wTPjoy4fpyC5Rnalgt3ifE+LTrq/+SHB18UMj4xsyRk987vCY9ue12/
# 5DpJhyT85WeLnU1u9QEG2UpSmp8pdQrbmpVZK9V82QjQwP5Djrl3SX3mTvcrs4Lc
# PasgGx0+DZK047Nnsq1RiDNxTWzPQE1Flgglg7Gnz9M52Zn3xLmAKD1qm+F4PLCn
# 8AihGUAkEnsadtDJGUEZc/C0FBpgFejb5YpZtVJwRbEi8Ya4ng6JqdUfr09OVVDq
# cqMK0EzfuzYhIvUzgH/tjBZCyejTCuFa7Bzikcs5xRcX43DG0N6v1B/lmPMGrDW1
# 3G2pP8m9gs6Jxc4izwtuUbb1Zy8GlIZEviDGIYSxStlicK+qR4qOnL06mNAe26RI
# fj+gPORke6Od/mrk0VK5OVFQa/JUIXN1APai3FJKazAnzp3fZ79M9zko4Ca06NSP
# oClVXbOKk5nQGhWnjP/Q99p/VRU0Or1ba/mMfX2lxl0gM2Vva5BZjJDOrzbaR8Nr
# 7B9C/ZRw1zuC13USzdUT/ASsY92uH07u4MFucJIkgzClzoSn5zeutRC4VUIwxJrx
# 8n5Jlu+PMnOcx6VM99etVDRHw0CLpVS6nSCGDVyoMYIhDzCCIQsCAQEwbDBcMQsw
# CQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTEyMDAGA1UEAxMp
# R2xvYmFsU2lnbiBHQ0MgUjQ1IEVWIENvZGVTaWduaW5nIENBIDIwMjACDDjATBs0
# gFlMPflq4jANBglghkgBZQMEAgEFAKCBhDAYBgorBgEEAYI3AgEMMQowCKACgACh
# AoAAMBkGCSqGSIb3DQEJAzEMBgorBgEEAYI3AgEEMBwGCisGAQQBgjcCAQsxDjAM
# BgorBgEEAYI3AgEVMC8GCSqGSIb3DQEJBDEiBCB+j+2NW2cxErIJO9Of0Stwefdk
# XhQOKaP9ERrxy/e1FTANBgkqhkiG9w0BAQEFAASCAgA0fx/afvl3cDsbvNG/ZiGV
# Z5l2m+yG8pzV1SFhglzrXmPIHqzg+49c4g+f4Fj3nU36yhbbkIhQH90zsScyTsIR
# oLNIiwozEWe3y7wM3RiuwdmJSPmBWiXLTvL9kEEd/MPruyMwzHRRyVuNcJfiblZL
# hfXwtkO9K00g/D774pOL61yEFEjDHmH289FtUrKa6lAbCKHY2fDGNfGnvKH8nBGm
# aaEIhZDiFRTmpvqhUs3UDLIMxpByhrjOw6BHd6h70srduzBdBqyZybdpzxxRO/ml
# 7Dsn7/49nDz9egARu3N78P+owEhg2T7WGhfhTN80+4GBhZpDbw5vQm8d/3GSSPAO
# 9IUomOJsGiBpA04RjLKD+qKAsGw5cKXZjKcvtzjOJLQGgM1C/YdYqLqlXbC7nG+U
# RiUlthKjki7t51q65VaXMoT12Flv0yde06ysIybRXsOt7Wuf6vxUR5kkUFZKx2Rz
# pqw5oJvepBA6O9SO8Y9CIpbBrEn49CxgIG0++Hxd02zEKmMnmBB8SulsLGgyrBBR
# RsRAujhe5ufC13dzL6v2OY2IwG1GQG0pHEVPDH7CUoGSbuS0e/QIU+bbW3NDVyhC
# exhGfm9TuIoqJlaJOaXbITus70nZ3tUbZH6p/FjyVz0hyobBl1dbJ+8m5wToQszJ
# 1rjngOzqI5I38Kzsm81jwqGCHe0wgh3pBgorBgEEAYI3AwMBMYId2TCCHdUGCSqG
# SIb3DQEHAqCCHcYwgh3CAgEDMQ0wCwYJYIZIAWUDBAICMIHkBgsqhkiG9w0BCRAB
# BKCB1ASB0TCBzgIBAQYLKwYBBAGgMgIDAgIwMTANBglghkgBZQMEAgEFAAQg9QVx
# LOF4+Cw04/6KuM3FU/hzTAHMS6UOBBlSxDcX75UCFFUsRfrZ6fO1myUI6MYE7gX+
# f/uPGA8yMDI2MDMwNzE5MjU0OFowAwIBAaBdpFswWTELMAkGA1UEBhMCQkUxGTAX
# BgNVBAoTEEdsb2JhbFNpZ24gbnYtc2ExLzAtBgNVBAMTJkdsb2JhbHNpZ24gUjQ1
# IFRTQSBmb3IgQ29kZVNpZ24gMjAyNTEwoIIZYDCCBoowggRyoAMCAQICEQCEcj/B
# lcwW8dsrovZg3yvkMA0GCSqGSIb3DQEBDAUAMF4xCzAJBgNVBAYTAkJFMRkwFwYD
# VQQKExBHbG9iYWxTaWduIG52LXNhMTQwMgYDVQQDEytHbG9iYWxTaWduIE9mZmxp
# bmUgUjQ1IFRpbWVzdGFtcGluZyBDQSAyMDI1MB4XDTI1MTAxNTA3MjUwNFoXDTM3
# MDExMDAwMDAwMFowWTELMAkGA1UEBhMCQkUxGTAXBgNVBAoTEEdsb2JhbFNpZ24g
# bnYtc2ExLzAtBgNVBAMTJkdsb2JhbHNpZ24gUjQ1IFRTQSBmb3IgQ29kZVNpZ24g
# MjAyNTEwMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA0UqNoY2GQEgo
# owkEnNkTEDzyqIxP8X+FIo2tiZ7Ce0p5doA6MSo0PR3FOq1Q/KYMtbASnVcbSyxY
# WN4M2PzwNosCLOmcwp8bIbY0obDdUcs0a83OBavtMUMeqgA1DJ/epqhxP9KJOdwz
# 25qQZJFA8rRjO/Z0H8PVlcpmIAPk5GwNP0DpjzTSSGego19Ld8CX4S9HGol7YQQT
# BFisU+b9lO2UWwqvw1Q1wPaF9YhVQVWgaceezCy9NJ8h7sdCJ2Eu0a+eDN7TYZu/
# tJmsackxWudbmNTx7UyTLqf5d0RqKEOWMHgh9oQ6FDcCjgu0JBW5JYT3atuxF5Ln
# oPKizp0Q5lTta/gdcjAG5ekLldC/jjwdUigQD6ZiBZJZidEqIm21KsbE83o43SPs
# sEC1HF4paIMClrCeOoesI5VOQFak+xRAWM1gk7eoX+0i0GzxrNNgWvKGmjX6NEi/
# mgKTfeJbhAf8LhNZYKba3JC820JQqeejoJACWKamLrovk37ngiFNAgMBAAGjggHG
# MIIBwjAOBgNVHQ8BAf8EBAMCB4AwFgYDVR0lAQH/BAwwCgYIKwYBBQUHAwgwDAYD
# VR0TAQH/BAIwADAdBgNVHQ4EFgQUMvrT4QdoJ5BrCNI/HTyMZTYoBhkwHwYDVR0j
# BBgwFoAUdwI7ATEPHnR3w0jIwwdjVYilO6IwgaUGCCsGAQUFBwEBBIGYMIGVMEIG
# CCsGAQUFBzABhjZodHRwOi8vb2NzcC5nbG9iYWxzaWduLmNvbS9nc29mZmxpbmVy
# NDV0aW1lc3RhbXBjYTIwMjUwTwYIKwYBBQUHMAKGQ2h0dHA6Ly9zZWN1cmUuZ2xv
# YmFsc2lnbi5jb20vY2FjZXJ0L2dzb2ZmbGluZXI0NXRpbWVzdGFtcGNhMjAyNS5j
# cnQwSgYDVR0fBEMwQTA/oD2gO4Y5aHR0cDovL2NybC5nbG9iYWxzaWduLmNvbS9n
# c29mZmxpbmVyNDV0aW1lc3RhbXBjYTIwMjUuY3JsMFYGA1UdIARPME0wCAYGZ4EM
# AQQCMEEGCSsGAQQBoDIBHjA0MDIGCCsGAQUFBwIBFiZodHRwczovL3d3dy5nbG9i
# YWxzaWduLmNvbS9yZXBvc2l0b3J5LzANBgkqhkiG9w0BAQwFAAOCAgEAjq5wpo9H
# hpGKbp4s+v1hZvdbOio54ZDCmJW3H7YKTAbZYcrFZ4hDnwb4XL6BUMOADhxvTUVb
# 2aZAZ2oOQ7dRzNyYNtYEVQgcBcj9RE/CA8aH5WMeTg+EgjSx7OrwoOqxZDU/Nb/W
# cvLtMV8t1m6DeYiv0m05ixqgqwBiYoSy63yIb3XcFa3Qcoh7Xi8YI7oE+iUKIAkq
# ARdiaph9GQQpedeW7W07lmS60FLOb75dqPfDHY7vcsiUVi8z1XFJEZPXfMQQYut/
# BQTgLqN33Mk4RnJ3u8FDmvk4IYcCB22d6cU2KYKOXAsRGbB/BF8Ik1E8B39KnrLL
# IP9l3XBh+7NAEeE8jidkJwGsmDgDP1zgcTc1EUIOJoTHTy/xfFQjCo95KCWlXMdm
# pIdAfNpXv2gVPKtjmvvjsDsIhxeXvn/ZvsocmG8BkqIFiFd4YEZflciq3IfIL9+R
# R6/3Fc/J9PwjxkVxomCWez9cDjoGHZDA9ogylWEG8uSMf8QgdP5Uii56hKBXs35p
# wiWxoAAlbMDLMYSRFI7cRHEHPWn2pZ3viDrbNL/8gkYpCbJ5lW0tMedA5Njh/lQo
# lPbP18s4Hwu1s67UOXYiHXNirItyTEyZuGtC4hCGVorQ+fuj6JDpEPPt+U7BzsFI
# bPOyP8siY5SGaQFeDn9fyTfG1lQdxHZuFs4wggagMIIEiKADAgECAhEAg9qGN7ef
# DIQMlHuEClJ4HzANBgkqhkiG9w0BAQwFADBTMQswCQYDVQQGEwJCRTEZMBcGA1UE
# ChMQR2xvYmFsU2lnbiBudi1zYTEpMCcGA1UEAxMgR2xvYmFsU2lnbiBUaW1lc3Rh
# bXBpbmcgUm9vdCBSNDUwHhcNMjUwNzE2MDMwNTA0WhcNNDEwNzE2MDAwMDAwWjBe
# MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTE0MDIGA1UE
# AxMrR2xvYmFsU2lnbiBPZmZsaW5lIFI0NSBUaW1lc3RhbXBpbmcgQ0EgMjAyNTCC
# AiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAKR3FvjtfYvi3QKGRWM0vuV/
# lmL51qBalwH4q9Ycp4VE3R+EVtQ2/sjpq0Nu+Y5bEOm86Gd5zdAjJ3GV+BNEIaae
# kL8du/n5aX83jEGnA+1XIllIhd0y4ru8E9mwQkhDVJ94XxjBuDyb2BbzuaOAwNYj
# bx4RuKEs3LaHaCX2+HPBI1WoQsPzS4swd62vjVntoGQUmmSq2jMRbeu+sqse0XEJ
# 42xeEdDixqL7Gbdt71OvQOQXaD8XQPc7vazPqkwbDw1VCXsWMBFVNBmOwSeZlFlp
# 4sgDx1tFr+AM5ObeyNF1CIwN/tjhpK9HVQV1QQ8hHhgVa2FxHYX1S8LoehwRiRXS
# KxGOSqBpaaE/uAQUHuFwh+bL9eVKPjO9lSlozwORMd53xSQuD+RVtUkeqJQkmiK7
# Z2c3Ps4JqUsog/hnBNrnUEUB2w4u+Hp14Gs5NPpLaR2dWfKE70mkiERKL10i2x9y
# gvLhSJF7nJe1zx+2RiK5hfXheM7qKORSFJ7L1etJC6hQ8z5PlGy5oGQ1Aj5RRQQX
# oWmjb4sRMpsfTjdil3r/jHyujmQoahm6zgtsL7wBR8ipU2W1R3ajYrtLuWtk9QvX
# Uk0Z8tf4cS8HMS/QVj80C8IbIsl6lMzO7g9JQ/fj2DGsVGIKFUsam+MbzwolaizW
# myPlrYxM2H0ND/urQUxbAgMBAAGjggFiMIIBXjAOBgNVHQ8BAf8EBAMCAYYwEwYD
# VR0lBAwwCgYIKwYBBQUHAwgwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHQ4EFgQU
# dwI7ATEPHnR3w0jIwwdjVYilO6IwHwYDVR0jBBgwFoAURrIcd+F7FfClOaFw3tHE
# Luptst4wgY4GCCsGAQUFBwEBBIGBMH8wNwYIKwYBBQUHMAGGK2h0dHA6Ly9vY3Nw
# Lmdsb2JhbHNpZ24uY29tL3RpbWVzdGFtcHJvb3RyNDUwRAYIKwYBBQUHMAKGOGh0
# dHA6Ly9zZWN1cmUuZ2xvYmFsc2lnbi5jb20vY2FjZXJ0L3RpbWVzdGFtcHJvb3Ry
# NDUuY3J0MD8GA1UdHwQ4MDYwNKAyoDCGLmh0dHA6Ly9jcmwuZ2xvYmFsc2lnbi5j
# b20vdGltZXN0YW1wcm9vdHI0NS5jcmwwEQYDVR0gBAowCDAGBgRVHSAAMA0GCSqG
# SIb3DQEBDAUAA4ICAQAyo+5+0W7kZjGWWF6pTD0SaEel9Z//6rlxi1XzqRaEfWGt
# OS/eMNMsAY3pchhn/pwAfgmRUO4FDndmtm6X6VvF8OQKzPmIzxH/ALQuBOmK9dOV
# NWDY5U0hEaFiSnNYZEjeZuHabGLXhcpMxUFcF+dIWCh98melXLuh2tBAOaH8y6Yt
# oyto20Qwl2xTS4uZjtcSkXQdgMwBdH+QtNG+B93ebAjgfJ407wMGkzBhDHk5C1jO
# GfWPt1DrTqaNULsV2w6V1ZC162htuYi9Vbb48RAMuOd+J1CDQfcITkYnCSUky22u
# nzitn/9UBlhHXsFixwMxPjeaB9EKDUQC6SeSYyH3iJ7zvrEHQYnZz7iyifeR04+1
# 8jLyYRWY7WxyRpRFseFICnLjSiwy11gC38EAzdg3st5E33eMsOJ0HZNT3LSyytxe
# C9p5y7/eATJKWPkX5ed6DhDeMk77cAmD/lQ7ma5LXOzWnKE1cWZyhpL/GtWpHJHT
# sEGQvovAMtVcszAF/02+Pq93OuQe3KQLatzqFIo11CYL2cXIuk1+/LJi9k2MtrVp
# /vBuJK5178thC0dl2JyhDkuDFZq0grIzOZ8Uq4IRNdFQ10GxH2vd5YYpHxaCLiDS
# s1gy6r7vGbnJsRLn8aR4zZ50VV1/2b+SkKvKbvzEKDs+gwu+J+fjuW0sL0+bIDCC
# BqMwggSLoAMCAQICEHhKqoFzZpyQCVTkIclH68AwDQYJKoZIhvcNAQEMBQAwTDEg
# MB4GA1UECxMXR2xvYmFsU2lnbiBSb290IENBIC0gUjYxEzARBgNVBAoTCkdsb2Jh
# bFNpZ24xEzARBgNVBAMTCkdsb2JhbFNpZ24wHhcNMjAxMjA5MDAwMDAwWhcNMzQx
# MjEwMDAwMDAwWjBTMQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBu
# di1zYTEpMCcGA1UEAxMgR2xvYmFsU2lnbiBUaW1lc3RhbXBpbmcgUm9vdCBSNDUw
# ggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC6dDPsJ9wSOCEbxdNhKNZa
# vE/fi8yRhEMkV7xkIbw7HB89T4ytB7fzxdcC6REUgpqqtJRyO3ENGu9oa4V5jq9m
# 6liYDbrBfHnS/82zbzFF0AV0BAByaid+uDc/Oojtl4P1qzVND59ZO/Uv31nFfKUy
# dmCWyO3u+AR+GVFyqL9EQXq8ex47AJu8uuCWv5D+jZvDcosAEvggOmA498HMhYr7
# h3kuoSsg5sughZEjtsQoB1Qo3uwQMU+K8s0UHx7dVRzqKDFM+SFqqM3zlmf6AUGb
# zQ8LaH+73vFD6hflsNxwIrNpNll0a8bliSp85QuBXas/j7jRdnLzfKKp4pdBv8yM
# Rf5hyfZsBwsABOgVI0+CKi3278P6ETZIodH9ejk6NF2jLA6bd1AgNEDdsQMxrV/p
# YodzlgNh95Sw2VxsT+cUxeHxew0jnM1wjB1q3kotiyq720IUBQeq+xTcMdP2H2zL
# vmhmRHBNbRf5cesFc46RknXraFwe9kRhGCli3RdmiOwouklv2z53/rkxH3UcGKKm
# R73Y7kiFO/2z4g8/KpjGmvqCb7GlpYYdWjr6pGx0D3dSYWp/hyneOZuL7rNFYDAk
# lxUSKoUwkyaslqYt6HBtC6kyrSybKAp2QvJVYVGYlN7t9sUXbzwVELAOrbDexRb0
# ZdHML1pWCM+ZxPBVkcIseQIDAQABo4IBeDCCAXQwDgYDVR0PAQH/BAQDAgGGMBMG
# A1UdJQQMMAoGCCsGAQUFBwMIMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFEay
# HHfhexXwpTmhcN7RxC7qbbLeMB8GA1UdIwQYMBaAFK5sBaOTE+Ki5+LXHNbH8H/I
# Z1OgMHsGCCsGAQUFBwEBBG8wbTAuBggrBgEFBQcwAYYiaHR0cDovL29jc3AyLmds
# b2JhbHNpZ24uY29tL3Jvb3RyNjA7BggrBgEFBQcwAoYvaHR0cDovL3NlY3VyZS5n
# bG9iYWxzaWduLmNvbS9jYWNlcnQvcm9vdC1yNi5jcnQwNgYDVR0fBC8wLTAroCmg
# J4YlaHR0cDovL2NybC5nbG9iYWxzaWduLmNvbS9yb290LXI2LmNybDBHBgNVHSAE
# QDA+MDwGBFUdIAAwNDAyBggrBgEFBQcCARYmaHR0cHM6Ly93d3cuZ2xvYmFsc2ln
# bi5jb20vcmVwb3NpdG9yeS8wDQYJKoZIhvcNAQEMBQADggIBAItIujZXPHLF2nX5
# 7zL1hr3cEijjiC5PNl8mmewPASEQlpI4xnBrbfOu1A69Je+Gf+KJjZWlfilEA02q
# mKjxt9zqKWMh3O3NiArLEGlheSlCDCO86cXvUh4vMzfVT2Z6ZqlHVDOx3Rby2GRx
# ozGU5W/2TUvihGzQySVnT8hL0M5LBdY9+31B+oqxwCHgfgiw2WQr+eryxwr0zy4M
# NGDubLuS8D/xe1ISaHdZgfUcLqQ6jDkDDe3lzK9mSHlj1Um4/0vSJU9ITpM7k3ew
# mkhstqAds3SeX70iBDt8Nw2FtcOau92cWgONtA2fTHY01YWtRXu1n7suibusyL+S
# Y0jGP8oXqg28ABFfi+jjQ4SKQzTN/TvAonvbH7hnyIwV3j+mf8co76Fvb7JBzwIi
# 6wH4S8jSdm8l317aaGg9e0QEwkFuSTunmFYE7dEmKwSU2+TtZo49gJ2kpFV5UF7j
# +BofwBZvkBU8iqZIoQx7uirgsamHBUab7SVVPTdpmO1GmZiFRwoeYtv9nOXBQ0KO
# vc9v9oyR/YLkn+yt45VVBfNJL2009/9n7plAu9OagEJA2iOJYB+DcZK16ebKCvnd
# x2yyWEGcZo2bKm8fb1cEQ1yDXTtpnN45+oRNNfN7G22L8W8DwSlS4pS/e1SL30B6
# C3ACdz8viAcCAHXSr8bWIjIZozvoMIIFgzCCA2ugAwIBAgIORea7A4Mzw4VlSOb/
# RVEwDQYJKoZIhvcNAQEMBQAwTDEgMB4GA1UECxMXR2xvYmFsU2lnbiBSb290IENB
# IC0gUjYxEzARBgNVBAoTCkdsb2JhbFNpZ24xEzARBgNVBAMTCkdsb2JhbFNpZ24w
# HhcNMTQxMjEwMDAwMDAwWhcNMzQxMjEwMDAwMDAwWjBMMSAwHgYDVQQLExdHbG9i
# YWxTaWduIFJvb3QgQ0EgLSBSNjETMBEGA1UEChMKR2xvYmFsU2lnbjETMBEGA1UE
# AxMKR2xvYmFsU2lnbjCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAJUH
# 6HPKZvnsFMp7PPcNCPG0RQssgrRIxutbPK6DuEGSMxSkb3/pKszGsIhrxbaJ0cay
# /xTOURQh7ErdG1rG1ofuTToVBu1kZguSgMpE3nOUTvOniX9PeGMIyBJQbUJmL025
# eShNUhqKGoC3GYEOfsSKvGRMIRxDaNc9PIrFsmbVkJq3MQbFvuJtMgamHvm566qj
# uL++gmNQ0PAYid/kD3n16qIfKtJwLnvnvJO7bVPiSHyMEAc4/2ayd2F+4OqMPKq0
# pPbzlUoSB239jLKJz9CgYXfIWHSw1CM69106yqLbnQneXUQtkPGBzVeS+n68UARj
# NN9rkxi+azayOeSsJDa38O+2HBNXk7besvjihbdzorg1qkXy4J02oW9UivFyVm4u
# iMVRQkQVlO6jxTiWm05OWgtH8wY2SXcwvHE35absIQh1/OZhFj931dmRl4QKbNQC
# TXTAFO39OfuD8l4UoQSwC+n+7o/hbguyCLNhZglqsQY6ZZZZwPA1/cnaKI0aEYdw
# gQqomnUdnjqGBQCe24DWJfncBZ4nWUx2OVvq+aWh2IMP0f/fMBH5hc8zSPXKbWQU
# LHpYT9NLCEnFlWQaYw55PfWzjMpYrZxCRXluDocZXFSxZba/jJvcE+kNb7gu3Gdu
# yYsRtYQUigAZcIN5kZeR1BonvzceMgfYFGM8KEyvAgMBAAGjYzBhMA4GA1UdDwEB
# /wQEAwIBBjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBSubAWjkxPioufi1xzW
# x/B/yGdToDAfBgNVHSMEGDAWgBSubAWjkxPioufi1xzWx/B/yGdToDANBgkqhkiG
# 9w0BAQwFAAOCAgEAgyXt6NH9lVLNnsAEoJFp5lzQhN7craJP6Ed41mWYqVuoPId8
# AorRbrcWc+ZfwFSY1XS+wc3iEZGtIxg93eFyRJa0lV7Ae46ZeBZDE1ZXs6KzO7V3
# 3EByrKPrmzU+sQghoefEQzd5Mr6155wsTLxDKZmOMNOsIeDjHfrYBzN2VAAiKrlN
# IC5waNrlU/yDXNOd8v9EDERm8tLjvUYAGm0CuiVdjaExUd1URhxN25mW7xocBFym
# Fe944Hn+Xds+qkxV/ZoVqW/hpvvfcDDpw+5CRu3CkwWJ+n1jez/QcYF8AOiYrg54
# NMMl+68KnyBr3TsTjxKM4kEaSHpzoHdpx7Zcf4LIHv5YGygrqGytXm3ABdJ7t+uA
# /iU3/gKbaKxCXcPu9czc8FB10jZpnOZ7BN9uBmm23goJSFmH63sUYHpkqmlD75HH
# TOwY3WzvUy2MmeFe8nI+z1TIvWfspA9MRf/TuTAjB0yPEL+GltmZWrSZVxykzLsV
# iVO6LAUP5MSeGbEYNNVMnbrt9x+vJJUEeKgDu+6B5dpffItKoZB0JaezPkvILFa9
# x8jvOOJckvB595yEunQtYQEgfn7R8k8HWV+LLUNS60YMlOH1Zkd5d9VUWx+tJDfL
# RVpOoERIyNiwmcUVhAn21klJwGW45hpxbqCo8YLoRT5s1gLXCmeDBVrJpBAxggNh
# MIIDXQIBATBzMF4xCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52
# LXNhMTQwMgYDVQQDEytHbG9iYWxTaWduIE9mZmxpbmUgUjQ1IFRpbWVzdGFtcGlu
# ZyBDQSAyMDI1AhEAhHI/wZXMFvHbK6L2YN8r5DALBglghkgBZQMEAgKgggFBMBoG
# CSqGSIb3DQEJAzENBgsqhkiG9w0BCRABBDArBgkqhkiG9w0BCTQxHjAcMAsGCWCG
# SAFlAwQCAqENBgkqhkiG9w0BAQwFADA/BgkqhkiG9w0BCQQxMgQwymZEa0BeKkHe
# CoE2FhDrG+6FufwJRFYWxGcui1H9pjNs8DrJQuNJxnu1yeSYs9vgMIG0BgsqhkiG
# 9w0BCRACLzGBpDCBoTCBnjCBmwQggyrXLlI/3qyD+kaUvOfGzCYXZIgoZlZliMit
# yjqDhVEwdzBipGAwXjELMAkGA1UEBhMCQkUxGTAXBgNVBAoTEEdsb2JhbFNpZ24g
# bnYtc2ExNDAyBgNVBAMTK0dsb2JhbFNpZ24gT2ZmbGluZSBSNDUgVGltZXN0YW1w
# aW5nIENBIDIwMjUCEQCEcj/BlcwW8dsrovZg3yvkMA0GCSqGSIb3DQEBDAUABIIB
# gGq1BPRmjlEGT6Wy03Ifn6k5qj/QOlvj3/6Vob9FAWevxZhM/pYgbrRdHMgNIQjT
# iucRasZx/KCDmDXoJnKWyB4FOqUckXSw4qX5Juy9oQT5d8gABpPhWzxS/Eo0ya/I
# /7HdzkcIo2VkgN4lCiVUXbWwCKm2FlT1vw+fFt73yOj3wjbf817gnLEik9vwTO8j
# 3yA5l93HpG4AcWUrmBD0iSMpQS+yMXu52eZh6Pi8b869CE9areXGoUoycP+b3NRg
# MVAWBDSJ1o3BtHq0vu6iSecpQecvfGnvfqoI4TSlj9aYLMdPL3r3QN7FEX/udWKq
# s+nMNAEOPbNF19aFT7I64yx5JdPhlhHkK/yahf3pPa3riTd0JXXWhtrBjvq9U7ow
# 7G/2NNrb7EcU2mUxjczlsGkRsYc18EjqBi9xKbZB7z8tTEr7cw9G65uNYdmM9aF4
# UpCBLIV++UcTF7Cn1d+Ba7FLiEOw9GSw02216AfsobsMU5i1HPjFhcdM5EW/nQb2
# IQ==
# SIG # End signature block
