import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { of } from 'rxjs';

// Create a mock AuthService
class MockAuthService {
  // Mock any properties or methods your component uses
  // For example, if your template uses `authService.isAuthenticated$`:
  isAuthenticated$ = of(false); 
  
  logout() {
    // Mock the logout method
  }
}

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AppComponent, // The component is standalone, so we import it directly
        RouterTestingModule // Provides a mock router
      ],
      providers: [
        // Provide the mock service for AuthService
        { provide: AuthService, useClass: MockAuthService } 
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the correct title 'ProximaForte BioVerify™'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.appTitle).toEqual('ProximaForte BioVerify™');
  });

  it('should render the app title in the toolbar', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges(); // Trigger change detection
    const compiled = fixture.nativeElement as HTMLElement;
    // Note: This selector might need to be adjusted based on your actual HTML structure.
    // This example assumes you have a <span> or similar element with the title.
    const titleElement = compiled.querySelector('mat-toolbar span'); 
    expect(titleElement?.textContent).toContain('ProximaForte BioVerify™');
  });

  it('should have an observable for breadcrumbs defined', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.breadcrumbs$).toBeDefined();
  });
});
