package marketplace.marketplace.Messages;

import akka.actor.typed.ActorRef;
import marketplace.marketplace.Messages.GatewayMessages;

// Messages used by the DeleteOrder actor
public class DeleteOrderMessages {

    // Marker interface for DeleteOrder commands
    public interface Command {}

    // Command to initiate order cancellation
    public static final class CancelOrderRequest implements Command {
        public CancelOrderRequest() {} // Constructor 
    }

    // Response after an order is cancelled
    public static final class CancelOrderResponse implements Command {
        public final GatewayMessages.OrderInfo orderInfo;

        public CancelOrderResponse(GatewayMessages.OrderInfo orderInfo) {
            this.orderInfo = orderInfo;
        }
    }
}
