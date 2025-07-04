package marketplace.marketplace;

import java.io.IOException;
import java.time.Duration;
import java.net.http.HttpClient;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.Props;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import marketplace.marketplace.HTTP.MyHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.AddressFromURIString;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Actors.GatewayActor;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarketplaceServiceApplication {

    // Declare shared static variables
    public static ActorSystem<GatewayMessages.Command> system;
    public static ActorRef<GatewayMessages.Command> gateway;
    public static Duration askTimeout;
    public static Scheduler scheduler;
    public static HttpClient httpClient;
    public static int akkaPort;
    private static final Logger logger = LoggerFactory.getLogger(MarketplaceServiceApplication.class);

    public static void main(String[] args) throws IOException {
        System.out.println(" MarketplaceApp is starting...");

        // Set default Akka port to 8083 (primary node)
        int akkaPort = 8083;
        if (args.length > 0) {
            akkaPort = Integer.parseInt(args[0]); // If port argument is passed, use it
        }
        
        logger.info("Akka node starting on port: {}", akkaPort);

        // Load Akka config, setting the port dynamically
        Config config = ConfigFactory.parseString("akka.remote.artery.canonical.port=" + akkaPort)
            .withFallback(ConfigFactory.load("application.conf"));
        System.out.println("Joining Akka cluster using seed nodes: " + config.getStringList("akka.cluster.seed-nodes"));

        // Create the Akka ActorSystem with GatewayActor as the root
        system = ActorSystem.create(GatewayActor.create(), "ClusterSystem", config);
        
        // Initialize ask timeout, scheduler, and HTTP client
        askTimeout = Duration.ofSeconds(30);
        scheduler = system.scheduler();
        httpClient = HttpClient.newHttpClient();

        // Get cluster reference and join the cluster
        Cluster cluster = Cluster.get(system);
        cluster.manager().tell(Join.create(AddressFromURIString.parse("akka://ClusterSystem@127.0.0.1:8083")));
        
        logger.info("Node joining the cluster with address: {}", cluster.selfMember().address());

        // If primary node (port 8083), start GatewayActor and HTTP server
        if (akkaPort == 8083) {
            gateway = system.systemActorOf(GatewayActor.create(), "gatewayActor", Props.empty());
            startHttpServer(); // Start HTTP server
        } else {
            // Otherwise, just join the cluster without HTTP server
            logger.info("Secondary node running on port {} (no HTTP server started)", akkaPort);
        }
    }

    // Method to start the HTTP server
    private static void startHttpServer() throws IOException {
        MyHttpServer server = new MyHttpServer(system, gateway);
        server.startServer();
        logger.info("HTTP Server started and listening on port 8081");
    }
}
