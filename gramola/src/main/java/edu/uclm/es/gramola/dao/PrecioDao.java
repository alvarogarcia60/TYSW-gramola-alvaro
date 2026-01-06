package edu.uclm.es.gramola.dao;

import org.springframework.data.repository.CrudRepository;

import edu.uclm.es.gramola.model.Precio;

public interface PrecioDao extends CrudRepository<Precio, String> {
}