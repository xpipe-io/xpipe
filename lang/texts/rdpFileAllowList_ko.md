# RDP 데스크톱 통합

XPipe에서 이 RDP 연결을 사용하여 애플리케이션 및 스크립트를 빠르게 실행할 수 있습니다. 그러나 RDP의 특성상 이 기능을 사용하려면 서버에서 원격 애플리케이션 허용 목록을 편집해야 합니다.

이 작업을 수행하지 않고 고급 데스크톱 통합 기능을 사용하지 않고 XPipe를 사용하여 RDP 클라이언트를 시작할 수도 있습니다.

## RDP 허용 목록

RDP 서버는 허용 목록이라는 개념을 사용하여 애플리케이션 시작을 처리합니다. 즉, 허용 목록이 비활성화되어 있거나 특정 애플리케이션이 허용 목록에 명시적으로 추가되지 않는 한 원격 애플리케이션을 직접 실행하는 데 실패합니다.

허용 목록 설정은 서버의 레지스트리에서 `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList`에서 찾을 수 있습니다.

### 모든 애플리케이션 허용

허용 목록을 비활성화하여 모든 원격 애플리케이션을 XPipe에서 직접 시작할 수 있도록 허용할 수 있습니다. 이를 위해 PowerShell에서 서버에서 다음 명령을 실행할 수 있습니다: `Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList' -Name "fDisabledAllowList" -Value 1`.

### 허용된 애플리케이션 추가

또는 개별 원격 애플리케이션을 목록에 추가할 수도 있습니다. 이렇게 하면 목록에 있는 애플리케이션을 XPipe에서 직접 실행할 수 있습니다.

`TSAppAllowList`의 `Applications` 키 아래에 임의의 이름으로 새 키를 만듭니다. 이름에 대한 유일한 요구 사항은 "Applications" 키의 하위 키 내에서 고유해야 한다는 것입니다. 이 새 키에는 다음 값이 포함되어야 합니다: `Name`, `Path` 및 `CommandLineSetting`. PowerShell에서 다음 명령을 사용하여 이 작업을 수행할 수 있습니다:

```
$appName="메모장"
appPath="C:\Windows\System32\notepad.exe"

regKey="HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Terminal Server\TSAppAllowList\Applications"
New-item -경로 "$regKey\$appName"
New-ItemProperty -Path "$regKey\$appName" -Name "Name" -Value "$appName" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "Path" -Value "$appPath" -Force
New-ItemProperty -Path "$regKey\$appName" -Name "CommandLineSetting" -Value "1" -PropertyType DWord -Force
```

XPipe가 스크립트를 실행하고 터미널 세션을 열 수 있도록 허용하려면 `C:\Windows\System32\cmd.exe`도 허용 목록에 추가해야 합니다.

## 보안 고려 사항

RDP 연결을 시작할 때 언제든지 동일한 애플리케이션을 수동으로 실행할 수 있으므로 서버가 어떤 식으로든 안전하지 않습니다. 허용 목록은 클라이언트가 사용자 입력 없이 애플리케이션을 즉시 실행하지 못하도록 하기 위한 것입니다. 결국 XPipe를 신뢰할지 여부는 사용자에게 달려 있습니다. 이 연결은 기본적으로 바로 시작할 수 있으며, XPipe의 고급 데스크톱 통합 기능을 사용하려는 경우에만 유용합니다.
