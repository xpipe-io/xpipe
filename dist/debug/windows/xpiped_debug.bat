@echo off
set CDS_JVM_OPTS=JVM-ARGS
set "_JAVA_OPTIONS="
set "JAVA_TOOL_OPTIONS="
chcp 65001 > NUL
CALL "%~dp0\..\runtime\bin\xpiped.bat" %*
pause
