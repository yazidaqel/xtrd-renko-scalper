package io.xtrd.trading.events;

public enum SessionStatus implements IEvent {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}
