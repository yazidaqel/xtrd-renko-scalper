package io.xtrd.fix;

import io.xtrd.EventProcessor;
import io.xtrd.trading.ExecutionReport;
import io.xtrd.trading.Order;
import io.xtrd.trading.OrderOperation;
import io.xtrd.trading.Side;
import io.xtrd.trading.events.OrderCommandEvent;
import io.xtrd.trading.events.PriceEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

public class SandBoxOrderExecutor {
    private HashMap<String, Order> orders = new HashMap<>();
    private EventProcessor eventProcessor;

    public SandBoxOrderExecutor(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        eventProcessor.addConsumer(PriceEvent.class, getPriceConsumer());
        eventProcessor.addConsumer(OrderCommandEvent.class, getOrderConsumer());
    }

    private Consumer<OrderCommandEvent> getOrderConsumer() {
        return event -> {
            if (event.getOperation() == OrderOperation.add) {
                newOrder(event.getOrder());
            } else if (event.getOperation() == OrderOperation.delete) {
                cancelOrder(event.getOrder());
            }
        };
    }

    private Consumer<PriceEvent> getPriceConsumer(){
        return priceEvent->{
            Iterator<Order> iterator = orders.values().iterator();
            while (iterator.hasNext()) {
                Order order = iterator.next();
                if (order.getSide() == Side.Buy && order.getPrice().compareTo(priceEvent.getPrice()) >= 0) {
                    //filled, should be removed from limit orders
                    iterator.remove();
                    sendFillExecutionReport(order);
                } else if (order.getSide() == Side.Sell && order.getPrice().compareTo(priceEvent.getPrice()) <= 0) {
                    //filled, should be removed from limit orders
                    iterator.remove();
                    sendFillExecutionReport(order);
                }
            }
        };
    }

    private void sendFillExecutionReport(Order order) {
        ExecutionReport executionReport = ExecutionReport
                .builder()
                .setOrderId(order.getClOrdID())
                .setClOrdID(order.getClOrdID())
                .setLeavesQty(BigDecimal.ZERO)
                .setCumQty(order.getSize())
                .setLastQty(order.getSize())
                .setOrderStatus(ExecutionReport.OrderStatus.FILLED)
                .setTransactionTime(System.currentTimeMillis())
                .build();
        eventProcessor.putEvent(executionReport);
    }

    private void newOrder(Order order) {
        orders.put(order.getClOrdID(), order);
        ExecutionReport executionReport = ExecutionReport
                .builder()
                .setOrderId(order.getClOrdID())
                .setClOrdID(order.getClOrdID())
                .setLeavesQty(order.getSize())
                .setCumQty(BigDecimal.ZERO)
                .setLastQty(BigDecimal.ZERO)
                .setOrderStatus(ExecutionReport.OrderStatus.NEW)
                .setTransactionTime(System.currentTimeMillis())
                .build();
        eventProcessor.putEvent(executionReport);
    }

    private void cancelOrder(Order order) {
        Order orderTmp = orders.remove(order.getClOrdID());
        if (orderTmp!=null) {
            ExecutionReport executionReport = ExecutionReport
                    .builder()
                    .setOrderId(order.getClOrdID())
                    .setClOrdID(order.getClOrdID())
                    .setLeavesQty(order.getSize())
                    .setCumQty(BigDecimal.ZERO)
                    .setLastQty(BigDecimal.ZERO)
                    .setOrderStatus(ExecutionReport.OrderStatus.CANCELED)
                    .setTransactionTime(System.currentTimeMillis())
                    .build();
            eventProcessor.putEvent(executionReport);
        } else {
            ExecutionReport executionReport = ExecutionReport
                    .builder()
                    .setOrderId(order.getClOrdID())
                    .setClOrdID(order.getClOrdID())
                    .setLeavesQty(order.getSize())
                    .setCumQty(BigDecimal.ZERO)
                    .setLastQty(BigDecimal.ZERO)
                    .setOrderStatus(ExecutionReport.OrderStatus.REJECTED)
                    .setTransactionTime(System.currentTimeMillis())
                    .build();
            eventProcessor.putEvent(executionReport);
        }
    }

}
