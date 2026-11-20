import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ApiModule, BASE_PATH } from './api';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { bearerTokenInterceptor } from './core/auth/bearer-token.interceptor';

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(withInterceptors([bearerTokenInterceptor])),
        provideAnimations(),
        importProvidersFrom(ApiModule),
        { provide: BASE_PATH, useValue: environment.apiUrl }
    ]
};