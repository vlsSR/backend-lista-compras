package com.listacompra.backend.dto;

import com.listacompra.backend.model.Product;
import com.listacompra.backend.model.ShoppingList;

public class SseEnvelope {

    private String type;
    private ShoppingList listPayload;
    private Product productPayload;
    private String deletedId;

    public SseEnvelope() {
    }

    public SseEnvelope(String type, ShoppingList listPayload, Product productPayload, String deletedId) {
        this.type = type;
        this.listPayload = listPayload;
        this.productPayload = productPayload;
        this.deletedId = deletedId;
    }

    // --- Constructores de conveniencia para cada tipo de evento ---

    public static SseEnvelope listCreated(ShoppingList list) {
        return new SseEnvelope("LIST_CREATED", list, null, null);
    }

    public static SseEnvelope listDeleted(String listId) {
        return new SseEnvelope("LIST_DELETED", null, null, listId);
    }

    public static SseEnvelope productCreated(Product product) {
        return new SseEnvelope("PRODUCT_CREATED", null, product, null);
    }

    public static SseEnvelope productUpdated(Product product) {
        return new SseEnvelope("PRODUCT_UPDATED", null, product, null);
    }

    public static SseEnvelope productDeleted(String productId) {
        return new SseEnvelope("PRODUCT_DELETED", null, null, productId);
    }

    public String getType() {
        return type;
    }

    public ShoppingList getListPayload() {
        return listPayload;
    }

    public Product getProductPayload() {
        return productPayload;
    }

    public String getDeletedId() {
        return deletedId;
    }
}
