import { Component, inject } from '@angular/core';
import { LanguageService } from '../../core/services/language.facade';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    template: `
        <div class="space-y-6">
            <div>
                <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
                    {{ t('dashboard.title') }}
                </h1>
                <p class="text-gray-600 dark:text-gray-400 mt-1">
                    {{ t('dashboard.subtitle') }}
                </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-8 text-center">
                <span class="text-5xl">🗺️</span>
                <h2 class="text-xl font-semibold mt-4 text-gray-900 dark:text-white">
                    Mapa en construcción
                </h2>
                <p class="text-gray-600 dark:text-gray-400 mt-2">
                    Aquí irá el mapa con los vehículos en tiempo real.
                </p>
            </div>
        </div>
    `
})
export class DashboardComponent {
    private langSvc = inject(LanguageService);
    t(key: string): string { return this.langSvc.t(key); }
}