import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService } from '../../core/services/language.facade';

@Component({
    selector: 'app-vehicles',
    standalone: true,
    imports: [CommonModule],
    template: `
        <div class="space-y-8 animate-fade-in-up">
            <div>
                <h1 class="text-4xl font-display font-bold text-slate-900 dark:text-white mb-2">
                    {{ t('vehicles.title') }}
                </h1>
                <p class="text-slate-600 dark:text-slate-400">{{ t('vehicles.subtitle') }}</p>
            </div>

            <div class="glass-card p-12 text-center">
                <div class="w-20 h-20 mx-auto rounded-3xl bg-gradient-primary flex items-center justify-center shadow-glow mb-4 animate-float">
                    <span class="text-4xl">🚚</span>
                </div>
                <h2 class="text-2xl font-display font-bold text-slate-900 dark:text-white mb-2">
                    Lista de vehículos en construcción
                </h2>
            </div>
        </div>
    `
})
export class VehiclesComponent {
    private langSvc = inject(LanguageService);
    t(key: string): string { return this.langSvc.t(key); }
}