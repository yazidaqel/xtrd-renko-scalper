package io.xtrd.trading.events;

import io.xtrd.trading.MarketData;

import java.util.List;

public class TradesEvent implements IEvent {
    private List<MarketData> marketData;

    public TradesEvent(List<MarketData> marketData) {
        this.marketData = marketData;
    }

    public List<MarketData> getMarketData() {
        return marketData;
    }
}
