package io.xtrd.trading.events;

import java.math.BigDecimal;

public class PriceEvent implements IEvent {
    private BigDecimal price;

    public PriceEvent(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
