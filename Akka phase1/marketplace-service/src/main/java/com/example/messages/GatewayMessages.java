package com.example.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayMessages {

    public interface GatewayMessage extends Serializable {}

    public static class CreateOrder implements GatewayMessages.GatewayMessage {
        public final String order_id;
        public final String user_id;
        public final String id;
        public final int stock_quantity;

        @JsonCreator
        public CreateOrder(
            @JsonProperty("order_id") String order_id,
            @JsonProperty("user_id") String user_id,
            @JsonProperty("product_id") String id,
            @JsonProperty("stock_quantity") int stock_quantity
        ) {
            this.order_id = order_id;
            this.user_id = user_id;
            this.id = id;
            this.stock_quantity = stock_quantity;
        }

               
        
private static final AtomicInteger orderCounter = new AtomicInteger(1);  // Declare orderCounter

public static CreateOrder fromJson(String json) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode root = objectMapper.readTree(json);

    // Validate and extract user_id
    JsonNode user_idNode = root.get("user_id");
    if (user_idNode == null || user_idNode.isNull()) {
        throw new IOException("Missing or null 'user_id' field");
    }
    String user_id = user_idNode.asText();

    //  Generate orderId here instead of using "default_order_id"
    String order_id = String.valueOf(orderCounter.getAndIncrement());


    // Validate and extract id
    JsonNode itemsNode = root.get("items");
    if (itemsNode == null || !itemsNode.isArray() || itemsNode.size() == 0) {
        throw new IOException("Missing or empty 'items' array");
    }

    JsonNode firstItem = itemsNode.get(0);
    JsonNode idNode = firstItem.get("product_id");
    JsonNode stockQuantityNode = firstItem.get("quantity");

    if (idNode == null || idNode.isNull()) {
        throw new IOException("Missing or null 'product_id' field");
    }
    if (stockQuantityNode == null || stockQuantityNode.isNull()) {
        throw new IOException("Missing or null 'quantity' field");
    }

    String id = idNode.asText();
    int stockQuantity = stockQuantityNode.asInt();

    //  Construct and return CreateOrder with generated orderId
    return new CreateOrder(order_id, user_id, id, stockQuantity);


}
}



static class CreateOrderDeserializer extends JsonDeserializer<CreateOrder> {
        @Override
        public CreateOrder deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode root = mapper.readTree(p);

            String order_id = root.has("order_id") ? root.get("order_id").asText() : "default_order_id";
            String user_id = root.get("user_id").asText();

            JsonNode itemsNode = root.get("items");
            if (itemsNode == null || !itemsNode.isArray() || itemsNode.size() == 0) {
                throw new IOException("Invalid 'items' field: Must be a non-empty array");
            }

            JsonNode firstItem = itemsNode.get(0);
            String id = firstItem.get("product_id").asText();
            int stock_quantity = firstItem.get("quantity").asInt();

            return new CreateOrder(order_id, user_id, id, stock_quantity);
        }
    }




public static class OrderCreated {
    public final String order_id;
    public final String status;
    public final double totalAmount;

    public OrderCreated(String order_id, String status, double totalAmount) {
        this.order_id = order_id;
        this.status = status;
        this.totalAmount = totalAmount;
    }
}



public static class DeleteUserOrders implements Serializable {
    
    public final String order_id;  

    public DeleteUserOrders( String order_id) {
        this.order_id =order_id;
    }
}




public static class UpdateOrderStatus implements GatewayMessage {
    public final String order_id;
    public final String status;

    @JsonCreator
    public UpdateOrderStatus(@JsonProperty("order_id") String order_id, @JsonProperty("status") String status) {
        this.order_id = order_id;
        this.status = status;
    }
}




public static class OrderStatusUpdated implements GatewayMessage {
    public final String order_id;
    public final String status;

    public OrderStatusUpdated(String order_id, String status) {
        this.order_id = order_id;
        this.status = status;
    }
}



public static class GetOrder implements GatewayMessage {
        public final String order_id;

        public GetOrder(String order_id) {
            this.order_id = order_id;
        }
    }


public static class GetAllOrders implements GatewayMessage {}

    public static class MarkOrderAsDelivered implements GatewayMessage {
        public final String order_id;

        public MarkOrderAsDelivered(String order_id) {
            this.order_id = order_id;
        }
    }


public static class GetUserOrders implements GatewayMessage {
        public final String user_id;

        public GetUserOrders(String user_id) {
            this.user_id = user_id;
        }
    }


public static class ResetMarketplace implements GatewayMessage {}

  
    public static class CheckUser implements GatewayMessage {
        public final String user_id;
        public final ProductMessages.ProductDetails product;

        public CheckUser(String user_id, ProductMessages.ProductDetails product) {
            this.user_id = user_id;
            this.product = product;
        }
    }



public static class UserCheckResult implements GatewayMessage {
        public final boolean exists;
        public final ProductMessages.ProductDetails product;

        public UserCheckResult(boolean exists, ProductMessages.ProductDetails product) {
            this.exists = exists;
            this.product = product;
        }
    }



public static class DeductWallet implements GatewayMessage {
        public final String user_id;
        public final double amount;
        public final ProductMessages.ProductDetails product;

        public DeductWallet(String user_id, double amount, ProductMessages.ProductDetails product) {
            this.user_id = user_id;
            this.amount = amount;
            this.product = product;
        }
    }


public static class WalletDeductionResult implements GatewayMessage {
        public final boolean success;
        public final ProductMessages.ProductDetails product;

        public WalletDeductionResult(boolean success, ProductMessages.ProductDetails product) {
            this.success = success;
            this.product = product;
        }
    }



public static class GetProduct implements GatewayMessage {
        public final String id;

        public GetProduct(String id) {
            this.id = id;
        }
    }
}
