package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.messages.GatewayMessages;
import com.example.messages.OrderMessages;
import com.example.messages.ProductMessages;
import com.example.services.WalletService;
import com.example.services.AccountService;
import com.example.actors.ProductRegionActor;
import com.example.actors.OrderRegionActor;


import java.util.concurrent.atomic.AtomicInteger;


public class GatewayActor extends AbstractActor {
    
    
    private static final AtomicInteger orderCounter = new AtomicInteger(1);
    private final ActorRef productRegionActor;
    private final ActorRef orderRegionActor;
    private final ActorRef walletActor;
    private final WalletService walletService;
    private final AccountService accountService;


    // Constructor with both services (WalletService and AccountService)
    public GatewayActor(ActorRef productRegionActor, ActorRef orderRegionactor, ActorRef walletActor, WalletService walletService, AccountService accountService) {
        this.productRegionActor = productRegionActor;
        this.orderRegionActor = getContext().actorOf(Props.create(OrderRegionActor.class), "orderRegionActor");
        this.walletActor = walletActor;
        this.walletService = walletService;
        this.accountService = accountService;
    }

    // Static Props method that accepts both services and creates GatewayActor
   public static Props props(ActorRef productRegion, ActorRef orderRegionActor, ActorRef walletActor, WalletService walletService, AccountService accountService) {
    return Props.create(GatewayActor.class, () -> new GatewayActor(productRegion, orderRegionActor, walletActor, walletService, accountService));
}




    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GatewayMessages.CreateOrder.class, this::handleCreateOrder)
                .match(GatewayMessages.GetOrder.class, this::handleGetOrder)
                .match(GatewayMessages.DeleteUserOrders.class, this::handleDeleteUserOrders)
                .match(GatewayMessages.GetProduct.class, this::handleGetProduct)
                .match(ProductMessages.GetAllProducts.class, this::handleGetAllProducts)
                .match(ProductMessages.GetProductById.class, this::handleGetProductById)
                .match(GatewayMessages.GetUserOrders.class, this::handleGetUserOrders)
                .match(GatewayMessages.UpdateOrderStatus.class, this::handleUpdateOrderStatus)
                .match(GatewayMessages.OrderStatusUpdated.class, this::handleOrderStatusUpdated)
                .match(GatewayMessages.OrderCreated.class, this::handleOrderCreated)
                .match(OrderMessages.OrderProcessed.class, this::handleOrderProcessed)
                .build();
    }

 
    



private ActorRef originalSender;  // Store HTTP sender

private void handleCreateOrder(GatewayMessages.CreateOrder msg) {
    originalSender = getSender();  // Store the HTTP sender

    System.out.println(" [GatewayActor] Creating PostOrderActor for Order ID: " + msg.order_id);
    ActorRef postOrderActor = getContext().actorOf(PostOrderActor.props(
        msg, accountService, walletService, getSelf(), productRegionActor, orderRegionActor
    ), "order-" + msg.order_id);
    
    postOrderActor.tell("start", getSelf());
}




private void handleOrderCreated(GatewayMessages.OrderCreated msg) {
    System.out.println(" [GatewayActor] Order Created: " + msg.order_id);

    // ✅ Send order ID back to the original HTTP sender
    if (originalSender != null) {
        originalSender.tell(msg, getSelf());
        System.out.println(" [GatewayActor] Order details sent back to original sender: " + originalSender);
    } else {
        System.out.println(" [GatewayActor] No original sender found!");
    }
}




    
    // ✅ Fetch User Orders
private void handleGetUserOrders(GatewayMessages.GetUserOrders msg) {
    System.out.println(" [GatewayActor] Fetching orders for User ID: " + msg.user_id);
    orderRegionActor.forward(new OrderMessages.GetUserOrders(msg.user_id), getContext());
}



    // ✅ Fetch Order
    private void handleGetOrder(GatewayMessages.GetOrder msg) {
        System.out.println(" [GatewayActor] Getting Order: " + msg.order_id);
        orderRegionActor.forward(new OrderMessages.GetOrder(msg.order_id), getContext());
    }




    // ✅ Handle Order Deletion
    private void handleDeleteUserOrders(GatewayMessages.DeleteUserOrders msg) {
        originalSender = getSender(); 
        System.out.println(" [GatewayActor] Deleting Order: " + msg.order_id);
        ActorRef deleteOrderActor = getContext().actorOf(DeleteOrderActor.props(msg, walletService, orderRegionActor, productRegionActor));
        deleteOrderActor.tell("start", getSelf());
    }




    // ✅ Fetch Product Details
    private void handleGetProduct(GatewayMessages.GetProduct msg) {
        System.out.println(" [GatewayActor] Getting Product: " + msg.id);
        productRegionActor.tell(new ProductMessages.GetProduct(msg.id), getSelf());

        // Log response received
        getContext().become(receiveBuilder()
            .match(ProductMessages.ProductDetails.class, product -> {
                System.out.println("[GatewayActor] Product found: " + product.id);
                sender().tell(product, getSelf());
            })
            .matchAny(obj -> {
                System.out.println(" [GatewayActor] Unexpected response: " + obj);
            })
            .build());
    }






    // ✅ Fetch All Products
    private void handleGetAllProducts(ProductMessages.GetAllProducts msg) {
        System.out.println(" [GatewayActor] Getting all products");
        productRegionActor.forward(msg, getContext());
    }



    // ✅ Fetch Product by ID
    private void handleGetProductById(ProductMessages.GetProductById msg) {
        System.out.println(" [GatewayActor] Getting Product by ID: " + msg.id);
        productRegionActor.forward(msg, getContext());
        System.out.println(" [GatewayActor] message forwarded to productregion");
    }





    // ✅ Handle Order Status Update
    private void handleUpdateOrderStatus(GatewayMessages.UpdateOrderStatus msg) {
        System.out.println(" [GatewayActor] Updating Order Status: Order ID: " + msg.order_id + ", New Status: " + msg.status);
        orderRegionActor.forward(new OrderMessages.UpdateOrderStatus(msg.order_id, msg.status), getContext());
    }



    // ✅ Handle Order Status Updated Response
    private void handleOrderStatusUpdated(GatewayMessages.OrderStatusUpdated msg) {
        System.out.println(" [GatewayActor] Order Status Updated: Order ID: " + msg.order_id + ", Status: " + msg.status);
    }
    



    
    private void handleOrderProcessed(OrderMessages.OrderProcessed msg) {
    System.out.println(" [GatewayActor] Received OrderProcessed for Order ID: " + msg.order_id);
    System.out.println(" [GatewayActor] Original Sender: " + originalSender);

    if (originalSender != null) {
        originalSender.tell(msg, getSelf());
        System.out.println(" [GatewayActor] Sent OrderProcessed back to original sender: " + originalSender);
    } else {
        System.out.println(" [GatewayActor] No original sender found for order status update!");
    }
}




   
}
