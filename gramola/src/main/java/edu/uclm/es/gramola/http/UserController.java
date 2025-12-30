package edu.uclm.es.gramola.http;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String bar = body.get("bar");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        // Validaciones obligatorias
        if (email == null || pwd1 == null || bar == null || clientId == null || clientSecret == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos");

        if (!pwd1.equals(pwd2))
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Las contraseñas no coinciden");

        this.service.register(bar, email, pwd1, clientId, clientSecret);
    }

    @GetMapping("/confirmToken/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        // Valida el token (si no es válido, tu servicio debería lanzar una excepción)
        this.service.confirmToken(email, token);

        // Redirigimos al puerto 4200 (Angular)
        // Pasamos ambos parámetros para facilitar la vida al componente de pago
        response.sendRedirect("http://localhost:4200/payment?token=" + token + "&email=" + email);
    }
}