# Diseño y Planificación - Iteración 2
**Gabriel Mittelstedt**

## Trabajo en equipo

### Implementacion realizada por Axel Agustin Limberger

**Limberger Axel Agustin:**

- **HU-13 — Gestión de saldo a favor:** Lógica en la entidad `CuentaCliente` (métodos ricos: `getSaldoAFavor()`, `aplicarSaldoAFavor()`, `registrarSaldoAFavor()`), servicio en `PagoService` para aplicar saldo en transacciones y vistas para visualizar y aplicar el saldo disponible.

- **HU-14 — Registrar pago combinado (múltiples facturas):** `PagoService.registrarPagoCombinado()` que distribuye montos entre facturas, crea `Pago` y `DetallePago`, y genera un recibo único; vista `seleccionar-facturas.html` para seleccionar facturas (checkboxes), usar saldo a favor y registrar el pago en una sola transacción atómica.

- **HU-15 — Consulta de pagos y recibos:** DTOs `ReciboDTO` y `ReciboDTO.DetallePagoDTO` para generar recibos desde entidades; `ReciboService` con métodos (`generarReciboDesdePago()`, `generarReciboDesdeMultiplesPagos()`, `generarReciboConsolidado()`); `PagoService.listarFiltrados()` para buscar por nombre y rango de fechas; vistas `pagos/lista.html` (formulario de filtros y agrupación por recibo) y `pagos/recibo-detalle.html` (desglose por método de pago).


### Implementación realizada por Gabriel Mittelstedt

#### HU-05: Anulación de factura individual
- Modelo rico: métodos `puedeSerAnulada()`, `anular()` y `agregarNotaCredito()` en `Factura`
- Entidad `NotaCredito` y repositorio con Query Methods
- Servicio `anularFactura()` con validaciones y numeración automática por serie
- Vistas: `confirmar-anulacion.html`, botón en `detalle.html`, endpoints GET/POST

#### HU-20: Facturación proporcional (individual)
- Value Object `PeriodoFacturacion` con cálculo de días efectivos y descripción
- Factory Method `ItemFactura.crearProporcional()`: fórmula `(días_efectivos / días_mes) × precio_mensual`
- Servicio `emitirFacturaProporcional()` con orquestación completa
- Vista `formulario-proporcional.html` con validaciones JavaScript
- Endpoints GET/POST y botón en lista de facturas

#### Correcciones y mejoras
- Validación de fechas: frontend (JS) + backend (`validarFechas()`)
- Actualización automática a VENCIDA: método `actualizarSiEstaVencida()` llamado al listar
- Query Methods: migración de `@Query` a métodos como `findFirstBySerieOrderByNroFacturaDesc()`
- Inclusión de VENCIDA en listado de facturas impagas para pagos

#### Documentación
- Diagrama de clases con patrones explicitados (Rich Model, Factory Method, Value Object)
- JavaDocs en `Factura`, `ItemFactura`, `NotaCredito`
 
## Diseño OO


## Wireframe y Casos de Uso

### Wireframe: Anulación de Factura Individual (HU-05)

**Vista: confirmar-anulacion.html**

![Wireframe Confirmación Anulación](imagenes/Wireframe_Confirmar_Anulacion.png)

**Vista: detalle.html (con sección de notas de crédito)**

![Wireframe Detalle Factura con Notas de Crédito](imagenes/Wireframe_Detalle_Factura_NC.png)

**Caso de Uso: Anulación de Factura Individual (HU-05)**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | La factura existe en el sistema con estado PENDIENTE o VENCIDA<br>La factura no tiene pagos registrados |
| **Flujo Principal** | 1. El administrador accede al detalle de la factura<br>2. El sistema muestra el botón "Anular Factura" (solo si estado=PENDIENTE o VENCIDA)<br>3. El administrador hace clic en "Anular Factura"<br>4. El sistema muestra la vista de confirmación con:<br>&nbsp;&nbsp;- Datos completos de la factura a anular<br>&nbsp;&nbsp;- Alerta de advertencia (operación irreversible)<br>&nbsp;&nbsp;- Campo obligatorio: motivo de anulación (textarea, máx. 500 caracteres)<br>&nbsp;&nbsp;- Detalle de lo que ocurrirá: generación de nota de crédito, cambio de estado<br>5. El administrador ingresa el motivo de anulación<br>6. El administrador hace clic en "Confirmar Anulación"<br>7. El sistema valida que el motivo no esté vacío<br>8. El sistema ejecuta `Factura.puedeSerAnulada()` para verificar reglas de negocio<br>9. El sistema crea una `NotaCredito` con:<br>&nbsp;&nbsp;- Serie igual a la factura<br>&nbsp;&nbsp;- Número correlativo (obtiene último + 1 de esa serie)<br>&nbsp;&nbsp;- Tipo igual a la factura<br>&nbsp;&nbsp;- Monto igual al total de la factura<br>&nbsp;&nbsp;- Motivo ingresado por el usuario<br>&nbsp;&nbsp;- Fecha emisión actual<br>10. El sistema ejecuta `Factura.anular()` que cambia estado a ANULADA<br>11. El sistema persiste la nota de crédito y actualiza la factura<br>12. El sistema muestra mensaje flash de éxito "Factura anulada exitosamente"<br>13. El sistema redirige al detalle de la factura donde se visualiza:<br>&nbsp;&nbsp;- Estado ANULADA (badge gris)<br>&nbsp;&nbsp;- Tabla de notas de crédito asociadas |
| **Flujos Alternativos** | **7a.** Si el motivo está vacío:<br>&nbsp;&nbsp;1. El sistema muestra error de validación HTML5 "Por favor, rellene este campo"<br>&nbsp;&nbsp;2. Vuelve al paso 5<br><br>**8a.** Si la factura no puede ser anulada (tiene pagos, estado inválido):<br>&nbsp;&nbsp;1. El sistema lanza `IllegalStateException` con mensaje descriptivo<br>&nbsp;&nbsp;2. El sistema muestra mensaje de error flash<br>&nbsp;&nbsp;3. Redirige al detalle de la factura<br><br>**9a.** Si falla la generación del número de nota de crédito:<br>&nbsp;&nbsp;1. El sistema rollback de la transacción<br>&nbsp;&nbsp;2. Muestra error técnico<br>&nbsp;&nbsp;3. Vuelve al paso 4 |
| **Postcondición** | Se genera una nota de crédito con numeración automática<br>La factura cambia su estado a ANULADA<br>Se mantiene la trazabilidad (relación bidireccional Factura-NotaCredito)<br>La operación queda registrada con motivo |

---

### Wireframe: Facturación Proporcional Individual (HU-20)

**Vista: formulario-proporcional.html**

![Wireframe Formulario Proporcional](imagenes/Wireframe_Formulario_Proporcional.png)

**Vista: lista.html (con botón "Factura Proporcional")**

![Wireframe Lista Facturas con Botón Proporcional](imagenes/Wireframe_Lista_Facturas_Proporcional.png)

**Caso de Uso: Facturación Proporcional por Fecha de Alta (HU-20)**

| Elemento | Descripción |
|----------|-------------|
| **Actor** | Administrador |
| **Precondición** | El cliente existe en el sistema con estado ACTIVA<br>El cliente tiene servicios contratados activos<br>El emisor tiene condición IVA Responsable Inscripto |
| **Flujo Principal** | 1. El administrador accede a "Gestión de Facturas"<br>2. El administrador hace clic en "Factura Proporcional"<br>3. El sistema muestra el formulario con campos:<br>&nbsp;&nbsp;- Cliente (select con clientes activos)<br>&nbsp;&nbsp;- Fecha inicio del período (date input)<br>&nbsp;&nbsp;- Fecha fin del período (date input)<br>&nbsp;&nbsp;- Fecha de emisión (date input, por defecto fecha actual)<br>&nbsp;&nbsp;- Fecha de vencimiento (date input)<br>&nbsp;&nbsp;- Porcentaje de descuento (number, opcional, 0-100%)<br>&nbsp;&nbsp;- Motivo del descuento (text, obligatorio si hay descuento)<br>4. El administrador selecciona el cliente<br>5. El administrador ingresa las fechas del período (inicio y fin)<br>6. El administrador ingresa fecha de emisión y vencimiento<br>7. El administrador opcionalmente ingresa descuento y motivo<br>8. El sistema valida en tiempo real (JavaScript):<br>&nbsp;&nbsp;- Fin período > inicio período<br>&nbsp;&nbsp;- Vencimiento > emisión<br>&nbsp;&nbsp;- Si descuento > 0, motivo obligatorio<br>9. El administrador hace clic en "Generar Factura Proporcional"<br>10. El sistema ejecuta validaciones backend (`Factura.validarFechas()`)<br>11. El sistema crea un `PeriodoFacturacion` (Value Object) que:<br>&nbsp;&nbsp;- Calcula días efectivos del período<br>&nbsp;&nbsp;- Calcula días totales del mes<br>&nbsp;&nbsp;- Genera descripción en español: "del DD al DD de MMMM YYYY (X días)"<br>12. El sistema obtiene servicios contratados activos del cliente<br>13. Para cada servicio, el sistema invoca `ItemFactura.crearProporcional()`:<br>&nbsp;&nbsp;- Calcula precio proporcional: (días_efectivos / días_mes) × precio_mensual<br>&nbsp;&nbsp;- Genera descripción automática con el período<br>&nbsp;&nbsp;- Copia alícuota IVA del servicio<br>&nbsp;&nbsp;- Establece cantidad = 1<br>14. El sistema determina tipo de factura según reglas AFIP<br>15. El sistema asigna serie y obtiene número correlativo<br>16. El sistema calcula totales (subtotal, IVA, descuento, total)<br>17. El sistema persiste la factura con estado PENDIENTE<br>18. El sistema muestra mensaje de éxito con datos de la factura<br>19. El sistema redirige al detalle de la factura creada |
| **Flujos Alternativos** | **8a.** Si fin período ≤ inicio período:<br>&nbsp;&nbsp;1. El sistema muestra error de validación HTML5 "La fecha fin debe ser posterior a la fecha inicio"<br>&nbsp;&nbsp;2. Marca el campo con borde rojo<br>&nbsp;&nbsp;3. Vuelve al paso 5<br><br>**8b.** Si vencimiento ≤ emisión:<br>&nbsp;&nbsp;1. El sistema muestra error "La fecha de vencimiento debe ser posterior a la emisión"<br>&nbsp;&nbsp;2. Vuelve al paso 6<br><br>**8c.** Si hay descuento sin motivo:<br>&nbsp;&nbsp;1. El sistema muestra error "Debe especificar el motivo del descuento"<br>&nbsp;&nbsp;2. Vuelve al paso 7<br><br>**12a.** Si el cliente no tiene servicios contratados activos:<br>&nbsp;&nbsp;1. El sistema muestra error "El cliente no tiene servicios activos para facturar"<br>&nbsp;&nbsp;2. Redirige al formulario manteniendo datos ingresados<br>&nbsp;&nbsp;3. Vuelve al paso 4<br><br>**11a.** Si el período abarca múltiples meses:<br>&nbsp;&nbsp;1. El sistema lanza `IllegalArgumentException`<br>&nbsp;&nbsp;2. Muestra error "El período debe estar dentro del mismo mes"<br>&nbsp;&nbsp;3. Vuelve al paso 5 |
| **Postcondición** | Se crea una factura con ítems proporcionales calculados automáticamente<br>Cada ítem tiene descripción detallada del período y días facturados<br>Los precios reflejan el cálculo proporcional exacto<br>La factura queda con estado PENDIENTE y saldo igual al total |


### Wireframe: Gestión de Saldo a Favor (HU-13)

**Vista: Detalle del Cliente (mostrando saldo a favor)**

![Wireframe Saldo a favor](imagenes/Wireframe_Saldo_Favor_Cliente.png)

La visualización del saldo a favor se integra en la vista de gestion del cliente donde se muestra en la tabla el monto disponible cuando el cliente tiene crédito a favor.

**Vista: Selección de Facturas para Aplicar Saldo**

![Wireframe Saldo a favor](imagenes/Wireframe_Saldo_Favor.png)

En la vista de pago combinado (`pagos/seleccionar-facturas.html`), se incluye un campo para ingresar el monto de saldo a favor a aplicar, con validaciones que previenen el uso de más saldo del disponible.

**Vista: Recibo con Saldo a Favor Aplicado**

![Wireframe Saldo a favor](imagenes/Wireframe_Saldo_Favor_Detalle.png)

El recibo (`pagos/recibo-detalle.html`) muestra claramente cuándo se utilizó saldo a favor como método de pago.

---

### Wireframe: Registrar Pago Combinado - Múltiples Facturas (HU-14)

**Vista: Selección de Facturas y Registro de Pago**

![Pago combinado](imagenes/Wireframe_PagoCombinado.png)

La funcionalidad de pago combinado se implementa en la vista `pagos/seleccionar-facturas.html`, que permite al administrador:
- Ver todas las facturas impagas del cliente en una tabla interactiva
- Seleccionar múltiples facturas mediante checkboxes
- Opcionalmente aplicar saldo a favor disponible
- Ingresar un monto adicional con método de pago (efectivo, transferencia, tarjeta)




**Vista: Detalle del Recibo Consolidado**

![DetalleRecibo](imagenes/Wireframe_Detalle_Recibo.png)

El recibo en `pagos/recibo-detalle.html` muestra claramente:
- Todas las facturas afectadas por el pago combinado
- Los métodos de pago utilizados (puede combinar saldo a favor + otro método)
- El detalle de distribución del monto entre las facturas

---

### Wireframe: Consulta de Pagos y Recibos (HU-15)

**Vista: Listado de Pagos y Recibos**

![Recibos](imagenes/Wireframe_Recibos.png)

La vista `pagos/lista.html` muestra un historial completo de todos los pagos registrados, presentados como recibos consolidados.

Características principales:
- **Filtros de búsqueda:** Permite filtrar por nombre de cliente y rango de fechas (desde/hasta)
- **Tabla responsive:** Muestra número de recibo, fecha, cliente, monto, método de pago y referencia
- **Agrupación inteligente:** Pagos combinados aparecen como un solo recibo con método "Combinado"
- **Botón "Ver":** Accede al detalle completo del recibo

**Vista: Detalle del Recibo**

![DetalleRecibo](imagenes/Wireframe_Detalle_Recibo.png)

La vista `pagos/recibo-detalle.html` muestra la información completa de un recibo, incluyendo:
- **Encabezado:** Número de recibo, fecha, cliente (nombre y CUIT/DNI)
- **Información del pago:** Monto total, método(s) de pago, referencia
- **Desglose de pagos:** Tabla detallada mostrando:
  - Enlace a cada factura afectada (cuando aplica)
  - Monto aplicado a cada factura
- **Navegación:** Botones para volver al listado o ir a facturas


---

## Backlog de Iteración 2
* **HU-02:** Modificación de cliente (Responsable: Axel Dos Santos)
* **HU-03:** Gestión de estado de cuenta (Responsable: Axel Dos Santos)
* **HU-05:** Anulación de factura individual (Responsable: Gabriel Mittelstedt)
* **HU-08:** Anulación de facturación masiva (Responsable: Marcos Doubermann)
* **HU-09:** Consulta de facturación masiva (Responsable: Marcos Doubermann)
* **HU-13:** Gestión de saldo a favor (Responsable: Axel Limberger)
* **HU-14:** Registrar pago combinado (múltiples facturas) (Responsable: Axel Limberger)
* **HU-15:** Consulta de pagos y recibos (Responsable: Axel Limberger)
* **HU-18:** Modificación de servicio (Responsable: Leandro Escalada)
* **HU-19:** Baja de servicio (Responsable: Leandro Escalada)
* **HU-20:** Facturación proporcional por fecha de alta (Responsable: Gabriel Mittelstedt)
* **HU-22:** Desvincular servicio de un cliente (Responsable: Axel Dos Santos)

## Tareas
