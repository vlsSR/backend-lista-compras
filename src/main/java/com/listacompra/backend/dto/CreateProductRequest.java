package com.listacompra.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateProductRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String name;

    private Integer quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
