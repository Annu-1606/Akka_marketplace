package marketplace.marketplace.Messages;

import akka.actor.typed.ActorRef;
import java.io.Serializable;
import marketplace.marketplace.Messages.GatewayMessages;

// Interface for messages related to Product actor
public interface ProductMessages extends Serializable {

    // Marker interface for all Product-related commands
    interface Command extends Serializable {}

    // Command to request product information
    class GetProductById implements Command {
        public final int productId;
        public final ActorRef<GatewayMessages.ProductInfo> replyTo;

        public GetProductById(int productId, ActorRef<GatewayMessages.ProductInfo> replyTo) {
            this.productId = productId;
            this.replyTo = replyTo;
        }
    }

    // Command to reduce stock of a product
    class ReduceStock implements Command {
        public final int quantity;
        public final ActorRef<StockResponse> replyTo;

        public ReduceStock(int quantity, ActorRef<StockResponse> replyTo) {
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    // Command to restore stock of a product
    class RestoreStock implements Command {
        public final int quantity;
        public final ActorRef<StockResponse> replyTo;

        public RestoreStock(int quantity, ActorRef<StockResponse> replyTo) {
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    // Response after a stock operation (reduce/restore)
    class StockResponse implements Serializable {
        public final boolean success;
        public final String message;
        public final int currentStock;

        public StockResponse(boolean success, String message, int currentStock) {
            this.success = success;
            this.message = message;
            this.currentStock = currentStock;
        }
    }
}
