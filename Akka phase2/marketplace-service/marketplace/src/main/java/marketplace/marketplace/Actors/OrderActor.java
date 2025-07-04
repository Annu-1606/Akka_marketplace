package marketplace.marketplace.Actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import marketplace.marketplace.Messages.OrderMessages;
import marketplace.marketplace.Messages.GatewayMessages;

import java.util.*;

// OrderActor handles actions related to a single order
public class OrderActor extends AbstractBehavior<OrderMessages.Command> {
    // Order details
    private final int orderId;
    private final int user_id;
    private int total_price;
    private String status; 
    private final List<OrderMessages.OrderItem> items;

    // List to keep track of user IDs
    private final List<Integer> userIdList;

    // Factory method to create a new OrderActor
    public static Behavior<OrderMessages.Command> create(int orderId, int user_id, int total_price, List<OrderMessages.OrderItem> items, List<Integer> userIdList) {
        return Behaviors.setup(context -> new OrderActor(context, orderId, user_id, total_price, items, userIdList));
    }

    // Constructor
    private OrderActor(ActorContext<OrderMessages.Command> context, int orderId, int user_id, int total_price, List<OrderMessages.OrderItem> items, List<Integer> userIdList) {
        super(context);
        this.orderId = orderId;
        this.user_id = user_id;
        this.total_price = total_price;
        this.status = "PLACED"; // New orders start with "PLACED" status
        this.items = items;
        this.userIdList = userIdList;

        // Add user_id to userIdList if not already present
        if (!this.userIdList.contains(user_id)) {
            this.userIdList.add(user_id);
        }
    }

    // Define the messages this actor can handle
    @Override
    public Receive<OrderMessages.Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(OrderMessages.GetOrderById.class, this::onGetOrderById)
            .onMessage(OrderMessages.UpdateOrderById.class, this::onUpdateOrderById)
            .onMessage(OrderMessages.CancelOrderById.class, this::onCancelOrderById)
            .build();
    }



     // Handle CancelOrder request: only allow cancellation if status is "PLACED"
     private Behavior<OrderMessages.Command> onCancelOrderById(OrderMessages.CancelOrderById msg) {
        if ("PLACED".equals(status)) {
            status = "CANCELLED";
            msg.replyTo.tell(new GatewayMessages.OrderInfo(orderId, user_id, total_price, status, getItemsInfo()));
        } else {
            // If not allowed, reply with invalid OrderInfo
            msg.replyTo.tell(new GatewayMessages.OrderInfo(-1, -1, 0, "", new ArrayList<>()));
        }
        return this;
    }





    // Handle UpdateOrder request: only allow update to "DELIVERED" if status is "PLACED"
    private Behavior<OrderMessages.Command> onUpdateOrderById(OrderMessages.UpdateOrderById msg) {
        if ("PLACED".equals(status) && msg.updateData.contains("DELIVERED")) {
            status = "DELIVERED";
            msg.replyTo.tell(new GatewayMessages.OrderInfo(orderId, user_id, total_price, status, getItemsInfo()));
        } else {
            // If not allowed, reply with invalid OrderInfo
            msg.replyTo.tell(new GatewayMessages.OrderInfo(-1, -1, 0, "", new ArrayList<>()));
        }
        return this;
    }



    // Handle GetOrderById request: reply with current order information
    private Behavior<OrderMessages.Command> onGetOrderById(OrderMessages.GetOrderById msg) {
        msg.replyTo.tell(new GatewayMessages.OrderInfo(orderId, user_id, total_price, status, getItemsInfo()));
        return this;
    }

    
   

    // Helper method to convert order items into OrderItemInfo list
    private List<OrderMessages.OrderItemInfo> getItemsInfo() {
        List<OrderMessages.OrderItemInfo> infos = new ArrayList<>();
        for (OrderMessages.OrderItem item : items) {
            infos.add(new OrderMessages.OrderItemInfo(item.id, item.productId, item.quantity));
        }
        return infos;
    }
}
