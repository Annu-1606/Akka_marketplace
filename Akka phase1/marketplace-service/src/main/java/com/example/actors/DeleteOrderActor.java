package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.example.messages.GatewayMessages;
import com.example.messages.OrderMessages;
import com.example.messages.ProductMessages;
import com.example.services.WalletService;
import scala.concurrent.Future;
import scala.compat.java8.FutureConverters;
import com.example.actors.OrderRegionActor;
import com.example.actors.ProductRegionActor;
import akka.actor.ActorSelection;

import java.time.Duration;
import java.util.concurrent.CompletionStage;



public class DeleteOrderActor extends AbstractActor {

    private final GatewayMessages.DeleteUserOrders deleteOrderMsg;
    private final WalletService walletService;
    private final ActorRef orderRegionActor;
    private final ActorRef productRegionActor;
    private ActorRef originalSender;

    public DeleteOrderActor(GatewayMessages.DeleteUserOrders deleteOrderMsg, WalletService walletService, ActorRef orderRegionActor, ActorRef productRegionActor) {
        this.deleteOrderMsg = deleteOrderMsg;
        this.walletService = walletService;
        this.orderRegionActor = orderRegionActor;
        this.productRegionActor = productRegionActor;
    }

    public static Props props(GatewayMessages.DeleteUserOrders deleteOrderMsg, WalletService walletService, ActorRef orderRegionActor, ActorRef productRegionActor) {
        return Props.create(DeleteOrderActor.class, () -> new DeleteOrderActor(deleteOrderMsg, walletService, orderRegionActor, productRegionActor));
    }

    



    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("start", msg -> processDelete(getSender()))
                .match(ProductMessages.QuantityUpdated.class, this::handleQuantityUpdate)
                .match(ProductMessages.QuantityUpdateFailed.class, this::handleQuantityUpdateFailed)
                .match(OrderMessages.OrderStatusUpdated.class, this::handleOrderStatusUpdate)
                .build();
    }




    private void processDelete(ActorRef sender) {
        this.originalSender = sender;
        Timeout timeout = Timeout.create(Duration.ofSeconds(5));
        Future<Object> orderFuture = Patterns.ask(orderRegionActor, new OrderMessages.GetOrderExisting(deleteOrderMsg.order_id), timeout);
        System.out.println(" order_id sent to delete order " + deleteOrderMsg.order_id);
        CompletionStage<Object> completionStage = FutureConverters.toJava(orderFuture);
        System.out.println(" [Deleteorder Actor]: recieved at processdelete ");
        completionStage.thenApply(response -> {
            if (response instanceof OrderMessages.OrderDetails) {
                OrderMessages.OrderDetails order = (OrderMessages.OrderDetails) response;
                System.out.println(" [Deleteorder Actor]: Retrieved Order: " + order.order_id);
                System.out.println(" [Deleteorder Actor]: order status is: " + order.status);
                if (!"Cancelled".equals(order.status)) {
                System.out.println(" [Deleteorder Actor]: Initiating refund for order: " + order.order_id);
                    walletService.updateBalance(order.user_id, "credit", order.totalAmount)
                        .thenAccept(res -> {
                            if (res != null && res.status().isSuccess()) {
                            System.out.println(" [Deleteorder Actor]: Updating stock for product " + order.id);
                                productRegionActor.tell(new ProductMessages.UpdateQuantity(order.id, order.stock_quantity, self()), self());
                            } else {
                            System.out.println(" [Deleteorder Actor]: Refund Failed for order " + order.order_id);
                                originalSender.tell(new OrderMessages.OrderProcessed(order.order_id, order.user_id, "Refund Failed"), self());
                                getContext().stop(self());
                            }
                        });
                } else {
                    originalSender.tell(new OrderMessages.OrderProcessed(order.order_id, order.user_id, "Order Already Cancelled"), self());
                   System.out.println(" [Deleteorder Actor]: order already cancelled " + order.id);
                    getContext().stop(self());
                }
            } else {
                originalSender.tell(new OrderMessages.OrderProcessed(deleteOrderMsg.order_id, "UnknownUser", "Order Not Found"), self());
                getContext().stop(self());
            }
            return null;
        }).exceptionally(ex -> {
            originalSender.tell(new OrderMessages.OrderProcessed(deleteOrderMsg.order_id, "UnknownUser", "Error Processing Order"), self());
            getContext().stop(self());
            return null;
        });
        
    }





    private void handleQuantityUpdate(ProductMessages.QuantityUpdated msg) {
        System.out.println("[deleteOrderActor] recieved quantity updated");
        orderRegionActor.tell(new OrderMessages.UpdateOrderStatus(deleteOrderMsg.order_id, "Cancelled"), self());
        System.out.println("[deleteorder actor] quantity updated");
    }





    private void handleQuantityUpdateFailed(ProductMessages.QuantityUpdateFailed msg) {
        System.out.println(" [deleteOrderActor] recieved quantity updatedfailed");
        originalSender.tell(new OrderMessages.OrderProcessed(deleteOrderMsg.order_id, deleteOrderMsg.order_id, "Stock Update Failed"), self());
        getContext().stop(self());
        System.out.println("[deleteOrderActor] quantity update failed");
    }





   private void handleOrderStatusUpdate(OrderMessages.OrderStatusUpdated msg) {
    System.out.println("[DeleteOrderActor] Received OrderStatusUpdated");

    System.out.println("[DeleteOrderActor] Sending to " + originalSender);
    ActorSelection gatewaySelection = getContext().actorSelection("/user/gatewayActor");
    gatewaySelection.tell(new OrderMessages.OrderProcessed(msg.order_id, deleteOrderMsg.order_id, "Order Cancelled Successfully"), self());


    getContext().stop(self());
    System.out.println("[DeleteOrderActor] OrderStatusUpdated processed.");
}



}

