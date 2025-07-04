package com.example.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.messages.ProductMessages;

public class ProductActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String id;
    private final String name;
    private final String description;
    private final double price;
    private int stock_quantity;
    private final ActorRef productRegionActor;  // ✅ Reference to ProductRegionActor

    public ProductActor(String id, String name, String description, double price, int stock_quantity, ActorRef productRegionActor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock_quantity = stock_quantity;
        this.productRegionActor = productRegionActor;
    }




    public static Props props(String id, String name, String description, double price, int stock_quantity, ActorRef productRegionActor) {
        return Props.create(ProductActor.class, () -> new ProductActor(id, name, description, price, stock_quantity, productRegionActor));
    }




    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProductMessages.UpdateQuantity.class, msg -> {
                    if (this.stock_quantity + msg.quantityChange < 0) {
                        log.warning("Quantity update failed for {}: insufficient stock", id);
                        getSender().tell(new ProductMessages.QuantityUpdateFailed(id, "Not enough stock"), getSelf());
                    } else {
                        this.stock_quantity += msg.quantityChange;
                        log.info("Updated quantity for {}: {}", id, stock_quantity);

                        //  Notify ProductRegionActor to update its list
                        productRegionActor.tell(new ProductMessages.QuantityUpdated(id, stock_quantity), getSelf());
                    }
                })
                .build();
    }
}
