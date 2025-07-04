package com.example.messages;

import com.example.models.Product;
import java.io.Serializable;
import java.util.List;
import akka.actor.ActorRef;

public class ProductMessages {

    public interface ProductMessage extends Serializable {}



    public static class RegisterProduct implements ProductMessage {
        public final String id;
        public final String name;
        public final String description;
        public final double price;
        public final int stock_quantity;

        public RegisterProduct(String id, String name, String description, double price, int stock_quantity) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock_quantity = stock_quantity;
        }
    }




    public static class ProductDetails implements ProductMessage {
        public final String id;
        public final String name;
        public final String description;
        public final double price;
        public final int stock_quantity;

        public ProductDetails(String id, String name, String description, double price, int stock_quantity) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock_quantity = stock_quantity;
        }

        // Constructor Overload - If we get a `Product` model directly
        public ProductDetails(Product product) {
            this(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStock_quantity());
        }
    }



    public static class ProductList implements ProductMessage {
        public final List<ProductDetails> products;

        public ProductList(List<ProductDetails> products) {
            this.products = List.copyOf(products); // Immutable List
        }
    }




    public static class UpdateQuantity implements ProductMessage {
        public final String id;
        public final int quantityChange;
         public final ActorRef postOrderActor;  

        public UpdateQuantity(String id, int quantityChange, ActorRef postOrderActor) {
            this.id = id;
            this.quantityChange = quantityChange;
            this.postOrderActor = postOrderActor;
        }
    }



    public static class QuantityUpdated implements ProductMessage {
        public final String id;
        public final int newQuantity;

        public QuantityUpdated(String id, int newQuantity) {
            this.id = id;
            this.newQuantity = newQuantity;
        }
    }




    public static class QuantityUpdateFailed implements ProductMessage {
        public final String id;
        public final String reason;

        public QuantityUpdateFailed(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }



    public static class GetProduct implements ProductMessage {
        public final String id;

        public GetProduct(String id) {
            this.id = id;
        }
    }



    public static class GetAllProducts implements ProductMessage {
        public GetAllProducts() {} // No unnecessary fields
    }




    public static class GetProductById implements ProductMessage {
        public final String id;

        public GetProductById(String id) {
            this.id = id;
        }
    }




    
    public static class ProductNotFound implements ProductMessage {
        public final String id;

        public ProductNotFound(String id) {
            this.id = id;
        }
    }
}
