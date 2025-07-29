import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AgentLayoutComponent } from './agent-layout.component';



describe('AgentDashboardComponent', () => {
  let component: AgentLayoutComponent;
  let fixture: ComponentFixture<AgentLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
