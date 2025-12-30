import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // IMPORTANTE: Añade esta línea

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet], // IMPORTANTE: Añádelo a los imports
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  title = 'gramolafe';
}