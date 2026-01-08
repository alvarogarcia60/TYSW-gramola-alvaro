package edu.uclm.es.gramola.http;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("users")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class UserController {

    @Autowired private UserService service;
    @Autowired private UserDao userRepo;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        service.register(body.get("bar"), body.get("email"), body.get("pwd1"), 
                         body.get("clientId"), body.get("clientSecret"));
    }

    @GetMapping("/confirmToken/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        service.confirmToken(email, token);
        // Ayer usábamos 127.0.0.1 para que Spotify y Stripe no fallaran
        response.sendRedirect("http://127.0.0.1:4200/payment?token=" + token + "&email=" + email);
    }

    @PostMapping("/pay")
    public void pay(@RequestBody Map<String, Object> data) {
        service.saveStripeTransaction((String) data.get("email"), data);
    }

    @PostMapping("/login")
    public User login(@RequestBody Map<String, String> body) {
        return service.login(body.get("email"), body.get("password"));
    }

    @GetMapping("/loginSpotify")
    public void loginSpotify(HttpServletResponse response, @RequestParam String email) throws IOException {
        User user = userRepo.findById(email).orElseThrow();
        String redirectUri = "http://127.0.0.1:4200/callback";
        String scopes = "user-read-private user-read-email playlist-read-private user-read-playback-state user-modify-playback-state user-read-currently-playing streaming";
        
        String url = "https://accounts.spotify.com/authorize" +
                     "?response_type=code" +
                     "&client_id=" + user.getClientId() +
                     "&scope=" + java.net.URLEncoder.encode(scopes, "UTF-8") +
                     "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8") +
                     "&state=" + email;
        response.sendRedirect(url);
    }

    @GetMapping("/spotifyCallback")
    public ResponseEntity<Map<String, Object>> spotifyCallback(
            @RequestParam(required = false) String code, 
            @RequestParam(required = false) String email) {
        
        System.out.println("🔍 Callback recibido - code: " + (code != null ? "presente" : "null") + ", email: " + email);
        
        Map<String, Object> response = new java.util.HashMap<>();
        
        if (code == null || email == null) {
            System.err.println("❌ Faltan parámetros en el callback - code: " + code + ", email: " + email);
            response.put("success", false);
            response.put("message", "Missing parameters");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean saved = service.getAuthorizationToken(code, email);
        response.put("success", saved);
        response.put("message", saved ? "Token saved successfully" : "Failed to save token");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Map<String, String> response = new java.util.HashMap<>();
        
        if (email == null || email.trim().isEmpty()) {
            response.put("message", "Email es requerido");
            return ResponseEntity.badRequest().body(response);
        }
        
        service.forgotPassword(email);
        response.put("message", "Si el email existe en nuestro sistema, recibirás un enlace para recuperar tu contraseña");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        
        Map<String, String> response = new java.util.HashMap<>();
        
        if (email == null || token == null || newPassword == null || newPassword.trim().isEmpty()) {
            response.put("success", "false");
            response.put("message", "Faltan parámetros requeridos");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = service.resetPassword(email, token, newPassword);
        
        if (success) {
            response.put("success", "true");
            response.put("message", "Contraseña cambiada exitosamente");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", "false");
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/transactions/{email}")
    public ResponseEntity<Object> getTransactions(@PathVariable String email) {
        return ResponseEntity.ok(service.getTransactionHistory(email));
    }

    @GetMapping("/subscription-status/{email}")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(@PathVariable String email) {
        return ResponseEntity.ok(service.getSubscriptionStatus(email));
    }

    @PostMapping("/renew-subscription")
    public ResponseEntity<Map<String, Object>> renewSubscription(@RequestParam String email, @RequestParam(defaultValue = "monthly") String plan) {
        return ResponseEntity.ok(service.renewSubscription(email, plan));
    }
}