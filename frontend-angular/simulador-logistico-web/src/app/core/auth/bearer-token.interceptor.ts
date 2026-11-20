import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthenticationService } from './authentication.service';

export const bearerTokenInterceptor: HttpInterceptorFn = (request, next) => {
    const apiUrl = new URL(environment.apiUrl);
    const requestUrl = new URL(request.url, apiUrl);
    const publicDeviceEndpoints = [
        '/api/v1/device-enrollments/claim',
        '/api/v1/device/state'
    ];

    if (requestUrl.origin !== apiUrl.origin || publicDeviceEndpoints.includes(requestUrl.pathname)) {
        return next(request);
    }

    const authentication = inject(AuthenticationService);
    return from(authentication.validAccessToken()).pipe(
        switchMap(token => next(token
            ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
            : request))
    );
};
