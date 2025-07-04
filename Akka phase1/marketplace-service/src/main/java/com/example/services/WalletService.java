package com.example.services;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.Materializer;
import akka.util.ByteString;

import java.util.concurrent.CompletableFuture;

public class WalletService {

    private final String walletServiceUrl = "http://host.docker.internal:8082"; // Ensure it's HTTP or change if using HTTPS
    private final ActorSystem system;
    private final Materializer materializer;

    // Constructor
    public WalletService(ActorSystem system) {
        this.system = system;
        this.materializer = Materializer.createMaterializer(system);
    }




    //  Get Balance
    public CompletableFuture<HttpResponse> getBalance(String user_id) {
        HttpRequest request = HttpRequest.create(walletServiceUrl + "/wallets/" + user_id);
        return Http.get(system).singleRequest(request)
                .toCompletableFuture()
                .thenApply(response -> {
                    response.discardEntityBytes(system); // ✅ Prevent memory leak
                    return response;
                })
                .exceptionally(ex -> {
                    System.err.println(" Error fetching balance for user " + user_id + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }




    //  Update Balance (Fixes "400 Bad Request")
    public CompletableFuture<HttpResponse> updateBalance(String user_id, String action, double amount) {
        // Construct JSON payload
        String jsonPayload = String.format("{\"action\": \"%s\", \"amount\": %.2f}", action, amount);
        RequestEntity entity = HttpEntities.create(ContentTypes.APPLICATION_JSON, jsonPayload);

        // Send PUT request with payload
        HttpRequest request = HttpRequest.PUT(walletServiceUrl + "/wallets/" + user_id)
                                         .withEntity(entity);

        return Http.get(system).singleRequest(request)
                .toCompletableFuture()
                .thenApply(response -> {
                    response.discardEntityBytes(system); 
                    return response;
                })
                .exceptionally(ex -> {
                    System.err.println(" Error updating balance for user " + user_id + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }





    //  Credit Method (Handles crediting money)
    public void credit(String user_id, double amount) {
        CompletableFuture<HttpResponse> response = updateBalance(user_id, "credit", amount);
        response.thenAccept(res -> {
            if (res != null && res.status().isSuccess()) {
                System.out.println(" Successfully credited " + amount + " to user " + user_id);
            } else {
                System.err.println(" Failed to credit amount to user " + user_id);
            }
        });
    }




    //  Check if a user exists in the Wallet Service
    public CompletableFuture<Boolean> checkUserExists(String user_id) {
        HttpRequest request = HttpRequest.create(walletServiceUrl + "/wallets/" + user_id);
        return Http.get(system).singleRequest(request)
                .toCompletableFuture()
                .thenCompose(response -> {
                    // Read response body to avoid memory leaks
                    return Unmarshaller.entityToString().unmarshal(response.entity(), system)
                            .thenApply(body -> {
                                response.discardEntityBytes(system);
                                return response.status().isSuccess();
                            });
                })
                .exceptionally(ex -> {
                    System.err.println(" Error checking user existence: " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }
}

