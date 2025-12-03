# Retrospectiva - Iteración 2

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

La segunda iteración fue más fluida. La experiencia previa permitió enfocarme en funcionalidades de negocio más complejas (propagación de precios, integración con facturación) sin perder tiempo en curvas de aprendizaje. El patrón de modelo rico se consolidó en mi forma de trabajar.
