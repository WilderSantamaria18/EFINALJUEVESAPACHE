#!/usr/bin/env bash
set -euo pipefail

# Script: iniciar-servicios.sh
# Inicia los 6 servicios en modo JMS en segundo plano.
# Uso: ./iniciar-servicios.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$SCRIPT_DIR"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: no se encontro '$cmd' en PATH. Instala $cmd e intenta de nuevo."
    exit 1
  fi
}

require_cmd java
require_cmd mvn

mkdir -p logs

ensure_built() {
  if [[ ! -d "target/classes" ]]; then
    echo "Compilando proyecto (mvn -DskipTests compile)..."
    mvn -DskipTests -q compile
  fi

  if [[ ! -f "target/classpath.txt" ]]; then
    echo "Generando classpath de dependencias (mvn dependency:build-classpath)..."
    mvn -q -DincludeScope=runtime -Dmdep.outputFile=target/classpath.txt dependency:build-classpath
  fi
}

start_bg() {
  local name="$1"
  local main_class="$2"
  local logfile="logs/${name}.log"

  ensure_built

  local cp
  cp="target/classes:$(cat target/classpath.txt)"

  echo "Iniciando $name..."
  nohup java -cp "$cp" "$main_class" >"$logfile" 2>&1 &
  local pid=$!
  echo "$name $pid" >> logs/pids.txt
  echo "PID $pid (log: $logfile)"
}

# Limpia el archivo de PIDs para esta ejecucion
: > logs/pids.txt

start_bg "reniec" "com.iphone.store.services.ReniecService"
start_bg "ruc" "com.iphone.store.services.RucService"
start_bg "producto" "com.iphone.store.services.ProductoService"
start_bg "inventario" "com.iphone.store.services.InventarioService"
start_bg "venta" "com.iphone.store.services.VentaService"
start_bg "empleado" "com.iphone.store.services.EmpleadoService"

echo
echo "Listo. Servicios iniciados en segundo plano."
echo "Logs en: $(pwd)/logs"
echo "PIDs en: $(pwd)/logs/pids.txt"
echo "Para detenerlos: kill <PID> (usa logs/pids.txt)"
