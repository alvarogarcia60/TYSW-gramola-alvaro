# Gramolafe

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.5.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Gestión de borrado en Gramola (Frontend)

- Alcance: la eliminación de canciones afecta únicamente a la cola de Gramola/BD. Spotify no se modifica.
- Confirmación: el botón de borrar solicita confirmación y muestra que el borrado es solo en Gramola.
- Restricción: las acciones de borrado están limitadas a administradores (dueños del bar).
- Limpieza masiva: los administradores pueden usar el botón "Limpiar cola" para vaciar la cola en Gramola.
- Auto-limpieza: las canciones reproducidas desaparecen automáticamente de Gramola conforme avanza la reproducción.

Endpoints backend relacionados (ya implementados):

- `DELETE /music/delete-song/{id}`: elimina una canción de la cola en BD.
- `DELETE /music/clear-queue?email=<barEmail>`: limpieza masiva de la cola en BD.
