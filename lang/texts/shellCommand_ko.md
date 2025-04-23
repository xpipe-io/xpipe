## 사용자 지정 셸 연결

선택한 호스트 시스템에서 지정된 명령을 실행하여 사용자 지정 명령을 사용하여 셸을 엽니다. 이 셸은 로컬 또는 원격 셸일 수 있습니다.

이 기능은 셸이 `cmd`, `bash` 등과 같은 표준 유형이어야 한다는 점에 유의하세요. 터미널에서 다른 유형의 셸과 명령을 열려면 사용자 지정 터미널 명령 유형을 대신 사용할 수 있습니다. 표준 셸을 사용하면 파일 브라우저에서도 이 연결을 열 수 있습니다.

### 대화형 프롬프트

비밀번호 프롬프트와 같은 예기치 않은 필수 입력 프롬프트가 있는 경우 셸 프로세스가 시간 초과되거나 중단될 수 있습니다
입력 프롬프트가 있을 경우 셸 프로세스가 시간 초과되거나 중단될 수 있습니다. 따라서 항상 대화형 입력 프롬프트가 없는지 확인해야 합니다.

예를 들어, 비밀번호가 필요하지 않은 경우 `ssh user@host`와 같은 명령이 정상적으로 작동합니다.

### 사용자 지정 로컬 셸

대부분의 경우 일부 스크립트와 명령이 제대로 작동하도록 하기 위해 일반적으로 기본적으로 비활성화되어 있는 특정 옵션을 사용하여 셸을 실행하는 것이 유용합니다. 예를 들어

-   [지연 확장
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [파워쉘 실행
    정책](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    모드](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- 그리고 원하는 셸에 대한 다른 실행 옵션도 가능합니다

예를 들어 다음 명령을 사용하여 사용자 지정 셸 명령을 만들면 됩니다:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`