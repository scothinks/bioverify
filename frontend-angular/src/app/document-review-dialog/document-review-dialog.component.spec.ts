import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DocumentReviewDialogComponent } from './document-review-dialog.component';

describe('DocumentReviewDialogComponent', () => {
  let component: DocumentReviewDialogComponent;
  let fixture: ComponentFixture<DocumentReviewDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentReviewDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DocumentReviewDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
