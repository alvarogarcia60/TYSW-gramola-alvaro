package edu.uclm.es.gramola.model;

public class TransactionRequest {
    private String tokenId;
    private String email;
    private int amount; // En céntimos
    private String songTitle;
    private String songArtist;
    private String songCover;

    // Getters y Setters
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getSongTitle() { return songTitle; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }
    public String getSongArtist() { return songArtist; }
    public void setSongArtist(String songArtist) { this.songArtist = songArtist; }
    public String getSongCover() { return songCover; }
    public void setSongCover(String songCover) { this.songCover = songCover; }
}