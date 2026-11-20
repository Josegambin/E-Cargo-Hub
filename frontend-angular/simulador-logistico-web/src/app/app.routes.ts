import { Routes } from '@angular/router';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { AuthStatusComponent } from './core/components/auth-status.component';
import { requireRoles } from './core/auth/require-roles.guard';

export const routes: Routes = [
    {
        path: 'forbidden',
        component: AuthStatusComponent,
        data: {
            title: 'Acceso denegado',
            message: 'Tu cuenta no tiene permisos para esta sección.'
        }
    },
    {
        path: 'auth-unavailable',
        component: AuthStatusComponent,
        data: {
            title: 'Servicio de acceso no disponible',
            message: 'No se pudo conectar con el proveedor de identidad. Inténtalo de nuevo más tarde.'
        }
    },
    {
        path: 'vehicle-display',
        loadComponent: () => import('./features/vehicle-display/vehicle-display.component')
            .then(m => m.VehicleDisplayComponent)
    },
    {
        path: '',
        component: LayoutComponent,
        canActivate: [requireRoles],
        data: { roles: ['ADMIN', 'VIEWER'] },
        children: [
            {
                path: 'dashboard',
                loadComponent: () => import('./features/dashboard/dashboard.component')
                    .then(m => m.DashboardComponent)
            },
            {
                path: 'vehicles',
                loadComponent: () => import('./features/vehicles/vehicles.component')
                    .then(m => m.VehiclesComponent)
            },
            {
                path: 'commands',
                canActivate: [requireRoles],
                data: { roles: ['ADMIN'] },
                loadComponent: () => import('./features/commands/command.component')
                    .then(m => m.CommandsComponent)
            },
            {
                path: 'telemetry',
                loadComponent: () => import('./features/telemetry/telemetry.component')
                    .then(m => m.TelemetryComponent)
            },
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full'
            }
        ]
    },
    {
        path: '**',
        redirectTo: 'dashboard'
    }
];