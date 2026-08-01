package com.listacompra.backend.controller;

import com.listacompra.backend.dto.CreateListRequest;
import com.listacompra.backend.dto.SseEnvelope;
import com.listacompra.backend.model.ShoppingList;
import com.listacompra.backend.repository.ProductRepository;
import com.listacompra.backend.repository.ShoppingListRepository;
import com.listacompra.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    private final ShoppingListRepository listRepository;
    private final ProductRepository productRepository;
    private final SseService sseService;

    public ListController(ShoppingListRepository listRepository,
                           ProductRepository productRepository,
                           SseService sseService) {
        this.listRepository = listRepository;
        this.productRepository = productRepository;
        this.sseService = sseService;
    }

    @GetMapping
    public List<ShoppingList> getLists() {
        return listRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<ShoppingList> createList(@Valid @RequestBody CreateListRequest request) {
        ShoppingList list = new ShoppingList(request.getName(), System.currentTimeMillis());
        ShoppingList saved = listRepository.save(list);

        sseService.broadcast(SseEnvelope.listCreated(saved));

        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable String listId) {
        listRepository.deleteById(listId);
        productRepository.deleteByListId(listId);

        sseService.broadcast(SseEnvelope.listDeleted(listId));

        return ResponseEntity.noContent().build();
    }
}
