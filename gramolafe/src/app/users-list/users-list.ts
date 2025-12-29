import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../user';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css'
})
export class UsersListComponent {
  users: any[] = [];
  err = '';
  constructor(private usersSvc: UserService) {}
  load() {
    this.err = '';
    this.usersSvc.getAll().subscribe({
      next: r => this.users = r,
      error: e => this.err = e?.error?.message || 'Error listando usuarios'
    });
  }
}
