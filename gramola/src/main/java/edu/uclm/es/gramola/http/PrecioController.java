package edu.uclm.es.gramola.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.PrecioDao;
import edu.uclm.es.gramola.model.Precio;

@RestController
@RequestMapping("precios")
@CrossOrigin("*")
public class PrecioController {

    @Autowired
    private PrecioDao precioDao;

    @GetMapping("/lista")
    public Iterable<Precio> getListaPrecios() {
        return this.precioDao.findAll();
    }
}