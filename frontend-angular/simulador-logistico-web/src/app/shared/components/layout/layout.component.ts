import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

// Material
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

// Servicios
import { WebSocketService } from '../../../core/services/websocket.facade';
import { ThemeService } from '../../../core/services/theme.facade';
import { LanguageService } from '../../../core/services/language.facade';

@Component({
    selector: 'app-layout',
    standalone: true,
    imports: [
        CommonModule,
        RouterOutlet,
        RouterLink,
        RouterLinkActive,
        MatToolbarModule,
        MatSidenavModule,
        MatListModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule,
        MatMenuModule,
    ],
    templateUrl: './layout.component.html',
    styleUrl: './layout.component.css'
})
export class LayoutComponent {

    private ws = inject(WebSocketService);
    private themeSvc = inject(ThemeService);
    private langSvc = inject(LanguageService);

    readonly connected = this.ws.connected;
    readonly isDark = this.themeSvc.isDark;
    readonly currentLang = this.langSvc.currentLang;

    // 🌍 Traducción (para usar en templates)
    t(key: string): string {
        return this.langSvc.t(key);
    }

    toggleTheme(): void {
        this.themeSvc.toggleTheme();
    }

    setLanguage(lang: 'es' | 'en'): void {
        this.langSvc.setLanguage(lang);
    }
}