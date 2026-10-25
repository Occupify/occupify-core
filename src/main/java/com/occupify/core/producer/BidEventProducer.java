package com.occupify.core.producer;

import com.occupify.core.event.BidCancelledEvent;
import com.occupify.core.event.BidCreatedEvent;

public interface BidEventProducer {

  void publishBidCreated(BidCreatedEvent event);

  void publishBidCancelled(BidCancelledEvent event);
}
