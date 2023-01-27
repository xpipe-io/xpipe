CALL "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
set JAVA_HOME=%GRAALVM_HOME%
"%~dp0\..\gradlew.bat" :cli:nativeCompile