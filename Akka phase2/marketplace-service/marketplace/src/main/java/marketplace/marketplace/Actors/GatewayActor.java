package marketplace.marketplace.Actors;


import akka.actor.typed.ActorRef; 
import akka.actor.typed.javadsl.*; 
import akka.actor.typed.Behavior; 
import java.io.*; 
import java.nio.file.*; 
import java.util.*; 
import java.util.concurrent.ConcurrentHashMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Messages.ProductMessages;
import marketplace.marketplace.Messages.OrderMessages;
import marketplace.marketplace.Actors.ProductActor;
import marketplace.marketplace.Actors.DeleteOrderActor;
import marketplace.marketplace.CsvLoader;
import java.io.FileInputStream;
import java.io.IOException;


public class GatewayActor extends AbstractBehavior<GatewayMessages.Command> {
    
    // Maps to hold Product and Order Actors
    private final Map<Integer, ActorRef<ProductMessages.Command>> productActors = new ConcurrentHashMap<>();
    private final Map<Integer, ActorRef<OrderMessages.Command>> orderActors = new ConcurrentHashMap<>();
    
    // List to keep track of all user IDs who placed orders
    private final List<Integer> userIdList = new ArrayList<>();
    
    // Counter for generating unique order IDs
    private int orderIdCounter = 1; 

    // Factory method to create GatewayActor
    public static Behavior<GatewayMessages.Command> create() {
        return Behaviors.setup(GatewayActor::new);
    }

   

    // Define how the actor handles different incoming messages
    @Override
    public Receive<GatewayMessages.Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(GatewayMessages.GetAllProducts.class, this::onGetAllProducts)
            .onMessage(GatewayMessages.GetProductById.class, this::onGetProductById)
            .onMessage(GatewayMessages.CreateOrder.class, this::onCreateOrder)
            .onMessage(GatewayMessages.GetOrderById.class, this::onGetOrderById)
            .onMessage(GatewayMessages.UpdateOrderById.class, this::onUpdateOrderById)
            .onMessage(GatewayMessages.DeleteOrderRequest.class, this::onDeleteOrder)
            .onMessage(GatewayMessages.GlobalReset.class, this::onGlobalReset)
            .build();
    }


     // Constructor: Loads product data from CSV and spawns Product Actors
     private GatewayActor(ActorContext<GatewayMessages.Command> context) {
        super(context);
        List<CsvLoader.ProductData> products = CsvLoader.loadProducts("products.csv");
        for (CsvLoader.ProductData product : products) {
            ActorRef<ProductMessages.Command> productActor = getContext().spawn(
                ProductActor.create(
                    product.id,
                    product.name,
                    product.description,
                    product.price,
                    product.stockQuantity
                ),
                "Product" + product.id
            );
            //System.out.println("Product " + product.id + " loaded");
            productActors.put(product.id, productActor);
        }
        System.out.println("Products loaded from CSV successfully.");
    }

    // Handle request to get all product IDs
    private Behavior<GatewayMessages.Command> onGetAllProducts(GatewayMessages.GetAllProducts msg) {
        List<Integer> ids = new ArrayList<>(productActors.keySet());
        msg.replyTo.tell(new GatewayMessages.ProductsResponse(ids));
        return this;
    }

    // Handle request to get specific product info
    private Behavior<GatewayMessages.Command> onGetProductById(GatewayMessages.GetProductById msg) {
        ActorRef<ProductMessages.Command> prod = productActors.get(msg.productId);
        if (prod != null) {
            prod.tell(new ProductMessages.GetProductById(msg.productId, msg.replyTo));
        } else {
            // Send default response if product doesn't exist
            msg.replyTo.tell(new GatewayMessages.ProductInfo(-1, "", "", 0, 0));
        }
        return this;
    }

    // Handle request to get order info
 private Behavior<GatewayMessages.Command> onGetOrderById(GatewayMessages.GetOrderById msg) {
    ActorRef<OrderMessages.Command> orderActor = orderActors.get(msg.orderId);
    if (orderActor != null) {
        orderActor.tell(new OrderMessages.GetOrderById(msg.orderId, msg.replyTo));
    } else {
        // Send default response if order doesn't exist
        msg.replyTo.tell(new GatewayMessages.OrderInfo(-1, -1, 0, "", new ArrayList<>()));
    }
    return this;
}



    // Handle request to create a new order
    private Behavior<GatewayMessages.Command> onCreateOrder(GatewayMessages.CreateOrder msg) {
        String uniqueName = "PostOrder-" + java.util.UUID.randomUUID().toString();
        getContext().spawn(PostOrderActor.create(
            msg.orderData, 
            msg.replyTo, 
            productActors, 
            orderActors, 
            orderIdCounter, 
            userIdList, 
            getContext().getSystem().scheduler()
            ),
            uniqueName
        );
        orderIdCounter++; // Increment the order ID counter
        return this;
    }

   
   

    // Handle request to delete an order
    private Behavior<GatewayMessages.Command> onDeleteOrder(GatewayMessages.DeleteOrderRequest msg) {
        String uniqueName = "DeleteOrder-" + java.util.UUID.randomUUID().toString();
        System.out.println("Delete order in gateway before spawn");
        getContext().spawn(DeleteOrderActor.create(
            msg.orderId, 
            msg.replyTo, 
            productActors, 
            orderActors
            ),
            uniqueName
        );
        System.out.println("Delete order in gateway after spawn");
        return this;
    }



     // Handle request to update an order status
     private Behavior<GatewayMessages.Command> onUpdateOrderById(GatewayMessages.UpdateOrderById msg) {
        ActorRef<OrderMessages.Command> orderActor = orderActors.get(msg.orderId);
        if (orderActor != null) {
            orderActor.tell(new OrderMessages.UpdateOrderById(msg.orderId, msg.updateData, msg.replyTo));
        } else {
            // Send default response if order doesn't exist
            msg.replyTo.tell(new GatewayMessages.OrderInfo(-1, -1, 0, "", new ArrayList<>()));
        }
        return this;
    }


    

    // Handle global reset: cancel all active orders
    private Behavior<GatewayMessages.Command> onGlobalReset(GatewayMessages.GlobalReset msg) {
        for (Map.Entry<Integer, ActorRef<OrderMessages.Command>> entry : orderActors.entrySet()) {
            int orderId = entry.getKey();
            String uniqueName = "DeleteOrder-" + java.util.UUID.randomUUID().toString();
            getContext().spawn(DeleteOrderActor.create(orderId, msg.replyTo, productActors, orderActors), uniqueName);
        }
        msg.replyTo.tell(new GatewayMessages.SuccessResponse(true, "Global reset: Cancelled all orders"));
        return this;
    }
}
