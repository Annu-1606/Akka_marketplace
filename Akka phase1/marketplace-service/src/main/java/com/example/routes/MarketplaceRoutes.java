package com.example.routes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.actor.ActorRef;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.example.messages.GatewayMessages;
import com.example.messages.OrderMessages;
import com.example.messages.ProductMessages;
import akka.http.javadsl.marshallers.jackson.Jackson;
import scala.concurrent.Future;
import akka.http.javadsl.server.PathMatchers;
import scala.compat.java8.FutureConverters;
import akka.http.javadsl.model.StatusCodes;
import java.util.Map;
import java.util.HashMap;
import java.time.Duration;
import static akka.http.javadsl.server.Directives.*;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class MarketplaceRoutes {

    private final ActorRef gatewayActor;
    private final Timeout timeout = Timeout.create(Duration.ofSeconds(5));
    public MarketplaceRoutes(ActorRef gatewayActor) {
        this.gatewayActor = gatewayActor;
    }




    public Route createRoute() {
        return concat(
            // Get all products
            path("products", () ->
                get(() -> {
                    System.out.println("Received request to fetch all products");
                    Future<Object> future = Patterns.ask(gatewayActor, new ProductMessages.GetAllProducts(), timeout);
                    CompletionStage<Object> result = FutureConverters.toJava(future);
                    return onSuccess(() -> result, response -> {
                        System.out.println("Response received for /products: " + response); 
                        // Marshal response to JSON before sending
                        return completeOK(response, Jackson.marshaller());
                    });
                })
            ),
            
            


            
              pathPrefix("products", () ->
            path(PathMatchers.segment(), (String id) ->
                get(() -> {
                    System.out.println(" Received request to fetch product with ID: " + id);
                    Future<Object> future = Patterns.ask(gatewayActor, new ProductMessages.GetProductById(id), timeout);
                    CompletionStage<Object> result = FutureConverters.toJava(future);
                    return onSuccess(() -> result, response -> {
                        if (response instanceof ProductMessages.ProductDetails) {
                            return completeOK(response, Jackson.marshaller());
                        } else {
                            return complete(StatusCodes.NOT_FOUND, "Product not found");
                        }
                    });
                })
            )
        ),
        
 
            
            

        // delete a specific order
pathPrefix("orders", () -> concat(
    // Get an order by ID
    path(PathMatchers.segment(), order_id -> 
        get(() -> {
            System.out.println(" Fetching Order ID: " + order_id);
            Future<Object> future = Patterns.ask(gatewayActor, new GatewayMessages.GetOrder(order_id), timeout);
            CompletionStage<Object> result = FutureConverters.toJava(future);
            return onSuccess(() -> result, response -> complete(response.toString()));
        })
    ),





    // Update an order
    path(PathMatchers.segment(), order_id -> 
        put(() -> entity(Unmarshaller.entityToString(), statusJson -> {
            try {
                System.out.println(" Updating Order ID: " + order_id);
                GatewayMessages.UpdateOrderStatus updateOrder = new GatewayMessages.UpdateOrderStatus(order_id, statusJson);
                Future<Object> future = Patterns.ask(gatewayActor, updateOrder, timeout);
                CompletionStage<Object> result = FutureConverters.toJava(future);

                return onSuccess(() -> result, response -> {
                    if (response instanceof GatewayMessages.OrderStatusUpdated) {
                        return complete(StatusCodes.OK, response, Jackson.marshaller());
                    } else {
                        return complete(StatusCodes.BAD_REQUEST, "Order update failed");
                    }
                });
            } catch (Exception e) {
                return complete(StatusCodes.INTERNAL_SERVER_ERROR, "Error updating order: " + e.getMessage());
            }
        }))
    ),






    // Delete an order
path(PathMatchers.segment(), order_id ->
    delete(() -> {
        System.out.println(" Deleting Order ID: " + order_id);
        Future<Object> future = Patterns.ask(gatewayActor, new GatewayMessages.DeleteUserOrders(order_id), timeout);
        CompletionStage<Object> result = FutureConverters.toJava(future);

        return onSuccess(() -> result, response -> {
            if (response instanceof OrderMessages.OrderProcessed orderProcessed) {
                String status = orderProcessed.message.toLowerCase();

                switch (status) {
                    
                    case "order cancelled successfully":
                        System.out.println(" Order cancelled successfully: " + orderProcessed.order_id);
                        return complete(StatusCodes.OK, orderProcessed, Jackson.marshaller());

                    case "order already cancelled":
                    case "refund failed":
                        System.out.println(" Business rule error: " + orderProcessed.status);
                        return complete(StatusCodes.BAD_REQUEST, orderProcessed, Jackson.marshaller());

                    case "order not found":
                        System.out.println(" Order not found: " + orderProcessed.order_id);
                        return complete(StatusCodes.NOT_FOUND, orderProcessed, Jackson.marshaller());

                    default:
                        System.out.println(" Unexpected status: " + orderProcessed.status);
                        return complete(StatusCodes.INTERNAL_SERVER_ERROR, "Unexpected response");
                }
            } else {
                System.out.println(" Unexpected response type: " + response.getClass().getSimpleName());
                return complete(StatusCodes.BAD_REQUEST, "Order deletion failed");
            }
        });
    })
)

)),


            
            // Get all orders for a user
            pathPrefix("orders", () ->
                pathPrefix("users", () ->
                    path(PathMatchers.segment(), user_id -> 
                        get(() -> {
                            Future<Object> future = Patterns.ask(gatewayActor, new GatewayMessages.GetUserOrders(user_id), timeout);
                            CompletionStage<Object> result = FutureConverters.toJava(future);
                            return onSuccess(() -> result, response -> complete(response.toString()));
                        })
                    )
                )
            ),
            



            
            // Create an order
path("orders", () ->
    post(() -> entity(Unmarshaller.entityToString(), orderJson -> {
        try {
            System.out.println(" Received order creation request: " + orderJson);

            GatewayMessages.CreateOrder createOrder = GatewayMessages.CreateOrder.fromJson(orderJson);
            System.out.println(" Parsed CreateOrder: Order_iD=" + createOrder.order_id + ", user_id=" + createOrder.user_id + ", id=" + createOrder.id + ", stock_Quantity=" + createOrder.stock_quantity);

            System.out.println(" Sending CreateOrder message to GatewayActor...");
            Future<Object> future = Patterns.ask(gatewayActor, createOrder, timeout);
            CompletionStage<Object> result = FutureConverters.toJava(future);

            return onSuccess(() -> result, response -> {
    if (response instanceof GatewayMessages.OrderCreated orderCreated) {
        System.out.println("✅ Order created successfully: " + orderCreated.order_id);
        try {
            int orderIdInt = Integer.parseInt(orderCreated.order_id);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("order_id", orderIdInt);
            responseMap.put("status", orderCreated.status);
            responseMap.put("totalAmount", orderCreated.totalAmount);

            return complete(StatusCodes.CREATED, responseMap, Jackson.marshaller());
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid order_id format: " + orderCreated.order_id);
            return complete(StatusCodes.BAD_REQUEST, "Invalid order ID format");
        }
    } else {
        System.out.println("❌ Order creation failed. Unexpected response type: " + response.getClass().getSimpleName());
        return complete(StatusCodes.BAD_REQUEST, "Order creation failed");
    }
});


        } catch (IOException e) {
            System.out.println("Error parsing order JSON: " + e.getMessage());
            return complete(StatusCodes.INTERNAL_SERVER_ERROR, "Error parsing order JSON: " + e.getMessage());
        }
    }))
),




            
            // Reset marketplace
            path("marketplace", () ->
                delete(() -> {
                    Future<Object> future = Patterns.ask(gatewayActor, new GatewayMessages.ResetMarketplace(), timeout);
                    CompletionStage<Object> result = FutureConverters.toJava(future);
                    return onSuccess(() -> result, response -> complete(response.toString()));
                })
            ),
            
            
            // Delete all orders of a user
            pathPrefix("marketplace", () ->
                pathPrefix("users", () ->
                    path(PathMatchers.segment(), user_id -> 
                        delete(() -> {
                            Future<Object> future = Patterns.ask(gatewayActor, new GatewayMessages.DeleteUserOrders(user_id), timeout);
                            CompletionStage<Object> result = FutureConverters.toJava(future);
                            return onSuccess(() -> result, response -> complete(response.toString()));
                        })
                    )
               )
            )
        );
    }
}
