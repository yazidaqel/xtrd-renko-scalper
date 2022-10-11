package io.xtrd.fix;

import io.xtrd.ApiConstants;
import quickfix.DecimalField;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.SecurityListRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;



public class MessagesFactory {

    public Message getSecurityListRequestMessage(String exchange, String reqID) {
        SecurityListRequest result = new SecurityListRequest();
        result.set(new SecurityListRequestType(SecurityListRequestType.ALL_SECURITIES));
        result.set(new SecurityReqID(reqID));
        result.set(new SecurityExchange(exchange));
        return result;
    }

    public Message getNewOrderSingle(String account, String exchange, String symbol, Side side, BigDecimal quantity, BigDecimal price, String clOrdID) {
        NewOrderSingle result = new NewOrderSingle(new ClOrdID(clOrdID), side, new TransactTime(LocalDateTime.now()), new OrdType(OrdType.LIMIT));
        result.set(new Account(account));
        result.setField(new DecimalField(OrderQty.FIELD, quantity));
        result.setField(new DecimalField(Price.FIELD, price));
        result.set(new Symbol(symbol));
        result.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
        result.set(new ExDestination(exchange));
        result.set(new SecurityType(ApiConstants.CRYPTOSPOT));
        return result;
    }

    public Message getOrderCancelRequest(String account, String symbol, String origClOrdID, String clOrdID, String orderID, Side side) {
        OrderCancelRequest result = new OrderCancelRequest(new OrigClOrdID(origClOrdID), new ClOrdID(clOrdID), side, new TransactTime(LocalDateTime.now()));
        result.set(new Account(account));
        result.set(new OrderID(orderID));
        result.set(new Symbol(symbol));
        return result;
    }

    public Message getSubscribeRequestMessage(String symbol, String exchange, String reqID) {
        MarketDataRequest result = new MarketDataRequest();
        result.set(new MDReqID(reqID));
        result.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_UPDATES));
        result.set(new MarketDepth(0));
        MarketDataRequest.NoMDEntryTypes entryTypes = new MarketDataRequest.NoMDEntryTypes();
        entryTypes.set(new MDEntryType(MDEntryType.BID));
        result.addGroup(entryTypes);
        entryTypes.set(new MDEntryType(MDEntryType.OFFER));
        result.addGroup(entryTypes);
        entryTypes.set(new MDEntryType(MDEntryType.TRADE));
        result.addGroup(entryTypes);
        MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();
        noRelatedSym.set(new Symbol(symbol));
        noRelatedSym.set(new SecurityExchange(exchange));
        result.addGroup(noRelatedSym);
        return result;
    }

    public Message getUnsubscribeRequestMessage(String symbol, String exchange, String reqID) {
        MarketDataRequest result = new MarketDataRequest();
        result.set(new MDReqID(reqID));
        result.set(new MarketDepth(0));
        result.set(new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_UPDATE_REQUEST));
        MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();
        noRelatedSym.set(new Symbol(symbol));
        noRelatedSym.set(new SecurityExchange(exchange));
        result.addGroup(noRelatedSym);
        return result;
    }
}
