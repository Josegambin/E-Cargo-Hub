import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-auth-status',
    standalone: true,
    imports: [RouterLink],
    template: `
        <main class="flex min-h-screen items-center justify-center bg-slate-950 p-6 text-white">
            <section class="max-w-lg rounded-2xl bg-slate-900 p-8 text-center">
                <h1 class="text-2xl font-bold">{{ title }}</h1>
                <p class="mt-3 text-slate-300">{{ message }}</p>
                <a class="mt-6 inline-block rounded-lg bg-cyan-400 px-5 py-3 font-semibold text-slate-950"
                    routerLink="/dashboard">Volver a intentar</a>
            </section>
        </main>
    `
})
export class AuthStatusComponent {
    @Input() title = 'Acceso denegado';
    @Input() message = 'Tu cuenta no tiene permisos para esta sección.';
}
