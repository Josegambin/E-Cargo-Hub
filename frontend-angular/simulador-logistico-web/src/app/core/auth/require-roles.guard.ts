import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthenticationService } from './authentication.service';

export const requireRoles: CanActivateFn = async (route, state) => {
    const authentication = inject(AuthenticationService);
    const router = inject(Router);

    if (!authentication.browserClient) {
        return false;
    }

    try {
        if (!(await authentication.initialize())) {
            await authentication.login(state.url);
            return false;
        }
        if (!authentication.authenticated) {
            await authentication.login(state.url);
            return false;
        }

        const requiredRoles = route.data['roles'] as string[] | undefined;
        return !requiredRoles || authentication.hasAnyRole(requiredRoles)
            ? true
            : router.parseUrl('/forbidden');
    } catch {
        return router.parseUrl('/auth-unavailable');
    }
};
