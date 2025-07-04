package com.example.messages;

import akka.cluster.sharding.ShardRegion;

public class OrderMessageExtractor implements ShardRegion.MessageExtractor {

    private final int numberOfShards;

    public OrderMessageExtractor(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    @Override
    public String entityId(Object message) {
        if (message instanceof OrderMessages.GetOrder) {
            return ((OrderMessages.GetOrder) message).order_id;
        }
        if (message instanceof OrderMessages.UpdateOrderStatus) {
            return ((OrderMessages.UpdateOrderStatus) message).order_id;
        }
        return null;
    }

    @Override
    public Object entityMessage(Object message) {
        return message;
    }

    @Override
    public String shardId(Object message) {
        String entityId = entityId(message);
        return entityId != null ? String.valueOf(Math.abs(entityId.hashCode() % numberOfShards)) : null;
    }
}
