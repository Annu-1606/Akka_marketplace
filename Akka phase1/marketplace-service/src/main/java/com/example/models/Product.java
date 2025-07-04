package com.example.models;

public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final double price;
    private int stock_quantity;  

    public Product(String id, String name, String description, int stock_quantity, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.stock_quantity = stock_quantity;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public int getStock_quantity() {
        return stock_quantity;
    }

    public void setStock_quantity(int stock_quantity) {  
        this.stock_quantity = stock_quantity;
    }
}
