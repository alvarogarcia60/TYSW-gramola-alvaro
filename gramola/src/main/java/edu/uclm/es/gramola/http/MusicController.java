package edu.uclm.es.gramola.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.services.MusicService;

@RestController
@RequestMapping("music")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" }) // Cambia el "*" por esto
public class MusicController {

    @Autowired private MusicService musicService;

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String texto, @RequestParam(required = false) String email) {
        return this.musicService.search(texto, email);
    }

    @GetMapping("/getPlaylist")
    public ResponseEntity<?> getMyPlaylist(@RequestParam String email) {
        // Verificar suscripción activa
        Map<String, Object> subscription = this.musicService.checkSubscription(email);
        if (!(Boolean) subscription.get("active")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Suscripción expirada");
            error.put("message", "Tu suscripción ha expirado. Por favor renueva tu suscripción para continuar.");
            return ResponseEntity.status(403).body(error);
        }
        return ResponseEntity.ok(this.musicService.getMyPlaylist(email));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Map<String, Object> songData, @RequestParam String email) {
        // Verificar suscripción activa para el dueño del bar
        Map<String, Object> subscription = this.musicService.checkSubscription(email);
        if (!(Boolean) subscription.get("active")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Suscripción expirada");
            error.put("message", "La suscripción del bar ha expirado. El propietario debe renovarla.");
            return ResponseEntity.status(403).body(error);
        }
        this.musicService.addSong(songData, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add-paid")
    public ResponseEntity<Map<String, String>> addPaid(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? body.get("email").toString() : null;
        String paymentIntentId = body.get("paymentIntentId") != null ? body.get("paymentIntentId").toString() : null;
        Object songDataObj = body.get("songData");

        System.out.println("📥 Recibida solicitud add-paid. Email: " + email + ", PaymentIntent: " + paymentIntentId);

        Map<String, String> resp = new HashMap<>();
        if (!(songDataObj instanceof Map) || email == null || paymentIntentId == null) {
            System.err.println("⚠️ Parámetros inválidos en /add-paid");
            resp.put("success", "false");
            resp.put("message", "Parámetros inválidos");
            return ResponseEntity.badRequest().body(resp);
        }

        @SuppressWarnings("unchecked")
        boolean ok = this.musicService.addSongPaid((Map<String, Object>) songDataObj, email, paymentIntentId);
        if (ok) {
            System.out.println("✅ Canción añadida exitosamente");
            resp.put("success", "true");
            resp.put("message", "Canción añadida tras pago verificado");
            return ResponseEntity.ok(resp);
        } else {
            System.err.println("❌ Falló la verificación del pago");
            resp.put("success", "false");
            resp.put("message", "Pago no verificado o intent inválido");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @DeleteMapping("/delete-song/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, @RequestParam String email) {
        boolean ok = this.musicService.deleteSongForBar(id, email);
        Map<String, String> resp = new HashMap<>();
        resp.put("success", String.valueOf(ok));
        if (ok) {
            resp.put("message", "Canción eliminada para " + email);
            return ResponseEntity.ok(resp);
        } else {
            resp.put("message", "La canción no pertenece a " + email + " o no existe");
            return ResponseEntity.status(403).body(resp);
        }
    }

    @DeleteMapping("/clear-queue")
    public ResponseEntity<Map<String, String>> clearQueue(@RequestParam String email) {
        this.musicService.clearQueue(email);
        Map<String, String> resp = new HashMap<>();
        resp.put("success", "true");
        resp.put("message", "Cola borrada para " + email);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices(@RequestParam String email) {
        return this.musicService.getDevices(email);
    }

    @GetMapping("/playback-state")
    public Map<String, Object> getPlaybackState(@RequestParam String email) {
        return this.musicService.getPlaybackState(email);
    }

    @PostMapping("/play")
    public ResponseEntity<Map<String, String>> play(@RequestParam String email) {
        boolean ok = this.musicService.play(email);
        Map<String, String> resp = new HashMap<>();
        resp.put("success", String.valueOf(ok));
        resp.put("message", ok ? "Reproducción iniciada" : "Error al iniciar reproducción");
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pause(@RequestParam String email) {
        boolean ok = this.musicService.pause(email);
        Map<String, String> resp = new HashMap<>();
        resp.put("success", String.valueOf(ok));
        resp.put("message", ok ? "Reproducción pausada" : "Error al pausar reproducción");
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    @PostMapping("/select-device")
    public ResponseEntity<Map<String, String>> selectDevice(@RequestParam String email, @RequestParam String deviceId) {
        boolean ok = this.musicService.selectDevice(email, deviceId);
        Map<String, String> resp = new HashMap<>();
        resp.put("success", String.valueOf(ok));
        resp.put("message", ok ? "Dispositivo seleccionado" : "Error al seleccionar dispositivo");
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }

    @GetMapping("/check-subscription")
    public ResponseEntity<Map<String, Object>> checkSubscription(@RequestParam String email) {
        Map<String, Object> result = this.musicService.checkSubscription(email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/spotify-queue")
    public ResponseEntity<Map<String, Object>> getSpotifyQueue(@RequestParam String email) {
        Map<String, Object> result = this.musicService.getSpotifyQueue(email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync-queue")
    public ResponseEntity<Map<String, Object>> syncQueue(@RequestParam String email) {
        Map<String, Object> result = this.musicService.syncQueue(email);
        return ResponseEntity.ok(result);
    }
}