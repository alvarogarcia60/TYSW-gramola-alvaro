package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "precios")
public class Precio {
    @Id
    private String id;
    private String nombre;
    private Double importe;
    private String descripcion;

    // Constructores, Getters y Setters
    public Precio() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}