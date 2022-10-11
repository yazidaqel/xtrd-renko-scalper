package io.xtrd.trading;

import io.xtrd.EventProcessor;
import io.xtrd.trading.events.PriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class RenkoModel {
    private final Logger logger = LoggerFactory.getLogger(RenkoModel.class);
    private BigDecimal previousBrickTopPrice;
    private BigDecimal previousBrickBottomPrice;
    private long lastBrickTime = 0;
    private EventProcessor eventProcessor;

    public RenkoModel(BigDecimal size, EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        eventProcessor.addConsumer(PriceEvent.class, getPriceConsumer(size));
    }

    Consumer<PriceEvent> getPriceConsumer(BigDecimal size) {
        return priceEvent -> {
            BigDecimal price = priceEvent.getPrice();
            if (logger.isDebugEnabled()) {
                logger.debug("New price received {}", price);
            }
            if (previousBrickTopPrice == null) {
                //init
                previousBrickTopPrice = price;
                previousBrickBottomPrice = price;
            } else {
                if (price.compareTo(previousBrickTopPrice.add(size)) >= 0) {
                    //creating higher renko bricks
                    previousBrickBottomPrice = previousBrickTopPrice;
                    previousBrickTopPrice = previousBrickTopPrice.add(size);
                    addBrickToProcessor(previousBrickBottomPrice, previousBrickTopPrice);
                    // Perhaps the price has grown in more than one brick
                    getPriceConsumer(size).accept(priceEvent);
                } else if (price.compareTo(previousBrickBottomPrice.subtract(size)) <= 0) {
                    //creating lower renko bricks
                    previousBrickTopPrice = previousBrickBottomPrice;
                    previousBrickBottomPrice = previousBrickBottomPrice.subtract(size);
                    addBrickToProcessor(previousBrickTopPrice, previousBrickBottomPrice);
                    getPriceConsumer(size).accept(priceEvent);
                }
            }
        };
    }

    private void addBrickToProcessor(BigDecimal openPrice, BigDecimal closePrice) {
        long currentBrickTime = System.currentTimeMillis();
        if (currentBrickTime <= lastBrickTime) {
            currentBrickTime = lastBrickTime + 1;
        }
        lastBrickTime = currentBrickTime;
        Brick brick = new Brick(currentBrickTime, openPrice, closePrice);
        if (logger.isDebugEnabled()) {
            logger.debug("Created new renko brick: ", brick);
        }
        eventProcessor.putEvent(brick);
    }

}
