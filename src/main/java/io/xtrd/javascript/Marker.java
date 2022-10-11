package io.xtrd.javascript;

import java.math.BigDecimal;

public class Marker {
    private int id;
    private BigDecimal price;
    private long timestamp;
    private String text;
    private Type type;

    public enum Type {
        buyLimit, buy, sellLimit, sell
    }

    public Marker(int id, BigDecimal price, long timestamp, String text, Type type) {
        this.id = id;
        this.price = price;
        this.timestamp = timestamp;
        this.text = text;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder().append(id);
        sb.append(", ").append(price);
        sb.append(", ").append(timestamp);
        sb.append(", '").append(text).append("'");
        sb.append(", '").append(type).append("'");
        return sb.toString();
    }
}
