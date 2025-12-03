# Retrospectiva - Iteración 2

## Integrante: Limberger Axel Agustin

Esta retrospectiva documenta mi experiencia personal implementando las historias de usuario **HU-13: Gestión de saldo a favor**, **HU-14: Registrar pago combinado (múltiples facturas)** y **HU-15: Consulta de pagos y recibos** durante la segunda iteración del proyecto de facturación de servicios.

---

### Historia HU-13: Gestión de Saldo a Favor

**Logros principales:**
- ✅ Implementé la lógica para gestionar el saldo a favor de los clientes cuando realizan pagos que exceden el monto de las facturas
- ✅ Desarrollé la funcionalidad para aplicar automáticamente el saldo a favor en nuevas facturas
- ✅ Integré la gestión de saldo con el módulo de pagos existente de la iteración 1

### Historia HU-14: Registrar Pago Combinado (Múltiples Facturas)

**Logros principales:**
- ✅ Implementé la funcionalidad para seleccionar y pagar múltiples facturas en una sola transacción
- ✅ Desarrollé la lógica de distribución de pagos entre varias facturas pendientes
- ✅ Creé las vistas Thymeleaf necesarias para la selección y confirmación de facturas a pagar
- ✅ Integré la generación de recibos que documentan el pago combinado

### Historia HU-15: Consulta de Pagos y Recibos

**Logros principales:**
- ✅ Implementé el sistema de generación de recibos a partir de los datos de pagos existentes
- ✅ Desarrollé las vistas para consultar el historial de pagos y recibos de cada cliente
- ✅ Creé la vista de detalle de recibo con toda la información del pago realizado
- ✅ Implementé toda la lógica de gestión de pagos utilizando el patrón de modelo rico

---

## Desafíos y dificultades encontradas

### Desafío 1: Mala selección de HU en la Iteración 1

**Problema:**
En la iteración 1 solo seleccioné las historias de pago parcial y pago total (HU-11 y HU-12), sin considerar que las funcionalidades de saldo a favor, pago combinado y consulta de recibos estaban estrechamente relacionadas. Esto generó que en la iteración 2 tuviera que realizar muchas modificaciones y refactorizaciones al código existente.

**Impacto:**
- Tuve que reescribir partes significativas del módulo de pagos
- La integración con las nuevas funcionalidades requirió más tiempo del esperado
- Algunos cambios afectaron código que ya estaba funcionando correctamente

**Aprendizaje:**
Al planificar las iteraciones es fundamental analizar las dependencias entre historias de usuario. Las funcionalidades relacionadas deberían agruparse en la misma iteración para evitar refactorizaciones costosas posteriormente.

### Desafío 2: Gestión del tiempo con otras materias

**Problema:**
La carga académica de otras materias consumió gran parte del tiempo disponible para el proyecto, lo que impidió implementar todas las funcionalidades al 100% como hubiera querido.

**Impacto:**
- No pude dedicar el tiempo suficiente para pulir algunos detalles de la implementación
- Algunas pruebas quedaron pendientes
- La documentación del código no quedó tan completa como en la iteración anterior

**Cómo lo manejé:**
- Prioricé las funcionalidades core de cada historia de usuario
- Me enfoqué en que el flujo principal funcionara correctamente
- Dejé documentados los puntos pendientes para futuras mejoras

---

## Reflexiones sobre el trabajo realizado

### Aspectos positivos

1. **Consolidación del módulo de pagos:** A pesar de los desafíos, logré completar toda la gestión de pagos del sistema, integrando pagos totales, parciales, combinados y la gestión de saldo a favor.

2. **Implementación de modelo rico:** Mantuve la lógica de negocio centralizada en el dominio, siguiendo el patrón de modelo rico que iniciamos en la iteración 1.

3. **Integración con módulos existentes:** Las nuevas funcionalidades se integraron correctamente con el módulo de clientes y facturas desarrollado por otros integrantes del equipo.

### Aspectos a mejorar

1. **Testing:** No implementé pruebas unitarias ni de integración. Las pruebas fueron principalmente manuales a través de la interfaz.

2. **Planificación de historias:** Debí haber analizado mejor las dependencias entre HU al inicio del proyecto para agruparlas de forma más eficiente.

3. **Funcionalidad de impresión:** No pude implementar la generación de PDF para recibos y facturas.

---

## Plan de mejoras para futuras iteraciones

### Prioridad Alta

1. **Implementar pruebas unitarias y de integración:**
   - Crear tests para la lógica de distribución de pagos
   - Probar casos límite en pagos combinados y saldo a favor
   - Alcanzar una cobertura de código aceptable

### Prioridad Media

2. **Generación de PDF para recibos y facturas:**
   - Implementar funcionalidad para imprimir/exportar recibos en formato PDF
   - Permitir la descarga de facturas en PDF para los clientes

3. **Excepciones de negocio específicas:**
   - Crear excepciones personalizadas (`SaldoInsuficienteException`, `FacturaYaPagadaException`)
   - Mejorar el manejo de errores en el módulo de pagos

### Prioridad Baja

4. **Mejoras de UX:**
   - Agregar confirmaciones visuales antes de procesar pagos
   - Mostrar preview de distribución de pagos antes de confirmar

---

## Conclusiones finales

Esta segunda iteración me permitió completar el módulo de pagos del sistema de facturación. Aunque enfrenté desafíos relacionados con la planificación inicial y la gestión del tiempo, logré implementar las tres historias de usuario asignadas.

La principal lección aprendida es la importancia de analizar las dependencias entre funcionalidades al momento de planificar las iteraciones. Las historias de usuario relacionadas deberían desarrollarse juntas para minimizar el retrabajo.

El módulo de pagos ahora soporta pagos totales, parciales, combinados, gestión de saldo a favor y consulta de recibos, cumpliendo con los requerimientos del sistema de facturación.

---

## Integrante: Leandro Escalada

Esta retrospectiva documenta mi experiencia implementando las historias de usuario **HU-18: Modificación de servicio** y **HU-19: Baja de servicio** durante la segunda iteración del proyecto.

### ¿Qué se logró?

- Implementé el método `modificar()` en `Servicio` siguiendo el patrón de modelo rico
- Agregué Bean Validation completo (`@NotBlank`, `@Size`, `@DecimalMin`, `@Digits`, `@NotNull`)
- Implementé la propagación automática de precio a los `ServicioContratado` activos
- Creé los métodos `desactivar()` y `puedeFacturarse()` para baja lógica
- Integré la funcionalidad con facturación masiva (servicios inactivos se excluyen)
- Refactoricé el enum `TipoAlicuotaIVA` agregando el método `getPorcentaje()`

## Desafíos encontrados

### Propagación de precios a contratos activos

**Problema:**
Al modificar el precio de un servicio, necesitaba actualizar automáticamente el `precioContratado` en todos los `ServicioContratado` activos, sin afectar las facturas ya emitidas.

**Cómo lo resolví:**
- Iteré sobre los contratos activos y actualicé el precio en `modificarServicio()`
- Las facturas históricas conservan el `precioUnitario` original en `ItemFactura`

### Integración con facturación masiva

**Problema:**
Los servicios dados de baja no debían incluirse en la facturación masiva ni individual, pero la lógica existente no contemplaba este filtro.

**Cómo lo resolví:**
- Creé el método `puedeFacturarse()` en `Servicio` que retorna el valor de `activo`
- Modifiqué `FacturaService.generarFacturaMasiva()` para verificar `servicio.puedeFacturarse()`

### Refactorización del enum TipoAlicuotaIVA

**Problema:**
El cálculo del porcentaje de IVA estaba en un switch/case dentro de `Servicio`, lo que dificultaba el mantenimiento.

**Cómo lo resolví:**
- Agregué el campo `porcentaje` al enum
- Implementé `getPorcentaje()` que retorna el valor directamente
- Eliminé el switch/case: ahora `Servicio` delega al enum

## Lecciones aprendidas

- Los enums en Java pueden tener campos y métodos 
- La propagación de cambios entre entidades requiere pensar en qué datos son históricos y cuales  deben estar vigentes

## Comparación con Iteración 1

La dificultad disminuyó en comparación con la iteración anterior:
- Pude concentrarme en la lógica de negocio en lugar de aprender herramientas
- La estimación de tiempos fue más acertada
- Menos errores en templates Thymeleaf

## Trabajo en equipo

- Mejor comunicación que en la iteración 1
- Los módulos del proyecto ya estaban estables, lo que facilitó la integración
- No hubo dependencias ni bloqueos que afectaran el desarrollo

## Conclusiones finales

La segunda iteración fue más fluida. La experiencia previa permitió enfocarme en funcionalidades de negocio más complejas (propagación de precios, integración con facturación) sin perder tiempo en curvas de aprendizaje.

---

## Integrante: Gabriel Leonardo Mittelstedt
### Tareas cumplidas
* **HU-05 (Anulación de factura individual):** Implementé la lógica de negocio para anular facturas, incluyendo la generación automática de la entidad `NotaCredito` y asegurando la trazabilidad bidireccional en el modelo rico.
* **HU-20 (Facturación proporcional):** Desarrollé el cálculo de importes proporcionales basado en fechas utilizando el patrón *Value Object* (`PeriodoFacturacion`) y un *Factory Method* en `ItemFactura` para encapsular la lógica matemática.
* **Refactorización técnica:** Migré las consultas personalizadas de SQL nativo (`@Query`) a **Query Methods** estándar de Spring Data en los repositorios, mejorando la legibilidad y mantenibilidad del código.
* **Mejoras generales:** Implementé la actualización automática del estado de facturas a "VENCIDA" y reforcé las validaciones de consistencia de fechas tanto en el backend como en el frontend.

### Tareas incumplidas
* **Testing exhaustivo:** Debido a la falta de tiempo, no pude extender la cobertura de pruebas unitarias e integración para cubrir todos los casos borde complejos (ej. años bisiestos en proporcionales), limitándome a validar los flujos principales.
* **Feedback visual dinámico:** Quedó pendiente implementar una mejora en la interfaz de usuario para que el cálculo del monto proporcional se previsualice en tiempo real en el formulario antes de confirmar la emisión de la factura.

### Dificultades
* **Gestión de tiempos:** La principal dificultad fue la concurrencia de fechas con exámenes y entregas finales de otras asignaturas, lo que redujo significativamente el tiempo disponible para etapas de refactorización y pruebas.
* **Lógica de fechas:** La implementación precisa del cálculo de días efectivos requirió un esfuerzo de análisis mayor al estimado inicialmente.

### Conclusión
Logré entregar las funcionalidades críticas asignadas respetando la arquitectura establecida y las buenas prácticas del modelo de dominio. Como lección aprendida, para futuros proyectos es indispensable **planificar con un margen de holgura mayor** que contemple la carga académica externa, para así poder garantizar una mayor cobertura de pruebas y entregar un producto más robusto desde la primera iteración.

## Integrante: Axel Dos Santos

Esta retrospectiva documenta mi experiencia implementando las historias de usuario **HU-02: Modificación de cliente**, **HU-03: Gestión de estado de cuenta** y **HU-22: Desvincular servicio de un cliente** durante la segunda iteración.

### ¿Qué se logró?

- ✅ Implementé el método `actualizarDatos()` en `CuentaCliente` siguiendo el patrón de modelo rico
- ✅ Creé la entidad `CambioEstadoCuenta` con auditoría automática y relación bidireccional
- ✅ Desarrollé el método `cambiarEstado()` con validaciones de negocio en el dominio
- ✅ Implementé la baja lógica de servicios mediante `desvincularServicio()` y conservación del histórico
- ✅ Integré Bean Validation completo en formularios y entidades
- ✅ Creé vistas de confirmación con información detallada y advertencias para operaciones críticas

## Desafíos encontrados

### Validaciones en múltiples capas

**Problema:**
Era necesario validar datos tanto en formularios como en la lógica de dominio, evitando duplicación de código.

**Cómo lo resolví:**
- Utilicé Bean Validation (`@NotBlank`, `@Size`, `@NotNull`) en entidades
- Delegué validaciones de negocio específicas al dominio (modelo rico)
- Manejé excepciones del dominio en el servicio para mostrar mensajes amigables

## Lecciones aprendidas

- La inmutabilidad de ciertos campos (como CUIT/DNI) se puede manejar eficientemente ocultando el campo en el formulario de edición
- Los históricos son fundamentales para trazabilidad y auditoría en sistemas de gestión
- Las vistas de confirmación con información detallada mejoran la UX en operaciones irreversibles

## Conclusiones finales

Las tres historias de usuario implementadas completaron la gestión integral de clientes y sus servicios. La arquitectura basada en modelo rico demostró su valor al centralizar validaciones de negocio en el dominio, y la conservación de históricos garantiza trazabilidad completa del sistema.

## Integrante: Marcos Daubermann

Esta retrospectiva documenta mi experiencia implementando las historias de usuario **HU-07: Emisión de facturación masiva por período** (deuda técnica de la Iteración 1), **HU-08: Anulación de facturación masiva** y **HU-09: Consulta de facturación masiva** durante la segunda iteración del proyecto.

### ¿Qué se logró?

- ✅ Saldé la deuda técnica implementando la lógica completa de facturación masiva (`LoteFacturacion`), orquestando la generación de múltiples facturas en una sola ejecución.
- ✅ Implementé la **anulación en cascada** (HU-08): al anular un lote, el sistema anula automáticamente todas las facturas asociadas y genera las notas de crédito correspondientes.
- ✅ Garanticé la integridad de datos mediante el uso estricto de `@Transactional`: si falla la generación de una sola factura dentro del lote, se realiza un rollback completo.
- ✅ Desarrollé las vistas de consulta (HU-09) permitiendo visualizar el estado de los lotes, montos totales y el desglose de comprobantes generados.
- ✅ Reutilicé la lógica de negocio de facturación individual (HU-04) creada por Gabriel para evitar duplicación de código en el cálculo de impuestos y totales.

## Desafíos encontrados

### Gestión de la Deuda Técnica

**Problema:**
Comenzar la iteración con una funcionalidad compleja pendiente (HU-07) generó presión inicial y requirió coordinar muy bien los tiempos para no bloquear las nuevas tareas de anulación (HU-08).

**Cómo lo resolví:**
- Prioricé la HU-07 durante los primeros días de la iteración.
- Me apoyé en el código ya existente de `FacturaService` para no "reinventar la rueda" en la creación de cada factura individual dentro del bucle masivo.

### Transaccionalidad y Manejo de Errores en Lote

**Problema:**
El mayor desafío técnico fue decidir qué hacer si fallaba una factura en medio de un proceso masivo de 100 clientes. ¿Se guardan las anteriores o se cancela todo?

**Cómo lo resolví:**
- Implementé una estrategia de "todo o nada" usando la anotación `@Transactional` de Spring.
- Agregué validaciones previas (ej. verificar que el período no estuviera ya facturado) para fallar rápido antes de iniciar el procesamiento pesado.

### Validaciones de Anulación Masiva

**Problema:**
La anulación de un lote (HU-08) tiene una regla de negocio estricta: no puede anularse si alguna factura del lote ya recibió un pago. Verificar esto eficientemente sin iterar innecesariamente fue un reto.

**Cómo lo resolví:**
- Implementé el método rico `puedeSerAnulado()` en la entidad `LoteFacturacion`.
- Utilicé *Query Methods* optimizados para verificar la existencia de pagos en las facturas asociadas al lote antes de proceder con la anulación.

## Lecciones aprendidas

- **El costo de la deuda técnica:** Arrastrar una funcionalidad *core* de una iteración a otra complica la planificación. Es vital cerrar los flujos principales lo antes posible.

## Conclusiones finales

A pesar de haber comenzado con desventaja por la carga pendiente, logré completar el módulo de Facturación Masiva en su totalidad.
Debo tratar de organizar mejor los tiempos, pero a veces con las otras materias es difícil.