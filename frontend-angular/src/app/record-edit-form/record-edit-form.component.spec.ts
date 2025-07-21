import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecordEditFormComponent } from './record-edit-form.component';

describe('RecordEditFormComponent', () => {
  let component: RecordEditFormComponent;
  let fixture: ComponentFixture<RecordEditFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecordEditFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecordEditFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
