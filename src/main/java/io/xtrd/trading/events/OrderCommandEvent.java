package io.xtrd.trading.events;

import io.xtrd.trading.Order;
import io.xtrd.trading.OrderOperation;

public class OrderCommandEvent implements IEvent {
    private OrderOperation operation;
    private Order order;

    public OrderCommandEvent(OrderOperation operation, Order order) {
        this.operation = operation;
        this.order = order;
    }

    public OrderOperation getOperation() {
        return operation;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OrderCommandEvent{");
        sb.append("operation=").append(operation);
        sb.append(", order=").append(order);
        sb.append('}');
        return sb.toString();
    }
}
