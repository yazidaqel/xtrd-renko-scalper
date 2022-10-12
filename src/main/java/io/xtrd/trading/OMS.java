package io.xtrd.trading;

import io.xtrd.EventProcessor;
import io.xtrd.trading.events.OrderCommandEvent;
import io.xtrd.trading.events.OrderDrawEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OMS {
    private static final Logger LOGGER = LoggerFactory.getLogger(OMS.class);
    private final AtomicInteger orderId = new AtomicInteger(1);
    private final AtomicInteger positionHeldCounter = new AtomicInteger(0);
    private final int maxPositionHeld;
    private final List<Order> executedOrders;
    private final List<Order> positionHeldOrders;
    private final HashMap<String, Order> limitOrders;
    private BigDecimal cumulativeSize;
    private final EventProcessor eventProcessor;
    private final Symbol symbol;
    private Order lastLimitOrder;

    public OMS(
            Symbol symbol,
            BigDecimal renkoBrickSize,
            BigDecimal orderSize,
            int maxPositionHeld,
            EventProcessor eventProcessor
    ) {
        this.executedOrders = new ArrayList<>();
        this.positionHeldOrders = new ArrayList<>();
        this.limitOrders = new HashMap<>();
        this.cumulativeSize = BigDecimal.ZERO;
        this.maxPositionHeld = maxPositionHeld;
        this.symbol = symbol;
        this.eventProcessor = eventProcessor;
        eventProcessor.addConsumer(Brick.class, getRenkoBrickConsumer(renkoBrickSize, orderSize));
        eventProcessor.addConsumer(ExecutionReport.class, getExecutionReportConsumer());
    }

    private Consumer<ExecutionReport> getExecutionReportConsumer() {
        return executionReport -> {
            Order order = limitOrders.get(executionReport.getClOrdID());
            if (order != null) {
                order.setOrderID(executionReport.getOrderId());
                if (executionReport.getOrderStatus() == ExecutionReport.OrderStatus.FILLED) {
                    order.setExecuteTime(executionReport.getTransactionTime());
                    if (order.getLinkedExecBrick() != null) {
                        //can be removed from limitsOrder
                        limitOrders.remove(order.getClOrdID());
                        executedOrders.add(order);
                    }
                    if (order.getSide() == Side.Sell) {
                        cumulativeSize = cumulativeSize.subtract(order.getSize());
                    } else {
                        positionHeldOrders.add(order);
                        cumulativeSize = cumulativeSize.add(order.getSize());
                    }
                    eventProcessor.putEvent(new OrderDrawEvent(OrderOperation.fill, order));
                } else if (executionReport.getOrderStatus() == ExecutionReport.OrderStatus.CANCELED) {
                    limitOrders.remove(order.getClOrdID());
                    eventProcessor.putEvent(new OrderDrawEvent(OrderOperation.delete, order));
                } else if (executionReport.getOrderStatus() == ExecutionReport.OrderStatus.NEW) {
                    order.setCreateTime(executionReport.getTransactionTime());
                    eventProcessor.putEvent(new OrderDrawEvent(OrderOperation.add, order));
                } else if (executionReport.getOrderStatus() == ExecutionReport.OrderStatus.REJECTED) {
                    limitOrders.remove(order.getClOrdID());
                }
            }
        };
    }

    private Consumer<Brick> getRenkoBrickConsumer(BigDecimal renkoBrickSize, BigDecimal orderSize) {
        return brick -> {
            if (brick.getOpen().compareTo(brick.getClose()) < 0) {
                processBuyRisingBrick(renkoBrickSize, orderSize, brick);
            } else {
                processSellFallingBrick(renkoBrickSize, orderSize, brick);
            }
        };
    }

    private void processBuyRisingBrick(BigDecimal renkoBrickSize, BigDecimal orderSize, Brick brick) {
        if (positionHeldCounter.incrementAndGet() <= maxPositionHeld) {
            //brick closed down, close sell limit orders, open new buy limit order
            closeLimitOrders(Side.Sell);
            //link limit order to brick time
            Iterator<Order> iterator = limitOrders.values().iterator();
            while (iterator.hasNext()) {
                Order order = iterator.next();
                if (order.getSide() == Side.Buy && order.getPrice().compareTo(brick.getClose()) >= 0 && order.getLinkedExecBrick() == null) {
                    order.setLinkedExecBrick(brick);
                    if (order.getExecuteTime() != 0) {
                        //filled, should be redrawn and removed from limit orders
                        iterator.remove();
                        executedOrders.add(order);
                        eventProcessor.putEvent(new OrderDrawEvent(OrderOperation.fill, order));
                    }
                }
            }
            if (lastLimitOrder == null || brick.getClose().compareTo(lastLimitOrder.getPrice()) > 0) {
                BigDecimal newOrderSize = orderSize;
                if (cumulativeSize.compareTo(BigDecimal.ZERO) < 0) {
                    newOrderSize = cumulativeSize.negate();
                }
                Order order = new Order(symbol, getNextOrderId(), Side.Buy, brick.getClose().add(renkoBrickSize), newOrderSize);
                order.setLinkedLimitBrick(brick);
                limitOrders.put(order.getClOrdID(), order);
                lastLimitOrder = order;
                eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.add, order));
            }
        } else {
            LOGGER.info("Brick will be ignored");
        }
    }

    private void processSellFallingBrick(BigDecimal renkoBrickSize, BigDecimal orderSize, Brick brick) {

        //brick closed up, close buy limit orders, open new sell limit order
        closeLimitOrders(Side.Buy);
        //link limit order to brick time
        Iterator<Order> iterator = limitOrders.values().iterator();
        while (iterator.hasNext()) {
            Order order = iterator.next();
            if (order.getSide() == Side.Sell && order.getPrice().compareTo(brick.getClose()) <= 0 && order.getLinkedExecBrick() == null) {
                order.setLinkedExecBrick(brick);
                if (order.getExecuteTime() != 0) {
                    //filled, should be redrawn and removed from limit orders
                    iterator.remove();
                    executedOrders.add(order);
                    eventProcessor.putEvent(new OrderDrawEvent(OrderOperation.fill, order));
                }
            }
        }
        if (lastLimitOrder == null || brick.getClose().compareTo(lastLimitOrder.getPrice()) < 0) {
            BigDecimal newOrderSize = orderSize;
            if (cumulativeSize.compareTo(BigDecimal.ZERO) > 0) {
                newOrderSize = cumulativeSize;
            }
            Order order = new Order(symbol, getNextOrderId(), Side.Sell, brick.getClose().add(renkoBrickSize), newOrderSize);
            order.setLinkedLimitBrick(brick);
            limitOrders.put(order.getClOrdID(), order);
            lastLimitOrder = order;
            eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.add, order));
        }

        if (!positionHeldOrders.isEmpty()) {
            positionHeldOrders.forEach(order -> {
                Order closeOrder = new Order(symbol, getNextOrderId(), Side.Sell, brick.getClose().add(renkoBrickSize), order.getSize());
                closeOrder.setLinkedLimitBrick(brick);
                limitOrders.put(closeOrder.getClOrdID(), order);
                eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.add, closeOrder));
                closeOrder.setLinkedExecBrick(brick);
            });

            positionHeldOrders.clear();
            positionHeldCounter.set(0);
        }

    }

    private void closeLimitOrders(Side side) {
        LOGGER.info("Fire order command event, Side={}", side);
        Iterator<Order> iterator = limitOrders.values().iterator();
        while (iterator.hasNext()) {
            Order order = iterator.next();
            if (order.getSide() == side) {
                eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.delete, order));
            }
        }
    }

    private String getNextOrderId() {
        return String.valueOf(orderId.getAndIncrement());
    }
}
