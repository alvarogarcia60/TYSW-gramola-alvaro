package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.services.UserService;

@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserService service;

    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");

        if(!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Passwords do not match");
        }

        if(pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Password must be at least 8 characters");
        }

        if(!email.contains("@") || !email.contains(".")) {
            throw new RuntimeException("Invalid email address");
        }
        this.service.register(email, pwd1);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }

    @GetMapping("/confirm/Token/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token) {
        this.service.confirmToken(email, token);
    }

    //HACER LOGIN
}