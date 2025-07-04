package marketplace.marketplace.HTTP;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.MarketplaceServiceApplication;

// Class to create and manage a simple HTTP server
public class MyHttpServer {
    private final ActorSystem<GatewayMessages.Command> system; // ActorSystem for the Marketplace
    private final ActorRef<GatewayMessages.Command> gateway;   // Gateway actor reference

    public MyHttpServer(ActorSystem<GatewayMessages.Command> system, ActorRef<GatewayMessages.Command> gateway) {
        this.system = system;
        this.gateway = gateway;
    }

    // Method to start the HTTP server
    public void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0); // Create HTTP server on port 8081
            MarketplaceRoutes routes = new MarketplaceRoutes(gateway, MarketplaceServiceApplication.askTimeout, MarketplaceServiceApplication.scheduler);

            server.createContext("/", routes); // Set up the root context with MarketplaceRoutes
            server.setExecutor(Executors.newFixedThreadPool(10)); // Use a fixed thread pool for handling requests
            server.start(); // Start the server
            System.out.println(" Server started at http://localhost:8081");
        } catch (Exception e) {
            System.err.println(" Failed to start server: " + e.getMessage());
            e.printStackTrace();
            system.terminate(); // Terminate the actor system if server fails to start
        }
    }
}
