package io.xtrd.trading.events;

import io.xtrd.trading.MarketData;

import java.util.List;

public class MarketDataEvent implements IEvent {
    private List<MarketData> marketData;

    public MarketDataEvent(List<MarketData> marketData) {
        this.marketData = marketData;
    }

    public List<MarketData> getMarketData() {
        return marketData;
    }
}
