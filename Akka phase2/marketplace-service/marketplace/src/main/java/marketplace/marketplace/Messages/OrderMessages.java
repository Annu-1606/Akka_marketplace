package marketplace.marketplace.Messages;

import akka.actor.typed.ActorRef;
import marketplace.marketplace.Messages.GatewayMessages;
import java.util.List;

// Messages related to orders
public class OrderMessages {

    // Marker interface for all order commands
    public interface Command {}

    // Command to get details of a specific order
    public static final class GetOrderById implements Command {
        public final int orderId;
        public final ActorRef<GatewayMessages.OrderInfo> replyTo;

        public GetOrderById(int orderId, ActorRef<GatewayMessages.OrderInfo> replyTo) {
            this.orderId = orderId;
            this.replyTo = replyTo;
        }
    }

    // Command to update an existing order
    public static final class UpdateOrderById implements Command {
        public final int orderId;
        public final String updateData;
        public final ActorRef<GatewayMessages.OrderInfo> replyTo;

        public UpdateOrderById(int orderId, String updateData, ActorRef<GatewayMessages.OrderInfo> replyTo) {
            this.orderId = orderId;
            this.updateData = updateData;
            this.replyTo = replyTo;
        }
    }

    // Command to cancel an order ny ID
    public static final class CancelOrderById implements Command {
        public final int orderId;
        public final ActorRef<GatewayMessages.OrderInfo> replyTo;

        public CancelOrderById(int orderId, ActorRef<GatewayMessages.OrderInfo> replyTo) {
            this.orderId = orderId;
            this.replyTo = replyTo;
        }
    }

    // Class representing an item in an order
    public static final class OrderItem {
        public final int id;
        public final int productId;
        public final int quantity;

        public OrderItem(int id, int productId, int quantity) {
            this.id = id;
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    // Class representing information about an order item
    public static final class OrderItemInfo {
        public final int id;
        public final int productId;
        public final int quantity;

        public OrderItemInfo(int id, int productId, int quantity) {
            this.id = id;
            this.productId = productId;
            this.quantity = quantity;
        }

        // Converts order item info to JSON format
        public String toJson() {
            return String.format("{\"id\":%d,\"product_id\":%d,\"quantity\":%d}", id, productId, quantity);
        }
    }
}
