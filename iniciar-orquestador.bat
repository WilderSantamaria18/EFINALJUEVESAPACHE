@echo off
cd /d "%~dp0"

echo ========================================
echo Iniciando Orquestador ESB
echo ========================================
echo.
echo Asegurate de que:
echo 1. ActiveMQ este corriendo
echo 2. Todos los servicios esten iniciados
echo.
pause

set CLASSPATH=target\classes
for %%i in (target\lib\*.jar) do call :addToClasspath %%i
goto :afterClasspath

:addToClasspath
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:afterClasspath

java -cp "%CLASSPATH%" com.iphone.store.orchestrator.ESBOrchestrator

pause
