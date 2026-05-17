@echo off
setlocal

set BASE_DIR=%~dp0
set WRAPPER_JAR=%BASE_DIR%.mvn\wrapper\maven-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper jar not found: %WRAPPER_JAR% 1>&2
  echo Download it from the wrapperUrl in .mvn\wrapper\maven-wrapper.properties. 1>&2
  exit /b 1
)

java -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
