@echo off
echo ========================================
echo   EJECUTAR PROYECTO COMPLETO
echo ========================================
echo.

echo PASO 1: Iniciar ActiveMQ (colas JMS)
echo -------------------------------------
echo Opcion A - Docker (recomendado):
echo   docker run -d --name activemq -p 61616:61616 -p 8161:8161 rmohr/activemq
echo.
echo Opcion B - ActiveMQ local:
echo   Descarga: https://activemq.apache.org/components/classic/download/
echo   Ejecuta: bin\activemq.bat start
echo.
pause

echo.
echo PASO 2: Compilar proyecto
echo -------------------------------------
call mvn clean compile
if errorlevel 1 (
    echo ERROR: Compilacion fallida
    pause
    exit /b 1
)

echo.
echo PASO 3: Ejecutar flujos ESB (Punto a + c)
echo -------------------------------------
echo Iniciando Main.java con menu interactivo...
echo.
call mvn exec:java -Dexec.mainClass="com.iphone.store.Main"

pause
