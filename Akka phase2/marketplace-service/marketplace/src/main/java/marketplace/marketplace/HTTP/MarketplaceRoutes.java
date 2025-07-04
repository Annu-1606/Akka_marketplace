package marketplace.marketplace.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import marketplace.marketplace.Messages.GatewayMessages;

public class MarketplaceRoutes implements HttpHandler {

    private final ActorRef<GatewayMessages.Command> gateway;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public MarketplaceRoutes(ActorRef<GatewayMessages.Command> gateway, Duration askTimeout, Scheduler scheduler) {
        this.gateway = gateway;
        this.askTimeout = askTimeout;
        this.scheduler = scheduler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        System.out.println("HTTP " + method + " " + path + (query != null ? "?" + query : ""));

        String[] parts = path.split("/");
        try {
            if (parts.length >= 2) {
                switch (parts[1]) {
                    case "products":
                        handleProducts(exchange, parts, method);
                        return;
                    case "orders":
                        handleOrders(exchange, parts, method);
                        return;
                    case "marketplace":
                        handleMarketplace(exchange, parts, method);
                        return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + ex.getMessage() + "\"}");
            return;
        }
        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
    }

    private void handleProducts(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && method.equalsIgnoreCase("GET")) {
            CompletionStage<GatewayMessages.ProductsResponse> future =
                AskPattern.ask(gateway, GatewayMessages.GetAllProducts::new, askTimeout, scheduler);

            future.thenAccept(resp -> sendResponse(exchange, 200, resp.toJson()));
        } else if (parts.length == 3 && method.equalsIgnoreCase("GET")) {
            int productId = Integer.parseInt(parts[2]);
            CompletionStage<GatewayMessages.ProductInfo> future =
                AskPattern.ask(gateway, replyTo -> new GatewayMessages.GetProductById(productId, replyTo), askTimeout, scheduler);

            future.thenAccept(productInfo -> {
                if (productInfo.productId == -1)
                    sendResponse(exchange, 404, "{\"error\":\"Product not found\"}");
                else
                    sendResponse(exchange, 200, productInfo.toJson());
            });
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Invalid products endpoint\"}");
        }
    }

    private void handleOrders(HttpExchange exchange, String[] parts, String method) throws IOException {
        if (parts.length == 2 && method.equalsIgnoreCase("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CompletionStage<GatewayMessages.OrderInfo> future =
                AskPattern.ask(gateway, replyTo -> new GatewayMessages.CreateOrder(body, replyTo), askTimeout, scheduler);

            future.thenAccept(orderInfo -> {
                if (!"PLACED".equals(orderInfo.status))
                    sendResponse(exchange, 400, "{\"error\":\"Order creation failed\"}");
                else
                    sendResponse(exchange, 201, orderInfo.toJson());
            });
        } else if (parts.length == 3) {
            int orderId = Integer.parseInt(parts[2]);
            if (method.equalsIgnoreCase("GET")) {
                CompletionStage<GatewayMessages.OrderInfo> future =
                    AskPattern.ask(gateway, replyTo -> new GatewayMessages.GetOrderById(orderId, replyTo), askTimeout, scheduler);

                future.thenAccept(orderInfo -> {
                    if (orderInfo.orderId == -1)
                        sendResponse(exchange, 404, "{\"error\":\"Order not found\"}");
                    else
                        sendResponse(exchange, 200, orderInfo.toJson());
                });
            } else if (method.equalsIgnoreCase("PUT")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                CompletionStage<GatewayMessages.OrderInfo> future =
                    AskPattern.ask(gateway, replyTo -> new GatewayMessages.UpdateOrderById(orderId, body, replyTo), askTimeout, scheduler);

                future.thenAccept(orderInfo -> {
                    if (!"DELIVERED".equals(orderInfo.status))
                        sendResponse(exchange, 400, "{\"error\":\"Order update failed\"}");
                    else
                        sendResponse(exchange, 200, orderInfo.toJson());
                });
            } else if (method.equalsIgnoreCase("DELETE")) {
                CompletionStage<GatewayMessages.SuccessResponse> future =
                    AskPattern.ask(gateway, replyTo -> new GatewayMessages.DeleteOrderRequest(orderId, replyTo), askTimeout, scheduler);

                future.thenAccept(resp -> {
                    if (!resp.success)
                        sendResponse(exchange, 400, "{\"error\":\"Order cancellation failed\"}");
                    else
                        sendResponse(exchange, 200, resp.toJson());
                });
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Invalid orders method\"}");
            }
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Invalid orders endpoint\"}");
        }
    }

    private void handleMarketplace(HttpExchange exchange, String[] parts, String method) {
        if (parts.length == 2 && method.equalsIgnoreCase("DELETE")) {
            CompletionStage<GatewayMessages.SuccessResponse> future =
                AskPattern.ask(gateway, GatewayMessages.GlobalReset::new, askTimeout, scheduler);

            future.thenAccept(resp -> sendResponse(exchange, 200, "{\"message\":\"" + resp.message + "\"}"));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Invalid marketplace endpoint\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
