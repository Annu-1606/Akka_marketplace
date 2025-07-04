package marketplace.marketplace.Messages;

import marketplace.marketplace.Actors.GatewayActor;
import marketplace.marketplace.Actors.ProductActor;
import marketplace.marketplace.Messages.GatewayMessages;
import marketplace.marketplace.Messages.ProductMessages;
import marketplace.marketplace.Actors.PostOrderActor;

// Messages used by PostOrderActor
public class PostOrderMessages {

    // Marker interface for all commands handled by PostOrderActor
    public interface Command { }

    // Command to initialize PostOrderActor
    public static final class Initialize implements Command { }

    // Response message carrying product details from ProductActor
    public static final class ProductDetailResponse implements PostOrderActor.Command {
        public final GatewayMessages.ProductInfo productInfo;

        public ProductDetailResponse(GatewayMessages.ProductInfo productInfo) {
            this.productInfo = productInfo;
        }
    }

    // Response message carrying result of stock reduction operation
    public static final class StockReductionResponse implements PostOrderActor.Command {
        public final int productId;
        public final ProductMessages.StockResponse StockResponse;

        public StockReductionResponse(int productId, ProductMessages.StockResponse StockResponse) {
            this.productId = productId;
            this.StockResponse = StockResponse;
        }
    }
}
