@echo off
echo ========================================
echo   INICIANDO ACTIVEMQ
echo ========================================
echo.

REM Descomentar y ajustar la ruta si tienes ActiveMQ instalado localmente
REM set ACTIVEMQ_HOME=C:\apache-activemq-5.17.3
REM cd /d %ACTIVEMQ_HOME%\bin
REM call activemq.bat start

echo OPCION 1: Usar ActiveMQ embebido (recomendado)
echo ---------------------------------------------
echo ActiveMQ se iniciara automaticamente cuando ejecutes Main.java
echo.

echo OPCION 2: Descargar ActiveMQ standalone
echo ---------------------------------------------
echo 1. Descargar: https://activemq.apache.org/components/classic/download/
echo 2. Extraer en C:\apache-activemq-5.17.3
echo 3. Ejecutar: C:\apache-activemq-5.17.3\bin\activemq.bat start
echo.

echo OPCION 3: Usar Docker (mas rapido)
echo ---------------------------------------------
echo docker run -d --name activemq -p 61616:61616 -p 8161:8161 rmohr/activemq
echo.

echo Consola web: http://localhost:8161/admin
echo Usuario: admin / Password: admin
echo.

pause
