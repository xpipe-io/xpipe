# 추가 RDP 옵션

연결을 추가로 사용자 지정하려면 .rdp 파일에 포함된 것과 동일한 방식으로 RDP 속성을 제공하면 됩니다. 사용 가능한 속성의 전체 목록은 [RDP 문서](https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/clients/rdp-files)를 참조하세요.

이러한 옵션의 형식은 `옵션:유형:값`입니다. 예를 들어 데스크톱 창의 크기를 사용자 지정하려면 다음 구성을 전달할 수 있습니다:
```
desktopwidth:i:*폭*
데스크톱높이:i:*높이*
```
