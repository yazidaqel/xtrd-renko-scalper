package io.xtrd.trading;

import io.xtrd.trading.events.IEvent;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExecutionReport implements IEvent {
    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    private String orderId;
    private String clOrdID;
    private OrderStatus orderStatus;
    private BigDecimal lastQty;
    private BigDecimal cumQty;
    private BigDecimal leavesQty;
    private long transactionTime;
    private String ordRejReason;
    private String text;

    private ExecutionReport() {
    }

    public static Builder builder() {
        return new ExecutionReport().new Builder();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public BigDecimal getLastQty() {
        return lastQty;
    }

    public BigDecimal getCumQty() {
        return cumQty;
    }

    public BigDecimal getLeavesQty() {
        return leavesQty;
    }

    public long getTransactionTime() {
        return transactionTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExecutionReport{");
        sb.append("orderId='").append(orderId).append('\'');
        sb.append(", clOrdID='").append(clOrdID).append('\'');
        sb.append(", transactionTime=").append(formatter.format(new Date(transactionTime)));
        sb.append(", orderStatus=").append(orderStatus);
        sb.append(", lastQty=").append(lastQty);
        sb.append(", cumQty=").append(cumQty);
        sb.append(", leavesQty=").append(leavesQty);
        if (ordRejReason != null) {
            sb.append(", ordRejReason=").append(ordRejReason);
        }
        if (text != null) {
            sb.append(", text=").append(text);
        }
        sb.append('}');
        return sb.toString();
    }

    public enum OrderStatus {
        NEW('0'),
        PARTIALLY_FILLED('1'),
        FILLED('2'),
        CANCELED('4'),
        REJECTED('8'),
        PENDING_NEW('A'),
        UNDEFINED('*');

        char value;

        OrderStatus(char value) {
            this.value = value;
        }

        public static OrderStatus fromChar(char value) {
            switch (value) {
                case '0':
                    return NEW;
                case '1':
                    return PARTIALLY_FILLED;
                case '2':
                    return FILLED;
                case '4':
                    return CANCELED;
                case '8':
                    return REJECTED;
                case 'A':
                    return PENDING_NEW;
                default:
                    return UNDEFINED;
            }
        }

        public char getValue() {
            return value;
        }
    }

    public class Builder {

        private Builder() {
        }

        public Builder setOrderId(String orderId) {
            ExecutionReport.this.orderId = orderId;
            return this;
        }

        public Builder setClOrdID(String clOrdID) {
            ExecutionReport.this.clOrdID = clOrdID;
            return this;
        }

        public Builder setOrderStatus(OrderStatus orderStatus) {
            ExecutionReport.this.orderStatus = orderStatus;
            return this;
        }

        public Builder setLastQty(BigDecimal lastQty) {
            ExecutionReport.this.lastQty = lastQty;
            return this;
        }

        public Builder setCumQty(BigDecimal cumQty) {
            ExecutionReport.this.cumQty = cumQty;
            return this;
        }

        public Builder setLeavesQty(BigDecimal leavesQty) {
            ExecutionReport.this.leavesQty = leavesQty;
            return this;
        }

        public Builder setTransactionTime(long transactionTime) {
            ExecutionReport.this.transactionTime = transactionTime;
            return this;
        }

        public Builder setOrdRejReason(String ordRejReason) {
            ExecutionReport.this.ordRejReason = ordRejReason;
            return this;
        }

        public Builder setText(String text) {
            ExecutionReport.this.text = text;
            return this;
        }

        public ExecutionReport build() {
            return ExecutionReport.this;
        }

    }
}
