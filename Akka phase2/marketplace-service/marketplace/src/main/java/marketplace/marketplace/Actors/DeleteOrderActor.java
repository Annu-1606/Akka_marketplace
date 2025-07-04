package marketplace.marketplace.Actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.Behavior;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Messages.ProductMessages;
import marketplace.marketplace.Messages.OrderMessages;
import marketplace.marketplace.Actors.ProductActor;
import marketplace.marketplace.Messages.DeleteOrderMessages;
import marketplace.marketplace.MarketplaceServiceApplication;

public class DeleteOrderActor extends AbstractBehavior<DeleteOrderMessages.Command> {

    // Enum to track the states of the DeleteOrderActor
    private enum DeleteOrderState { INITIAL, WAITING, SUCCESSFUL, FAILED }

    // Instance variables to store order and product details
    private final int orderId;
    private final ActorRef<GatewayMessages.SuccessResponse> replyTo;
    private final Map<Integer, ActorRef<OrderMessages.Command>> orderActors;
    private final Map<Integer, ActorRef<ProductMessages.Command>> productActors;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeleteOrderState state;  // The current state of the actor

    // Constructor to initialize the actor and trigger the cancellation process
    private DeleteOrderActor(ActorContext<DeleteOrderMessages.Command> context,
                        int orderId,
                        ActorRef<GatewayMessages.SuccessResponse> replyTo,
                        Map<Integer, ActorRef<ProductMessages.Command>> productActors,
                        Map<Integer, ActorRef<OrderMessages.Command>> orderActors) {
        super(context);
        this.orderId = orderId;
        this.replyTo = replyTo;
        this.productActors = productActors;
        this.orderActors = orderActors;
        this.state = DeleteOrderState.INITIAL;  // Initial state is set to INITIAL
        getContext().getSelf().tell(new DeleteOrderMessages.CancelOrderRequest());  // Initiate the cancellation
    }

    @Override
    public Receive<DeleteOrderMessages.Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(DeleteOrderMessages.CancelOrderRequest.class, this::onCancelOrderRequest)  // Handle Cancel request messages
            .onMessage(DeleteOrderMessages.CancelOrderResponse.class, this::onCancelOrderResponse)  // Handle CancelOrderResponse messages
            .build();
    }





    // Handles the response from the OrderActor after attempting to cancel the order
    private Behavior<DeleteOrderMessages.Command> onCancelOrderResponse(DeleteOrderMessages.CancelOrderResponse response) {
        GatewayMessages.OrderInfo info = response.orderInfo;  // Get the order info from the response
        if ("CANCELLED".equals(info.status)) {  // If the order was successfully cancelled
            // Restore the stock for each item in the cancelled order
            for (OrderMessages.OrderItemInfo item : info.items) {
                ActorRef<ProductMessages.Command> prodActor = productActors.get(item.productId);
                if (prodActor != null) {
                    prodActor.tell(new ProductMessages.RestoreStock(item.quantity,
                        getContext().messageAdapter(ProductMessages.StockResponse.class, op -> null)));
                }
            }
            // Refund the user for the cancelled order
            refundWallet(info.user_id, info.total_price);
            replyTo.tell(new GatewayMessages.SuccessResponse(true, "Order " + orderId + " cancelled successfully"));  // Send success response
            state = DeleteOrderState.SUCCESSFUL;  // Transition to SUCCESSFUL state
        } else {
            replyTo.tell(new GatewayMessages.SuccessResponse(false, "Failed to cancel order"));  // Send failure response
            state = DeleteOrderState.FAILED;  // Transition to FAILED state
        }
        
        return Behaviors.stopped();  // Stop the actor after handling the cancellation response
    }





    

// Handles the cancellation request and sends a message to the corresponding OrderActor
private Behavior<DeleteOrderMessages.Command> onCancelOrderRequest(DeleteOrderMessages.CancelOrderRequest msg) {
    ActorRef<OrderMessages.Command> orderActor = orderActors.get(orderId);  // Retrieve the OrderActor for this orderId
    if (orderActor != null) {
        // Set up a response adapter to handle the cancellation response
        ActorRef<GatewayMessages.OrderInfo> adapter =
                getContext().messageAdapter(GatewayMessages.OrderInfo.class,
                    info -> new DeleteOrderMessages.CancelOrderResponse(info));
        // Request the order cancellation
        orderActor.tell(new OrderMessages.CancelOrderById(orderId, adapter));
        state = DeleteOrderState.WAITING;  // Transition to WAITING state
    } else {
        replyTo.tell(new GatewayMessages.SuccessResponse(false, "Failed to cancel order"));  // Send failure response if no OrderActor found
        state = DeleteOrderState.FAILED;  // Transition to FAILED state
        return Behaviors.stopped();  // Stop the actor since cancellation failed
    }
    return this;
}





    // Helper method to refund the user's wallet after order cancellation
    private void refundWallet(int user_id, int amount) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "credit");  // The action is "credit" for refund
            data.put("amount", amount);  // The amount to refund
            String json = objectMapper.writeValueAsString(data);  // Convert the data to JSON
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://host.docker.internal:8082/wallets/" + user_id))  // Send the request to the wallet service
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))  // Send a PUT request with the JSON body
                .build();
            MarketplaceServiceApplication.httpClient.send(request, BodyHandlers.ofString());  // Send the request
        } catch (Exception e) {
            e.printStackTrace();  // Print the exception if an error occurs
        }
    }






    // Static method to create and initialize the DeleteOrderActor
    public static Behavior<DeleteOrderMessages.Command> create(int orderId,
                                           ActorRef<GatewayMessages.SuccessResponse> replyTo,
                                           Map<Integer, ActorRef<ProductMessages.Command>> productActors,
                                           Map<Integer, ActorRef<OrderMessages.Command>> orderActors) {
        return Behaviors.setup(context -> new DeleteOrderActor(context, orderId, replyTo, productActors, orderActors));  // Set up the actor
    }
}
