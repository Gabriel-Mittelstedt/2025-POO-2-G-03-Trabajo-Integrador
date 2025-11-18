# Retrospectiva - Iteración 1

## Integrante: Axel Dos Santos

Esta retrospectiva documenta mi experiencia personal implementando las historias de usuario **HU-01: Alta de cliente** y **HU-21: Asignar servicio a un cliente** durante la primera iteración del proyecto de facturación de servicios.

---

### Historia HU-01: Alta de Cliente

**Logros principales:**
-  Implementé completamente el modelo `CuentaCliente` con todas las validaciones requeridas usando Bean Validation
-  Creé las enumeraciones `TipoCondicionIVA` y `EstadoCuenta` siguiendo las normativas AFIP
-  Desarrollé el controlador con manejo correcto de errores y validaciones
-  Implementé el servicio con la lógica de negocio para validar duplicados de CUIT/DNI
-  Diseñé las vistas Thymeleaf con formularios reactivos y mensajes de error claros
-  Documenté todo el código con JavaDoc de forma concisa pero completa
-  Implementé valores por defecto automáticos (estado ACTIVA, saldo 0.00) usando @PrePersist


### Historia HU-21: Asignar Servicio a un Cliente

**Logros principales:**
-  Implementé un modelo rico donde `CuentaCliente` tiene métodos de negocio (`contratarServicio()`, `tieneServicioContratadoActivo()`)
-  Creé la relación bidireccional @OneToMany entre CuentaCliente y ServicioContratado
-  Desarrollé 4 endpoints en el controlador (detalle, asignar, post asignación, histórico)
-  Implementé el campo `activo` en ServicioContratado para permitir históricos futuros
-  Creé 3 vistas Thymeleaf (detalle.html, agregar-servicio.html, historico-servicios.html)
-  Registré automáticamente la fecha actual y el precio vigente del servicio al momento de la contratación
-  Validé duplicados para evitar asignar el mismo servicio dos veces a un cliente


---

## Desafíos y dificultades encontradas

### Desafío 1: Primera vez con Spring Boot, Thymeleaf y Lombok

**Problema:**
Al ser mi primera vez usando estas tecnologías, tuve que aprender simultáneamente:
- Anotaciones de Spring (@Controller, @Service, @Autowired, @Valid)
- Sintaxis de Thymeleaf (th:field, th:object, th:errors, th:each)
- Funcionamiento de Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor)

**Cómo lo resolví:**
- Dediqué tiempo a estudiar la documentación oficial de Spring Boot
- Analicé ejemplos de código en proyectos similares
- Realicé pruebas incrementales para entender el comportamiento de cada anotación

**Aprendizaje:**
Ahora comprendo la diferencia entre Bean Validation (validaciones de formato) y validaciones de negocio en el Service. También entendí cómo Lombok reduce el boilerplate sin afectar la funcionalidad.

### Desafío 2: Gestión del tiempo

**Problema:**
Subestimé el tiempo necesario para:
- Aprender las nuevas tecnologías
- Escribir JavaDoc completo
- Corregir errores de sintaxis en templates
- Realizar pruebas exhaustivas

**Impacto:**
Tuve que dedicar más horas de las planificadas, especialmente en la fase de depuración de Thymeleaf.

**Cómo lo manejaré en la próxima iteración:**
- Reservar tiempo específico para aprendizaje de tecnologías nuevas al inicio
- Realizar pruebas tempranas y frecuentes (no esperar a tener todo completo)
- Usar checklists para validaciones y testing

---

## Reflexiones sobre el trabajo realizado

### Aspectos positivos

1. **Documentación JavaDoc completa:** Aunque fue tedioso, ahora todo el código está bien documentado y es fácil de entender para otros integrantes del equipo.

2. **Separación clara de responsabilidades:** La arquitectura MVC quedó bien definida. Cada capa tiene su propósito claro.

3. **Código reutilizable:** Los métodos `contratarServicio()`, `tieneServicioContratadoActivo()` y `getServiciosContratadosActivos()` pueden usarse en otras partes del sistema (por ejemplo, en la facturación).

4. **Validaciones robustas:** La combinación de Bean Validation + validaciones de negocio hace que el sistema rechace datos inválidos en múltiples niveles.

### Aspectos a mejorar

1. **Pruebas unitarias:** No implementé pruebas unitarias durante esta iteración. Solo realicé pruebas manuales en el navegador.

2. **Manejo de excepciones más específico:** Actualmente uso `IllegalArgumentException` genérico. Podría crear excepciones de negocio personalizadas (`ClienteDuplicadoException`, `ServicioYaContratadoException`).


---

## Plan de mejoras para la Iteración 2

### Prioridad Alta

1. **Implementar pruebas unitarias desde el inicio:**
   - Crear tests para cada método de servicio
   - Probar validaciones de Bean Validation
   - Alcanzar al menos 70% de cobertura de código


### Prioridad Media

4. **Mejorar manejo de excepciones:**
   - Crear excepciones personalizadas para casos de negocio


### Prioridad Baja

7. **Mejorar UX de los formularios:**
   - Agregar validaciones JavaScript en el frontend

8. **Refactorizar código repetitivo:**
   - Extraer métodos comunes en clases utility

---

## Conclusiones finales

Esta primera iteración fue un gran desafío de aprendizaje. Aunque cumplí con las dos historias de usuario asignadas, el proceso me tomó más tiempo del planificado debido a la curva de aprendizaje de las tecnologías.

---

## Integrante: Gabriel Mittelstedt

### HU-04: Emisión de Factura Individual y HU-06: Consulta de Factura Individual

**¿Qué se logró?**
- ✅ Implementé el patrón RICO con lógica de negocio en las entidades `Factura` e `ItemFactura`
- ✅ Generación automática de ítems desde servicios contratados del cliente
- ✅ Determinación automática del tipo de factura según reglas AFIP
- ✅ Sistema de numeración correlativa por serie
- ✅ Vistas completas: formulario de emisión, listado con filtros y detalle de factura
- ✅ Integración exitosa con el módulo de clientes/servicios

**Desafíos encontrados:**
- **Primera vez con Spring Boot y JPA:** Tuve que aprender simultáneamente las anotaciones, el ciclo de vida de entidades, las relaciones bidireccionales y cómo funciona Thymeleaf.
- **Dependencia de otros módulos:** Inicialmente tuve que hardcodear servicios asignados a clientes porque el módulo de `ServicioContratado` no estaba disponible. Una vez que mi compañero lo completó, pude refactorizar para usar la integración real.
- **Errores en templates:** Múltiples correcciones en referencias de campos, nombres inconsistentes entre modelo y vistas, enum sin método `getDescripcion()`.
- **Simplificación tardía:** Creé una API REST completa con DTOs que luego eliminé. Debí planificar mejor desde el inicio qué se necesitaba realmente.

**Lecciones aprendidas:**
- Importancia de coordinar dependencias entre módulos del equipo
- Los errores en templates Thymeleaf no se detectan en compilación, requieren pruebas manuales

**Mejoras para próxima iteración:**
- Implementar pruebas unitarias desde el inicio
- Planificar mejor la arquitectura antes de codificar (evitar trabajo innecesario)
- Coordinar con el equipo qué módulos están listos para integrarse
- Crear excepciones personalizadas para casos de negocio