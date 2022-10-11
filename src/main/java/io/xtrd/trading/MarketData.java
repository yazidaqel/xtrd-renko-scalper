package io.xtrd.trading;

import java.math.BigDecimal;

public class MarketData {
    private BigDecimal price;
    private BigDecimal size;
    private Type type;
    private Side side;


    public enum Type {
        Trade, Snapshot, New, Update, Delete, Reset;

    }

    public MarketData(BigDecimal price, BigDecimal size, Type type, Side side) {
        this.price = price;
        this.size = size;
        this.type = type;
        this.side = side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSize() {
        return size;
    }

    public Type getType() {
        return type;
    }

    public Side getSide() {
        return side;
    }
}
