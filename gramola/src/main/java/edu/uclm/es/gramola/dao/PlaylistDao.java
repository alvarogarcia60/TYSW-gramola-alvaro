package edu.uclm.es.gramola.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.Playlist;

@Repository
public interface PlaylistDao extends CrudRepository<Playlist, Long> {
    // Método principal: Ordenar por queuePosition es vital para la lógica de "colarse"
    List<Playlist> findByBarEmailOrderByQueuePositionAsc(String barEmail);
    
    // Método alternativo sin orden (por compatibilidad si se necesita)
    List<Playlist> findByBarEmail(String barEmail);
}