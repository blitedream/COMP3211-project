@echo off
setlocal
title PIM - Personal Information Manager
pushd "%~dp0"
if errorlevel 1 (
    echo Cannot open the project folder.
    pause
    exit /b 1
)
where java >nul 2>&1
if errorlevel 1 goto missingjava
where javac >nul 2>&1
if errorlevel 1 goto missingjava
echo Starting PIM...
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
set "pimExit=%errorlevel%"
if not "%pimExit%"=="0" (
    echo.
    echo PIM could not start or stopped unexpectedly. See the error above.
    pause
)
popd
exit /b %pimExit%
:missingjava
echo.
echo PIM needs a Java Development Kit with java and javac on PATH.
echo Install or configure a JDK, then reopen this launcher.
echo See README.md for setup requirements.
pause
popd
exit /b 1
