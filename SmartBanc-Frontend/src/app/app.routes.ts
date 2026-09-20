import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path:'login', title:'Ingresa · SmartBancs', loadComponent: () => import('./features/auth/auth.page').then(m => m.AuthPage), data:{mode:'login'} },
  { path:'register', title:'Crea tu cuenta · SmartBancs', loadComponent: () => import('./features/auth/auth.page').then(m => m.AuthPage), data:{mode:'register'} },
  { path:'forgot-password', title:'Cambia tu contraseña · SmartBancs', loadComponent: () => import('./features/auth/auth.page').then(m => m.AuthPage), data:{mode:'password'} },
  { path:'app', canActivate:[authGuard], canActivateChild:[authGuard], loadComponent: () => import('./layout/app-shell').then(m => m.AppShell), children:[
    { path:'', title:'Mi resumen · SmartBancs', loadComponent: () => import('./features/dashboard/dashboard.page').then(m => m.DashboardPage) },
    { path:'settings', title:'Configuración · SmartBancs', loadComponent: () => import('./features/settings/settings.page').then(m => m.SettingsPage) }
  ]},
  { path:'', pathMatch:'full', redirectTo:'app' },
  { path:'**', redirectTo:'app' }
];
