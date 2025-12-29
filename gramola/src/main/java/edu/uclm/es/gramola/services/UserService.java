package edu.uclm.es.gramola.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenDao tokenDao;

    private Map<String, User> users = new HashMap<>();

    public void register(String email, String pwd) {
        Optional<User> optUser = this.userDao.findById(email);
        if (optUser.isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setPwd(pwd);
            user.setCreationToken(new Token());
            this.userDao.save(user);
        }
        else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
    }

    public void delete(String email) {
        this.userDao.deleteById(email);
    }

    public void confirmToken(String email, String token) {
        User user = this.users.get(email);
        if(user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario");
        }
        Token userToken = user.getCreationToken();
        if(!userToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token incorrecto");
        }
        if(userToken.getCreationTime()<System.currentTimeMillis()-(60*1000*30)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token caducado");
        }
        if(userToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token ya usado");
        }
        userToken.use();
    }



}
