/**
 * Polyfills para E-Cargo Hub
 *
 * Algunos paquetes (como @stomp/stompjs o sus dependencias) usan la variable
 * global `global` de Node.js, que no existe en el navegador. Aquí la definimos
 * como `window` para que esos paquetes funcionen.
 */

// Solución al error "global is not defined"
(window as any).global = window;

// Polyfill de `process` (algunos paquetes lo usan para `process.env`)
(window as any).process = {
    env: { DEBUG: undefined },
    version: '',
    nextTick: (fn: Function) => setTimeout(fn, 0)
};