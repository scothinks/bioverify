import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRole = route.data['expectedRole'];
  const currentUserRole = authService.getUserRole();

  if (authService.isLoggedIn() && currentUserRole === expectedRole) {
    return true;
  }

  // Redirect to login for unauthorized access
  // A more advanced implementation could redirect to an "Access Denied" page
  router.navigate(['/login']);
  return false;
};