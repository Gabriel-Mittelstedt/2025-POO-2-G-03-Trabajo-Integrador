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
- Implementó la **HU-04: Emisión de factura individual**, aplicando el patrón RICO con lógica de negocio en las entidades `Factura` e `ItemFactura`. Desarrolló la generación automática de ítems desde servicios contratados, el sistema de numeración correlativa por serie, y la determinación automática del tipo de factura según reglas AFIP.
- Implementó la **HU-06: Consulta de factura individual**, creando las vistas de listado con filtros y detalle completo con cálculos de subtotales, IVA, descuentos y saldo pendiente.
- Integró el módulo de facturación con los servicios contratados del cliente desarrollados por Axel Dos Santos.

**Marcos Daubermann:**
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

### Wireframe: Emisión y Consulta de Factura Individual

**Vista: formulario-individual.html (HU-04)**

![Wireframe Formulario Factura Individual](imagenes/Wireframe_Formulario_Factura.png)

**Vista: lista.html (HU-06)**

![Wireframe Lista Facturas](imagenes/Wireframe_Lista_Facturas.png)

**Vista: detalle.html (HU-06)**

![Wireframe Detalle Factura](imagenes/Wireframe_Detalle_Factura.png)


### Wireframe: Pago de Factura total y Parcial


**Vista: formulario-total (HU-11)**

![Wireframe Pago Total](imagenes/Wireframe_Pago-Total.png) 

**Vista: formulario-parcial (HU-12)** 

![Wireframe Pago Parcial](imagenes/Wireframe_Pago-Parcial.png) 

**Caso de Uso: Emisión de Factura Individual (HU-04)**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | El cliente existe en el sistema con estado ACTIVA<br>El cliente tiene al menos un servicio contratado activo<br>El emisor tiene condición IVA configurada como Responsable Inscripto |
| **Flujo Principal** | 1. El administrador accede a "Nueva Factura Individual"<br>2. El sistema muestra el formulario con la lista de clientes activos<br>3. El administrador selecciona un cliente del desplegable<br>4. El administrador ingresa el período (texto libre, ej: "Noviembre 2025")<br>5. El administrador selecciona la fecha de emisión (por defecto fecha actual)<br>6. El administrador selecciona la fecha de vencimiento<br>7. El administrador opcionalmente ingresa porcentaje de descuento (0-100%) y motivo<br>8. El administrador hace clic en "Emitir Factura"<br>9. El sistema obtiene todos los servicios contratados activos del cliente<br>10. El sistema crea automáticamente un ítem por cada servicio con: descripción, precio contratado, cantidad=1, alícuota IVA del servicio<br>11. El sistema determina el tipo de factura aplicando reglas AFIP:<br>&nbsp;&nbsp;- RI (emisor) + RI (cliente) = Factura A<br>&nbsp;&nbsp;- RI (emisor) + CF (cliente) = Factura B<br>&nbsp;&nbsp;- CF (emisor) + CF (cliente) = Factura C<br>12. El sistema asigna serie según tipo (A→1, B→2, C→3)<br>13. El sistema obtiene el siguiente número correlativo para esa serie<br>14. Cada ítem calcula su subtotal, IVA y total<br>15. La factura calcula totales: subtotal, total IVA, descuento, total general<br>16. El sistema establece estado inicial PENDIENTE y saldo pendiente igual al total<br>17. El sistema persiste la factura con todos sus ítems<br>18. El sistema muestra mensaje de éxito con datos de la factura generada<br>19. El sistema redirige al detalle de la factura creada |
| **Flujos Alternativos** | **9a.** Si el cliente no tiene servicios contratados activos:<br>&nbsp;&nbsp;1. El sistema muestra error "El cliente no tiene servicios activos para facturar"<br>&nbsp;&nbsp;2. Vuelve al paso 2<br><br>**7a.** Si se ingresa descuento sin motivo:<br>&nbsp;&nbsp;1. El sistema muestra error "Debe indicar el motivo del descuento"<br>&nbsp;&nbsp;2. Vuelve al paso 7<br><br>**7b.** Si el descuento es inválido (menor a 0 o mayor a 100):<br>&nbsp;&nbsp;1. El sistema muestra error "El descuento debe estar entre 0% y 100%"<br>&nbsp;&nbsp;2. Vuelve al paso 7<br><br>**6a.** Si la fecha de vencimiento es anterior a la fecha de emisión:<br>&nbsp;&nbsp;1. El sistema muestra error "La fecha de vencimiento debe ser posterior a la fecha de emisión"<br>&nbsp;&nbsp;2. Vuelve al paso 6 |
| **Postcondición** | Se crea una factura individual con:<br>- Tipo, serie y número asignados automáticamente<br>- Ítems generados desde servicios contratados<br>- Estado PENDIENTE<br>- Totales calculados correctamente<br>- Saldo pendiente igual al total |

---

**Caso de Uso: Consulta de Factura Individual (HU-06)**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | Existen facturas registradas en el sistema |
| **Flujo Principal** | 1. El administrador accede a "Gestión de Facturas"<br>2. El sistema muestra el listado de todas las facturas con:<br>&nbsp;&nbsp;- Serie y número de factura<br>&nbsp;&nbsp;- Tipo (badge con color)<br>&nbsp;&nbsp;- Cliente (nombre)<br>&nbsp;&nbsp;- Período (badge)<br>&nbsp;&nbsp;- Fecha de emisión<br>&nbsp;&nbsp;- Fecha de vencimiento<br>&nbsp;&nbsp;- Total formateado<br>&nbsp;&nbsp;- Estado (badge con colores: verde=Pagada, amarillo=Pendiente, rojo=Vencida, azul=Parcialmente Pagada, gris=Anulada)<br>3. El administrador opcionalmente aplica filtros:<br>&nbsp;&nbsp;- Por estado (desplegable)<br>&nbsp;&nbsp;- Por tipo de factura (desplegable)<br>&nbsp;&nbsp;- Por período (texto libre)<br>4. El administrador hace clic en "Ver Detalle" de una factura<br>5. El sistema muestra la vista completa con:<br>&nbsp;&nbsp;**Cabecera:** Tipo, serie y número de factura<br>&nbsp;&nbsp;**Datos del cliente:** nombre, CUIT/DNI, condición IVA, domicilio<br>&nbsp;&nbsp;**Datos de factura:** período, fechas, estado<br>&nbsp;&nbsp;**Tabla de ítems:** descripción, cantidad, precio unitario, subtotal, alícuota IVA, monto IVA, total por ítem<br>&nbsp;&nbsp;**Totales:** subtotal general, total IVA, descuento aplicado, total general, saldo pendiente<br>6. El sistema muestra botones de acción según el estado:<br>&nbsp;&nbsp;- Si estado=PENDIENTE y saldo>0: botones "Registrar Pago" y "Anular Factura"<br>&nbsp;&nbsp;- Si estado=PAGADA_PARCIALMENTE: botón "Registrar Pago"<br>&nbsp;&nbsp;- Si estado=PAGADA_TOTALMENTE o ANULADA: sin acciones<br>7. El administrador puede volver al listado |
| **Flujos Alternativos** | **2a.** Si no existen facturas en el sistema:<br>&nbsp;&nbsp;1. El sistema muestra mensaje "No hay facturas registradas"<br>&nbsp;&nbsp;2. Muestra botón para crear la primera factura<br><br>**3a.** Si los filtros no devuelven resultados:<br>&nbsp;&nbsp;1. El sistema muestra mensaje "No se encontraron facturas con los criterios especificados"<br>&nbsp;&nbsp;2. Muestra botón para limpiar filtros |
| **Postcondición** | El administrador visualiza la información completa de la factura sin modificar datos |

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

---

### Tareas para HU-04: Emisión de Factura Individual

**Análisis y Diseño:**
- [x] Analizar requisitos de emisión de factura individual
- [x] Definir modelo de dominio rico (patrón RICO)
- [x] Diseñar integración con `ServicioContratado` para generación automática de ítems
- [x] Establecer reglas AFIP para determinación de tipo de factura

**Modelo (Entidades):**
- [x] Crear entidad `Factura` con lógica de negocio:
  - Métodos: `agregarItem()`, `calcularTotales()`, `determinarTipoFactura()`, `aplicarDescuento()`, `validarClienteActivo()`
- [x] Crear entidad `ItemFactura` con auto-cálculo:
  - Métodos: `calcular()`, `obtenerValorAlicuota()`
- [x] Crear enum `EstadoFactura` con valores: PENDIENTE, PAGADA_PARCIALMENTE, PAGADA_TOTALMENTE, VENCIDA, ANULADA

**Repositorio:**
- [x] Crear `FacturaRepository` con queries personalizadas:
  - `findByClienteId()`, `findByEstado()`, `findByPeriodo()`, `findUltimoNumeroFactura()`

**Servicio:**
- [x] Crear `FacturaService` con método `emitirFacturaDesdeServiciosContratados()`
- [x] Implementar asignación automática de serie y numeración correlativa

**Controlador:**
- [x] Crear `FacturaViewController` con endpoint POST `/facturas/nueva-individual`

**Vista (Templates Thymeleaf):**
- [x] Crear `formulario-individual.html` con campos: cliente, periodo, fechas, descuento

---

### Tareas para HU-06: Consulta de Factura Individual

**Controlador:**
- [x] Implementar endpoint GET `/facturas` para listado
- [x] Implementar endpoint GET `/facturas/{id}` para detalle
- [x] Implementar endpoints GET `/facturas/cliente/{clienteId}` y `/facturas/periodo/{periodo}`

**Vista (Templates Thymeleaf):**
- [x] Crear `lista.html` con tabla de facturas (serie, número, tipo, cliente, periodo, total, estado)
- [x] Crear `detalle.html` con:
  - Datos del cliente y factura
  - Tabla de ítems con cálculos
  - Resumen de totales (subtotal, IVA, descuento, total, saldo pendiente)


### Tareas para HU-11 y HU-12: Registrar pagos (Total y Parcial)

**Análisis y Diseño:**
- [x] Analizar requisitos de las historias HU-11 y HU-12
- [x] Definir flujo simple de pago (total y parcial) y efectos sobre la factura

**Modelo (Entidades) y Repositorios:**
- [x] Crear entidad `Pago` y `Recibo` (persistencia JPA)
- [x] Crear `PagoRepository` y `ReciboRepository`

**Servicio:**
- [x] Crear `PagoService` con métodos `registrarPagoTotal` y `registrarPagoParcial`
- [x] Generar `Recibo` automáticamente al registrar pagos

**Controlador y Vistas:**
- [x] `PagoController` con endpoints para mostrar formularios y procesar pagos
- [x] Templates Thymeleaf: `pagos/formulario-total.html`, `pagos/formulario-parcial.html`, `pagos/detalle.html`, `pagos/lista.html`

**Resultados / Observaciones:**
- Implementación básica y funcional: registra pagos, actualiza saldo, cambia estado de la factura y genera recibos.
- No se añadieron validaciones avanzadas (ej.: límites, centros de costos, conciliaciones bancarias).