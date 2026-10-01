import { Injectable, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Language = 'es' | 'en';

@Injectable({ providedIn: 'root' })
export class LanguageService {

    private readonly STORAGE_KEY = 'ecargohub-lang';

    readonly currentLang = signal<Language>('es');

    private platformId = inject(PLATFORM_ID);

    private translations: Record<Language, Record<string, string>> = {
        es: {
            'app.title': 'E-Cargo Hub',
            'app.subtitle': 'Simulador Logístico',
            'nav.dashboard': 'Dashboard',
            'nav.vehicles': 'Vehículos',
            'nav.commands': 'Comandos',
            'nav.telemetry': 'Histórico',
            'status.connected': 'Conectado',
            'status.disconnected': 'Desconectado',
            'theme.light': 'Modo claro',
            'theme.dark': 'Modo oscuro',
            'menu.language': 'Idioma',
            'menu.theme': 'Tema',
            'dashboard.title': 'Panel de control',
            'dashboard.subtitle': 'Vista general de la flota en tiempo real',
            'vehicles.title': 'Vehículos',
            'vehicles.subtitle': 'Gestión de la flota',
            'commands.title': 'Comandos',
            'commands.subtitle': 'Control de vehículos',
            'telemetry.title': 'Histórico',
            'telemetry.subtitle': 'Histórico de telemetría',
        },
        en: {
            'app.title': 'E-Cargo Hub',
            'app.subtitle': 'Logistics Simulator',
            'nav.dashboard': 'Dashboard',
            'nav.vehicles': 'Vehicles',
            'nav.commands': 'Commands',
            'nav.telemetry': 'History',
            'status.connected': 'Connected',
            'status.disconnected': 'Disconnected',
            'theme.light': 'Light mode',
            'theme.dark': 'Dark mode',
            'menu.language': 'Language',
            'menu.theme': 'Theme',
            'dashboard.title': 'Control panel',
            'dashboard.subtitle': 'Real-time fleet overview',
            'vehicles.title': 'Vehicles',
            'vehicles.subtitle': 'Fleet management',
            'commands.title': 'Commands',
            'commands.subtitle': 'Vehicle control',
            'telemetry.title': 'History',
            'telemetry.subtitle': 'Telemetry history',
        }
    };

    constructor() {
        if (isPlatformBrowser(this.platformId)) {
            const saved = localStorage.getItem(this.STORAGE_KEY) as Language | null;
            if (saved === 'es' || saved === 'en') {
                this.currentLang.set(saved);
            }
        }
    }

    toggleLanguage(): void {
        const next: Language = this.currentLang() === 'es' ? 'en' : 'es';
        this.setLanguage(next);
    }

    setLanguage(lang: Language): void {
        this.currentLang.set(lang);
        if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem(this.STORAGE_KEY, lang);
        }
    }

    t(key: string): string {
        return this.translations[this.currentLang()][key] ?? key;
    }
}