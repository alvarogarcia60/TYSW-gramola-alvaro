import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../user';
import { Router } from '@angular/router';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css'
})
export class UsersListComponent implements OnInit {
  bares: any[] = [];
  err = '';

  constructor(private usersService: UserService, private router: Router) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.err = '';
    this.usersService.getAll().subscribe({
      next: (data) => {
        this.bares = data;
      },
      error: (e) => {
        this.err = "No se ha podido conectar con el servidor central.";
        console.error(e);
      }
    });
  }

  goToPayment() {
    this.router.navigate(['/payment']);
  }

  logout() {
    // Limpiamos los datos de sesión si existieran
    sessionStorage.clear();
    // Redirigimos al Login
    this.router.navigate(['/login']);
  }
}