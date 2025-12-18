#!/usr/bin/env bash
set -euo pipefail

# Script: ejecutar-servicio-interactivo.sh
# Uso: ./ejecutar-servicio-interactivo.sh

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

ensure_built() {
  # Compila si no existe target/classes
  if [[ ! -d "target/classes" ]]; then
    echo "Compilando proyecto (mvn -DskipTests compile)..."
    mvn -DskipTests -q compile
  fi

  # Genera classpath con dependencias si no existe
  if [[ ! -f "target/classpath.txt" ]]; then
    echo "Generando classpath de dependencias (mvn dependency:build-classpath)..."
    mvn -q -DincludeScope=runtime -Dmdep.outputFile=target/classpath.txt dependency:build-classpath
  fi
}

run_service() {
  local main_class="$1"
  ensure_built

  local cp
  cp="target/classes:$(cat target/classpath.txt)"

  java -cp "$cp" "$main_class" --interactive
}

while true; do
  echo
  echo "SERVICIOS - MODO INTERACTIVO"
  echo "1. RENIEC"
  echo "2. RUC"
  echo "3. PRODUCTO"
  echo "4. INVENTARIO"
  echo "5. VENTA"
  echo "6. EMPLEADO"
  echo "0. Salir"
  printf "Opcion: "

  read -r opt

  case "$opt" in
    1) run_service "com.iphone.store.services.ReniecService" ;;
    2) run_service "com.iphone.store.services.RucService" ;;
    3) run_service "com.iphone.store.services.ProductoService" ;;
    4) run_service "com.iphone.store.services.InventarioService" ;;
    5) run_service "com.iphone.store.services.VentaService" ;;
    6) run_service "com.iphone.store.services.EmpleadoService" ;;
    0) echo "Saliendo..."; exit 0 ;;
    *) echo "Opcion invalida" ;;
  esac

done
