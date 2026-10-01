import { Component, inject } from '@angular/core';
import { LanguageService } from '../../core/services/language.facade';

@Component({
    selector: 'app-commands',
    standalone: true,
    template: `
        <div class="space-y-6">
            <div>
                <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
                    {{ t('commands.title') }}
                </h1>
                <p class="text-gray-600 dark:text-gray-400 mt-1">
                    {{ t('commands.subtitle') }}
                </p>
            </div>

            <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-8 text-center">
                <span class="text-5xl">🎮</span>
                <h2 class="text-xl font-semibold mt-4 text-gray-900 dark:text-white">
                    Panel de control en construcción
                </h2>
            </div>
        </div>
    `
})
export class CommandsComponent {
    private langSvc = inject(LanguageService);
    t(key: string): string { return this.langSvc.t(key); }
}