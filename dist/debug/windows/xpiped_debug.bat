@echo off
set CDS_JVM_OPTS=JVM-ARGS
CALL "%~dp0\..\runtime\bin\xpiped.bat" %*
