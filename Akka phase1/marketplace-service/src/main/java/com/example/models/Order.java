package com.example.models;

import java.io.Serializable;

public class Order implements Serializable {

    private String order_id;
    private String user_id;
    private String productId;
    private int stock_quantity;
    private String status;

    public Order(String order_id, String user_id, String productId, int stock_quantity, String status) {
        this.order_id = order_id;
        this.user_id = user_id;
        this.productId = productId;
        this.stock_quantity = stock_quantity;
        this.status = status;
    }

    public String getOrder_id() {
        return order_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getProductId() {
        return productId;
    }

    public int getStock_quantity() {
        return stock_quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Order{" +
                "order_id='" + order_id + '\'' +
                ", user_id='" + user_id + '\'' +
                ", productId='" + productId + '\'' +
                ", stock_quantity=" + stock_quantity +
                ", status='" + status + '\'' +
                '}';
    }
}
