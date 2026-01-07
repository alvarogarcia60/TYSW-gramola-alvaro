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

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;

import edu.uclm.es.gramola.dao.PlaylistDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Playlist;
import edu.uclm.es.gramola.model.User;
import jakarta.annotation.PostConstruct;

@Service
public class MusicService {

    @Autowired private UserDao userRepo;
    @Autowired private PlaylistDao playlistRepo;
    @Autowired private RestTemplate restTemplate;

    @Value("${spotify.clientId:}")
    private String spotifyClientId;

    @Value("${spotify.clientSecret:}")
    private String spotifyClientSecret;

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecret;
    }

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

        if (currentQueue.isEmpty()) {
            newSong.setQueuePosition(1);
        } else {
            newSong.setQueuePosition(2);
            for (Playlist p : currentQueue) {
                if (p.getQueuePosition() >= 2) {
                    p.setQueuePosition(p.getQueuePosition() + 1);
                    this.playlistRepo.save(p);
                }
            }
        }
        
        this.playlistRepo.save(newSong);
        
        if (spotifyUri != null && !spotifyUri.isEmpty()) {
            addToSpotifyQueue(email, spotifyUri);
        }
    }

    @Scheduled(fixedRate = 180000)
    public void simulatePlayback() {
        Iterable<User> users = this.userRepo.findAll();
        for (User user : users) {
            String email = user.getEmail();
            if (email != null) this.playNext(email);
        }
    }

    public void playNext(String email) {
        List<Playlist> queue = this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
        if (queue != null && !queue.isEmpty()) {
            Playlist currentSong = queue.get(0);
            this.playlistRepo.delete(currentSong);
        }
    }

    public void deleteSong(Long id) {
        this.playlistRepo.deleteById(Objects.requireNonNull(id));
        System.out.println("🗑️ Canción con ID " + id + " eliminada de la playlist.");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> search(String texto, String email) {
        String token = "";
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
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            if (body == null || !body.containsKey("tracks")) return new ArrayList<>();
            Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");
            return (List<Map<String, Object>>) tracks.get("items");
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public List<Playlist> getMyPlaylist(String email) {
        return this.playlistRepo.findByBarEmailOrderByQueuePositionAsc(Objects.requireNonNull(email));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String getAccessToken(String clientId, String clientSecret) {
        String url = "https://accounts.spotify.com/api/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(params, headers), Map.class);
            Map body = response.getBody();
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
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();
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
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            return body != null ? body : new HashMap<>();
        } catch (Exception e) { return new HashMap<>(); }
    }

    private void addToSpotifyQueue(String email, String spotifyUri) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String userToken = user.getSpotiSimpleToken();
            if (userToken == null) return;
            
            String url = "https://api.spotify.com/v1/me/player/queue?uri=" + spotifyUri;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userToken);
            restTemplate.postForEntity(url, new HttpEntity<>(headers), String.class);
        } catch (Exception e) { System.err.println("Error en cola: " + e.getMessage()); }
    }

    public boolean addSongPaid(Map<String, Object> songData, String email, String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if (intent != null && "succeeded".equalsIgnoreCase(intent.getStatus())) {
                addSong(songData, email);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error verificando pago: " + e.getMessage());
            return false;
        }
    }
}