# Historias de Usuario

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 01 |
| **Título**                 | Alta de cliente |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero dar de alta un cliente creandole una cuenta, para poder gestionar su facturación y pagos. |
| **Criterios de aceptación** | <ul><li>Debo poder cargar: nombre, razón social, CUIT/DNI, domicilio, email, teléfono, condición de IVA y estado de cuenta (activa/suspendida/baja).</li><li>El sistema valida formato de CUIT/DNI y que condición de IVA sea válida.</li><li>El estado inicial por defecto es **activa**.</li><li>Si el CUIT ya existe, se rechaza el alta con mensaje claro.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 02 |
| **Título**                 | Modificación de cliente |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero modificar los datos de la cuenta de un cliente, para poder mantener su información actualizada. |
| **Criterios de aceptación** | <ul><li>Puedo modificar cualquier campo, incluyendo condición de IVA (registrando fecha de cambio).</li><li>Si cambia la condición de IVA, el sistema guarda el historial.</li><li>Validaciones iguales al alta.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 03 |
| **Título**                 | Gestión de estado de cuenta |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero cambiar el estado de la cuenta de un cliente (activa, suspendida, dada de baja), para poder reflejar su situación actual. |
| **Criterios de aceptación** | <ul><li>Solo se puede **facturar** clientes en estado **activa**.</li><li>Al pasar a “baja”, no se permiten nuevas facturas pero se mantiene el historial y deudas.</li><li>Al pasar a “suspendida”, se bloquea la facturación individual hasta reactivación.</li><li>El sistema registra usuario, fecha y motivo del cambio.</li></ul> |


| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 04 |
| **Título**                 | Emisión de factura individual |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero poder emitir una factura individual para un cliente activo, según los servicios contratados y su condición fiscal. |
| **Criterios de aceptación** | <ul><li>Solo se pueden emitir facturas para **cuentas activas**.</li><li>El sistema determina automáticamente el tipo de factura (A, B o C) según la condición fiscal del emisor y del cliente.</li><li>La factura incluye servicios, IVA, descuentos y totales.</li><li>Se registran fecha de emisión, vencimiento y período.</li><li>Permite aplicar **descuentos manuales** (con motivo).</li><li>El formato de la factura respeta la estructura AFIP.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 05 |
| **Título**                 | Anulación de factura individual |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero anular una factura individual generando una nota de crédito total, para corregir errores sin eliminar registros. |
| **Criterios de aceptación** | <ul><li>Solo se pueden anular facturas **no pagadas** o con **saldo completo**.</li><li>Se debe ingresar un **motivo de anulación**.</li><li>El sistema genera automáticamente una **nota de crédito total** vinculada a la factura original.</li><li>La factura cambia su estado a “**Anulada**”.</li><li>Se conserva trazabilidad entre la factura y la nota de crédito.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 06 |
| **Título**                 | Consulta de factura individual |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero ver el detalle completo de una factura para conocer sus detalles, impuestos y movimientos de pago. |
| **Criterios de aceptación** | <ul><li>Se visualizan: encabezado (tipo A/B/C, número, cliente, período, emisión, vencimiento), ítems/servicios, alícuotas de IVA, descuentos, subtotal, impuestos y **total**.</li><li>**Historial de pagos** (totales y parciales), recibos y **saldo pendiente**.</li><li>**Enlaces a nota(s) de crédito/débito** asociadas.</li></ul> |


| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 07 |
| **Título**                 | Emisión de facturación masiva por período |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero ejecutar una facturación masiva por período para generar automáticamente las facturas mensuales de todos los clientes activos. |
| **Criterios de aceptación** | <ul><li>La acción debe iniciarse manualmente (botón “Facturar mes”).</li><li>Solo incluye clientes con cuentas activas.</li><li>El sistema genera automáticamente el tipo de factura correcto (A, B o C) según la condición fiscal de cada cliente.</li><li>Se registran la fecha de ejecución, período, vencimiento y cantidad de facturas generadas.</li><li>El proceso ignora clientes suspendidos o dados de baja.</li><li>Antes de realizar la operación el sistema deberá solicitar una confirmación por parte del usuario.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 08 |
| **Título**                 | Anulación de facturación masiva |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero anular un proceso de facturación masiva completo en caso de error, generando las notas de crédito correspondientes a cada factura emitida. |
| **Criterios de aceptación** | <ul><li>Solo se pueden anular lotes donde las facturas no hayan sido pagadas.</li><li>Se debe ingresar un motivo de anulación general.</li><li>El sistema genera automáticamente notas de crédito vinculadas a cada factura afectada.</li><li>Se conserva trazabilidad entre la factura y la nota de crédito.</li><li>Antes de realizar la operación el sistema deberá solicitar una confirmación por parte del usuario.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 09 |
| **Título**                 | Consulta de facturación masiva |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero consultar un lote de facturación masiva para verificar el período, la cantidad de facturas generadas y acceder a cada una. |
| **Criterios de aceptación** | <ul><li>Se visualizan: período, fecha/hora de ejecución, **fecha de vencimiento aplicada**, cantidad de facturas generadas, **monto total** del lote y estado (exitoso/anulado/parcial).</li><li>Listado de facturas del lote con acceso a su detalle.</li><li>Si el proceso fue anulado, ver **referencias a notas de crédito emitidas** en bloque.</li></ul> |


| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 10 |
| **Título**                 | Listado y búsqueda de facturas |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero listar y buscar facturas para localizar rápidamente documentos por cliente, estado, tipo o período. |
| **Criterios de aceptación** | <ul><li>Filtros por cliente, período, estado (pagada/impaga/vencida/anulada), tipo (A/B/C) y lote.</li><li>Orden por fecha de emisión o vencimiento.</li><li>Acciones: **ver detalle** de factura, **registrar pago** (si aplica), **anular factura (si no fue pagada parcialmente)**.</li></ul> |


| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 11 |
| **Título**                 | Registrar pago total de factura |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero registrar un pago total de una factura para actualizar su estado a pagada y emitir un recibo. |
| **Criterios de aceptación** | <ul><li>Solo se pueden pagar facturas activas y no anuladas.</li><li>El sistema permite elegir el **método de pago** (efectivo, transferencia, tarjeta).</li><li>Al confirmar, la factura cambia su estado a “**Pagada**”.</li><li>Se genera un **recibo** con: número, fecha, monto, método, referencia y facturas asociadas.</li><li>Se registra el movimiento en el **estado de cuenta** del cliente.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 12 |
| **Título**                 | Registrar pago parcial de factura |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero registrar pagos parciales en facturas impagas para actualizar el saldo pendiente y mantener un historial de movimientos. |
| **Criterios de aceptación** | <ul><li>Puedo ingresar un **importe menor al total de la factura**.</li><li>El sistema calcula y muestra el **saldo restante**.</li><li>Cada pago parcial genera un **recibo independiente**.</li><li>El estado de la factura cambia a “**Parcialmente pagada**”.</li><li>Se pueden registrar varios pagos parciales hasta cubrir el total.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 13 |
| **Título**                 | Gestión de saldo a favor |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero registrar y aplicar saldo a favor de un cliente para compensar pagos adelantados o montos excedentes. |
| **Criterios de aceptación** | <ul><li>El sistema calcula automáticamente el saldo a favor cuando un pago excede el total de facturas.</li><li>Puedo **aplicar saldo disponible** a nuevas facturas o mantenerlo guardado.</li><li>Cada aplicación se refleja en el historial de pagos y el estado de cuenta.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 14 |
| **Título**                 | Registrar pago combinado (múltiples facturas) |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero aplicar un único pago a múltiples facturas impagas de un cliente, para optimizar la cobranza. |
| **Criterios de aceptación** | <ul><li>Puedo seleccionar varias facturas impagas del mismo cliente.</li><li>Indico el **monto total recibido** y el sistema distribuye automáticamente (o manualmente) el pago entre ellas.</li><li>Cada factura afectada actualiza su saldo.</li><li>Se genera un solo recibo con detalle de las facturas cubiertas.</li><li>Si sobra dinero, **se crea saldo a favor**.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 15 |
| **Título**                 | Consulta de pagos y recibos |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero consultar todos los pagos registrados (totales, parciales o combinados) para verificar los movimientos financieros del sistema. |
| **Criterios de aceptación** | <ul><li>Filtros: cliente, fecha, factura, método de pago, tipo (total/parcial).</li><li>Cada registro muestra: número de recibo, cliente, fecha, monto, método y facturas asociadas.</li><li>Permite abrir el recibo para ver el detalle del movimiento.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 16 |
| **Título**                 | Alta de servicio |
| **Persona**                | Administrador |
| **Descripción**            | Como **administrador** quiero poder crear nuevos servicios que ofrece la empresa, definiendo sus características principales (nombre, precio, IVA), para que luego puedan ser asignados a los clientes y facturados. |
| **Criterios de aceptación** | <ul><li>El formulario debe solicitar un nombre o descripción (Ej. "Abono Internet 50mb", "Servicio Cable Premium").</li><li>El formulario debe solicitar un precio para el servicio.</li><li>El formulario debe permitir seleccionar la alícuota de IVA correspondiente a ese servicio (Ej. 21%, 10.5%, 27%, 0%, Exento).</li><li>Al guardar, el servicio nuevo debe aparecer en el listado de servicios.</li><li>El sistema no debe permitir guardar un servicio sin nombre, precio o alícuota de IVA.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 17 |
| **Título**                 | Listado de servicio |
| **Persona**                | Administrador |
| **Descripción**            | Quiero poder ver un listado de todos los servicios que la empresa tiene configurados, para tener una vista general de la oferta comercial y poder acceder a modificarlos o darlos de baja (lógica). |
| **Criterios de aceptación** | <ul><li>Debe existir una pantalla que muestre todos los servicios activos.</li><li>El listado debe mostrar, como mínimo, el Nombre del servicio, su Precio y la Alícuota de IVA.</li><li>La lista debe tener una opción visible para "Editar" cada servicio.</li><li>La lista debe tener una opción visible para "Eliminar" o "desactivar" un servicio.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 18 |
| **Título**                 | Modificación de servicio |
| **Persona**                | Administrador |
| **Descripción**            | Quiero tener la libertad de modificar los datos de un servicio existente, especialmente su precio, para mantener actualizada la lista de precios de la empresa. |
| **Criterios de aceptación** | <ul><li>Desde el listado de servicios (HU - 17), al seleccionar "Editar", el sistema debe mostrar un formulario con los datos actuales del servicio.</li><li>Debo poder modificar el Precio del servicio.</li><li>Debo poder modificar el Nombre y la Alícuota de IVA del servicio.</li><li>Al guardar los cambios, la información actualizada debe reflejarse en el listado de servicios.</li><li>El cambio de precio no debe afectar a las facturas que ya fueron emitidas con el precio anterior.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 19 |
| **Título**                 | Baja de servicio |
| **Persona**                | Administrador |
| **Descripción**            | Como Administrador quiero poder dar de baja (desactivar) un servicio que la empresa ya no ofrece, para evitar que sea asignado por error a nuevos clientes. |
| **Criterios de aceptación** | <ul><li>El sistema debe solicitar una confirmación antes de realizar la baja.</li></ul> |
| **Nota técnica**           | En lugar de "Eliminar", el sistema debe "Desactivar" el servicio. Un servicio desactivado ya no aparece en la lista para ser asignado a clientes nuevos, pero se mantiene en los clientes que ya lo tenían contratado. |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 20 |
| **Título**                 | Facturación proporcional por alta parcial |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador, quiero que el sistema calcule y facture solo el monto proporcional al tiempo efectivamente contratado dentro del mes, para evitar cobrar un mes completo cuando el cliente se da de alta a mitad del período. |
| **Criterios de aceptación** | <ul><li>Si el cliente contrata un servicio luego del primer día del mes, el sistema calcula el valor proporcional en base a los días restantes del período.</li><li>El cálculo se aplica tanto en facturación individual como masiva.</li><li>El detalle de la factura debe mostrar el período parcial (ej. "15 al 31 de octubre").</li><li>Si el servicio se da de baja antes de fin de mes, el sistema también calcula el proporcional correspondiente.</li></ul> |

| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 21 |
| **Título**                 | Asignar servicio a un cliente |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador quiero poder asociar uno o varios servicios (previamente creados) a la cuenta de un cliente, para que el sistema sepa qué debe facturarle en cada período. |
| **Criterios de aceptación** | <ul><li>Desde el perfil de un cliente, debo ver una opción para "Asignar Servicio".</li><li>Debo poder seleccionar un servicio de la lista de servicios activos (creados en HU anteriores).</li><li>El sistema debe permitir que un mismo cliente tenga múltiples servicios asignados.</li><li>El perfil del cliente debe mostrar una lista de los servicios que tiene contratados actualmente.</li></ul> |


| Campo                      | Descripción |
|:---------------------------|:------------|
| **ID**                     | HU - 22 |
| **Título**                 | Desvincular servicio a un cliente |
| **Persona**                | Administrador |
| **Descripción**            | Como administrador quiero poder quitar un servicio de la cuenta de un cliente, para que no se le facture más por ese concepto en futuros períodos. |
| **Criterios de aceptación** | <ul><li>Al quitarlo, ese servicio no debe incluirse en la próxima facturación masiva para ese cliente.</li><li>El sistema debe pedir confirmación antes de desvincular el servicio.</li></ul> |