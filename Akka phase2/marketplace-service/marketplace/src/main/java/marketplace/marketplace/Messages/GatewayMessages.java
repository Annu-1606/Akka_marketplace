package marketplace.marketplace.Messages;

import akka.actor.typed.ActorRef;
import java.util.List;

// Interface containing all messages handled by the Gateway Actor
public interface GatewayMessages {

    // Marker interface for Gateway commands
    interface Command {}

    // Command to get all product IDs
    class GetAllProducts implements Command {
        public final ActorRef<ProductsResponse> replyTo;
        public GetAllProducts(ActorRef<ProductsResponse> replyTo) { this.replyTo = replyTo; }
    }

    // Command to get details of a specific product
    class GetProductById implements Command {
        public final int productId;
        public final ActorRef<ProductInfo> replyTo;
        public GetProductById(int productId, ActorRef<ProductInfo> replyTo) { this.productId = productId; this.replyTo = replyTo; }
    }

    // Command to create a new order
    class CreateOrder implements Command {
        public final String orderData;
        public final ActorRef<OrderInfo> replyTo;
        public CreateOrder(String orderData, ActorRef<OrderInfo> replyTo) { this.orderData = orderData; this.replyTo = replyTo; }
    }

    // Command to get details of a specific order
    class GetOrderById implements Command {
        public final int orderId;
        public final ActorRef<OrderInfo> replyTo;
        public GetOrderById(int orderId, ActorRef<OrderInfo> replyTo) { this.orderId = orderId; this.replyTo = replyTo; }
    }

    // Command to update an existing order
    class UpdateOrderById implements Command {
        public final int orderId;
        public final String updateData;
        public final ActorRef<OrderInfo> replyTo;
        public UpdateOrderById(int orderId, String updateData, ActorRef<OrderInfo> replyTo) { this.orderId = orderId; this.updateData = updateData; this.replyTo = replyTo; }
    }

    // Command to delete an order
    class DeleteOrderRequest implements Command {
        public final int orderId;
        public final ActorRef<SuccessResponse> replyTo;
        public DeleteOrderRequest(int orderId, ActorRef<SuccessResponse> replyTo) { this.orderId = orderId; this.replyTo = replyTo; }
    }

    // Command to reset the system state
    class GlobalReset implements Command {
        public final ActorRef<SuccessResponse> replyTo;
        public GlobalReset(ActorRef<SuccessResponse> replyTo) { this.replyTo = replyTo; }
    }

    // Class representing information about a single product
    class ProductInfo {
        public final int productId;
        public final String name;
        public final String description;
        public final int price;
        public final int stock_quantity;

        public ProductInfo(int productId, String name, String description, int price, int stock_quantity) {
            this.productId = productId;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock_quantity = stock_quantity;
        }

        // Convert product info to JSON format
        public String toJson() {
            return String.format("{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\",\"price\":%d,\"stock_quantity\":%d}",
                    productId, name, description, price, stock_quantity);
        }
    }

    // Response containing a list of product IDs
    class ProductsResponse {
        public final List<Integer> productIds;
        public ProductsResponse(List<Integer> productIds) { this.productIds = productIds; }

        // Convert product IDs to JSON array
        public String toJson() {
            StringBuilder sb = new StringBuilder("[");
            for (int id : productIds) {
                sb.append(String.format("{\"id\":%d},", id));
            }
            if (!productIds.isEmpty()) sb.deleteCharAt(sb.length()-1);
            sb.append("]");
            return sb.toString();
        }
    }

    // Class representing information about a single order
    class OrderInfo {
        public final int orderId;
        public final int user_id;
        public final int total_price;
        public final String status;
        public final List<OrderMessages.OrderItemInfo> items;

        public OrderInfo(int orderId, int user_id, int total_price, String status, List<OrderMessages.OrderItemInfo> items) {
            this.orderId = orderId;
            this.user_id = user_id;
            this.total_price = total_price;
            this.status = status;
            this.items = items;
        }

        // Convert order info to JSON format
        public String toJson() {
            StringBuilder itemsJson = new StringBuilder("[");
            for (OrderMessages.OrderItemInfo item : items) {
                itemsJson.append(item.toJson()).append(",");
            }
            if (!items.isEmpty()) itemsJson.deleteCharAt(itemsJson.length()-1);
            itemsJson.append("]");
            return String.format("{\"order_id\":%d,\"user_id\":%d,\"total_price\":%d,\"status\":\"%s\",\"items\":%s}",
                    orderId, user_id, total_price, status, itemsJson.toString());
        }
    }

    // Response containing a list of all orders
    class OrdersResponse {
        public final List<OrderInfo> orders;
        public OrdersResponse(List<OrderInfo> orders) { this.orders = orders; }

        // Convert list of orders to JSON array
        public String toJson() {
            StringBuilder sb = new StringBuilder("[");
            for (OrderInfo order : orders) {
                sb.append(order.toJson()).append(",");
            }
            if (!orders.isEmpty()) sb.deleteCharAt(sb.length()-1);
            sb.append("]");
            return sb.toString();
        }
    }

    // response indicating success or failure
    class SuccessResponse {
        public final boolean success;
        public final String message;

        public SuccessResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Convert response to JSON format
        public String toJson() {
            return String.format("{\"success\": %b, \"message\": \"%s\"}", success, message);
        }
    }
}
