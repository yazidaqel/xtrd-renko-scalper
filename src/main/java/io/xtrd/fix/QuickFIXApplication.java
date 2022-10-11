package io.xtrd.fix;

import io.xtrd.ApiConstants;
import io.xtrd.EventProcessor;
import io.xtrd.trading.MarketData;
import io.xtrd.trading.Order;
import io.xtrd.trading.OrderOperation;
import io.xtrd.trading.events.SessionStatus;
import io.xtrd.trading.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Message;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class QuickFIXApplication implements Application {
    private static final Logger logger = LoggerFactory.getLogger(QuickFIXApplication.class);
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final MessagesFactory messagesFactory = new MessagesFactory();
    private String userName;
    private String password;
    private String fixAccount;
    private SocketInitiator initiator;
    private List<io.xtrd.trading.Symbol> securityList = new ArrayList<>();
    private EventProcessor eventProcessor;
    private io.xtrd.trading.Symbol subscribedSymbol;

    public QuickFIXApplication(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        eventProcessor.addConsumer(OrderCommandEvent.class, getOrderConsumer());
        eventProcessor.addConsumer(SecurityListRequestEvent.class, request -> getSecurityList());
        eventProcessor.addConsumer(SubscribeEvent.class, (Consumer<SubscribeEvent>) event -> {
            subscribedSymbol = event.getSymbol();
            subscribe(event.getSymbol().getName());
        });
    }

    public QuickFIXApplication(EventProcessor eventProcessor, boolean useSandBoxExecutor) {
        this.eventProcessor = eventProcessor;
        if (useSandBoxExecutor) {
            //start sand box executor
            SandBoxOrderExecutor sandBoxOrderExecutor = new SandBoxOrderExecutor(eventProcessor);
        } else {
            //start real executor
            eventProcessor.addConsumer(OrderCommandEvent.class, getOrderConsumer());
        }

        eventProcessor.addConsumer(SecurityListRequestEvent.class, request -> getSecurityList());
        eventProcessor.addConsumer(SubscribeEvent.class, (Consumer<SubscribeEvent>) event -> {
            subscribedSymbol = event.getSymbol();
            subscribe(event.getSymbol().getName());
        });
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

    public void start(String settingsFileName) throws ConfigError {
        SessionSettings sessionSettings = new SessionSettings(settingsFileName);
        FileStoreFactory fileStoreFactory = new FileStoreFactory(sessionSettings);
        initiator = new SocketInitiator(this, fileStoreFactory, sessionSettings, new DefaultMessageFactory());
        initiator.start();
    }

    public void stop() {
        initiator.stop();
    }

    public void logon() throws Exception {
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            throw new Exception("Fix sessions are not initialized");
        }
        for (SessionID sessionID : sessions) {
            quickfix.Session.lookupSession(sessionID).sentLogon();
        }
    }

    public void logout() throws Exception {
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            throw new Exception("Fix session not initialized");
        }
        for (SessionID sessionID : sessions) {
            Session session = quickfix.Session.lookupSession(sessionID);
            if (session.isLoggedOn()) {
                session.sentLogout();
            }
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFixAccount(String fixAccount) {
        this.fixAccount = fixAccount;
    }

    @Override
    public void onCreate(SessionID sessionID) {
        if (logger.isDebugEnabled()) {
            logger.debug("Created new Session: [" + sessionID + "]");
        }
    }

    @Override
    public void onLogon(SessionID sessionID) {
        if (!sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
            eventProcessor.putEvent(SessionStatus.CONNECTED);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Session: {} is connected!", sessionID);
        }
    }

    @Override
    public void onLogout(SessionID sessionID) {
        if (!sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
            eventProcessor.putEvent(SessionStatus.DISCONNECTED);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Session: {} is disconnected!", sessionID);
        }
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        final Message.Header header = message.getHeader();
        try {
            if (header.getString(MsgType.FIELD).equals(MsgType.LOGON)) {
                message.setField(new Username(userName));
                message.setField(new Password(password));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Session: {} sent admin Message: {}", sessionID, message);
            }
        } catch (FieldNotFound fieldNotFound) {
            logger.error("An error occurred during sending admin Message from Session: [" + sessionID + "]. Error: [" + fieldNotFound.getMessage() + "]");
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) {
        if (logger.isDebugEnabled()) {
            logger.debug("Session: {} received admin Message: {}", sessionID, message);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionID) {
        if (logger.isDebugEnabled()) {
            logger.debug("Session: {} sent application Message: {}", sessionID, message);
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) {
        processMessage(message);
        if (sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Session: {} received application Message: {}", sessionID, message);
            }
        }
    }

    private void processMessage(Message message) {
        StringField msgTypeField = new StringField(MsgType.FIELD);
        try {
            String typeFieldValue = message.getHeader().getField(msgTypeField).getValue();
            if (SecurityList.MSGTYPE.equals(typeFieldValue)) {
                processSecurityList(message);
            } else if (MarketDataSnapshotFullRefresh.MSGTYPE.equals(typeFieldValue)) {
                processSnapshotFullRefresh(message);
            } else if (MarketDataIncrementalRefresh.MSGTYPE.equals(typeFieldValue)) {
                processIncrementalRefresh(message);
            } else if (OrderCancelReject.MSGTYPE.equals(typeFieldValue)) {
                processOrderCancelReject(message);
            } else if (ExecutionReport.MSGTYPE.equals(typeFieldValue)) {
                processExecutionReport(message);
            }
        } catch (FieldNotFound e) {
            logger.error("Message {}, field {} not found", message, msgTypeField, e);
        }
    }


    private void processOrderCancelReject(Message message) throws FieldNotFound {

    }

    private void processExecutionReport(Message message) throws FieldNotFound {
        String orderId = message.getString(OrderID.FIELD);
        //for cancel operation clOrdID can be found in OrigClOrdID
        String clOrdID = message.isSetField(OrigClOrdID.FIELD) ? message.getString(OrigClOrdID.FIELD) : message.getString(ClOrdID.FIELD);
        char orderStatus = message.getChar(OrdStatus.FIELD);
        BigDecimal lastQty = message.isSetField(LastQty.FIELD) ? new BigDecimal(message.getString(LastQty.FIELD)) : BigDecimal.ZERO;
        BigDecimal cumQty = new BigDecimal(message.getString(CumQty.FIELD));
        BigDecimal leavesQty = new BigDecimal(message.getString(LeavesQty.FIELD));
        long transactionTime = message.getUtcTimeStamp(TransactTime.FIELD).toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
        String ordRejReason = message.isSetField(OrdRejReason.FIELD) ? message.getString(OrdRejReason.FIELD) : null;
        String text = message.isSetField(Text.FIELD) ? message.getString(Text.FIELD) : null;
        io.xtrd.trading.ExecutionReport executionReport = io.xtrd.trading.ExecutionReport
                .builder()
                .setOrderId(orderId)
                .setClOrdID(clOrdID)
                .setOrderStatus(io.xtrd.trading.ExecutionReport.OrderStatus.fromChar(orderStatus))
                .setLastQty(lastQty)
                .setCumQty(cumQty)
                .setLeavesQty(leavesQty)
                .setTransactionTime(transactionTime)
                .setOrdRejReason(ordRejReason)
                .setText(text)
                .build();
        eventProcessor.putEvent(executionReport);
    }

    private void processSnapshotFullRefresh(Message message) throws FieldNotFound {
        List<MarketData> resultOrderBookData = new ArrayList<>();
        String symbolName = message.getString(Symbol.FIELD);
        String exchangeName = message.getString(SecurityExchange.FIELD);
        for (int i = 1; i <= message.getInt(NoMDEntries.FIELD); i++) {
            Group group = message.getGroup(i, NoMDEntries.FIELD);
            char entryType = group.getChar(MDEntryType.FIELD);
            BigDecimal size = new BigDecimal(group.getString(MDEntrySize.FIELD)).setScale(subscribedSymbol.getSizePower());
            BigDecimal price = new BigDecimal(group.getString(MDEntryPx.FIELD)).setScale(subscribedSymbol.getPricePower());
            switch (entryType) {
                case MDEntryType.BID:
                    resultOrderBookData.add(new MarketData(price, size, MarketData.Type.Snapshot, io.xtrd.trading.Side.Buy));
                    break;
                case MDEntryType.OFFER:
                    resultOrderBookData.add(new MarketData(price, size, MarketData.Type.Snapshot, io.xtrd.trading.Side.Sell));
                    break;
            }
        }
        if (!resultOrderBookData.isEmpty()) {
            eventProcessor.putEvent(new MarketDataEvent(resultOrderBookData));
        }
    }

    private void processIncrementalRefresh(Message message) throws FieldNotFound {
        String symbol = message.getString(Symbol.FIELD);
        List<MarketData> resultTrades = new ArrayList<>();
        List<MarketData> resultOrderBookData = new ArrayList<>();
        for (int i = 1; i <= message.getInt(NoMDEntries.FIELD); i++) {
            Group group = message.getGroup(i, NoMDEntries.FIELD);
            char entryType = group.getChar(MDEntryType.FIELD);
            char updateAction = group.getChar(MDUpdateAction.FIELD);
            MarketData.Type type = getType(updateAction);
            BigDecimal size = type == MarketData.Type.Delete || type == MarketData.Type.Reset ? BigDecimal.ZERO : new BigDecimal(group.getString(MDEntrySize.FIELD));
            size = size.setScale(subscribedSymbol.getSizePower());
            BigDecimal price = new BigDecimal(group.getString(MDEntryPx.FIELD)).setScale(subscribedSymbol.getPricePower());

            switch (entryType) {
                case MDEntryType.BID:
                    resultOrderBookData.add(new MarketData(price, size, type, io.xtrd.trading.Side.Buy));
                    break;
                case MDEntryType.OFFER:
                    resultOrderBookData.add(new MarketData(price, size, type, io.xtrd.trading.Side.Sell));
                    break;
                case MDEntryType.TRADE:
                    io.xtrd.trading.Side side = io.xtrd.trading.Side.Buy;
                    if (group.getInt(ApiConstants.TAG_AGGRESSOR_SIDE) == 1) {
                        side = io.xtrd.trading.Side.Sell;
                    }
                    resultTrades.add(new MarketData(price, size, MarketData.Type.Trade, side));
                    break;
            }
        }
        if (!resultTrades.isEmpty()) {
            eventProcessor.putEvent(new TradesEvent(resultTrades));
        }
        if (!resultOrderBookData.isEmpty()) {
            eventProcessor.putEvent(new MarketDataEvent(resultOrderBookData));
        }
    }

    private MarketData.Type getType(char updateAction) {
        MarketData.Type type;
        switch (updateAction) {
            case MDUpdateAction.NEW:
                type = MarketData.Type.New;
                break;
            case MDUpdateAction.CHANGE:
                type = MarketData.Type.Update;
                break;
            case MDUpdateAction.DELETE:
                type = MarketData.Type.Delete;
                break;
            case MDUpdateAction.DELETE_THRU:
                type = MarketData.Type.Reset;
                break;
            default:
                type = MarketData.Type.New;
        }
        return type;
    }


    private void processSecurityList(Message message) throws FieldNotFound {
        int requestResult = message.getInt(SecurityRequestResult.FIELD);
        if (requestResult == SecurityRequestResult.VALID_REQUEST) {
            Group group = message.getGroup(1, NoRelatedSym.FIELD);
            String symbolName = group.getString(Symbol.FIELD);
            String exchangeName = group.getString(SecurityExchange.FIELD);
            int sizePower = group.getInt(ApiConstants.TAG_SIZE_PRECISION);
            int pricePower = group.getInt(ApiConstants.TAG_PRICE_PRECISION);
            securityList.add(new io.xtrd.trading.Symbol(symbolName, sizePower, pricePower));
            if (message.getBoolean(LastFragment.FIELD)) {
                securityList.sort(Comparator.naturalOrder());
                eventProcessor.putEvent(new SecurityListEvent(securityList));
            }
        } else if (requestResult == SecurityRequestResult.INVALID_OR_UNSUPPORTED_REQUEST) {
            logger.warn("Received negative result to Security request: {}", ApiConstants.INVALID_OR_UNSUPPORTED_REQUEST);
        } else if (requestResult == SecurityRequestResult.NO_INSTRUMENTS_FOUND_THAT_MATCH_SELECTION_CRITERIA) {
            logger.warn("Received negative result to Security request: {}", ApiConstants.NO_INSTRUMENTS_FOUND_THAT_MATCH_SELECTION_CRITERIA);
        } else if (requestResult == SecurityRequestResult.NOT_AUTHORIZED_TO_RETRIEVE_INSTRUMENT_DATA) {
            logger.warn("Received negative result to Security request: {}", ApiConstants.NOT_AUTHORIZED_TO_RETRIEVE_INSTRUMENT_DATA);
        }
    }

    private void getSecurityList() {
        securityList.clear();
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (SessionID sessionID : sessions) {
            if (sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
                continue;
            }
            try {
                Session.sendToTarget(messagesFactory.getSecurityListRequestMessage(ApiConstants.EXCHANGE, getNextId()), sessionID);
            } catch (SessionNotFound e) {
                logger.error("Session: {} not found.", sessionID, e);
            } catch (Exception e) {
                logger.error("An error occurred during send security request. {}", e.getMessage(), e);
            }
        }
    }

    private void subscribe(String symbol) {
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (SessionID sessionID : sessions) {
            if (sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
                continue;
            }
            try {
                Session.sendToTarget(messagesFactory.getSubscribeRequestMessage(symbol, ApiConstants.EXCHANGE, getNextId()), sessionID);
            } catch (SessionNotFound e) {
                logger.error("Session: {} not found. Can't subscribe to Symbol {} ", sessionID, symbol, e);
            } catch (Exception e) {
                logger.error("An error occurred during subscribe to Symbol {}", symbol, e);
            }
        }
    }

    private String getNextId() {
        return String.valueOf(requestId.getAndIncrement());
    }

    private Side getOrderSide(Order order) {
        if (order.getSide() == io.xtrd.trading.Side.Buy) {
            return new Side(Side.BUY);
        } else {
            return new Side(Side.SELL);
        }
    }

    private void cancelOrder(Order order) {
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (SessionID sessionID : sessions) {
            if (!sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
                continue;
            }
            try {
                Session.sendToTarget(messagesFactory.getOrderCancelRequest(fixAccount, order.getSymbol().getName(), order.getClOrdID(), "D".concat(order.getClOrdID()), order.getOrderID(), getOrderSide(order)), sessionID);
            } catch (SessionNotFound e) {
                logger.error("Session: {} not found. Can't subscribe to Symbol {} ", sessionID, order.getSymbol(), e);
            } catch (Exception e) {
                logger.error("An error occurred during subscribe to Symbol {}", order.getSymbol(), e);
            }
        }
    }

    private void newOrder(Order order) {
        ArrayList<SessionID> sessions = initiator.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (SessionID sessionID : sessions) {
            if (!sessionID.getSessionQualifier().equals(ApiConstants.TRADE_SESSION_QUALIFIER)) {
                continue;
            }
            try {
                Session.sendToTarget(messagesFactory.getNewOrderSingle(fixAccount, ApiConstants.EXCHANGE, order.getSymbol().getName(), getOrderSide(order), order.getSize(), order.getPrice(), order.getClOrdID()), sessionID);
            } catch (SessionNotFound e) {
                logger.error("Session: {} not found. Can't subscribe to Symbol {} ", sessionID, order.getSymbol(), e);
            } catch (Exception e) {
                logger.error("An error occurred during subscribe to Symbol {}", order.getSymbol(), e);
            }
        }
    }
}
