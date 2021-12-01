cd ..\app\
SET "dir=%~dp0test_env"
CALL ..\gradlew.bat run -Dio.xpipe.storage.dir=%dir% -Dio.xpipe.beacon.port=21722 -Dio.xpipe.daemon.mode=gui
pause