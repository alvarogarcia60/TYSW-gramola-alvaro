package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String artist;
    private String coverUrl;
    private String barEmail;
    
    // Campo crítico para el requisito de "colarse"
    private int queuePosition;
    
    // URI de Spotify (formato: spotify:track:id) para añadir a la cola real
    private String spotifyUri;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getBarEmail() { return barEmail; }
    public void setBarEmail(String barEmail) { this.barEmail = barEmail; }
    
    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }
    
    public String getSpotifyUri() { return spotifyUri; }
    public void setSpotifyUri(String spotifyUri) { this.spotifyUri = spotifyUri; }
}