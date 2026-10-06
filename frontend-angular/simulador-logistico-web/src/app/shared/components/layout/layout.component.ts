import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

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
        MatIconModule,
        MatMenuModule,
        MatTooltipModule,
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

    t(key: string): string { return this.langSvc.t(key); }

    toggleTheme(): void { this.themeSvc.toggleTheme(); }
    setLanguage(lang: 'es' | 'en'): void { this.langSvc.setLanguage(lang); }
}