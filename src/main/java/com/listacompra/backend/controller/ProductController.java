package com.listacompra.backend.controller;

import com.listacompra.backend.dto.CreateProductRequest;
import com.listacompra.backend.dto.SseEnvelope;
import com.listacompra.backend.dto.UpdateProductRequest;
import com.listacompra.backend.model.Product;
import com.listacompra.backend.repository.ProductRepository;
import com.listacompra.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository productRepository;
    private final SseService sseService;

    public ProductController(ProductRepository productRepository, SseService sseService) {
        this.productRepository = productRepository;
        this.sseService = sseService;
    }

    @GetMapping("/lists/{listId}/products")
    public List<Product> getProducts(@PathVariable String listId) {
        return productRepository.findByListId(listId);
    }

    @PostMapping("/lists/{listId}/products")
    public ResponseEntity<Product> createProduct(@PathVariable String listId,
                                                  @Valid @RequestBody CreateProductRequest request) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        Product product = new Product(listId, request.getName(), quantity);
        Product saved = productRepository.save(product);

        sseService.broadcast(SseEnvelope.productCreated(saved));

        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable String productId,
                                                  @RequestBody UpdateProductRequest request) {
        Optional<Product> existing = productRepository.findById(productId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = existing.get();
        if (request.getName() != null) product.setName(request.getName());
        if (request.getQuantity() != null) product.setQuantity(request.getQuantity());
        if (request.getChecked() != null) product.setChecked(request.getChecked());

        Product saved = productRepository.save(product);

        sseService.broadcast(SseEnvelope.productUpdated(saved));

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        productRepository.deleteById(productId);

        sseService.broadcast(SseEnvelope.productDeleted(productId));

        return ResponseEntity.noContent().build();
    }
}
