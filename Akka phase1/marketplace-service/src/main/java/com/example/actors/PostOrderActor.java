package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.example.messages.*;
import com.example.services.AccountService;
import com.example.services.WalletService;
import akka.http.javadsl.model.HttpResponse;
import com.example.actors.ProductRegionActor;
import com.example.actors.OrderRegionActor;
import java.util.concurrent.CompletionStage;


public class PostOrderActor extends AbstractActor {

    private final GatewayMessages.CreateOrder createOrderMsg;
    private final AccountService accountService;
    private final WalletService walletService;
    private final ActorRef senderRef;
    private ProductMessages.ProductDetails productDetails;
    private boolean isDiscountAvailed = false; 
    private final ActorRef productRegionActor;
    private final ActorRef orderRegionActor;
    



public PostOrderActor(GatewayMessages.CreateOrder createOrderMsg, AccountService accountService, 
                      WalletService walletService, ActorRef senderRef, ActorRef productRegionActor, ActorRef orderRegionActor) {
    this.createOrderMsg = createOrderMsg;
    this.accountService = accountService;
    this.walletService = walletService;
    this.senderRef = senderRef;
    this.productRegionActor = productRegionActor;
    this.orderRegionActor = orderRegionActor; 
}





 public static Props props(GatewayMessages.CreateOrder createOrderMsg, AccountService accountService, WalletService walletService, ActorRef senderRef,ActorRef productRegionActor ,ActorRef orderRegionActor)  {
        return Props.create(PostOrderActor.class, () -> new PostOrderActor(createOrderMsg, accountService, walletService, senderRef,productRegionActor,orderRegionActor));
    }




    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("start", msg -> checkProductAvailability())
                .match(ProductMessages.ProductDetails.class, this::handleProductDetails)
                .match(ProductMessages.ProductNotFound.class, msg -> failOrder(" Product Not Found"))
                .match(GatewayMessages.WalletDeductionResult.class, this::handleWalletDeductionResult) 
                .match(ProductMessages.QuantityUpdated.class, this::handleQuantityUpdated)
                .match(ProductMessages.QuantityUpdateFailed.class, msg -> failOrder(" Stock Update Failed: " + msg.reason))
                .match(OrderMessages.OrderDetails.class, this::handleOrderDetails)
                .build();
    }




//  Step 1: Check if product is available
private void checkProductAvailability() {
        System.out.println(" Checking product availability...");
        System.out.println(" [PostOrderActor] Created with Product ID: " + createOrderMsg.id);
        System.out.println(" [PostOrderActor] senderRef (GatewayActor): " + senderRef.path());
        senderRef.tell(new ProductMessages.GetProductById(createOrderMsg.id), self());
        //gatewayActor.tell(new GatewayMessages.OrderCreated(order_id, msg.user_id, msg.id), getSelf());

    }




private void handleProductDetails(ProductMessages.ProductDetails product) {
        if (product.stock_quantity < createOrderMsg.stock_quantity) {
            failOrder(" Insufficient Product Quantity");
            return;
        }
        this.productDetails = product;
        checkUserExists();
    }



//  Step 2: Verify user exists via AccountService and check discount
private void checkUserExists() {
    System.out.println(" Checking if user exists and if discount is available...");

    CompletionStage<AccountService.UserResponseWithDiscount> userResponse = accountService.getUser(createOrderMsg.user_id);
    
    userResponse.thenAccept(userResponseWithDiscount -> {
        HttpResponse response = userResponseWithDiscount.response;
        boolean discountAvailed = userResponseWithDiscount.discountAvailed;
        System.out.println("discountavailed value " + discountAvailed);

        if (response != null && response.status().isSuccess()) {
            // Extract and apply discount status
            isDiscountAvailed = discountAvailed;
            if (!isDiscountAvailed) {
                System.out.println("🎉 Discount Applied! ");
            }

            //  Proceed to calculate total amount and deduct from wallet
            double totalAmount = calculateTotalAmount();
            sendWalletDeductionRequest(totalAmount);
        } else {
            failOrder(" User Not Found or Error in Response");
        }
    }).exceptionally(ex -> {
        failOrder(" Error checking user: " + ex.getMessage());
        return null;
    });
}




//  Step 3: Calculate total amount with discount (if availed)
private double calculateTotalAmount() {
        double totalAmount = productDetails.price * createOrderMsg.stock_quantity;
        if (!isDiscountAvailed) {
            System.out.println(" Discount Applied! 10% off.");
            totalAmount *= 0.9; 
        }
        return totalAmount;
    }




//  Step 4: Deduct from wallet
private void sendWalletDeductionRequest(double amount) {
        System.out.println(" Deducting wallet balance via WalletService...");

        CompletionStage<HttpResponse> deductionResponse = walletService.updateBalance(createOrderMsg.user_id, "debit", amount);
        deductionResponse.thenAccept(response -> {
    if (response != null && response.status().isSuccess()) {
        System.out.println(" Wallet deduction successful, proceeding with stock deduction.");
        self().tell(new GatewayMessages.WalletDeductionResult(true, productDetails), self()); // ✅ FIXED
    } else {
        System.out.println(" Wallet deduction failed.");
        self().tell(new GatewayMessages.WalletDeductionResult(false, productDetails), self()); // ✅ FIXED
    }
}).exceptionally(ex -> {
    System.out.println(" Error deducting wallet: " + ex.getMessage());
    self().tell(new GatewayMessages.WalletDeductionResult(false, productDetails), self()); // ✅ FIXED
    return null;
});

    }




    //  Step 5: Handle Wallet Deduction Result
private void handleWalletDeductionResult(GatewayMessages.WalletDeductionResult msg) {
        if (msg.success) {
            sendStockDeductionRequest();
        } else {
            failOrder(" Wallet Deduction Failed or Insufficient Balance");
        }
    }





    //  Step 6: Deduct stock

private void sendStockDeductionRequest() {
    System.out.println(" Reducing stock quantity...");

    // Ensure postOrderActor is available; it could be a member variable or passed to this method
    ActorRef postOrderActor = self();  // Assign the correct ActorRef for PostOrderActor

    // Send the UpdateQuantity message to the ProductRegionActor with the postOrderActor
    productRegionActor.tell(new ProductMessages.UpdateQuantity(createOrderMsg.id, -createOrderMsg.stock_quantity, postOrderActor), self());

    System.out.println(" Stock update message sent to ProductRegionActor");
}




    //  Step 7: Handle stock update confirmation
private void handleQuantityUpdated(ProductMessages.QuantityUpdated msg) {
        createOrder();
    }


  
    
//  Step 8: Create order
private void createOrder() {
        System.out.println(" Creating order...");
        double totalPrice = calculateTotalAmount();

        orderRegionActor.tell(
            new OrderMessages.CreateOrder(createOrderMsg.order_id, createOrderMsg.user_id,
                                          createOrderMsg.id, createOrderMsg.stock_quantity,
                                          totalPrice, "Confirmed"),
            self()
        );

        
    }




//  Step 9: Handle order details from OrderRegionActor and forward to GatewayActor
private void handleOrderDetails(OrderMessages.OrderDetails msg) {
    System.out.println(" Received Order Details: " + msg);

    //  Send OrderCreated message to GatewayActor (senderRef)
    senderRef.tell(new GatewayMessages.OrderCreated(msg.order_id, msg.status, msg.totalAmount), getSelf());
    System.out.println(" [PostOrderActor] Senderref for it " + senderRef);

    //  Stop self after sending
    getContext().stop(getSelf());
}



// Handle Order Failure
private void failOrder(String reason) {
        senderRef.tell(new OrderMessages.OrderProcessed(createOrderMsg.order_id, "Failed", reason), getSelf());
        getContext().stop(getSelf());
    }

    
}
