import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SearchSongs } from './search-songs';

describe('SearchSongs', () => {
  let component: SearchSongs;
  let fixture: ComponentFixture<SearchSongs>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchSongs]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SearchSongs);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
