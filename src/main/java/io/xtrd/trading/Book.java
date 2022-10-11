package io.xtrd.trading;

import io.xtrd.EventProcessor;
import io.xtrd.trading.events.MarketDataEvent;
import io.xtrd.trading.events.PriceEvent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

public class Book {
    private TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<> (Collections.reverseOrder());
    private TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private ConsumerType consumerType;
    private BigDecimal lastSentPrice;
    private EventProcessor eventProcessor;

    public enum ConsumerType{
        Avg, Bids, Asks
    }

    public Book(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        eventProcessor.addConsumer(MarketDataEvent.class, (Consumer<MarketDataEvent>)marketDataEvent -> process(marketDataEvent.getMarketData()));
    }

    public ConsumerType getType(PriceSource priceSource) {
        switch (priceSource){
            case TOB_ASKS:
                return ConsumerType.Asks;
            case TOB_BIDS:
                return ConsumerType.Bids;
            default:
                return ConsumerType.Avg;
        }
    }


    public void setTOBConsumerType(ConsumerType consumerType) {
        this.consumerType = consumerType;
    }

    private void sendTOBToConsumer() {
        BigDecimal newTOBPrice = null;
        if (consumerType == ConsumerType.Bids && !bids.isEmpty()) {
            newTOBPrice = bids.firstKey();
        } else if (consumerType == ConsumerType.Asks && !asks.isEmpty()) {
            newTOBPrice = asks.firstKey();
        } else if (!bids.isEmpty() && !asks.isEmpty()) {
            newTOBPrice = bids.firstKey().add(asks.firstKey()).divide(BigDecimal.valueOf(2));
        }
        if (newTOBPrice != null) {
            if (lastSentPrice == null || newTOBPrice.compareTo(lastSentPrice) != 0) {
                lastSentPrice = newTOBPrice;
                eventProcessor.putEvent(new PriceEvent(newTOBPrice));
            }
        }
    }

    private void process(List<MarketData> events) {
         if (events.get(0).getType() == MarketData.Type.Snapshot) {
             //drop all, add new data
             bids.clear();
             asks.clear();
             for (MarketData marketData : events) {
                 if (marketData.getSide() == Side.Buy) bids.put(marketData.getPrice(), marketData.getSize());
                 else if (marketData.getSide() == Side.Sell) asks.put(marketData.getPrice(), marketData.getSize());
             }
         } else if (events.get(0).getType() == MarketData.Type.Reset) {
             bids.clear();
             asks.clear();
         } else {
             //update book
             for (MarketData marketData : events) {
                 updateSide(marketData);
             }
         }
         sendTOBToConsumer();
     }

     private void updateSide(MarketData marketData) {
         TreeMap<BigDecimal, BigDecimal> workSide;
         if (marketData.getSide()==Side.Buy) {
             workSide = bids;
         } else {
             workSide = asks;
         }
         switch (marketData.getType()) {
             case New:
                 workSide.put(marketData.getPrice(), marketData.getSize());
                 break;
             case Update:
                 workSide.put(marketData.getPrice(), marketData.getSize());
                 break;
             case Delete:
                 workSide.remove(marketData.getPrice());
         }
     }
}
