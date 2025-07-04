package com.example.services;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.stream.Materializer;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.CompletableFuture;

public class AccountService {

    private final String accountServiceUrl = "http://host.docker.internal:8080/";
    private final ActorSystem system;
    private final Materializer materializer;
    private final ActorRef postOrderActor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccountService(ActorSystem system, ActorRef postOrderActor) {
        this.system = system;
        this.materializer = Materializer.createMaterializer(system);
        this.postOrderActor = postOrderActor;
        System.out.println("AccountService initialized with system: " + system.name());
    }





    public CompletableFuture<UserResponseWithDiscount> getUser(String user_id) {
        HttpRequest request = HttpRequest.create(accountServiceUrl + "users/" + user_id);

        return Http.get(system).singleRequest(request).toCompletableFuture()
                .thenCompose(response -> response.entity().getDataBytes()
                        .runFold(ByteString.emptyByteString(), ByteString::concat, materializer)
                        .thenCompose(byteString -> {
                            String responseBody = byteString.utf8String();
                            System.out.println("Received response: " + responseBody);
                            try {
                                JsonNode jsonNode = objectMapper.readTree(responseBody);
                                boolean discountAvailed = jsonNode.has("discountAvailed") && jsonNode.get("discountAvailed").asBoolean();

                                if (!discountAvailed) {
                                    // Update discountAvailed to true and send updated data back to the external service
                                    ((ObjectNode) jsonNode).put("discountAvailed", true);
                                    String updatedBody = objectMapper.writeValueAsString(jsonNode);

                                    HttpRequest updateRequest = HttpRequest.PUT(accountServiceUrl + "users/" + user_id + "/discount")
                                            .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, updatedBody));

                                    Http.get(system).singleRequest(updateRequest);
                                }
                                // Return original discountAvailed value (not modified)
                                return CompletableFuture.completedFuture(new UserResponseWithDiscount(response, discountAvailed));
                            } catch (Exception e) {
                                System.err.println("Error parsing JSON response");
                                e.printStackTrace();
                                return CompletableFuture.completedFuture(new UserResponseWithDiscount(response, false));
                            }
                        }))
                .exceptionally(ex -> {
                    System.err.println("Error occurred while requesting getUser for user ID: " + user_id);
                    ex.printStackTrace();
                    return new UserResponseWithDiscount(null, false);
                });
    }





    public static class UserResponseWithDiscount {
        public final HttpResponse response;
        public final boolean discountAvailed;

        public UserResponseWithDiscount(HttpResponse response, boolean discountAvailed) {
            this.response = response;
            this.discountAvailed = discountAvailed;
        }
    }



    
    public CompletableFuture<HttpResponse> updateUser(String user_id, String action, double amount) {
        System.out.println("Sending request to update user with ID: " + user_id + ", action: " + action + ", amount: " + amount);
        HttpRequest request = HttpRequest.PUT(accountServiceUrl + "/accounts/" + user_id + "/" + action + "/" + amount);
        return Http.get(system).singleRequest(request).toCompletableFuture()
                .thenApply(response -> {
                    System.out.println("Received response for updateUser with status: " + response.status());
                    return response;
                })
                .exceptionally(ex -> {
                    System.out.println("Error occurred while updating user with ID: " + user_id + ", action: " + action);
                    ex.printStackTrace();
                    return null;
                });
    }
}
