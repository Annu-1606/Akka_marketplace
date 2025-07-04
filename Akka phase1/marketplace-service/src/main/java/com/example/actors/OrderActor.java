package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.example.messages.OrderMessages;

public class OrderActor extends AbstractActor {

    private final String order_id;
    private final String user_id;
    private final String id;
    private final int stock_quantity;
    private final double totalAmount;
    private String status; // Created, Confirmed, Cancelled

    public OrderActor(String order_id, String user_id, String id, int stock_quantity, double totalAmount) {
        this.order_id = order_id;
        this.user_id = user_id;
        this.id = id;
        this.stock_quantity = stock_quantity;
        this.totalAmount = totalAmount;
        this.status = "Created"; // default
    }




    public static Props props(String order_id, String user_id, String id, int stock_quantity, double totalAmount) {
        return Props.create(OrderActor.class, () -> new OrderActor(order_id, user_id, id, stock_quantity, totalAmount));
    }




    @Override
   public Receive createReceive() {
    return receiveBuilder()
        .match(OrderMessages.GetOrder.class, msg -> {
            System.out.println(" [OrderActor]: Received GetOrder request for Order ID: " + order_id);
            System.out.println(" [OrderActor]: User ID: " + user_id + ", Product ID: " + id);
            System.out.println("[OrderActor]: Stock Quantity: " + stock_quantity + ", Total Amount: " + totalAmount + ", Status: " + status);
            System.out.println(" [DEBUG] OrderActor in getorder getSender() = " + getSender());
            getSender().tell(
                new OrderMessages.OrderDetails(order_id, user_id, id, stock_quantity, totalAmount, status), 
                getSelf()
            );
            
            System.out.println(" [OrderActor]: Sent OrderDetails response for Order ID: " + order_id);
        })


        .match(OrderMessages.GetOrderExisting.class, msg -> {
            System.out.println(" [OrderActor]: Received GetOrderExisting request for Order ID: " + order_id);
            System.out.println(" [OrderActor]: User ID: " + user_id + ", Product ID: " + id);
            System.out.println(" [OrderActor]: Stock Quantity: " + stock_quantity + ", Total Amount: " + totalAmount + ", Status: " + status);
            System.out.println(" [DEBUG] OrderActor in getorder getSender() = " + getSender());
            getSender().tell(
                new OrderMessages.OrderDetails(order_id, user_id, id, stock_quantity, totalAmount, status), 
                getSelf()
            );
            
            System.out.println(" [OrderActor]: Sent OrderDetails response for Order ID: " + order_id);
        })


        .match(OrderMessages.UpdateOrderStatus.class, msg -> {
            System.out.println(" [OrderActor]: Received UpdateOrderStatus request for Order ID: " + order_id);
            System.out.println(" [OrderActor]: Updating order status from '" + this.status + "' to '" + msg.status + "'");

            this.status = msg.status; // Update the status
            System.out.println("[DEBUG] OrderActor getSender() in updateorderstatus = " + getSender());
            getSender().tell(new OrderMessages.OrderStatusUpdated(order_id, status), getSelf());
            
            System.out.println("[OrderActor]: Order status updated successfully to: " + this.status);
        })
        .build();
}

}
