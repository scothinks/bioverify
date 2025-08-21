import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvalidDocumentQueueComponent } from './invalid-document-queue.component';

describe('InvalidDocumentQueueComponent', () => {
  let component: InvalidDocumentQueueComponent;
  let fixture: ComponentFixture<InvalidDocumentQueueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvalidDocumentQueueComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InvalidDocumentQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
