# Diseño y Planificación - Iteración 1

## Trabajo en equipo

Durante esta primera iteración, el equipo se distribuyó las historias de usuario según sus fortalezas y conocimientos previos. A continuación se detalla el trabajo realizado por cada integrante:

**Axel Dos Santos:**
- Implementó la **HU-01: Alta de cliente**, incluyendo el modelo de datos `CuentaCliente` con validaciones Bean Validation completas, el controlador con manejo de errores, el servicio con lógica de negocio y las vistas Thymeleaf.
- Implementó la **HU-21: Asignar servicio a un cliente**, desarrollando el modelo rico con métodos de negocio en `CuentaCliente`, la relación bidireccional con `ServicioContratado`, endpoints en el controlador y las vistas para asignación y consulta de histórico.
- Colaboró en la configuración inicial del proyecto Spring Boot 

**Leandro Escalada:**
- Implementó la **HU-16: Alta de servicio**, creando el modelo `Servicio` con validaciones y métodos de cálculo de IVA.
- Implementó la **HU-17: Listado de servicio**, desarrollando el repositorio con consultas personalizadas y la vista de lista con búsqueda.

**Gabriel Mittelstedt:**
- Implementó la **HU-04: Emisión de factura individual** (trabajo en progreso).
- Implementó la **HU-06: Consulta de factura individual** (trabajo en progreso).

**Marcos Douberman:**
- Implementó la **HU-07: Emisión de facturación masiva por período** (trabajo en progreso).
- Implementó la **HU-10: Listado y búsqueda de facturas** (trabajo en progreso).

**Axel Limberger:**
- Implementó la **HU-11: Registrar pago total de factura** (trabajo en progreso).
- Implementó la **HU-12: Registrar pago parcial de factura** (trabajo en progreso).

**Tareas compartidas del equipo:**
- Definición de la arquitectura MVC del proyecto
- Establecimiento de convenciones de código y nomenclatura
- Configuración del repositorio Git y estrategia de branches
- Documentación JavaDoc en todas las clases del modelo

---

## Diseño OO

(Insertar DC de la primera iteración)
---

## Wireframe y Casos de Uso

### Wireframe: Alta de Cliente

**Vista: formulario.html**
![a](imagenes/Wireframe_Alta_Cliente.png)


**Caso de Uso: Alta de Cliente**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | El administrador ha iniciado sesión y accedió a la sección de clientes |
| **Flujo Principal** | 1. El administrador hace clic en "Nuevo Cliente"<br>2. El sistema muestra el formulario de alta<br>3. El administrador completa los campos obligatorios (nombre, razón social, CUIT/DNI, domicilio, email, condición IVA, estado)<br>4. El administrador opcionalmente ingresa el teléfono<br>5. El administrador hace clic en "Guardar Cliente"<br>6. El sistema valida el formato del CUIT/DNI (7-11 dígitos)<br>7. El sistema valida que el CUIT/DNI no exista en la base de datos<br>8. El sistema valida el formato del email<br>9. El sistema guarda el cliente con estado inicial ACTIVA y saldo en 0.00<br>10. El sistema muestra mensaje de éxito y redirige al listado de clientes |
| **Flujos Alternativos** | **4a.** Si hay errores de validación (campo vacío, formato inválido):<br>&nbsp;&nbsp;1. El sistema muestra los errores debajo de cada campo afectado<br>&nbsp;&nbsp;2. El formulario conserva los datos ingresados<br>&nbsp;&nbsp;3. Vuelve al paso 3<br><br>**7a.** Si el CUIT/DNI ya existe:<br>&nbsp;&nbsp;1. El sistema muestra error "Ya existe un cliente con el CUIT/DNI: XXXXXXX"<br>&nbsp;&nbsp;2. Vuelve al paso 3 |
| **Postcondición** | Se crea un nuevo cliente con estado ACTIVA y saldo inicial 0.00 |

---

### Wireframe: Detalle de Cliente y Asignación de Servicios

**Vista: detalle.html**

![a](imagenes/Wireframe_DetalleCliente_AsignaciónDeServicio.png)

**Vista: agregar-servicio.html**

![a](imagenes/Wireframe_AgregarServicio.png)

**Vista: historico-servicios.html**

![a](imagenes/Wireframe_HistoricoDeServicios.png)

**Caso de Uso: Asignar Servicio a un Cliente**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | El cliente existe en el sistema con estado ACTIVA<br>Existen servicios creados y activos en el sistema |
| **Flujo Principal** | 1. El administrador accede al detalle del cliente<br>2. El administrador hace clic en "Asignar Servicio"<br>3. El sistema muestra una lista de servicios activos que el cliente NO tiene contratados<br>4. El administrador selecciona un servicio y hace clic en "Asignar"<br>5. El sistema registra la contratación con:<br>&nbsp;&nbsp;- Fecha actual (LocalDate.now())<br>&nbsp;&nbsp;- Precio actual del servicio<br>&nbsp;&nbsp;- Estado activo = true<br>6. El sistema valida que el servicio no esté ya contratado activamente<br>7. El sistema persiste la relación ServicioContratado<br>8. El sistema muestra mensaje de éxito "Servicio asignado exitosamente"<br>9. El sistema redirige al detalle del cliente |
| **Flujos Alternativos** | **6a.** Si el servicio ya está contratado activamente:<br>&nbsp;&nbsp;1. El sistema muestra error "El servicio 'X' ya está contratado para este cliente"<br>&nbsp;&nbsp;2. Vuelve al paso 3<br><br>**3a.** Si no hay servicios disponibles:<br>&nbsp;&nbsp;1. El sistema muestra mensaje "No hay servicios disponibles para asignar"<br>&nbsp;&nbsp;2. Muestra botón para volver al detalle |
| **Postcondición** | El servicio queda vinculado al cliente con precio histórico y fecha registrada |

**Caso de Uso: Ver Histórico de Servicios del Cliente**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | El cliente existe en el sistema |
| **Flujo Principal** | 1. El administrador accede al detalle del cliente<br>2. El administrador hace clic en "Ver Histórico"<br>3. El sistema muestra una tabla con TODOS los servicios contratados (activos e inactivos)<br>4. Para cada servicio se visualiza: nombre, descripción, fecha de alta, precio contratado, alícuota IVA, total con IVA y estado (Activo/Inactivo)<br>5. El sistema muestra un resumen con contadores: servicios activos, inactivos y total<br>6. El administrador puede volver al detalle del cliente |
| **Flujos Alternativos** | **3a.** Si el cliente no tiene servicios contratados:<br>&nbsp;&nbsp;1. El sistema muestra mensaje "No hay servicios contratados"<br>&nbsp;&nbsp;2. Muestra botón para asignar el primer servicio |
| **Postcondición** | El administrador visualiza el histórico completo sin modificar datos |

---

## Backlog de Iteración 1

Las siguientes historias de usuario fueron seleccionadas para implementarse en esta iteración:

1. **HU-01:** Alta de cliente - *Responsable: Axel Dos Santos*
2. **HU-16:** Alta de servicio - *Responsable: Leandro Escalada*
3. **HU-17:** Listado de servicio - *Responsable: Leandro Escalada*
4. **HU-21:** Asignar servicio a un cliente - *Responsable: Axel Dos Santos*
5. **HU-04:** Emisión de factura individual - *Responsable: Gabriel Mittelstedt*
6. **HU-07:** Emisión de facturación masiva por período - *Responsable: Marcos Douberman*
7. **HU-06:** Consulta de factura individual - *Responsable: Gabriel Mittelstedt*
8. **HU-10:** Listado y búsqueda de facturas - *Responsable: Marcos Douberman*
9. **HU-11:** Registrar pago total de factura - *Responsable: Axel Limberger*
10. **HU-12:** Registrar pago parcial de factura - *Responsable: Axel Limberger*

---

## Tareas

### Tareas para HU-01: Alta de Cliente

**Análisis y Diseño:**
- [x] Analizar requisitos de la historia de usuario
- [x] Identificar validaciones necesarias (CUIT/DNI, email, campos obligatorios)
- [x] Definir modelo de datos con atributos y restricciones
- [x] Diseñar enumeraciones (TipoCondicionIVA, EstadoCuenta)

**Modelo (Entidades):**
- [x] Crear clase `CuentaCliente` con anotaciones JPA
- [x] Agregar validaciones Bean Validation (@NotBlank, @Email, @Pattern)
- [x] Documentar con JavaDoc todos los campos y métodos
- [x] Implementar método `@PrePersist` para valores por defecto (estado ACTIVA, saldo 0.00)
- [x] Crear enum `TipoCondicionIVA` con valores según AFIP
- [x] Crear enum `EstadoCuenta` (ACTIVA, SUSPENDIDA, BAJA)

**Repositorio:**
- [x] Crear interfaz `CuentaClienteRepositorie` extendiendo JpaRepository
- [x] Agregar método personalizado `existsByCuitDni(String cuitDni)`

**Servicio:**
- [x] Crear clase `CuentaClienteService` con anotación @Service
- [x] Implementar método `crearCliente(CuentaCliente cliente)` con validación de duplicados
- [x] Implementar método `obtenerTodosLosClientes()` con @Transactional(readOnly=true)

**Controlador:**
- [x] Crear clase `CuentaClienteController` con @RequestMapping("/clientes")
- [x] Implementar endpoint GET `/clientes` para listar clientes
- [x] Implementar endpoint GET `/clientes/nuevo` para mostrar formulario
- [x] Implementar endpoint POST `/clientes/nuevo` con @Valid y BindingResult

**Vista (Templates Thymeleaf):**
- [x] Crear `formulario.html` con campos del cliente


---

### Tareas para HU-21: Asignar Servicio a un Cliente

**Análisis y Diseño:**
- [x] Analizar requisitos de asignación de servicios
- [x] Planificar flujo de asignación y validación de duplicados

**Modelo (Entidades):**
- [x] Crear relación @OneToMany en `CuentaCliente` hacia `ServicioContratado`
- [x] Implementar método rico `contratarServicio(Servicio)` en `CuentaCliente`
  - Validar duplicados con `tieneServicioContratadoActivo()`
  - Registrar fecha actual con `LocalDate.now()`
  - Copiar precio actual del servicio
- [x] Implementar método `tieneServicioContratadoActivo(Servicio)` 
- [x] Implementar método `getServiciosContratadosActivos()`
- [x] Agregar método sobrecargado `calcularPrecioConIva(BigDecimal)` en `Servicio`

**Servicio:**
- [x] Implementar método `obtenerClientePorId(Long id)` en `CuentaClienteService`
- [x] Implementar método `asignarServicio(Long clienteId, Long servicioId)`

**Controlador:**
- [x] Implementar endpoint GET `/clientes/{id}` para ver detalle del cliente
- [x] Implementar endpoint GET `/clientes/{id}/servicios/asignar` 
- [x] Implementar endpoint POST `/clientes/{clienteId}/servicios/{servicioId}/asignar`
- [x] Implementar endpoint GET `/clientes/{id}/servicios/historico`

**Vista (Templates Thymeleaf):**
- [x] Crear `agregar-servicio.html`:
- [x] Crear `historico-servicios.html`:

