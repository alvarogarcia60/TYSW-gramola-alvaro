import { Component } from '@angular/core';
import { RegisterComponent } from './register/register';
import { LoginComponent } from './login/login';
import { UsersListComponent } from './users-list/users-list';
import { UserDetailComponent } from './user-detail/user-detail';
import { UserDeleteComponent } from './user-delete/user-delete';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RegisterComponent,
    LoginComponent,
    UsersListComponent,
    UserDetailComponent,
    UserDeleteComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
