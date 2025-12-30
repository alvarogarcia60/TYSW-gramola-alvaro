package edu.uclm.es.gramola.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class StripeTransaction {
    @Id
    private String id; // El ID que nos da Stripe (pi_...)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String data; // Guardaremos el JSON completo de la transacción

    private String email; // Email del bar que realiza el pago

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}