package com.example.messages;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class OrderMessages {

    public interface OrderMessage extends Serializable {}

    // Order Creation
  public static class CreateOrder implements OrderMessage {
    public final String order_id;
    public final String user_id;
    public final String id;
    public final int stock_quantity;
    public final double totalAmount;
    public final String status;  // Add this field

    public CreateOrder(String order_id, String user_id, String id, int stock_quantity, double totalAmount, String status) {
        this.order_id = order_id;
        this.user_id = user_id;
        this.id = id;
        this.stock_quantity = stock_quantity;
        this.totalAmount = totalAmount;
        this.status = status;
    }
}



    // Fetch Order Details
    public static class GetOrder implements OrderMessage {
        public final String order_id;

        public GetOrder(String order_id) {
            this.order_id = order_id;
        }
    }


 public static class GetOrderExisting implements OrderMessage {
        public final String order_id;

        public GetOrderExisting(String order_id) {
            this.order_id = order_id;
        }
    }



    // Order Status Update
public static class UpdateOrderStatus implements OrderMessage {
        public final String order_id;
        public final String status;

        public UpdateOrderStatus(String order_id, String status) {
            this.order_id = order_id;
            this.status = status;
        }
    }
    



public static class OrderProcessed implements OrderMessage {

    @JsonProperty("order_id")
    public final String order_id;
    public final String status;  
    public final String message;

    public OrderProcessed(String order_id, String status, String message) {
    
        this.order_id = order_id;
        this.status = status;
        this.message = message;
    }
}




public static class OrderCreated {
    public final String order_id;
    public final String user_id;
    public final String id;
    public final int quantity;
    public final double price;

    public OrderCreated(String order_id, String user_id, String id, int quantity, double price) {
        this.order_id = order_id;
        this.user_id = user_id;
        this.id = id;
        this.quantity = quantity;
        this.price = price;
    }
}
    




    // Fetch User Orders
public static class GetUserOrders implements OrderMessage {
    public final String user_id;

    public GetUserOrders(String user_id) {
        this.user_id = user_id;
    }
}




    // Order Deletion
public static class DeleteOrder implements OrderMessage {
        public final String user_id;

        public DeleteOrder(String user_id) {
            this.user_id = user_id;
        }
    }




    // Order Details Response
public static class OrderDetails implements OrderMessage {
        public final String order_id;
        public final String user_id;
        public final String id;
        public final int stock_quantity;
        public final double totalAmount;
        public final String status;

        public OrderDetails(String order_id, String user_id, String id, int stock_quantity, double totalAmount, String status) {
            this.order_id = order_id;
            this.user_id = user_id;
            this.id = id;
            this.stock_quantity = stock_quantity;
            this.totalAmount = totalAmount;
            this.status = status;
        }
    }




    // Order Status Update Response
public static class OrderStatusUpdated implements OrderMessage {
        public final String order_id;
        public final String status;

        public OrderStatusUpdated(String order_id, String status) {
            this.order_id = order_id;
            this.status = status;
        }
    }




    // Order Processed Response
public static class OrderResponse implements OrderMessage {
        public final String order_id;
        public final String status;
        public final String message;

        public OrderResponse(String order_id, String status, String message) {
            this.order_id = order_id;
            this.status = status;
            this.message = message;
        }
    }
}
