# Integracja pulpitu RDP

Możesz użyć tego połączenia RDP w XPipe, aby szybko uruchamiać aplikacje i skrypty. Jednak ze względu na charakter RDP, musisz edytować listę zezwoleń aplikacji zdalnych na serwerze, aby to działało.

Możesz również tego nie robić i po prostu użyć XPipe do uruchomienia klienta RDP bez korzystania z zaawansowanych funkcji integracji pulpitu.

## Listy zezwoleń RDP

Serwer RDP wykorzystuje koncepcję list zezwoleń do obsługi uruchamiania aplikacji. Zasadniczo oznacza to, że o ile lista zezwoleń nie jest wyłączona lub określone aplikacje nie zostały wyraźnie dodane do listy zezwoleń, bezpośrednie uruchomienie jakichkolwiek aplikacji zdalnych zakończy się niepowodzeniem.

Możesz znaleźć ustawienia listy dozwolonych w rejestrze swojego serwera w `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`.

### Zezwalaj na wszystkie aplikacje

Możesz wyłączyć listę zezwoleń, aby umożliwić uruchamianie wszystkich zdalnych aplikacji bezpośrednio z XPipe. W tym celu możesz uruchomić następujące polecenie na serwerze w PowerShell: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### Dodawanie dozwolonych aplikacji

Alternatywnie możesz również dodać do listy poszczególne aplikacje zdalne. Pozwoli to na uruchamianie wymienionych aplikacji bezpośrednio z XPipe.

Pod kluczem `Applications` w `TSAppAllowList` utwórz nowy klucz o dowolnej nazwie. Jedynym wymaganiem dotyczącym nazwy jest to, aby była ona unikalna w obrębie elementów podrzędnych klucza "Applications". Ten nowy klucz musi zawierać następujące wartości: `Name`, `Path` i `CommandLineSetting`. Możesz to zrobić w PowerShell za pomocą następujących poleceń:

```
$appName="Notepad"
$appPath="C:\Windows\System32\notepad.exe"

$regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -Path "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

Jeśli chcesz zezwolić XPipe na uruchamianie skryptów i otwieranie sesji terminala, musisz dodać `C:\Windows\System32\cmd.exe` do listy dozwolonych.

## Względy bezpieczeństwa

Nie sprawia to, że twój serwer jest w jakikolwiek sposób niezabezpieczony, ponieważ zawsze możesz uruchomić te same aplikacje ręcznie podczas uruchamiania połączenia RDP. Listy zezwoleń mają raczej na celu uniemożliwienie klientom natychmiastowego uruchomienia dowolnej aplikacji bez udziału użytkownika. Ostatecznie to od Ciebie zależy, czy zaufasz XPipe w tym zakresie. Możesz uruchomić to połączenie po wyjęciu z pudełka, jest to przydatne tylko wtedy, gdy chcesz korzystać z którejkolwiek z zaawansowanych funkcji integracji pulpitu w XPipe.
