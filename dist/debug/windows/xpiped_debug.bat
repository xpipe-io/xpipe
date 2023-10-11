@echo off
set CDS_JVM_OPTS=JVM-ARGS
chcp 65001 > NUL
CALL "%~dp0\..\runtime\bin\xpiped.bat" %*
pause
