package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.messages.OrderMessages;
import com.example.models.Order;
import com.example.models.Product;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.compat.java8.FutureConverters;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;




public class OrderRegionActor extends AbstractActor {
    
    private final Map<String, ActorRef> orders = new HashMap<>();
    
    public static Props props() {
        return Props.create(OrderRegionActor.class, OrderRegionActor::new);
    }




@Override
public Receive createReceive() {
    return receiveBuilder()
            .match(OrderMessages.GetOrderExisting.class, this::handleGetOrderExisting)
            .match(OrderMessages.UpdateOrderStatus.class, this::handleUpdateOrderStatus)
            .match(OrderMessages.CreateOrder.class, this::handleCreateOrder)
            .match(OrderMessages.DeleteOrder.class, this::handleDeleteOrder) 
            .match(OrderMessages.GetUserOrders.class, this::handleGetUserOrders)
            .match(OrderMessages.OrderDetails.class, this::handleOrderDetails)
            //.match(OrderMessages.OrderStatusUpdated.class, this::handleOrderStatusUpdated)
            .build();
}




private void handleOrderDetails(OrderMessages.OrderDetails msg) {
    System.out.println(" [OrderRegionActor] Received order details: " + msg.order_id);

    // Check if the order exists in the orders map
    if (orders.containsKey(msg.order_id)) {
        System.out.println(" [OrderRegionActor] Order " + msg.order_id + " found and being processed.");
        getSender().tell(msg, getSelf()); // Forward the message back to the requester
    } else {
        System.out.println(" [OrderRegionActor] Order not found for order_id: " + msg.order_id);
        getSender().tell("Order Not Found", getSelf());
    }
}




//  Handle Delete Order Message
private void handleDeleteOrder(OrderMessages.DeleteOrder msg) {
     System.out.println(" [OrderRegionActor] recived delete request at handle delete order");
    // Collect all order IDs associated with the user_id
    List<String> ordersToDelete = orders.entrySet().stream()
        .filter(entry -> {
            ActorRef orderActor = entry.getValue();
            // Send a synchronous message to get order details
            Timeout timeout = Timeout.create(Duration.ofSeconds(2));
            Future<Object> future = Patterns.ask(orderActor, new OrderMessages.GetOrder(entry.getKey()), timeout);
            try {
                Object response = Await.result(future, timeout.duration());
                if (response instanceof OrderMessages.OrderDetails) {
                    return ((OrderMessages.OrderDetails) response).user_id.equals(msg.user_id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        })
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    // Delete each order
    if (!ordersToDelete.isEmpty()) {
        for (String order_id : ordersToDelete) {
            ActorRef orderActor = orders.remove(order_id);
            if (orderActor != null) {
                getContext().stop(orderActor);
            }
        }
        getSender().tell(new OrderMessages.OrderProcessed(msg.user_id, "Deleted", "All orders for user deleted successfully"), getSelf());
    } else {
        getSender().tell(new OrderMessages.OrderProcessed(msg.user_id, "Failed", "No orders found for user"), getSelf());
    }
}






private void handleGetOrderExisting(OrderMessages.GetOrderExisting msg) {
        System.out.println(" [OrderRegionActor] in getorderExisting.");
       System.out.println("Orders Map: " + orders);
        System.out.println(" Orders Map Keys: " + orders.keySet().stream()
    .map(k -> k + " (type: " + k.getClass().getSimpleName() + ")")
    .collect(Collectors.joining(", ")));

   System.out.println(" Looking for order_id in getorderexisting: " + msg.order_id + " (type: " + msg.order_id.getClass().getSimpleName() + ")");
        ActorRef orderActor = orders.get(String.valueOf(msg.order_id));
        
        System.out.println("[OrderRegionActor] Orders Map: " + orders.keySet());
        if (orderActor != null) {
           orderActor.forward(msg, getContext());
            System.out.println(" [OrderRegionActor] in getorderexisting and forwarded to orderactor.");
        } else {
        
        System.out.println(" orderactor null in orderregionactor.");
            getSender().tell("Order Not Found", getSelf());
        }
    }




private void handleUpdateOrderStatus(OrderMessages.UpdateOrderStatus msg) {
        ActorRef orderActor = orders.get(msg.order_id);
        if (orderActor != null) {
            orderActor.forward(msg, getContext());
        }
    }




private void handleCreateOrder(OrderMessages.CreateOrder msg) {
    System.out.println(" [OrderRegionActor] Creating order with ID: " + msg.order_id);
    
    if (!orders.containsKey(msg.order_id)) {
        ActorRef orderActor = getContext().actorOf(OrderActor.props(
            msg.order_id, 
            msg.user_id, 
            msg.id, 
            msg.stock_quantity, 
            msg.totalAmount
        ), "order-" + msg.order_id);
        
        System.out.println(" [OrderRegionActor] Storing order: " + msg.order_id);
        orders.put(msg.order_id, orderActor);
        
        System.out.println(" [OrderRegionActor] Stored OrderActor for order_id: " + msg.order_id);

        // Capture the original sender (PostOrderActor) before making the async request
        ActorRef senderRef = getSender();
        System.out.println(" [OrderRegionActor] Senderef in create order of orderregionactor " + senderRef);

        //  Fetch order details from the newly created actor
        Timeout timeout = Timeout.create(Duration.ofSeconds(2));
        Future<Object> future = Patterns.ask(orderActor, new OrderMessages.GetOrder(msg.order_id), timeout);

        //  Send order details back to the original sender (PostOrderActor)
        CompletionStage<Object> completionStage = FutureConverters.toJava(future);
        completionStage.thenAccept(response -> {
            if (response instanceof OrderMessages.OrderDetails) {
                System.out.println(" [OrderRegionActor] Forwarding order details back to PostOrderActor...");
                
                //  Use senderRef (PostOrderActor) instead of getSender()
                senderRef.tell(response, getSelf());  
                
                System.out.println(" [OrderRegionActor] Sent order details back to PostOrderActor: " + response);
                System.out.println(" [OrderRegionActor] Sent back to " + senderRef);
                
            } else {
                System.out.println(" [OrderRegionActor] Failed to retrieve order details.");
                
                //  Use senderRef (PostOrderActor) instead of getSender()
                senderRef.tell(new OrderMessages.OrderProcessed(msg.order_id, "Failed", "Order details retrieval failed"), getSelf());
            }
        }).exceptionally(ex -> {
            System.out.println(" [OrderRegionActor] Error fetching order details: " + ex.getMessage());
            
            //  Use senderRef (PostOrderActor) instead of getSender()
            senderRef.tell(new OrderMessages.OrderProcessed(msg.order_id, "Failed", "Error fetching order details"), getSelf());
            return null;
        });
    } else {
        System.out.println("⚠️ [OrderRegionActor] Order already exists: " + msg.order_id);
    }
}







//  Handle Fetching User Orders
private void handleGetUserOrders(OrderMessages.GetUserOrders msg) {
    System.out.println(" [OrderRegionActor] Fetching orders for User ID: " + msg.user_id);
    
    // Collect all orders belonging to the user
    List<OrderMessages.OrderDetails> userOrders = orders.entrySet().stream()
        .map(entry -> {
            ActorRef orderActor = entry.getValue();
            Timeout timeout = Timeout.create(Duration.ofSeconds(2));
            Future<Object> future = Patterns.ask(orderActor, new OrderMessages.GetOrder(entry.getKey()), timeout);
            try {
                Object response = Await.result(future, timeout.duration());
                if (response instanceof OrderMessages.OrderDetails) {
                    OrderMessages.OrderDetails orderDetails = (OrderMessages.OrderDetails) response;
                    if (orderDetails.user_id.equals(msg.user_id)) {
                        return orderDetails;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        })
        .filter(order -> order != null)
        .collect(Collectors.toList());

    // Send response back
    if (!userOrders.isEmpty()) {
        getSender().tell(userOrders, getSelf());
    } else {
        getSender().tell("No orders found for user", getSelf());
    }
}



}
