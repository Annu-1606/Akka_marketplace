package marketplace.marketplace.Actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.Behavior;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.stream.Collectors;
import static marketplace.marketplace.Messages.PostOrderMessages.*;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Messages.ProductMessages;
import marketplace.marketplace.Messages.PostOrderMessages;
import marketplace.marketplace.Messages.OrderMessages;
import marketplace.marketplace.Actors.ProductActor;
import marketplace.marketplace.MarketplaceServiceApplication;



public class PostOrderActor extends AbstractBehavior<PostOrderActor.Command> {

    public interface Command { }
    public static final class Initialize implements Command { }


    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String orderData;
    private final ActorRef<GatewayMessages.OrderInfo> replyTo;
    private final Map<Integer, ActorRef<ProductMessages.Command>> productActors;
    private final Map<Integer, ActorRef<OrderMessages.Command>> orderActors;
    private final int orderId;
    private final List<Integer> userIdList;
    private final Scheduler scheduler;


    private int userId;
    private List<Map<String, Object>> items;

   
    private final Map<Integer, GatewayMessages.ProductInfo> collectedProductInfos = new HashMap<>();
    private int pendingProductResponses = 0;

  
    private final Map<Integer, Boolean> stockReductionResults = new HashMap<>();
    private int pendingStockReductionResponses = 0;

    private int totalCost = 0;
    private int finalCost = 0;

    private PostOrderActor(ActorContext<Command> context,
                      String orderData,
                      ActorRef<GatewayMessages.OrderInfo> replyTo,
                      Map<Integer, ActorRef<ProductMessages.Command>> productActors,
                      Map<Integer, ActorRef<OrderMessages.Command>> orderActors,
                      int orderId,List<Integer> userIdList, Scheduler scheduler) {
        super(context);
        this.orderData = orderData;
        this.replyTo = replyTo;
        this.productActors = productActors;
        this.orderActors = orderActors;
        this.orderId = orderId;
        this.userIdList = userIdList;
        this.scheduler = scheduler;
        getContext().getSelf().tell(new Initialize());
      
    }

    public static Behavior<Command> create(String orderData,
                                           ActorRef<GatewayMessages.OrderInfo> replyTo,
                                           Map<Integer, ActorRef<ProductMessages.Command>> productActors,
                                           Map<Integer, ActorRef<OrderMessages.Command>> orderActors,
                                           int orderId, List<Integer> userIdList, Scheduler scheduler) {
        return Behaviors.setup(context -> new PostOrderActor(context, orderData, replyTo, productActors, orderActors, orderId, userIdList, scheduler));
    }

    private Behavior<Command> onInitialize(Initialize msg) {
        try {
            Map<String, Object> orderRequest = objectMapper.readValue(orderData, new TypeReference<Map<String,Object>>() {});
            userId = (Integer) orderRequest.get("user_id");
            
            if (getUser(userId) == 404) {
                replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "user actor missing for id " + userId, new ArrayList<>()));
                return Behaviors.stopped();
            }
            items = (List<Map<String, Object>>) orderRequest.get("items");
            pendingProductResponses = items.size();
          
            for (Map<String, Object> item : items) {
                int prodId = (Integer) item.get("product_id");
                ActorRef<GatewayMessages.ProductInfo> adapter = getContext().messageAdapter(GatewayMessages.ProductInfo.class,
                        info -> new ProductDetailResponse(info));
                ActorRef<ProductMessages.Command> prodActor = productActors.get(prodId);
                if (prodActor != null) {
                    prodActor.tell(new ProductMessages.GetProductById(prodId, adapter));
                } else {
                    replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Product actor missing for id " + prodId, new ArrayList<>()));
                    return Behaviors.stopped();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Invalid order data");
            replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Invalid order data", new ArrayList<>()));
            return Behaviors.stopped();
        }
 
        return this;
    }

    @Override
    public Receive<Command> createReceive() {
        return waitingForProductDetails();
    }

  
    private Receive<Command> waitingForProductDetails() {
        return newReceiveBuilder()
            .onMessage(Initialize.class, this::onInitialize)
            .onMessage(ProductDetailResponse.class, this::onProductDetailResponse)
            .build();
    }

   

// Waits for all stock reduction responses
private Receive<Command> waitingForStockReduction() {
    return newReceiveBuilder()
        .onMessage(StockReductionResponse.class, this::onStockReductionResponse) // On stock reduction response, handle it
        .build();
}



// Debit wallet method to deduct money
private boolean debitWallet(int user_id, int amount) {
    try {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "debit");
        data.put("amount", amount);
        String json = objectMapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://host.docker.internal:8082/wallets/" + user_id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = MarketplaceServiceApplication.httpClient.send(request, BodyHandlers.ofString());
        return response.statusCode() == 200; // Return true if debit was successful
    } catch (Exception e) {
        e.printStackTrace();
        return false; // Return false if there was an error
    }
}

// Refund wallet method to credit money back
private void refundWallet(int user_id, int amount) {
    try {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "credit");
        data.put("amount", amount);
        String json = objectMapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://host.docker.internal:8082/wallets/" + user_id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        MarketplaceServiceApplication.httpClient.send(request, BodyHandlers.ofString()); // Credit wallet
    } catch (Exception e) {
        e.printStackTrace();
    }
}

// Update user discount status after order
private void updateUserDiscount(int user_id, boolean discountAvailed) {
    try {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user_id);
        data.put("discount_availed", discountAvailed); // Set discount status
        String json = objectMapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://host.docker.internal:8080/users"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        MarketplaceServiceApplication.httpClient.send(request, BodyHandlers.ofString()); // Update user info
    } catch (Exception e) {
        e.printStackTrace();
    }
}




// Handles stock reduction responses
private Behavior<Command> onStockReductionResponse(StockReductionResponse response) {
    stockReductionResults.put(response.productId, response.StockResponse.success); // Store result of stock reduction
    pendingStockReductionResponses--; // Decrease pending responses count

    // If all stock reductions are processed
    if (pendingStockReductionResponses == 0) {
        boolean allSuccess = stockReductionResults.values().stream().allMatch(s -> s); // Check if all reductions were successful

        // If all successful, proceed with order processing
        if (allSuccess) {
            try {
                // Update user discount status
                updateUserDiscount(userId, false);

                List<OrderMessages.OrderItem> orderItems = new ArrayList<>();
                int orderItemIdCounter = 1;

                // Create order items from cart
                for (Map<String, Object> item : items) {
                    int prodId = (Integer) item.get("product_id");
                    int quantity = (Integer) item.get("quantity");
                    orderItems.add(new OrderMessages.OrderItem(orderItemIdCounter++, prodId, quantity));
                }

                // Convert order items to order item info and create order actor
                List<OrderMessages.OrderItemInfo> orderItemInfos = orderItems.stream()
                        .map(oi -> new OrderMessages.OrderItemInfo(oi.id, oi.productId, oi.quantity))
                        .collect(Collectors.toList());
                ActorRef<OrderMessages.Command> orderActor = getContext().spawn(
                        OrderActor.create(orderId, userId, finalCost, orderItems, userIdList),
                        "Order" + orderId);
                orderActors.put(orderId, orderActor);

                // Respond with order info
                replyTo.tell(new GatewayMessages.OrderInfo(orderId, userId, finalCost, "PLACED", orderItemInfos));
                return Behaviors.empty();
            } catch (Exception e) {
                // If error occurs, refund wallet and restore stock
                refundWallet(userId, finalCost);
                for (Map<String, Object> item : items) {
                    int prodId = (Integer) item.get("product_id");
                    int quantity = (Integer) item.get("quantity");
                    Boolean reduced = stockReductionResults.get(prodId);
                    if (reduced != null && reduced) {
                        ActorRef<ProductMessages.Command> prodActor = productActors.get(prodId);
                        if (prodActor != null) {
                            prodActor.tell(new ProductMessages.RestoreStock(quantity,
                                    getContext().messageAdapter(ProductMessages.StockResponse.class, op -> new StockReductionResponse(prodId, op))));
                        }
                    }
                }
                replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Stock reduction failed, order cancelled", new ArrayList<>()));
                return Behaviors.stopped();
            }
        } else {
            // If stock reduction fails, refund wallet and restore stock
            refundWallet(userId, finalCost);
            for (Map<String, Object> item : items) {
                int prodId = (Integer) item.get("product_id");
                int quantity = (Integer) item.get("quantity");
                Boolean reduced = stockReductionResults.get(prodId);
                if (reduced != null && reduced) {
                    ActorRef<ProductMessages.Command> prodActor = productActors.get(prodId);
                    if (prodActor != null) {
                        prodActor.tell(new ProductMessages.RestoreStock(quantity,
                                getContext().messageAdapter(ProductMessages.StockResponse.class, op -> new StockReductionResponse(prodId, op))));
                    }
                }
            }
            replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Stock reduction failed, order cancelled", new ArrayList<>()));
            return Behaviors.stopped();
        }
    }
    return this; // Stay in the current behavior
}




// Fetch user data from the service
private int getUser(int user_id) {
    try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://host.docker.internal:8080/users/" + user_id))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = MarketplaceServiceApplication.httpClient.send(request, BodyHandlers.ofString());
        System.out.println("Response: " + response.body());
        return response.statusCode(); // Return HTTP status code
    } catch (Exception e) {
        e.printStackTrace();
        return 404; // Return 404 if there's an error
    }
}


 // Handles the response when product details are received
 private Behavior<Command> onProductDetailResponse(ProductDetailResponse response) {
    GatewayMessages.ProductInfo info = response.productInfo; // Extract product info from the response
    collectedProductInfos.put(info.productId, info); // Store product info in a map
    pendingProductResponses--; // Decrease pending responses count

    // Proceed if all product responses are received
    if (pendingProductResponses == 0) {
        totalCost = 0;
        for (Map<String, Object> item : items) {
            int prodId = (Integer) item.get("product_id"); // Get product ID from item
            int quantity = (Integer) item.get("quantity"); // Get quantity from item
            GatewayMessages.ProductInfo pi = collectedProductInfos.get(prodId); // Fetch product info

            // If stock is insufficient, respond with error and stop the process
            if (pi == null || pi.stock_quantity < quantity) {
                replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Insufficient stock for product " + prodId, new ArrayList<>()));
                return Behaviors.stopped();
            }
            totalCost += quantity * pi.price; // Add product cost to total
        }

        // Apply discount if user hasn't used it
        boolean discountApplicable = !userIdList.contains(userId);
        finalCost = discountApplicable ? (int)(totalCost * 0.9) : totalCost;

        // Debit wallet, if fails, send failure response
        if (!debitWallet(userId, finalCost)) {
            replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Insufficient wallet balance", new ArrayList<>()));
            return Behaviors.stopped();
        }

        // Wait for stock reduction responses
        pendingStockReductionResponses = items.size();
        for (Map<String, Object> item : items) {
            int prodId = (Integer) item.get("product_id"); // Get product ID
            int quantity = (Integer) item.get("quantity"); // Get quantity
            ActorRef<ProductMessages.StockResponse> adapter = getContext().messageAdapter(ProductMessages.StockResponse.class, op -> new StockReductionResponse(prodId, op));
            ActorRef<ProductMessages.Command> prodActor = productActors.get(prodId); // Get product actor

            // If product actor exists, ask it to reduce stock
            if (prodActor != null) {
                prodActor.tell(new ProductMessages.ReduceStock(quantity, adapter));
            } else {
                // If product actor is missing, refund wallet and cancel order
                refundWallet(userId, finalCost);
                replyTo.tell(new GatewayMessages.OrderInfo(orderId, 0, 0, "Product actor missing for id " + prodId, new ArrayList<>()));
                return Behaviors.stopped();
            }
        }
        return waitingForStockReduction(); // Transition to waiting for stock reduction responses
    }
    return Behaviors.same(); // Stay in current state if still waiting for product responses
}

}