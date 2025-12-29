import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-detail.html',
  styleUrl: './user-detail.css'
})
export class UserDetailComponent {
  id?: number;
  user: any;
  err = '';
  constructor(private users: UserService) {}
  load() {
    this.err = ''; this.user = null;
    if (!this.id && this.id !== 0) { this.err = 'Indica un ID'; return; }
    this.users.getById(this.id!).subscribe({
      next: r => this.user = r,
      error: e => this.err = e?.error?.message || 'No encontrado'
    });
  }
}
