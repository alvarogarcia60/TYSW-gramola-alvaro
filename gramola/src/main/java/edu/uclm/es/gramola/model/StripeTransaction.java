package edu.uclm.es.gramola.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class StripeTransaction {

    @Id
    private String id;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String data;

    private String email;
    
    private long paymentDate; // Timestamp de cuándo se realizó el pago

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public long getPaymentDate() { return paymentDate; }
    public void setPaymentDate(long paymentDate) { this.paymentDate = paymentDate; }
}
