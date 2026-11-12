package com.universidad.catalogo.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.repository.ProductoRepository;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProductoRepository productoRepository;

    public DataSeeder(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(String... args) {
        if (productoRepository.count() == 0) {
            log.info("Perfil 'dev' detectado: Precargando catálogo inicial...");
            List<Producto> iniciales = List.of(
                    new Producto("Laptop Lenovo ThinkPad", 1299.99, "Computación"),
                    new Producto("Mouse Inalámbrico Logitech", 29.99, "Accesorios"),
                    new Producto("Monitor Dell 27 4K", 399.99, "Monitores")
            );
            productoRepository.saveAll(iniciales);
            log.info("Precarga completada: {} productos insertados.", iniciales.size());
        }
    }
}

