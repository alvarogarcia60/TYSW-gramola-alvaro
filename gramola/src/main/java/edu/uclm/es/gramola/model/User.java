package edu.uclm.es.gramola.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    private String email;
    private String bar;
    private String pwd;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    // Campo crítico para el Flujo 2 de OAuth 2.0 (Acceso a recursos)
    @Column(name = "spoti_simple_token", columnDefinition = "TEXT")
    private String spotiSimpleToken;

    private boolean paid;

    @Column(name = "expiration_date")
    private long expirationDate;

    private boolean playing;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "signature", columnDefinition = "LONGBLOB")
    private byte[] signature;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "creation_token_id", referencedColumnName = "id")
    private Token creationToken;

    // --- GETTERS Y SETTERS ---

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getBar() { return bar; }
    public void setBar(String bar) { this.bar = bar; }

    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = encryptPassword(pwd); }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    // Getter y Setter para el token de Spotify (Soluciona errores de compilación)
    public String getSpotiSimpleToken() { return spotiSimpleToken; }
    public void setSpotiSimpleToken(String spotiSimpleToken) { this.spotiSimpleToken = spotiSimpleToken; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public long getExpirationDate() { return expirationDate; }
    public void setExpirationDate(long expirationDate) { this.expirationDate = expirationDate; }

    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public byte[] getSignature() { return signature; }
    public void setSignature(byte[] signature) { this.signature = signature; }

    public Token getCreationToken() { return creationToken; }
    public void setCreationToken(Token creationToken) { this.creationToken = creationToken; }

    // --- LÓGICA DE NEGOCIO ---

    public String encryptPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar", e);
        }
    }
}