package edu.uclm.es.gramola.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    public void register(String bar, String email, String pwd, String clientId, String clientSecret) {
        userRepo.deleteById(Objects.requireNonNull(email, "El email no puede ser null"));

        User user = new User();
        user.setEmail(email);
        user.setBar(bar);
        
        // CORRECCIÓN: Pasa 'pwd' directamente. 
        // El setter de User.java ya se encarga de llamar a encryptPassword.
        user.setPwd(pwd); 
        
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);
        
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
                user.setPaid(true);
                user.setExpirationDate(System.currentTimeMillis() + 2592000000L); // 30 días
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

            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
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

        Token resetToken = null;
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
}