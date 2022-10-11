package io.xtrd.fix;

import io.xtrd.ApiConstants;
import io.xtrd.EventProcessor;
import io.xtrd.trading.*;
import io.xtrd.trading.events.OrderCommandEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Consumer;


public class TradeTest {
    private final static Logger logger = LoggerFactory.getLogger(TradeTest.class);
    private QuickFIXApplication fixApplication;
    private Properties config;
    private HashMap<String, Order> orders = new HashMap<>();
    private EventProcessor eventProcessor = new EventProcessor();

    private Consumer<ExecutionReport> getConsumer() {
        return executionReport -> {
            logger.info(executionReport.toString());
            if (executionReport.getOrderStatus() == ExecutionReport.OrderStatus.NEW) {
                //drop order
                Order order = orders.get(executionReport.getClOrdID());
                if (order != null) {
                    order.setOrderID(executionReport.getOrderId());
                    eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.delete, order));
                }
            }
        };
    }

    @Ignore
    @Test
    public void testAddAndDeleteOrders() throws Exception {
        config = getConfigurationFile();
        eventProcessor.start();
        startFixEngine(eventProcessor);
        eventProcessor.addConsumer(ExecutionReport.class, getConsumer());
        Thread.sleep(3000);
//        Order orderBuy = new Order(new Symbol("BTC/USDT",4,2), "11119", Side.Buy, new BigDecimal("9001.00"), new BigDecimal("0.01"));
//        orders.put(orderBuy.getClOrdID(), orderBuy);
//        eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.add, orderBuy));
//        Thread.sleep(20000);
        Order orderSell = new Order(new Symbol("BTC/USDT",4,2), "11121", Side.Sell, new BigDecimal("14000.00"), new BigDecimal("0.0001"));
        orders.put(orderSell.getClOrdID(), orderSell);
        eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.add, orderSell));
        Thread.sleep(20000);
        stopFixEngine();
        eventProcessor.stop();
    }

    @Ignore
    @Test
    public void testDeleteOrders() throws Exception {
        config = getConfigurationFile();
        eventProcessor.start();
        startFixEngine(eventProcessor);
        eventProcessor.addConsumer(ExecutionReport.class, getConsumer());
        Thread.sleep(3000);
//        Order orderBuy = new Order(new Symbol("BTC/USDT",4,2), "11115", Side.Buy, new BigDecimal("9001.00"), new BigDecimal("0.01"));
//        orders.put(orderBuy.getClOrdID(), orderBuy);
//        orderBuy.setOrderID("10000071");
//        eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.delete, orderBuy));
//        Thread.sleep(20000);
        Order orderSell = new Order(new Symbol("BTC/USDT",4,2), "6", Side.Sell, new BigDecimal("14000.00"), new BigDecimal("0.01"));
        orders.put(orderSell.getClOrdID(), orderSell);
        orderSell.setOrderID("10000117");
        eventProcessor.putEvent(new OrderCommandEvent(OrderOperation.delete, orderSell));
        Thread.sleep(10000);


        stopFixEngine();
        eventProcessor.stop();
    }



    private Properties getConfigurationFile() throws Exception {
        Properties config = new Properties();
        config.load(TradeTest.class.getResourceAsStream("/config.properties"));
        return config;
    }

    private void startFixEngine(EventProcessor eventProcessor) throws Exception {
        fixApplication = new QuickFIXApplication(eventProcessor);
        fixApplication.setUserName(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_USERNAME));
        fixApplication.setPassword(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_PASSWORD));
        fixApplication.setFixAccount(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_ACCOUNT_ID));
        fixApplication.start(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_CONFIG));
        fixApplication.logon();
    }

    private void stopFixEngine() throws Exception {
        if (fixApplication != null) {
            fixApplication.logout();
            fixApplication.stop();
        }
    }
}
