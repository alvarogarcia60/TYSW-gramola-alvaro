package edu.uclm.es.gramola.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.stripe.model.PaymentIntent;

import edu.uclm.es.gramola.dao.PlaylistDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Playlist;
import edu.uclm.es.gramola.model.User;

@Service
public class MusicService {

    @Autowired private UserDao userRepo;
    @Autowired private PlaylistDao playlistRepo;
    @Autowired private RestTemplate restTemplate;

    @Value("${spotify.clientId:}")
    private String spotifyClientId;

    @Value("${spotify.clientSecret:}")
    private String spotifyClientSecret;

    public void addSong(Map<String, Object> songData, String email) {
        List<Playlist> currentQueue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
        
        Playlist newSong = new Playlist();
        newSong.setBarEmail(email);
        newSong.setTitle(Objects.toString(songData.get("name"), "Desconocido"));

        if (songData.get("artists") instanceof List<?> artistsList && !artistsList.isEmpty()) {
            Object first = artistsList.get(0);
            if (first instanceof Map<?, ?> firstArtist) {
                newSong.setArtist(Objects.toString(firstArtist.get("name"), "Artista Desconocido"));
            }
        }

        if (songData.get("album") instanceof Map<?, ?> albumMap) {
            Object imagesObj = albumMap.get("images");
            if (imagesObj instanceof List<?> images && !images.isEmpty()) {
                Object firstImg = images.get(0);
                if (firstImg instanceof Map<?, ?> imgMap) {
                    newSong.setCoverUrl(Objects.toString(imgMap.get("url"), ""));
                }
            }
        }
        
        String spotifyUri = Objects.toString(songData.get("uri"), null);
        newSong.setSpotifyUri(spotifyUri);

        // Encolar al final para reflejar el orden real de reproducción de Spotify
        newSong.setQueuePosition(currentQueue.size() + 1);
        
        this.playlistRepo.save(newSong);
        renormalizeQueue(email);
        
        if (spotifyUri != null && !spotifyUri.isEmpty()) {
            addToSpotifyQueue(email, spotifyUri);
        }
    }

    public void playNext(String email) {
        List<Playlist> queue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
        if (queue != null && !queue.isEmpty()) {
            Playlist currentSong = queue.get(0);
            this.playlistRepo.delete(currentSong);
            renormalizeQueue(email);
        }
    }

    public void deleteSong(Long id) {
        Objects.requireNonNull(id);
        // Recuperar información antes de borrar para intentar reflejarlo en Spotify
        Playlist toDelete = this.playlistRepo.findById(id).orElse(null);
        String email = toDelete != null ? toDelete.getBarEmail() : null;

        this.playlistRepo.deleteById(id);
        System.out.println("🗑️ Canción con ID " + id + " eliminada de la playlist.");

        if (email != null) {
            renormalizeQueue(email);
        }

        // Nota: la API de Spotify no permite eliminar elementos de la cola.
        // Por diseño, esta eliminación solo afecta a la Gramola/BD.
    }

    /**
     * Elimina una canción únicamente si pertenece al bar indicado por email.
     * Devuelve true si se eliminó; false si no pertenece o no existe.
     */
    public boolean deleteSongForBar(Long id, String email) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(email);
        Playlist toDelete = this.playlistRepo.findById(id).orElse(null);
        if (toDelete == null) {
            System.out.println("⚠️ Intento de borrar ID inexistente: " + id);
            return false;
        }
        if (!email.equals(toDelete.getBarEmail())) {
            System.out.println("⛔ ID " + id + " no pertenece a " + email + ", pertenece a " + toDelete.getBarEmail());
            return false;
        }
        this.playlistRepo.deleteById(id);
        System.out.println("🗑️ Canción con ID " + id + " eliminada de la playlist de " + email);
        renormalizeQueue(email);
        return true;
    }

    public void clearQueue(String email) {
        Objects.requireNonNull(email);
        this.playlistRepo.deleteByBarEmail(email);
        System.out.println("🧹 Cola borrada en BD para: " + email);
    }

    @SuppressWarnings("unchecked")
    private void rebuildSpotifyQueue(String email, String deletedUri) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) {
                System.err.println("❌ Token de Spotify nulo para: " + email);
                return;
            }

            // Obtener la cola actual de Spotify
            List<Map<String, Object>> spotifyQueue = getSpotifyQueueInternal(email, userToken);
            System.out.println("📊 Cola Spotify actual: " + spotifyQueue.size() + " canciones");
            
            // Obtener la cola de BD (excluyendo la canción eliminada)
            List<Playlist> dbQueue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(email);
            System.out.println("📊 Cola BD actual: " + dbQueue.size() + " canciones (antes de filtrar eliminada)");

            // Construir lista de URIs de BD, excluyendo la eliminada
            List<String> dbUris = new ArrayList<>();
            for (Playlist p : dbQueue) {
                String uri = p.getSpotifyUri();
                if (uri != null && !uri.isEmpty() && !uri.equals(deletedUri)) {
                    dbUris.add(uri);
                }
            }
            
            System.out.println("📊 URIs de BD a mantener: " + dbUris.size());

            // Sincronizar: mantener cola Spotify limpia eliminando canciones que ya pasaron o se borraron
            // Spotify no tiene API para eliminar de cola, así que lo mejor es solo loguear
            System.out.println("✅ Sincronización completada. Cola Spotify debe actualizarse cuando se reproduzca la próxima canción.");
            
        } catch (Exception e) {
            System.err.println("⚠️ Error en rebuildSpotifyQueue: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSpotifyQueueInternal(String email, String userToken) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String url = "https://api.spotify.com/v1/me/player/queue";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("queue")) {
                List<Map<String, Object>> queue = (List<Map<String, Object>>) body.get("queue");
                for (Map<String, Object> track : queue) {
                    result.add(track);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo obtener cola de Spotify: " + e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String texto, String email) {
        String token;
        try {
            if (email != null) {
                User user = userRepo.findById(email).orElse(null);
                if (user != null && user.getClientId() != null && user.getClientSecret() != null) {
                    token = getAccessToken(user.getClientId(), user.getClientSecret());
                } else {
                    token = getAccessToken(spotifyClientId, spotifyClientSecret);
                }
            } else {
                token = getAccessToken(spotifyClientId, spotifyClientSecret);
            }
        } catch (Exception e) {
            token = getAccessToken(spotifyClientId, spotifyClientSecret);
        }
        String url = "https://api.spotify.com/v1/search?q=" + texto + "&type=track&limit=10";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        try {
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("tracks")) return new ArrayList<>();
            Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");
            return (List<Map<String, Object>>) tracks.get("items");
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public List<Playlist> getMyPlaylist(String email) {
        return this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
    }

    @SuppressWarnings("unchecked")
    private String getAccessToken(String clientId, String clientSecret) {
        String url = "https://accounts.spotify.com/api/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        
        try {
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.postForEntity(url, new HttpEntity<>(params, headers), Map.class);
            Map<String, Object> body = response.getBody();
            return (body != null) ? Objects.toString(body.get("access_token"), "") : "";
        } catch (Exception e) { return ""; }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDevices(String email) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return new ArrayList<>();

            String url = "https://api.spotify.com/v1/me/player/devices";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            return (body != null) ? (List<Map<String, Object>>) body.get("devices") : new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlaybackState(String email) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return new HashMap<>();

            String url = "https://api.spotify.com/v1/me/player";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            return body != null ? body : new HashMap<>();
        } catch (Exception e) { return new HashMap<>(); }
    }

    private void addToSpotifyQueue(String email, String spotifyUri) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) {
                System.err.println("⚠️ Token de Spotify nulo, no se puede añadir a cola: " + spotifyUri);
                return;
            }
            
            String url = "https://api.spotify.com/v1/me/player/queue?uri=" + spotifyUri;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            System.out.println("🎵 Añadiendo a cola Spotify: " + spotifyUri);
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(headers), String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Canción añadida a cola Spotify: " + spotifyUri);
            } else {
                System.err.println("⚠️ Fallo al añadir a cola Spotify (status " + response.getStatusCode() + "): " + spotifyUri);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al añadir a cola Spotify: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public boolean addSongPaid(Map<String, Object> songData, String email, String paymentIntentId) {
        try {
            System.out.println("🔍 Verificando PaymentIntent: " + paymentIntentId);
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            System.out.println("✅ PaymentIntent recuperado. Estado: " + intent.getStatus());
            
            if ("succeeded".equalsIgnoreCase(intent.getStatus())) {
                System.out.println("💳 Pago verificado. Añadiendo canción...");
                addSong(songData, email);
                return true;
            }
            System.err.println("❌ Pago no completado. Estado: " + intent.getStatus());
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error verificando pago: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    // Control de reproductor
    public boolean play(String email) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return false;

            String url = "https://api.spotify.com/v1/me/player/play";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            restTemplate.put(url, new HttpEntity<>(headers), String.class);
            user.setPlaying(true);
            userRepo.save(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error al reproducir: " + e.getMessage());
            return false;
        }
    }

    public boolean pause(String email) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return false;

            String url = "https://api.spotify.com/v1/me/player/pause";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            restTemplate.put(url, new HttpEntity<>(headers), String.class);
            user.setPlaying(false);
            userRepo.save(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error al pausar: " + e.getMessage());
            return false;
        }
    }

    public boolean selectDevice(String email, String deviceId) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return false;

            String url = "https://api.spotify.com/v1/me/player";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("device_ids", List.of(deviceId));
            body.put("play", user.isPlaying());

            restTemplate.put(url, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception e) {
            System.err.println("Error al seleccionar dispositivo: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> checkSubscription(String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userRepo.findById(email).orElseThrow();
            long now = System.currentTimeMillis();
            long expirationDate = user.getExpirationDate();
            
            boolean isActive = user.isPaid() && expirationDate > now;
            long daysRemaining = isActive ? (expirationDate - now) / (1000 * 60 * 60 * 24) : 0;
            
            result.put("active", isActive);
            result.put("expirationDate", expirationDate);
            result.put("daysRemaining", daysRemaining);
            result.put("isPaid", user.isPaid());
            
            return result;
        } catch (Exception e) {
            result.put("active", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSpotifyQueue(String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            
            if (userToken == null) {
                System.out.println("⚠️ Token de Spotify nulo para: " + email);
                result.put("error", "Token de Spotify no disponible");
                result.put("tracks", new ArrayList<>());
                return result;
            }

            String url = "https://api.spotify.com/v1/me/player/queue";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            
            System.out.println("🔄 Obteniendo cola de Spotify para: " + email);
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> tracks = new ArrayList<>();
            
            if (body != null && body.containsKey("queue")) {
                List<Map<String, Object>> queue = (List<Map<String, Object>>) body.get("queue");
                System.out.println("📊 Cola de Spotify tiene " + queue.size() + " canciones");
                
                int trackId = 1;
                for (Map<String, Object> track : queue) {
                    Map<String, Object> trackInfo = new HashMap<>();
                    
                    // Generar ID secuencial ya que Spotify queue no lo incluye
                    trackInfo.put("id", trackId++);
                    trackInfo.put("title", track.get("name"));
                    trackInfo.put("spotifyId", track.get("id"));
                    
                    if (track.get("artists") instanceof List<?> artists && !artists.isEmpty()) {
                        Map<String, Object> artist = (Map<String, Object>) artists.get(0);
                        trackInfo.put("artist", artist.get("name"));
                    }
                    
                    trackInfo.put("uri", track.get("uri"));
                    
                    if (track.get("album") instanceof Map<?, ?> album) {
                        List<?> images = (List<?>) album.get("images");
                        if (images != null && !images.isEmpty()) {
                            Map<String, Object> image = (Map<String, Object>) images.get(0);
                            trackInfo.put("coverUrl", image.get("url"));
                        }
                    }
                    
                    tracks.add(trackInfo);
                }
            } else {
                System.out.println("⚠️ Body vacío o sin 'queue' en respuesta de Spotify");
            }
            
            result.put("tracks", tracks);
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo cola de Spotify: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            result.put("error", e.getMessage());
            result.put("tracks", new ArrayList<>());
            return result;
        }
    }

    public Map<String, Object> syncQueue(String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Obtener cola de BD
            List<Playlist> dbQueue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(email);
            
            // Obtener cola de Spotify
            Map<String, Object> spotifyQueueMap = getSpotifyQueue(email);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> spotifyTracks = (List<Map<String, Object>>) spotifyQueueMap.get("tracks");
            
            // Crear lista de URIs de Spotify
            List<String> spotifyUris = new ArrayList<>();
            for (Map<String, Object> track : spotifyTracks) {
                String uri = Objects.toString(track.get("uri"), null);
                if (uri != null) spotifyUris.add(uri);
            }
            
            // Sincronizar: eliminar de BD las que no estén en Spotify
            int removed = 0;
            for (Playlist p : dbQueue) {
                String dbUri = p.getSpotifyUri();
                if (dbUri != null && !spotifyUris.contains(dbUri)) {
                    System.out.println("🗑️ Removiendo de BD (no en Spotify): " + dbUri);
                    this.playlistRepo.delete(p);
                    removed++;
                }
            }
            
            System.out.println("✅ Sincronización completada. Removidas " + removed + " canciones.");

            // Reindexar la cola en BD para mantener un orden consecutivo
            renormalizeQueue(email);
            
            result.put("success", true);
            result.put("message", "Sincronización completada");
            result.put("removedCount", removed);
            result.put("dbQueueSize", dbQueue.size() - removed);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error sincronizando cola: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    // Sincroniza automáticamente la cola en BD con la reproducción real de Spotify
    // Elimina de la BD las canciones que ya han sido reproducidas (no reinicia la canción actual)
    @Scheduled(fixedRate = 7000)
    public void autoCleanupPlayedTracks() {
        try {
            Iterable<User> users = this.userRepo.findAll();
            for (User user : users) {
                String email = user.getEmail();
                String token = user.getSpotiSimpleToken();
                if (email == null || token == null) continue;

                List<Playlist> dbQueue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(email);
                if (dbQueue.isEmpty()) continue;

                String nowUri = getCurrentPlayingUri(token);
                if (nowUri == null || nowUri.isEmpty()) continue; // sin reproducción activa

                Playlist first = dbQueue.get(0);
                if (first.getSpotifyUri() != null && first.getSpotifyUri().equals(nowUri)) {
                    // la primera coincide con lo que está sonando: nada que hacer
                    continue;
                }

                // Buscar la posición del tema que está sonando en la cola de BD
                int idx = -1;
                for (int i = 0; i < dbQueue.size(); i++) {
                    String uri = dbQueue.get(i).getSpotifyUri();
                    if (uri != null && uri.equals(nowUri)) { idx = i; break; }
                }

                if (idx > 0) {
                    // Se han reproducido las canciones 0..idx-1; eliminarlas de la BD
                    for (int j = 0; j < idx; j++) {
                        this.playlistRepo.delete(dbQueue.get(j));
                    }
                    renormalizeQueue(email);
                    System.out.println("🧹 Auto-sync: eliminadas " + idx + " canciones reproducidas para " + email);
                }
                // Si idx == -1, el tema actual no está en la cola (reproducción manual); no tocamos la BD
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error en autoCleanupPlayedTracks: " + e.getMessage());
        }
    }

    private String getCurrentPlayingUri(String userToken) {
        try {
            String url = "https://api.spotify.com/v1/me/player";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) body.get("item");
            if (item == null) return null;
            Object uri = item.get("uri");
            return uri != null ? uri.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void renormalizeQueue(String email) {
        try {
            List<Playlist> queue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
            int pos = 1;
            for (Playlist p : queue) {
                if (p.getQueuePosition() != pos) {
                    p.setQueuePosition(pos);
                    this.playlistRepo.save(p);
                }
                pos++;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error renumerando cola para " + email + ": " + e.getMessage());
        }
    }
}