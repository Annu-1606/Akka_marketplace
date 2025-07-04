package com.example.routes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.CompletionStage;

public class HttpServer {

    private final ActorSystem system;
    private final ActorRef gatewayActor;

    public HttpServer(ActorSystem system, ActorRef gatewayActor) {
        this.system = system;
        this.gatewayActor = gatewayActor;
    }

    public void startServer() {
        // Load configuration
        Config config = ConfigFactory.load();
        String host = config.getString("my-app.http.interface");
        int port = config.getInt("my-app.http.port");

        final Materializer materializer = Materializer.createMaterializer(system);
        MarketplaceRoutes routes = new MarketplaceRoutes(gatewayActor);

        CompletionStage<ServerBinding> binding = Http.get(system)
                .newServerAt(host, port)
                .bind(routes.createRoute());
System.out.println(" Routes successfully bound");

        binding.whenComplete((bindingResult, throwable) -> {
            if (throwable != null) {
                System.err.println("Failed to bind server: " + throwable.getMessage());
                system.terminate();
            } else {
                System.out.println("Server started at http://" + host + ":" + port);
            }
        });
    }
}

