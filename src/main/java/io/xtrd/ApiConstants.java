package io.xtrd;

public interface ApiConstants {
    int TAG_PRICE_PRECISION = 5001;
    int TAG_SIZE_PRECISION = 5002;
    int TAG_AGGRESSOR_SIDE = 2446;

    String TRADE_SESSION_QUALIFIER = "TRADE";

    String INVALID_OR_UNSUPPORTED_REQUEST = "Invalid or unsupported request";
    String NO_INSTRUMENTS_FOUND_THAT_MATCH_SELECTION_CRITERIA = "No instruments found that match selection criteria";
    String NOT_AUTHORIZED_TO_RETRIEVE_INSTRUMENT_DATA = "Not authorized to retrieve instrument data";

    String EXCHANGE = "BINANCE";
    String CRYPTOSPOT = "CRYPTOSPOT";

    String CONFIG_RENKO_SIZE = "renko.size";
    String CONFIG_ORDER_SIZE = "order.size";
    String CONFIG_FIX_SESSION_CONFIG = "fix.session.config";
    String CONFIG_FIX_SESSION_USERNAME = "fix.session.username";
    String CONFIG_FIX_SESSION_PASSWORD = "fix.session.password";
    String CONFIG_FIX_SESSION_ACCOUNT_ID = "fix.session.account.id";
    String CONFIG_SANDBOX_EXECUTION = "sandbox.execution";
    String CONFIG_PRICE_SOURCE = "price.source";
    String SAFARI_AGENT_STRING = "Safari/";
    int MINIMUM_WEBKIT_VERSION = 603;
}