/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package edu.uclm.es.gramola.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity  //Una entity debe tener un constructor vacío (sin parámetros)
public class Token {
    @Id @Column(length = 36)  //Porque se genera una cadena de 36 caracteres
    private String id;
    private long creationTime;
    private long useTime = 0;

    public Token() {
        this.id = UUID.randomUUID().toString();  //Cadena de 36 caracteres
        this.creationTime = System.currentTimeMillis();
    }

    public boolean isUsed() {
        return this.useTime > 0;
    }

    public void use() {
        this.useTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getUseTime() {
        return useTime;
    }

    public void setUseTime(long useTime) {
        this.useTime = useTime;
    }

    
}
