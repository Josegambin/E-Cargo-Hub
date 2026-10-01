import { Injectable, signal, effect, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, DOCUMENT } from '@angular/common';

export type ThemeMode = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {

    private readonly STORAGE_KEY = 'ecargohub-theme';

    readonly currentTheme = signal<ThemeMode>('light');
    readonly isDark = signal<boolean>(false);

    private document = inject(DOCUMENT);
    private platformId = inject(PLATFORM_ID);

    constructor() {
        if (isPlatformBrowser(this.platformId)) {
            // Cargar preferencia guardada
            const saved = localStorage.getItem(this.STORAGE_KEY) as ThemeMode | null;
            const initial: ThemeMode = saved ?? this.detectSystemTheme();
            this.currentTheme.set(initial);
            this.applyTheme(initial);

            // Aplicar cambios automáticamente
            effect(() => {
                this.applyTheme(this.currentTheme());
            });
        }
    }

    toggleTheme(): void {
        const next: ThemeMode = this.currentTheme() === 'light' ? 'dark' : 'light';
        this.setTheme(next);
    }

    setTheme(mode: ThemeMode): void {
        this.currentTheme.set(mode);
        this.isDark.set(mode === 'dark');

        if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem(this.STORAGE_KEY, mode);
        }
    }

    private detectSystemTheme(): ThemeMode {
        if (isPlatformBrowser(this.platformId) && window.matchMedia) {
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }
        return 'light';
    }

    private applyTheme(mode: ThemeMode): void {
        const html = this.document.documentElement;
        this.isDark.set(mode === 'dark');

        if (mode === 'dark') {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
    }
}