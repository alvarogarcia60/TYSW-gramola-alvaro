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

import edu.uclm.es.gramola.model.Playlist;
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
    public List<Playlist> getMyPlaylist(@RequestParam String email) {
        return this.musicService.getMyPlaylist(email);
    }

    @PostMapping("/add")
    public void add(@RequestBody Map<String, Object> songData, @RequestParam String email) {
        this.musicService.addSong(songData, email);
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
    public void delete(@PathVariable Long id) {
        this.musicService.deleteSong(id);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices(@RequestParam String email) {
        return this.musicService.getDevices(email);
    }

    @GetMapping("/playback-state")
    public Map<String, Object> getPlaybackState(@RequestParam String email) {
        return this.musicService.getPlaybackState(email);
    }
}