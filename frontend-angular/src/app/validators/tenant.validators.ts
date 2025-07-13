import { AbstractControl, ValidationErrors, ValidatorFn, AsyncValidatorFn } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { TenantService } from '../services/tenant.service';

export class TenantValidators {
  static subdomainPattern(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const subdomainRegex = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/;
      const isValid = subdomainRegex.test(control.value);
      
      if (!isValid) {
        return { 
          subdomainPattern: { 
            message: 'Subdomain must start and end with alphanumeric characters, and can contain hyphens in between' 
          } 
        };
      }
      
      return null;
    };
  }

  static stateCodePattern(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const stateCodeRegex = /^[A-Z]{2}$/;
      const isValid = stateCodeRegex.test(control.value);
      
      if (!isValid) {
        return { 
          stateCodePattern: { 
            message: 'State code must be exactly 2 uppercase letters' 
          } 
        };
      }
      
      return null;
    };
  }

  static uniqueSubdomain(tenantService: TenantService): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value) return of(null);
      
      return of(control.value).pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(subdomain => 
          tenantService.checkSubdomainAvailability(subdomain).pipe(
            map(isAvailable => isAvailable ? null : { 
              subdomainTaken: { 
                message: 'This subdomain is already taken' 
              } 
            }),
            catchError(() => of(null))
          )
        )
      );
    };
  }
}