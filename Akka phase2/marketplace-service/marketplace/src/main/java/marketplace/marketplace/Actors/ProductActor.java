package marketplace.marketplace.Actors;

import akka.actor.typed.ActorRef; 
import akka.actor.typed.javadsl.*; 
import akka.actor.typed.Behavior;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Messages.ProductMessages;
import marketplace.marketplace.MarketplaceServiceApplication;

//Actor responsible for managing individual product information and stock.

public class ProductActor extends AbstractBehavior<ProductMessages.Command> {
    private final int id;
    private final String name;
    private final String description;
    private final int price;
    private int stock_quantity; // Mutable field to track available stock

    
    // Factory method to create a new ProductActor.
    
    public static Behavior<ProductMessages.Command> create(int id, String name, String description, int price, int stock_quantity) {
        return Behaviors.setup(context -> new ProductActor(context, id, name, description, price, stock_quantity));
    }

    
    // Constructor to initialize product details.
     
    private ProductActor(ActorContext<ProductMessages.Command> context, int id, String name, String description, int price, int stock_quantity) {
        super(context);
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock_quantity = stock_quantity;
    }

    
    //Defines the behavior of the ProductActor - which messages it can handle.
     
    @Override
    public Receive<ProductMessages.Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(ProductMessages.GetProductById.class, this::onGetProductById)
            .onMessage(ProductMessages.ReduceStock.class, this::onReduceStock)
            .onMessage(ProductMessages.RestoreStock.class, this::onRestoreStock)
            .build();
    }

    // Handles a request to reduce stock when an order is placed.

    private Behavior<ProductMessages.Command> onReduceStock(ProductMessages.ReduceStock msg) {
        if (stock_quantity >= msg.quantity) {
            stock_quantity -= msg.quantity;
            msg.replyTo.tell(new ProductMessages.StockResponse(true, "Stock reduced", stock_quantity));
        } else {
            msg.replyTo.tell(new ProductMessages.StockResponse(false, "Insufficient stock", stock_quantity));
        }
        return this;
    }

   // Handles a request to fetch product information.
     
    private Behavior<ProductMessages.Command> onGetProductById(ProductMessages.GetProductById msg) {
        msg.replyTo.tell(new GatewayMessages.ProductInfo(id, name, description, price, stock_quantity));
        return this;
    }

    
    

    //Handles a request to restore stock (e.g., if an order is canceled).
     
    private Behavior<ProductMessages.Command> onRestoreStock(ProductMessages.RestoreStock msg) {
        stock_quantity += msg.quantity;
        msg.replyTo.tell(new ProductMessages.StockResponse(true, "Stock restored", stock_quantity));
        return this;
    }
}
