// Enhanced app.component.ts - Minimal enhancements for premium UI
import { Component, OnInit, OnDestroy, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './services/auth.service';
import { Observable, BehaviorSubject, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

// Interface for breadcrumb items
interface BreadcrumbItem {
  label: string;
  url?: string;
  icon?: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  

  // App configuration
  appTitle = 'ProximaForte BioVerify™';
  
  // State management
  private destroy$ = new Subject<void>();
  private loading$ = new BehaviorSubject<boolean>(false);
  private _breadcrumbs$ = new BehaviorSubject<BreadcrumbItem[]>([]); // <-- FIX: Renamed
  
  // UI state observables
  isLoading$ = this.loading$.asObservable();
  breadcrumbs$: Observable<BreadcrumbItem[]> = this._breadcrumbs$.asObservable(); // <-- FIX: Updated reference
  
  // UI configuration
  showBreadcrumbs = false; // Set to true when you want breadcrumbs
  isElevatedPage = false;
  loadingMessage = 'Loading application...';
  
  constructor(
    public authService: AuthService,
    private router: Router,
    private elementRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.setupRouterSubscriptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  navigateToHome(): void {
    this.router.navigate(['/dashboard']); // Adjust route as needed
  }

  logout(): void {
    this.authService.logout();
  }

  private setupRouterSubscriptions(): void {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        // Update page elevation based on route (optional)
        this.updatePageElevation(event.url);
        
        // Scroll to top on route change
        window.scrollTo(0, 0);
      });
  }

  private updatePageElevation(url: string): void {
    // Define which routes should have elevated styling
    const elevatedRoutes = ['/profile', '/settings', '/admin'];
    this.isElevatedPage = elevatedRoutes.some(route => url.startsWith(route));
  }


  focusMainContent(): void {
    const mainContent = this.elementRef.nativeElement.querySelector('.main-content');
    if (mainContent) {
      mainContent.focus();
      mainContent.scrollIntoView({ behavior: 'smooth' });
    }
  }
}