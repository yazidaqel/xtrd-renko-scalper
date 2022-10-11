package io.xtrd.trading;

import io.xtrd.trading.events.IEvent;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Brick implements IEvent {
    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    private long time;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    public long getTime() {
        return time;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getClose() {
        return close;
    }

    public Brick(long time, BigDecimal open, BigDecimal close) {
        this.time = time;
        low = open.compareTo(close)>=0 ? close : open;
        high = open.compareTo(close)>=0 ? open : close;
        this.open = open;
        this.close = close;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Brick{");
        sb.append("time=").append(formatter.format(new Date(time)));
        sb.append(", open=").append(open);
        sb.append(", close=").append(close);
        sb.append('}');
        return sb.toString();
    }
}
