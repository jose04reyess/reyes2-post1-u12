package com.universidad.catalogo.service;

import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto productoPrueba;

    @BeforeEach
    void setUp() {
        productoPrueba = new Producto(1L, "Teclado Mecánico RGB", 89.99, "Periféricos");
    }

    @Test
    @DisplayName("Debe guardar y retornar un nuevo producto exitosamente")
    void testCrearProducto() {
        when(productoRepository.save(any(Producto.class))).thenReturn(productoPrueba);

        Producto creado = productoService.crear(productoPrueba);

        assertNotNull(creado);
        assertEquals(1L, creado.getId());
        assertEquals("Teclado Mecánico RGB", creado.getNombre());
        assertEquals(89.99, creado.getPrecio());
        assertEquals("Periféricos", creado.getCategoria());
        verify(productoRepository, times(1)).save(productoPrueba);
    }

    @Test
    @DisplayName("Debe listar todos los productos del catálogo")
    void testListarTodos() {
        List<Producto> listaSimulada = List.of(
                productoPrueba,
                new Producto(2L, "Mouse Gamer", 45.50, "Periféricos")
        );
        when(productoRepository.findAll()).thenReturn(listaSimulada);

        List<Producto> resultado = productoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(productoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe encontrar un producto existente por su ID")
    void testBuscarPorId_Existente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoPrueba));

        Optional<Producto> resultado = productoService.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Teclado Mecánico RGB", resultado.get().getNombre());
        verify(productoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe retornar vacío si el producto no existe por su ID")
    void testBuscarPorId_NoExistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Producto> resultado = productoService.buscarPorId(99L);

        assertFalse(resultado.isPresent());
        verify(productoRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Debe eliminar un producto cuando existe en la base de datos")
    void testEliminar_Existe() {
        when(productoRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productoRepository).deleteById(1L);

        boolean eliminado = productoService.eliminar(1L);

        assertTrue(eliminado);
        verify(productoRepository, times(1)).existsById(1L);
        verify(productoRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe retornar false y no llamar a deleteById si el producto no existe")
    void testEliminar_NoExiste() {
        when(productoRepository.existsById(99L)).thenReturn(false);

        boolean eliminado = productoService.eliminar(99L);

        assertFalse(eliminado);
        verify(productoRepository, times(1)).existsById(99L);
        verify(productoRepository, never()).deleteById(anyLong());
    }
}

