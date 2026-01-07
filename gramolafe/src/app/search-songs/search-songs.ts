import { Component, OnInit, OnDestroy } from '@angular/core'; // Corregido: importado de @angular/core
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MusicService, TrackObject } from '../services/music';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-search-songs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-songs.html',
  styleUrl: './search-songs.css'
})
export class SearchSongsComponent implements OnInit, OnDestroy {
  textoBusqueda = "";
  tracks: TrackObject[] = [];
  miPlaylist: any[] = [];
  emailBar: string = "";
  barName: string = "La Gramola"; 
  mensajeToast: string | null = null;
  confirmacionPago: string | null = null;
  
  // Requisito Sección 4.1 y Figura 26 [cite: 228]
  devices: any[] = [];
  currentDevice: any = null;
  deviceError: string | null = null;

  isAdmin: boolean = false; 
  progreso = 0;
  isPaused = false;
  private refreshTimer: any;
  private progressTimer: any;

  constructor(
    private musicService: MusicService, 
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    // Detectamos si es cliente por el parámetro en la URL [cite: 184]
    const emailParam = this.route.snapshot.paramMap.get('barEmail');
    
    if (emailParam) {
      this.emailBar = emailParam;
      this.isAdmin = false;
    } else {
      // Si es Admin, recuperamos el email logueado [cite: 149]
      this.emailBar = sessionStorage.getItem("emailLogeado") || "";
      this.isAdmin = true;
      this.cargarDispositivos(); // Sección 4.1: Carga inicial de dispositivos [cite: 227]
    }

    // Feedback de pago local para el cliente [cite: 132]
    const lastPay = sessionStorage.getItem("lastPayment");
    if (lastPay) {
      const info = JSON.parse(lastPay);
      this.confirmacionPago = `¡Tu canción "${info.titulo}" ha sido enviada a la cola!`;
      setTimeout(() => {
        this.confirmacionPago = null;
        sessionStorage.removeItem("lastPayment");
      }, 8000);
    }

    this.cargarPlaylist();
    this.refreshTimer = setInterval(() => { this.cargarPlaylist(); }, 5000);
    
    this.progressTimer = setInterval(() => {
      if (this.miPlaylist.length > 0 && !this.isPaused) {
        this.progreso += (100 / 180); 
        if (this.progreso >= 100) this.progreso = 0;
      }
    }, 1000);
  }

  ngOnDestroy() {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
    if (this.progressTimer) clearInterval(this.progressTimer);
  }

  // Sección 4.1.1: Carga de dispositivos desde Spotify [cite: 233, 240]
  cargarDispositivos() {
    this.musicService.getDevices(this.emailBar).subscribe({
      next: (result: any) => {
        this.devices = result.devices || [];
        // Buscar el dispositivo activo según Figura 30 [cite: 247]
        this.currentDevice = this.devices.find((d: any) => d.is_active);
        
        if (!this.currentDevice) {
          this.deviceError = "No hay ningún dispositivo conectado"; // Mensaje Figura 30 
        } else {
          this.deviceError = null;
        }
      },
      error: (err) => {
        this.deviceError = err.message; // Gestión de errores Figura 27 [cite: 233]
      }
    });
  }

  seleccionarDispositivo(event: any) {
    const deviceId = event.target.value;
    // Requisito: El bar selecciona un dispositivo de salida [cite: 43]
    this.musicService.setDevice(this.emailBar, deviceId).subscribe(() => {
      this.cargarDispositivos();
    });
  }

  togglePlayPause() {
    if (this.isAdmin) {
      this.musicService.toggleReproduccion(this.emailBar).subscribe(() => {
        this.isPaused = !this.isPaused;
      });
    }
  }

  nextSong() {
    if (this.miPlaylist.length > 0) {
      this.musicService.deleteSong(this.miPlaylist[0].id).subscribe(() => {
        this.progreso = 0;
        this.cargarPlaylist();
      });
    }
  }

  cargarPlaylist() {
    this.musicService.getPlaylist(this.emailBar).subscribe((data: any) => {
      const nuevasTracks = data.tracks || data; 
      if (nuevasTracks.length < this.miPlaylist.length) this.progreso = 0;
      this.miPlaylist = nuevasTracks;
      this.isPaused = data.isPlaying !== undefined ? !data.isPlaying : this.isPaused;
      
      // Identidad dinámica: Recuperar nombre del bar [cite: 33, 72]
      if (data.barName) this.barName = `La Gramola de ${data.barName}`;
    });
  }

  // Alias explícito para requisito 4.4
  getCurrentPlayList() {
    this.cargarPlaylist();
  }

  buscar() {
    if (!this.textoBusqueda || this.textoBusqueda.trim().length < 3) return;
    this.musicService.search(this.textoBusqueda, this.emailBar).subscribe({
      next: (items: any[]) => {
        // Mapear a TrackObject[]
        this.tracks = (items || []).map((it: any) => {
          const cover = it?.album?.images?.[0]?.url || '';
          const artist = it?.artists?.[0]?.name || 'Artista desconocido';
          return {
            id: it?.id,
            name: it?.name,
            uri: it?.uri,
            artistName: artist,
            coverUrl: cover,
            raw: it
          } as TrackObject;
        });
      },
      error: () => this.mensajeToast = "El servicio de este bar no está activo."
    });
  }

  addSongToQueue(track: TrackObject) {
    if (this.isAdmin) {
      // El dueño añade canciones gratis [cite: 43]
      this.musicService.addSong(track.raw, this.emailBar).subscribe({
        next: () => {
          this.mensajeToast = "Canción añadida gratis por el dueño";
          this.cargarPlaylist();
          this.tracks = [];
          setTimeout(() => this.mensajeToast = null, 3000);
        },
        error: () => alert("Error al añadir. Revisa tu suscripción.")
      });
    } else {
      // El cliente debe pagar para añadir (Sección 2.4 y 4.8) [cite: 48, 105]
      sessionStorage.setItem("pendingSong", JSON.stringify(track.raw));
      this.router.navigate(['/payment'], { queryParams: { email: this.emailBar } });
    }
  }

  eliminarCancion(id: number) {
    this.musicService.deleteSong(id).subscribe(() => this.cargarPlaylist());
  }
}