import { Component, OnInit, OnDestroy } from '@angular/core'; 
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MusicService, TrackObject } from '../services/music';
import { UserService } from '../services/user.service';
import { GeolocationService } from '../services/geolocation.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-search-songs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-songs.html',
  styleUrls: ['./search-songs.css', './search-songs-responsive.css']
})
export class SearchSongsComponent implements OnInit, OnDestroy {
  textoBusqueda = "";
  tracks: TrackObject[] = [];
  miPlaylist: any[] = [];
  emailBar: string = "";
  barName: string = "La Gramola";
  mensajeToast: string | null = null;
  confirmacionPago: string | null = null;

  // Dispositivos y estado del reproductor
  devices: any[] = [];
  currentDevice: any = null;
  deviceError: string | null = null;
  subscriptionStatus: any = null;
  subscriptionWarning: string | null = null;
  nowPlaying: { title: string; artist: string } | null = null;
  nowPlayingCoverUrl: string | null = null;
  nowPlayingProgressMs: number | null = null;
  nowPlayingDurationMs: number | null = null;

  isAdmin: boolean = false;
  progreso = 0;
  isPaused = false;
  mobileMenuOpen = false;
  private refreshTimer: any;
  private progressTimer: any;
  private playbackTimer: any;
  private autoAdvanceInProgress = false;
  private lastAutoAdvancedId: number | null = null;

  constructor(
    private musicService: MusicService,
    private userService: UserService,
    private geolocationService: GeolocationService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const emailParam = this.route.snapshot.paramMap.get('barEmail');

    if (emailParam) {
      // Cliente solicitó música en el jukebox
      this.emailBar = emailParam;
      this.isAdmin = false;
      this.verificarGeolocalización(emailParam);
    } else {
      // Administrador del bar
      this.emailBar = sessionStorage.getItem("emailLogeado") || "";
      if (!this.emailBar) {
        const userStr = sessionStorage.getItem("user");
        if (userStr) {
          try {
            const user = JSON.parse(userStr);
            this.emailBar = user?.email || "";
          } catch {}
        }
      }
      this.isAdmin = true;
      this.cargarDispositivos();
      this.checkSubscription();
      this.loadPlaybackState();
    }

    // Mostrar confirmación de pago si existe
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
    this.playbackTimer = setInterval(() => { this.loadPlaybackState(); }, 5000);

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
    if (this.playbackTimer) clearInterval(this.playbackTimer);
  }

  // Cargar dispositivos disponibles desde Spotify
  cargarDispositivos() {
    this.musicService.getDevices(this.emailBar).subscribe({
      next: (result: any) => {
        this.devices = result.devices || [];
        // Buscar el dispositivo activo
        this.currentDevice = this.devices.find((d: any) => d.is_active);

        if (!this.currentDevice) {
          this.deviceError = "No hay ningún dispositivo conectado";
        } else {
          this.deviceError = null;
        }
      },
      error: (err) => {
        this.deviceError = err.message;
      }
    });
  }

  seleccionarDispositivo(event: any) {
    const deviceId = event.target.value;
    // Requisito: El bar selecciona un dispositivo de salida
    this.musicService.selectDevice(this.emailBar, deviceId).subscribe(() => {
      this.cargarDispositivos();
      this.mensajeToast = "Dispositivo actualizado";
      setTimeout(() => this.mensajeToast = null, 2000);
    });
  }

  checkSubscription() {
    this.userService.getSubscriptionStatus(this.emailBar).subscribe({
      next: (status: any) => {
        this.subscriptionStatus = status;
        this.subscriptionWarning = !status?.active
          ? "⚠️ Tu suscripción ha expirado. Renueva para continuar usando la Gramola."
          : null;
      },
      error: () => {
        this.subscriptionWarning = null;
      }
    });
  }

  play() {
    if (this.isAdmin) {
      this.musicService.play(this.emailBar).subscribe({
        next: () => {
          this.isPaused = false;
          this.mensajeToast = "Reproducción iniciada";
          setTimeout(() => this.mensajeToast = null, 2000);
        },
        error: () => {
          this.mensajeToast = "Error al reproducir. Verifica dispositivo Spotify.";
          setTimeout(() => this.mensajeToast = null, 3000);
        }
      });
    }
  }

  pause() {
    if (this.isAdmin) {
      this.musicService.pause(this.emailBar).subscribe({
        next: () => {
          this.isPaused = true;
          this.mensajeToast = "Reproducción pausada";
          setTimeout(() => this.mensajeToast = null, 2000);
        }
      });
    }
  }

  togglePlayPause() {
    if (this.isPaused) {
      this.play();
    } else {
      this.pause();
    }
  }

  nextSong() {
    if (!this.isAdmin) return;
    if (this.miPlaylist.length > 0) {
      this.musicService.deleteSong(this.miPlaylist[0].id, this.emailBar).subscribe(() => {
        this.progreso = 0;
        this.cargarPlaylist();
      });
    }
  }

  cargarPlaylist() {
    // Cargar la cola de la base de datos (la fuente de verdad para el frontend)
    this.musicService.getPlaylist(this.emailBar).subscribe({
      next: (data: any) => {
        console.log('📋 Cola de BD:', data);
        this.miPlaylist = data || [];
        const firstId = this.miPlaylist[0]?.id ?? null;
        if (firstId !== this.lastAutoAdvancedId) {
          this.autoAdvanceInProgress = false; // habilita avance automático para la nueva primera pista
        }
        
        if (this.miPlaylist.length < 1) {
          this.progreso = 0;
        }
      },
      error: (err: any) => {
        console.error('❌ Error al cargar cola de BD:', err);
        this.miPlaylist = [];
      }
    });
  }

  // Estado real del reproductor en Spotify
  loadPlaybackState() {
    this.musicService.getPlaybackState(this.emailBar).subscribe({
      next: (state: any) => {
        // Intentar mapear según estructura típica de Spotify o backend
        // 1) Spotify: state.item.name, state.item.artists[0].name, state.is_playing
        // 2) Backend propio: state.trackTitle, state.trackArtist, state.isPlaying
        const title = state?.item?.name || state?.trackTitle || null;
        const artist = (state?.item?.artists?.[0]?.name) || state?.trackArtist || null;
        if (title && artist) {
          this.nowPlaying = { title, artist };
        }
        // Portada
        const cover = state?.item?.album?.images?.[0]?.url || state?.coverUrl || null;
        this.nowPlayingCoverUrl = cover;
        // Progreso y duración
        const progressMs = (typeof state?.progress_ms === 'number') ? state.progress_ms : state?.progressMs;
        const durationMs = (typeof state?.item?.duration_ms === 'number') ? state.item.duration_ms : state?.durationMs;
        this.nowPlayingProgressMs = typeof progressMs === 'number' ? progressMs : null;
        this.nowPlayingDurationMs = typeof durationMs === 'number' ? durationMs : null;
        // Actualizar barra de progreso si tenemos datos reales
        if (this.nowPlayingProgressMs !== null && this.nowPlayingDurationMs) {
          const pct = (this.nowPlayingProgressMs / this.nowPlayingDurationMs) * 100;
          this.progreso = Math.max(0, Math.min(100, pct));
        }
        // Sincronizar pausa/reproducción si viene del backend o API
        if (typeof state?.is_playing === 'boolean') {
          this.isPaused = !state.is_playing;
        } else if (typeof state?.isPlaying === 'boolean') {
          this.isPaused = !state.isPlaying;
        }

        // Avance automático: si el primer tema está acabando, elimínalo de la cola/BD
        const firstTrackId = this.miPlaylist[0]?.id;
        if (this.isAdmin && firstTrackId && this.nowPlayingProgressMs !== null && this.nowPlayingDurationMs && this.nowPlayingDurationMs > 0) {
          const remainingMs = this.nowPlayingDurationMs - this.nowPlayingProgressMs;
          const nearTheEnd = remainingMs <= 5000; // 5 segundos antes del final
          
          // Eliminar si está cerca del final o si ya terminó
          if (!this.autoAdvanceInProgress && nearTheEnd) {
            this.autoAdvanceInProgress = true;
            this.lastAutoAdvancedId = firstTrackId;
            
            console.log('🔄 Auto-advance: Eliminando canción finalizada de la cola');
            
            this.musicService.deleteSong(firstTrackId, this.emailBar).subscribe({
              next: () => {
                this.progreso = 0;
                this.cargarPlaylist();
                console.log('✅ Canción eliminada automáticamente');
              },
              error: (err) => {
                console.error('❌ Error al eliminar canción:', err);
                this.autoAdvanceInProgress = false;
              }
            });
          }
        }
      },
      error: () => {
        // En caso de error, mantener último valor visible
      }
    });
  }

  // Utilidad: formatear milisegundos a mm:ss
  formatMs(ms: number | null | undefined): string {
    if (typeof ms !== 'number' || ms < 0) return '--:--';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
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

  // Borrado admin: solo afecta Gramola/BD (no Spotify)
  eliminarCancion(id: number) {
    if (!this.isAdmin) return;
    const ok = confirm('¿Eliminar la canción solo en Gramola/BD? Spotify no se modifica.');
    if (!ok) return;
    this.musicService.deleteSong(id, this.emailBar).subscribe({
      next: () => {
        this.mensajeToast = 'Canción eliminada en Gramola';
        this.cargarPlaylist();
        setTimeout(() => this.mensajeToast = null, 2000);
      },
      error: () => {
        this.mensajeToast = 'No se pudo eliminar en Gramola';
        setTimeout(() => this.mensajeToast = null, 2000);
      }
    });
  }

  // Limpieza masiva admin: solo Gramola/BD
  clearQueue() {
    if (!this.isAdmin) return;
    const ok = confirm('¿Limpiar toda la cola en Gramola/BD? Spotify seguirá intacto.');
    if (!ok) return;
    this.musicService.clearQueue(this.emailBar).subscribe({
      next: () => {
        this.mensajeToast = 'Cola limpiada en Gramola';
        this.cargarPlaylist();
        setTimeout(() => this.mensajeToast = null, 2000);
      },
      error: () => {
        this.mensajeToast = 'No se pudo limpiar la cola';
        setTimeout(() => this.mensajeToast = null, 2000);
      }
    });
  }

  // Geolocation verification for clients (bonus feature: 100m radius check)
  verificarGeolocalización(email: string) {
    this.geolocationService.getBarLocation(email).subscribe({
      next: async (bar: any) => {
        if (bar.latitude && bar.longitude) {
          this.geolocationService.isWithinRadius(bar.latitude, bar.longitude)
            .then(async (isInside) => {
              if (!isInside) {
                const distance = await this.calculateDisplayDistance(bar.latitude, bar.longitude);
                this.subscriptionWarning = `⚠️ Estás a ${distance}m del bar. Debes estar dentro de 100m para escuchar.`;
              }
            })
            .catch((err) => {
              console.error('Geolocation error:', err);
              this.subscriptionWarning = '⚠️ No se pudo verificar tu ubicación. Activa los permisos de localización.';
            });
        }
      },
      error: (err) => {
        console.error('Error getting bar location:', err);
        // No mostrar warning si falla - solo validar si podemos
      }
    });
  }

  // Helper to calculate distance for display
  private async calculateDisplayDistance(barLat: number, barLon: number): Promise<number> {
    if (navigator.geolocation) {
      return new Promise<number>((resolve) => {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            const distance = this.haversineDistance(
              pos.coords.latitude,
              pos.coords.longitude,
              barLat,
              barLon
            );
            resolve(Math.round(distance));
          },
          () => resolve(0)
        );
      });
    }
    return 0;
  }

  // Haversine formula to calculate distance in meters
  private haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371000; // Earth radius in meters
    const toRad = (deg: number) => deg * (Math.PI / 180);
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}