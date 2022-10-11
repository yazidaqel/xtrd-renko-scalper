package io.xtrd.trading.events;

import io.xtrd.trading.Symbol;

import java.util.List;

public class SecurityListEvent implements IEvent {
    private List<Symbol> securityList;

    public SecurityListEvent(List<Symbol> securityList) {
        this.securityList = securityList;
    }

    public List<Symbol> getSecurityList() {
        return securityList;
    }
}
