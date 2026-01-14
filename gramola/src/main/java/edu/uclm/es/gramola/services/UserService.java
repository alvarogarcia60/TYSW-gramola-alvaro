package edu.uclm.es.gramola.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    @Autowired private UserDao userRepo;
    @Autowired private TokenDao tokenRepo; 
    @Autowired private StripeTransactionDao stripeRepo;
    @Autowired private JavaMailSender mailSender;
    @Autowired private MusicService musicService;
    @Autowired private RestTemplate restTemplate;

    // REGISTRO: Crea usuario y envía email con token (Figura 13, Paso 7)
    public void register(String bar, String email, String pwd, String clientId, String clientSecret, String address, String signatureBase64) {
        userRepo.deleteById(Objects.requireNonNull(email, "El email no puede ser null"));

        User user = new User();
        user.setEmail(email);
        user.setBar(bar);
        
        // CORRECCIÓN: Pasa 'pwd' directamente. 
        // El setter de User.java ya se encarga de llamar a encryptPassword.
        user.setPwd(pwd); 
        
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);
        user.setAddress(address);
        
        // Obtener coordenadas desde Nominatim si se proporciona dirección
        if (address != null && !address.isEmpty()) {
            Map<String, Object> coords = getCoordinatesFromAddress(address);
            if (coords != null) {
                user.setLatitude((Double) coords.get("latitude"));
                user.setLongitude((Double) coords.get("longitude"));
                System.out.println("✅ Coordenadas obtenidas para " + bar + ": " + coords.get("latitude") + ", " + coords.get("longitude"));
            }
        }
        
        // Guardar firma si se proporciona (solo para bars)
        if (signatureBase64 != null && !signatureBase64.isEmpty()) {
            try {
                byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
                user.setSignature(signatureBytes);
                System.out.println("✅ Firma guardada para " + bar);
            } catch (IllegalArgumentException e) {
                System.err.println("⚠️ Error decodificando firma Base64: " + e.getMessage());
            }
        }
        
        user.setPaid(false);
        user.setExpirationDate(0L); 
        user.setPlaying(true); 

        Token token = new Token();
        token.setId(UUID.randomUUID().toString()); 
        token.setCreationTime(System.currentTimeMillis()); 
        
        tokenRepo.save(token); 
        user.setCreationToken(token);
        userRepo.save(user); 

        sendEmail(email, token.getId());
    }

    // Obtener coordenadas desde Nominatim (OpenStreetMap)
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCoordinatesFromAddress(String address) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + 
                        java.net.URLEncoder.encode(address, "UTF-8") + 
                        "&format=json&limit=1";
            
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<Map<String, Object>> results = response.getBody();
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> result = results.get(0);
                Map<String, Object> coords = new java.util.HashMap<>();
                coords.put("latitude", Double.parseDouble(result.get("lat").toString()));
                coords.put("longitude", Double.parseDouble(result.get("lon").toString()));
                return coords;
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo coordenadas de Nominatim: " + e.getMessage());
        }
        return null;
    }

    private void sendEmail(String email, String tokenId) {
        // 1. Construimos la URL de confirmación
        String urlConfirmacion = "http://localhost:8080/users/confirmToken/" + email + "?token=" + tokenId;

        // 2. LO NUEVO: Imprimir en la consola del Backend (terminal de VS Code)
        System.out.println("------------------------------------------------------------");
        System.out.println("📩 ENLACE DE CONFIRMACIÓN PARA: " + email);
        System.out.println(urlConfirmacion);
        System.out.println("------------------------------------------------------------");

        // 3. Envío real por correo electrónico
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("algarcimartinez@gmail.com"); 
            message.setTo(email);
            message.setSubject("Confirma tu cuenta en La Gramola");
            message.setText("Bienvenido a La Gramola. Confirma y paga aquí: " + urlConfirmacion);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo: " + e.getMessage());
        }
    }

    public void confirmToken(String email, String tokenId) {
        User user = userRepo.findById(email).orElseThrow();
        Token creationToken = user.getCreationToken();
        if (!creationToken.getId().equals(tokenId)) throw new RuntimeException("Token inválido");
        creationToken.setUseTime(System.currentTimeMillis());
        tokenRepo.save(creationToken);
    }

    // PAGO: Activa la cuenta o mete canción en cola tras confirmar con Stripe
    public void saveStripeTransaction(String email, Map<String, Object> data) {
        StripeTransaction st = new StripeTransaction();
        st.setId(String.valueOf(data.getOrDefault("tokenId", UUID.randomUUID().toString())));
        st.setEmail(email);
        st.setData(data.toString());
        st.setPaymentDate(System.currentTimeMillis()); // Registrar timestamp del pago
        this.stripeRepo.save(st);

        // Si el pago incluye datos de canción, va a la Rockola (Sección 4.6)
        if (data.get("songTitle") != null) {
            Map<String, Object> songMap = new java.util.HashMap<>();
            songMap.put("name", data.get("songTitle"));
            songMap.put("artists", java.util.List.of(java.util.Map.of("name", data.get("songArtist"))));
            songMap.put("album", java.util.Map.of("images", java.util.List.of(java.util.Map.of("url", data.get("songCover")))));
            this.musicService.addSong(songMap, email);
        } else {
            // Si es pago de suscripción del bar (Figura 13, Paso 25)
            User user = userRepo.findById(email).orElse(null);
            if (user != null) {
                // Determinar duración según plan (mensual/anual) y extender desde la fecha de expiración vigente
                String planId = data.getOrDefault("planId", "").toString().toUpperCase();
                long now = System.currentTimeMillis();
                long currentExpiration = user.getExpirationDate();
                long start = Math.max(now, currentExpiration); // si está activa, empieza a contar desde su expiración actual

                // Por defecto 30 días (mensual). Si detectamos anual, sumamos 365 días.
                long durationMs = 30L * 24 * 60 * 60 * 1000;
                if (planId.contains("ANUAL") || planId.contains("ANUALIDAD") || planId.contains("ANUALIDAD")) {
                    durationMs = 365L * 24 * 60 * 60 * 1000;
                }

                user.setPaid(true);
                user.setExpirationDate(start + durationMs);
                userRepo.save(user);
            }
        }
    }

    public User login(String email, String pwd) {
        User user = userRepo.findById(email).orElseThrow(() -> new RuntimeException("No existe el usuario"));
        if (!user.getPwd().equals(user.encryptPassword(pwd))) throw new RuntimeException("Password incorrecta");
        if (user.getCreationToken() == null || user.getCreationToken().getUseTime() == 0) 
            throw new RuntimeException("Confirma tu email primero");
        return user;
    }

    @SuppressWarnings("unchecked")
    public boolean getAuthorizationToken(String code, String email) {
        try {
            User user = userRepo.findById(email).orElseThrow();
            String url = "https://accounts.spotify.com/api/token";
            String redirectUri = "http://127.0.0.1:4200/callback";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("grant_type", "authorization_code");
            form.add("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = user.getClientId() + ":" + user.getClientSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>)(ResponseEntity<?>)restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("access_token")) {
                user.setSpotiSimpleToken(body.get("access_token").toString());
                userRepo.save(user);
                System.out.println("✅ Token de Spotify guardado para: " + email);
                return true;
            } else {
                System.err.println("❌ No access_token en respuesta de Spotify para: " + email);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error OAuth: " + e.getMessage());
            return false;
        }
    }

    // RECUPERAR CONTRASEÑA: Envía email con token para resetear contraseña
    public void forgotPassword(String email) {
        User user = userRepo.findById(email).orElse(null);
        if (user == null) {
            // Por seguridad, no decimos si el email existe o no
            System.out.println("⚠️ Intento de recuperación para email no registrado: " + email);
            return;
        }

        Token resetToken = new Token();
        resetToken.setId(UUID.randomUUID().toString());
        resetToken.setCreationTime(System.currentTimeMillis());
        tokenRepo.save(resetToken);

        String urlReset = "http://127.0.0.1:4200/reset-password?email=" + email + "&token=" + resetToken.getId();

        System.out.println("------------------------------------------------------------");
        System.out.println("🔐 ENLACE DE RECUPERACIÓN PARA: " + email);
        System.out.println(urlReset);
        System.out.println("------------------------------------------------------------");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("algarcimartinez@gmail.com");
            message.setTo(email);
            message.setSubject("Recupera tu contraseña en La Gramola");
            message.setText("Haz clic aquí para cambiar tu contraseña: " + urlReset + "\n\n" +
                           "Este enlace expira en 24 horas.\n" +
                           "Si no solicitaste esto, ignora este correo.");
            mailSender.send(message);
            System.out.println("✅ Email de recuperación enviado a: " + email);
        } catch (Exception e) {
            System.err.println("❌ Error al enviar email de recuperación: " + e.getMessage());
        }
    }

    // Cambiar contraseña con token válido
    public boolean resetPassword(String email, String token, String newPassword) {
        User user = userRepo.findById(email).orElse(null);
        if (user == null) {
            System.err.println("❌ Usuario no encontrado: " + email);
            return false;
        }

        Token resetToken;
        try {
            resetToken = tokenRepo.findById(token).orElse(null);
        } catch (Exception e) {
            System.err.println("❌ Token inválido: " + token);
            return false;
        }

        if (resetToken == null) {
            System.err.println("❌ Token no encontrado: " + token);
            return false;
        }

        // Verificar que el token no haya expirado (24 horas = 86400000 ms)
        long tokenAge = System.currentTimeMillis() - resetToken.getCreationTime();
        if (tokenAge > 86400000L) {
            System.err.println("❌ Token expirado para: " + email);
            return false;
        }

        // Verificar que el token no haya sido usado antes
        if (resetToken.isUsed()) {
            System.err.println("❌ Token ya fue usado: " + token);
            return false;
        }

        // Cambiar contraseña y marcar token como usado
        user.setPwd(newPassword);
        resetToken.use();

        userRepo.save(user);
        tokenRepo.save(resetToken);

        System.out.println("✅ Contraseña cambiada para: " + email);
        return true;
    }

    // Obtener historial de transacciones del usuario (ordenado por fecha de pago, más recientes primero)
    public List<StripeTransaction> getTransactionHistory(String email) {
        return stripeRepo.findByEmailOrderByPaymentDateDesc(email);
    }

    // Obtener estado de suscripción del usuario
    public Map<String, Object> getSubscriptionStatus(String email) {
        Map<String, Object> status = new java.util.HashMap<>();
        try {
            User user = userRepo.findById(email).orElseThrow();
            long now = System.currentTimeMillis();
            long expirationDate = user.getExpirationDate();
            
            boolean isActive = user.isPaid() && expirationDate > now;
            long daysRemaining = isActive ? (expirationDate - now) / (1000 * 60 * 60 * 24) : 0;
            
            status.put("email", email);
            status.put("bar", user.getBar());
            status.put("isPaid", user.isPaid());
            status.put("active", isActive);
            status.put("expirationDate", expirationDate);
            status.put("daysRemaining", daysRemaining);
            
            return status;
        } catch (Exception e) {
            status.put("error", "Usuario no encontrado");
            return status;
        }
    }

    // Renovar suscripción (mensual o anual)
    public Map<String, Object> renewSubscription(String email, String plan) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            User user = userRepo.findById(email).orElseThrow();
            long now = System.currentTimeMillis();
            long currentExpiration = user.getExpirationDate();
            long start = Math.max(now, currentExpiration); // si está activa, empieza a contar desde su expiración actual

            long durationMs = 30L * 24 * 60 * 60 * 1000; // Mensual por defecto
            if (plan != null && plan.toLowerCase().contains("anual")) {
                durationMs = 365L * 24 * 60 * 60 * 1000;
            }

            user.setPaid(true);
            user.setExpirationDate(start + durationMs);
            userRepo.save(user);

            // Registrar transacción de renovación
            StripeTransaction st = new StripeTransaction();
            st.setId(UUID.randomUUID().toString());
            st.setEmail(email);
            st.setData("Renovación de suscripción: " + plan);
            st.setPaymentDate(System.currentTimeMillis());
            stripeRepo.save(st);

            result.put("success", true);
            result.put("message", "Suscripción renovada");
            result.put("newExpirationDate", user.getExpirationDate());
            result.put("plan", plan);
            
            System.out.println("✅ Suscripción renovada para " + email + " - Plan: " + plan);
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            System.err.println("❌ Error renovando suscripción para " + email + ": " + e.getMessage());
            return result;
        }
    }
}