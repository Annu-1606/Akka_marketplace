package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import com.example.messages.ProductMessages;
import com.example.models.Product;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import akka.util.Timeout;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Try;
import scala.util.Success;
import scala.util.Failure;
import scala.concurrent.ExecutionContextExecutor;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;
import java.time.temporal.ChronoUnit;




public class ProductRegionActor extends AbstractActor {
    private final Map<String, ActorRef> productActors = new HashMap<>();
    private final List<Product> products = new ArrayList<>();
    private final Map<String, List<ActorRef>> pendingRequests = new HashMap<>(); 

    public static Props props() {
        return Props.create(ProductRegionActor.class, ProductRegionActor::new);
    }




    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProductMessages.RegisterProduct.class, this::handleRegisterProduct)
                .match(ProductMessages.UpdateQuantity.class, this::handleUpdateQuantity)
                .match(ProductMessages.GetProductById.class, this::handleGetProductById)
                .match(ProductMessages.QuantityUpdated.class, this::handleQuantityUpdated)
                .match(ProductMessages.QuantityUpdateFailed.class, this::handleQuantityUpdateFailed)
                .build();
    }




private void handleRegisterProduct(ProductMessages.RegisterProduct msg) {
        products.add(new Product(msg.id, msg.name, msg.description, msg.stock_quantity, msg.price));
        System.out.println(" Product registered: " + msg.name + " (ID: " + msg.id + ")");
    }




private void handleGetProductById(ProductMessages.GetProductById msg) {
        System.out.println(" [ProductRegionActor] Looking up product ID: " + msg.id);

        Optional<Product> productOpt = products.stream()
                .filter(p -> p.getId().equals(msg.id))
                .findFirst();

        productOpt.ifPresentOrElse(
                product -> getSender().tell(new ProductMessages.ProductDetails(product), getSelf()),
                () -> getSender().tell(new ProductMessages.ProductNotFound(msg.id), getSelf())
        );
    }




private void handleUpdateQuantity(ProductMessages.UpdateQuantity msg) {
        System.out.println("[ProductRegionActor] Stock update request received for product: " + msg.id);

        ActorRef productActor = productActors.get(msg.id);

        if (productActor == null) {
            System.out.println(" Creating new ProductActor for product: " + msg.id);
            Optional<Product> productOpt = products.stream()
                    .filter(p -> p.getId().equals(msg.id))
                    .findFirst();

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                productActor = getContext().actorOf(
                        ProductActor.props(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStock_quantity(), self()),
                        "product-" + msg.id
                );
                productActors.put(msg.id, productActor);
            } else {
                System.out.println(" Product not found in registry, cannot create ProductActor!");
                getSender().tell(new ProductMessages.ProductNotFound(msg.id), getSelf());
                return;
            }
        }

        // Store the requester (PostOrderActor or DeleteOrderActor) before forwarding
        pendingRequests.computeIfAbsent(msg.id, k -> new ArrayList<>()).add(getSender());

        // Forward update request to ProductActor with a timeout
        

 Timeout timeout = Timeout.create(java.time.Duration.of(3, ChronoUnit.SECONDS));
Future<Object> future = Patterns.ask(productActor, msg, timeout);

 ExecutionContextExecutor dispatcher = getContext().dispatcher();

       future.onComplete(new scala.runtime.AbstractFunction1<Try<Object>, Void>() {
    @Override
public Void apply(Try<Object> result) {
        if (result instanceof Success) {
            self().tell(((Success<Object>) result).get(), getSelf());
        } else if (result instanceof Failure) {
            self().tell(new ProductMessages.QuantityUpdateFailed(msg.id, "Timeout or failure"), getSelf());
        }
        return null;
    }
}, dispatcher);
    }
   




private void handleQuantityUpdated(ProductMessages.QuantityUpdated msg) {
        System.out.println(" [ProductRegionActor] Stock updated for product: " + msg.id + " -> New quantity: " + msg.newQuantity);

        // Update the local product list
        products.stream()
                .filter(p -> p.getId().equals(msg.id))
                .findFirst()
                .ifPresent(product -> product.setStock_quantity(msg.newQuantity));

        // Send update response to ALL requesters
        List<ActorRef> requesters = pendingRequests.remove(msg.id);
        if (requesters != null) {
            requesters.forEach(requester -> requester.tell(msg, getSelf()));
        }
    }

private void handleQuantityUpdateFailed(ProductMessages.QuantityUpdateFailed msg) {
        System.out.println(" [ProductRegionActor] Stock update failed for product: " + msg.id + " -> Reason: " + msg.reason);

        // Inform all pending requesters of failure
        List<ActorRef> requesters = pendingRequests.remove(msg.id);
        if (requesters != null) {
            requesters.forEach(requester -> requester.tell(msg, getSelf()));
        }
    }
}
