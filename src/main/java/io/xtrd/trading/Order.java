package io.xtrd.trading;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Order {
    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    private String clOrdID;
    private String orderID;
    private long createTime;
    private long executeTime;
    private Side side;
    private BigDecimal price;
    private BigDecimal size;
    private Symbol symbol;
    private Brick linkedLimitBrick;
    private Brick linkedExecBrick;


    public Order(Symbol symbol, String clOrdID, Side side, BigDecimal price, BigDecimal size) {
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.size = size;
        this.clOrdID = clOrdID;
    }

    public Brick getLinkedLimitBrick() {
        return linkedLimitBrick;
    }

    public void setLinkedLimitBrick(Brick linkedLimitBrick) {
        this.linkedLimitBrick = linkedLimitBrick;
    }

    public Brick getLinkedExecBrick() {
        return linkedExecBrick;
    }

    public void setLinkedExecBrick(Brick linkedExecBrick) {
        this.linkedExecBrick = linkedExecBrick;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return clOrdID.equals(order.clOrdID) &&
                side == order.side &&
                price.equals(order.price) &&
                size.equals(order.size) &&
                symbol.equals(order.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clOrdID, side, price, size, symbol);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("symbol=").append(symbol.getName());
        sb.append(", clOrdID=").append(clOrdID);
        sb.append(", orderID=").append(orderID);
        if (createTime!=0) {
            sb.append(", createTime=").append(formatter.format(new Date(createTime)));
        }
        if (executeTime!=0) {
            sb.append(", executeTime=").append(formatter.format(new Date(executeTime)));
        }
        sb.append(", side=").append(side);
        sb.append(", price=").append(price);
        sb.append(", size=").append(size);
        sb.append(", linked limit renko=").append(linkedLimitBrick);
        sb.append(", linked exec renko=").append(linkedExecBrick);
        sb.append('}');
        return sb.toString();
    }

    public String toJS() {
        final StringBuilder sb = new StringBuilder("");
        if (side == Side.Sell) {
            if (executeTime == 0) {
                sb.append("S Lim");
            } else {
                sb.append("Sell");
            }
        } else {
            if (executeTime == 0) {
                sb.append("B Lim");
            } else {
                sb.append("Buy");
            }
        }
        sb.append(" (");
        sb.append(size);
        sb.append(")");
        sb.append("<br>");
        sb.append(price);
        return sb.toString();
    }
}
