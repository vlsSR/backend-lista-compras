package com.listacompra.backend.repository;

import com.listacompra.backend.model.ShoppingList;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {
}
