package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.example.actors.GatewayActor;
import com.example.actors.ProductRegionActor;
import com.example.actors.OrderActor;
import com.example.routes.HttpServer;
import com.example.services.WalletService;
import com.example.services.AccountService;
import com.example.utils.CsvLoader; 



public class MarketplaceApp {
    public static void main(String[] args) {
        System.out.println(" MarketplaceApp is starting...");
        
        // Create ActorSystem
        ActorSystem system = ActorSystem.create("marketplaceSystem");
        System.out.println(" Marketplace Service started...");

        //  Create ProductRegionActor 
        ActorRef productRegion = system.actorSelection("/user/productRegion")
            .resolveOne(java.time.Duration.ofSeconds(3))
            .exceptionally(ex -> system.actorOf(ProductRegionActor.props(), "productRegion"))
            .toCompletableFuture()
            .join();
        
        System.out.println(" ProductRegionActor created at: " + productRegion.path());

        //  Load products from CSV
        CsvLoader.loadProducts(productRegion);

        // Create OrderRegionActor
        ActorRef orderRegion = system.actorOf(
            OrderActor.props("order_id", "user_id", "id", 1, 100.0),
            "orderRegion"
        );

        //  Create WalletService
        WalletService walletService = new WalletService(system);

        //  Create PostOrderActor for passing to AccountService
        ActorRef postOrderActor = system.actorOf(OrderActor.props("order_id", "user_id", "id", 1, 100.0), "postOrderActor");

        //  Create AccountService with ActorSystem and postOrderActor (Fixes Compilation Error)
        AccountService accountService = new AccountService(system, postOrderActor);

        //  Ensure GatewayActor logs correct productRegion path
        ActorRef gatewayActor = system.actorSelection("/user/gatewayActor")
            .resolveOne(java.time.Duration.ofSeconds(3))
            .exceptionally(ex -> system.actorOf(GatewayActor.props(productRegion, orderRegion, null, walletService, accountService), "gatewayActor"))
            .toCompletableFuture()
            .join();

        System.out.println("📢 GatewayActor initialized with ProductRegion at: " + productRegion.path());

        //  Start HTTP Server
        HttpServer httpServer = new HttpServer(system, gatewayActor);
        httpServer.startServer();
    }
}
