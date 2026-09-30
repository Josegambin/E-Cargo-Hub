# Guía de Contribución

¡Gracias por tu interés en contribuir a **E-Cargo Hub**! Este documento describe las normas y el flujo de trabajo para contribuir al proyecto.

> **Nota:** Este proyecto es un Trabajo de Fin de Grado (TFG) en desarrollo activo. **No se aceptan contribuciones externas** durante el período de desarrollo. Sin embargo, si encuentras un bug o tienes una sugerencia, puedes abrir un *issue*.

---

## 📋 Código de Conducta

Al participar en este proyecto, te comprometes a:

- Respetar a todos los colaboradores independientemente de su experiencia, género, orientación sexual, discapacidad, apariencia, raza o religión.
- Aceptar críticas constructivas de forma profesional.
- Centrarte en lo que es mejor para el proyecto.
- Mostrar empatía hacia otros miembros de la comunidad.

---

## 🐛 Reportar bugs

Antes de abrir un issue, por favor comprueba:

1. Que el bug no esté ya reportado en los [issues existentes](https://github.com/jmgambin/E-Cargo-Hub/issues).
2. Que estás usando la última versión del código.
3. Que has leído la documentación en `docs/`.

Al abrir un issue, incluye:

- **Descripción clara** del problema.
- **Pasos para reproducirlo** (numerados).
- **Comportamiento esperado** vs **comportamiento observado**.
- **Entorno** (SO, versión de Java, versión de Node, etc.).
- **Logs** relevantes (backend, frontend, consola del navegador).
- **Capturas de pantalla** si aplica.

---

## 🔧 Flujo de trabajo con Git

### Ramas

- `main` → código estable, siempre funcional.
- `develop` → integración de nuevas funcionalidades.
- `feature/nombre-feature` → desarrollo de una nueva funcionalidad.
- `fix/nombre-bug` → corrección de un bug.
- `docs/nombre-doc` → cambios en documentación.

### Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>(<alcance>): <descripción corta>

[descripción larga opcional]

[footer opcional]
```

**Tipos permitidos:**

- `feat`: nueva funcionalidad.
- `fix`: corrección de bug.
- `docs`: cambios en documentación.
- `style`: cambios de formato (espacios, punto y coma, etc.).
- `refactor`: refactorización sin cambio de funcionalidad.
- `perf`: mejoras de rendimiento.
- `test`: añadir o modificar tests.
- `chore`: tareas de mantenimiento.
- `ci`: cambios en CI/CD.
- `build`: cambios en el sistema de build.

**Ejemplos:**

```
feat(commands): añadir comando RESUME
fix(kafka): corregir idempotencia con Redis
docs(readme): actualizar instrucciones de instalación
refactor(service): extraer lógica de simulación a clase propia
```

### Pull Requests

1. **Haz fork** del repositorio.
2. **Crea una rama** desde `develop`.
3. **Realiza tus cambios** siguiendo el estilo del proyecto.
4. **Ejecuta los tests** localmente (`./mvnw test` y `npm test`).
5. **Actualiza la documentación** si es necesario.
6. **Actualiza el CHANGELOG** en la sección `[Unreleased]`.
7. **Abre un Pull Request** hacia `develop`.

**Formato del PR:**

```markdown
## Descripción
[Qué hace este PR]

## Motivación
[Por qué es necesario]

## Cambios
- ...
- ...

## Testing
[Cómo se ha probado]

## Checklist
- [ ] Tests pasan
- [ ] Documentación actualizada
- [ ] CHANGELOG actualizado
- [ ] No hay conflictos con `develop`
```

---

## 💻 Estilo de código

### Backend (Java)

- **Indentación**: 4 espacios.
- **Longitud máxima de línea**: 120 caracteres.
- **Nombres de clases**: `PascalCase`.
- **Nombres de métodos y variables**: `camelCase`.
- **Constantes**: `UPPER_SNAKE_CASE`.
- **Paquetes**: `com.ecargohub.backend.<capa>`.

**Ejemplo:**

```java
@Service
public class VehicleCommandService {

    private static final int MAX_RETRIES = 3;

    private final VehicleRepository vehicleRepository;

    public VehicleCommandService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {
        // ...
    }
}
```

### Frontend (TypeScript/Angular)

- **Indentación**: 2 espacios.
- **Nombres de archivos**: `kebab-case` (`vehicle-list.component.ts`).
- **Nombres de clases**: `PascalCase`.
- **Nombres de métodos y variables**: `camelCase`.
- **Interfaces**: `PascalCase` sin prefijo `I`.

**Ejemplo:**

```typescript
@Component({
  selector: 'app-vehicle-list',
  templateUrl: './vehicle-list.component.html'
})
export class VehicleListComponent implements OnInit {

  vehicles: VehicleDto[] = [];

  constructor(private vehicleService: VehicleService) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  private loadVehicles(): void {
    this.vehicleService.getAll().subscribe({
      next: (data) => this.vehicles = data,
      error: (err) => console.error(err)
    });
  }
}
```

### SQL

- **Palabras clave en mayúsculas**: `SELECT`, `FROM`, `WHERE`, etc.
- **Nombres de tablas y columnas**: `snake_case`.

**Ejemplo:**

```sql
SELECT v.id, v.name, v.plate
FROM vehicles v
WHERE v.created_at > NOW() - INTERVAL '7 days'
ORDER BY v.created_at DESC;
```

---

## 🧪 Tests

- **Cobertura mínima**: 60%.
- **Tests unitarios**: en `src/test/java/...` para el backend, `*.spec.ts` para el frontend.
- **Tests de integración**: con `@SpringBootTest` y Testcontainers.
- **Nombres descriptivos**:

```java
@Test
void sendCommand_conVehiculoInexistente_lanzaResourceNotFound() {
    // ...
}
```

---

## 📚 Documentación

- Toda funcionalidad nueva **debe** estar documentada.
- Actualiza el `README.md` si cambias la API o el arranque.
- Actualiza `docs/API.md` si añades o modificas endpoints.
- Actualiza `docs/ERS.md` si añades requisitos funcionales.
- Añade un `@Operation` de Swagger en cada endpoint nuevo.

---

## ❓ ¿Dudas?

Si tienes alguna pregunta, puedes:

- Abrir un [issue](https://github.com/jmgambin/E-Cargo-Hub/issues) con la etiqueta `question`.
- Contactar al autor: **Jose Manuel Gambin Manresa**.

---

¡Gracias por contribuir a **E-Cargo Hub**! 🚚