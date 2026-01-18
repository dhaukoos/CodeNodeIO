@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set GRADLE_WRAPPER=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%GRADLE_WRAPPER%" (
    echo Error: gradle-wrapper.jar not found at %GRADLE_WRAPPER%
    exit /b 1
)

if "%JAVA_HOME%"=="" (
    set JAVA_CMD=java
) else (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)

"%JAVA_CMD%" -classpath "%GRADLE_WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*

:end
@endlocal

@if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem using an error code of 0 or 1.
if not ""=="%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

