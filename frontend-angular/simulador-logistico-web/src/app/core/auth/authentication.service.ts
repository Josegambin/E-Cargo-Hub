import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import type Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    private keycloak?: Keycloak;
    private initialization?: Promise<boolean>;

    initialize(): Promise<boolean> {
        if (!this.isBrowser) {
            return Promise.resolve(false);
        }
        this.initialization ??= import('keycloak-js').then(({ default: KeycloakClient }) => {
            this.keycloak = new KeycloakClient({
                url: environment.identityUrl,
                realm: environment.identityRealm,
                clientId: environment.identityClientId
            });
            return this.keycloak.init({
                onLoad: 'check-sso',
                pkceMethod: 'S256',
                checkLoginIframe: false
            });
        });
        return this.initialization;
    }

    get authenticated(): boolean {
        return this.keycloak?.authenticated === true;
    }

    get browserClient(): boolean {
        return this.isBrowser;
    }

    get username(): string | undefined {
        return this.keycloak?.tokenParsed?.['preferred_username'];
    }

    hasRole(role: string): boolean {
        return this.keycloak?.realmAccess?.roles?.includes(role) ?? false;
    }

    hasAnyRole(roles: readonly string[]): boolean {
        return roles.some(role => this.hasRole(role));
    }

    async login(redirectUrl = '/dashboard'): Promise<void> {
        if (!this.isBrowser) {
            return;
        }
        await this.initialize();
        await this.keycloak?.login({ redirectUri: new URL(redirectUrl, location.origin).toString() });
    }

    async logout(): Promise<void> {
        if (this.isBrowser && this.keycloak) {
            await this.keycloak.logout({ redirectUri: location.origin });
        }
    }

    async validAccessToken(): Promise<string | undefined> {
        if (!this.isBrowser || !(await this.initialize()) || !this.authenticated || !this.keycloak) {
            return undefined;
        }
        await this.keycloak.updateToken(30);
        return this.keycloak.token;
    }

    get accessTokenExpiresAt(): number | undefined {
        const expiration = this.keycloak?.tokenParsed?.exp;
        return typeof expiration === 'number' ? expiration * 1000 : undefined;
    }
}
