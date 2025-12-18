@echo off
cd /d "%~dp0"

echo Compilando el proyecto...
call mvnd clean package -DskipTests

if errorlevel 1 (
    echo Error al compilar el proyecto
    pause
    exit /b 1
)

echo.
echo Compilacion exitosa
echo.

set CLASSPATH=target\classes
for %%i in (target\lib\*.jar) do call :addToClasspath %%i
goto :afterClasspath

:addToClasspath
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:afterClasspath

echo ========================================
echo Iniciando Servicios ESB
echo ========================================

start "RENIEC Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.ReniecService"
timeout /t 2 /nobreak > nul

start "RUC Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.RucService"
timeout /t 2 /nobreak > nul

start "Producto Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.ProductoService"
timeout /t 2 /nobreak > nul

start "Venta Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.VentaService"
timeout /t 2 /nobreak > nul

start "Inventario Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.InventarioService"
timeout /t 2 /nobreak > nul

start "Empleado Service" cmd /k "java -cp "%CLASSPATH%" com.iphone.store.services.EmpleadoService"
timeout /t 2 /nobreak > nul

echo.
echo Todos los servicios han sido iniciados
echo Presiona cualquier tecla para salir...
pause > nul
