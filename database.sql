CREATE DATABASE IF NOT EXISTS iphone_store;

USE iphone_store_ESB;

CREATE TABLE IF NOT EXISTS reniec (
    dni VARCHAR(8) PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE,
    direccion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS ruc (
    ruc VARCHAR(11) PRIMARY KEY,
    razon_social VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    direccion VARCHAR(200),
    telefono VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS productos (
    codigo VARCHAR(10) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL
);

CREATE TABLE IF NOT EXISTS ventas (
    id INT PRIMARY KEY AUTO_INCREMENT,
    fecha DATE NOT NULL,
    dni_cliente VARCHAR(8),
    total DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    codigo_producto VARCHAR(10) NOT NULL,
    cantidad INT NOT NULL,
    ubicacion VARCHAR(50) NOT NULL,
    fecha_actualizacion DATE
);

CREATE TABLE IF NOT EXISTS empleados (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dni VARCHAR(8) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    cargo VARCHAR(50) NOT NULL,
    salario DECIMAL(10,2) NOT NULL,
    fecha_ingreso DATE
);

INSERT INTO reniec (dni, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, direccion) VALUES
('12345678', 'Juan Carlos', 'Perez', 'Lopez', '1985-03-15', 'Av. Larco 123, Miraflores'),
('87654321', 'Maria Elena', 'Garcia', 'Rojas', '1990-07-22', 'Jr. Ucayali 456, Lima'),
('11223344', 'Pedro Luis', 'Martinez', 'Silva', '1988-11-30', 'Av. Arequipa 789, Lince'),
('44332211', 'Ana Sofia', 'Rodriguez', 'Vargas', '1992-05-18', 'Calle Lima 321, Surco');

INSERT INTO ruc (ruc, razon_social, estado, direccion, telefono) VALUES
('20123456789', 'COMERCIAL TECH SAC', 'ACTIVO', 'Av. Javier Prado 1234, San Isidro', '01-4567890'),
('20987654321', 'DISTRIBUIDORA PERU EIRL', 'ACTIVO', 'Jr. Lampa 567, Lima', '01-7654321'),
('20111222333', 'SERVICIOS GLOBALES SA', 'ACTIVO', 'Av. Aviacion 890, San Borja', '01-1112233'),
('20444555666', 'IMPORTACIONES DEL SUR SAC', 'SUSPENDIDO', 'Calle Los Pinos 432, Miraflores', '01-4445556');

INSERT INTO productos (codigo, nombre, precio, stock) VALUES
('IPHONE13', 'iPhone 13 128GB', 2999.00, 50),
('IPHONE14', 'iPhone 14 256GB', 3999.00, 30),
('IPHONE15', 'iPhone 15 Pro 512GB', 5499.00, 20),
('AIRPODS', 'AirPods Pro 2da Gen', 999.00, 100);

INSERT INTO ventas (fecha, dni_cliente, total, estado) VALUES
('2024-11-01', '12345678', 2999.00, 'COMPLETADO'),
('2024-11-05', '87654321', 3999.00, 'COMPLETADO'),
('2024-11-10', '11223344', 999.00, 'PENDIENTE');

INSERT INTO inventario (codigo_producto, cantidad, ubicacion, fecha_actualizacion) VALUES
('IPHONE13', 50, 'ALMACEN-A', '2024-11-01'),
('IPHONE14', 30, 'ALMACEN-A', '2024-11-01'),
('IPHONE15', 20, 'ALMACEN-B', '2024-11-01'),
('AIRPODS', 100, 'ALMACEN-C', '2024-11-01');

INSERT INTO empleados (dni, nombre_completo, cargo, salario, fecha_ingreso) VALUES
('12345678', 'Juan Carlos Perez Lopez', 'VENDEDOR', 1500.00, '2023-01-15'),
('87654321', 'Maria Elena Garcia Rojas', 'GERENTE', 3500.00, '2022-06-01'),
('11223344', 'Pedro Luis Martinez Silva', 'ALMACENERO', 1200.00, '2023-09-10');
