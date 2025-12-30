package edu.uclm.es.gramola.services;

import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    @Autowired
    private UserDao userRepo;

    @Autowired
    private TokenDao tokenRepo; 

    @Autowired
    private JavaMailSender mailSender;

    public void register(String bar, String email, String pwd, String clientId, String clientSecret) {
        // Escenario alternativo: si el bar existe pero no confirmó/pagó, se borra para reintentar
        userRepo.deleteById(Objects.requireNonNull(email, "El email no puede ser null"));

        User user = new User();
        user.setEmail(email);
        user.setBar(bar);
        user.setPwd(pwd); 
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);

        Token token = new Token();
        token.setId(UUID.randomUUID().toString()); 
        token.setCreationTime(System.currentTimeMillis()); 
        
        tokenRepo.save(token); 
        user.setCreationToken(token);
        userRepo.save(user); 

        // --- CAMBIO AQUÍ: IMPRIMIR EN CONSOLA Y NO ENVIAR MAIL ---
        String link = "http://localhost:8080/users/confirmToken/" + email + "?token=" + token.getId();
        System.out.println("\n************************************************************");
        System.out.println("COPIA ESTE ENLACE PARA PAGAR:");
        System.out.println(link);
        System.out.println("************************************************************\n");

        // Comenta la siguiente línea para que no falle el registro
        // sendEmail(email, token.getId());
    }

    private void sendEmail(String email, String tokenId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Confirma tu cuenta en La Gramola");
        message.setText("Bienvenido a La Gramola. Haz clic aquí para confirmar y pagar: " +
                "http://localhost:8080/users/confirmToken/" + email + "?token=" + tokenId);
        mailSender.send(message);
    }

    public void confirmToken(String email, String tokenId) {
        User user = userRepo.findById(Objects.requireNonNull(email, "El email no puede ser null"))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Token creationToken = Objects.requireNonNull(user.getCreationToken(), "Falta token de creación");
        if (!creationToken.getId().equals(tokenId)) {
            throw new RuntimeException("Token inválido");
        }
        
        creationToken.setUseTime(System.currentTimeMillis());
        tokenRepo.save(creationToken);
    }
}