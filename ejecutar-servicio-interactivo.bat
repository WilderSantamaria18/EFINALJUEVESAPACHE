@echo off
cd /d "%~dp0"

REM Verificar si ya esta compilado
if exist "target\classes\com\iphone\store\Main.class" (
    echo Proyecto ya compilado. Saltando compilacion...
    goto :setupClasspath
)

echo Compilando el proyecto...
call mvn compile -DskipTests -q

if errorlevel 1 (
    echo Error al compilar el proyecto
    pause
    exit /b 1
)

:setupClasspath

:setupClasspath
set CLASSPATH=target\classes

REM Copiar dependencias si no existen
if not exist "target\lib" (
    echo Copiando dependencias...
    call mvn dependency:copy-dependencies -DoutputDirectory=target\lib -q
)

for %%i in (target\lib\*.jar) do call :addToClasspath %%i
goto :afterClasspath

:addToClasspath
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:afterClasspath

:menu
cls
echo ================================================================
echo      EJECUTAR SERVICIOS CON LOGICA DE NEGOCIO (MODO CONSOLA)
echo ================================================================
echo   1. RENIEC Service     - Validacion DNI con logica
echo   2. RUC Service        - Validacion RUC con algoritmo
echo   3. Producto Service   - Precios, descuentos, promociones
echo   4. Inventario Service - Gestion stock, alertas, reservas
echo   5. Venta Service      - Calculo totales, descuentos, puntos
echo   6. Empleado Service   - Comisiones, metas, bonificaciones
echo   7. TODOS (modo JMS)   - Iniciar todos los servicios
echo   0. Salir
echo ================================================================

echo.
set /p opcion="Seleccione opcion: "

if "%opcion%"=="1" goto reniec
if "%opcion%"=="2" goto ruc
if "%opcion%"=="3" goto producto
if "%opcion%"=="4" goto inventario
if "%opcion%"=="5" goto venta
if "%opcion%"=="6" goto empleado
if "%opcion%"=="7" goto todos
if "%opcion%"=="0" exit /b 0
echo Opcion invalida
pause
goto menu

:reniec
cls
echo.
echo ========================================
echo    RENIEC SERVICE - Modo Interactivo
echo ========================================
echo.
java -cp "%CLASSPATH%" com.iphone.store.services.ReniecService --interactive < CON
echo.
pause
goto menu

:ruc
cls
java -cp "%CLASSPATH%" com.iphone.store.services.RucService --interactive
pause
goto menu

:producto
cls
java -cp "%CLASSPATH%" com.iphone.store.services.ProductoService --interactive
pause
goto menu

:inventario
cls
java -cp "%CLASSPATH%" com.iphone.store.services.InventarioService --interactive
pause
goto menu

:venta
cls
java -cp "%CLASSPATH%" com.iphone.store.services.VentaService --interactive
pause
goto menu

:empleado
cls
java -cp "%CLASSPATH%" com.iphone.store.services.EmpleadoService --interactive
pause
goto menu

:todos
cls
echo Iniciando todos los servicios en modo JMS...
start "RENIEC Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.ReniecService"
timeout /t 2 /nobreak > nul

start "RUC Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.RucService"
timeout /t 2 /nobreak > nul

start "Producto Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.ProductoService"
timeout /t 2 /nobreak > nul

start "Inventario Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.InventarioService"
timeout /t 2 /nobreak > nul

start "Venta Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.VentaService"
timeout /t 2 /nobreak > nul

start "Empleado Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.EmpleadoService"
timeout /t 2 /nobreak > nul

echo.
echo Todos los servicios iniciados en ventanas separadas
pause
goto menu
