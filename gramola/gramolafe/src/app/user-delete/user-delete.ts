import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';

@Component({
  selector: 'app-user-delete',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-delete.html',
  styleUrl: './user-delete.css'
})
export class UserDeleteComponent {
  id?: number;
  ok = '';
  err = '';
  constructor(private users: UserService) {}
  remove() {
    this.ok = this.err = '';
    if (!this.id && this.id !== 0) { this.err = 'Indica un ID'; return; }
    this.users.delete(this.id!).subscribe({
      next: _ => this.ok = 'Eliminado ✅',
      error: e => this.err = e?.error?.message || 'Error eliminando ❌'
    });
  }
}
