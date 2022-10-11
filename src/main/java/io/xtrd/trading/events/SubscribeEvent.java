package io.xtrd.trading.events;

import io.xtrd.trading.Symbol;

public class SubscribeEvent implements IEvent {
    private Symbol symbol;

    public SubscribeEvent(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }
}
