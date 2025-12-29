package edu.uclm.es.gramola.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;


@Entity
public class User {
    @Id
    private String email;
    private String pwd;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, optional = false)
    @JoinColumn(name = "creation_token_id", referencedColumnName = "id")
    private Token creationToken;

    public void setPwd(String pwd) {
        this.pwd = this.encryptPassword(pwd);  //Guardarla encriptada
    }

    public String getPwd() {
        return pwd;  
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setCreationToken(Token token) {
        this.creationToken = token;
    }

    public Token getCreationToken() {
        return creationToken;
    }

    public void setPassword(String password) {
        this.pwd = encryptPassword(password);
    }

    public String encryptPassword(String password) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }

}
