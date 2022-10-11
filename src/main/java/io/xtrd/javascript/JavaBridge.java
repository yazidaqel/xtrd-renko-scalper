package io.xtrd.javascript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaBridge {
    private final Logger logger = LoggerFactory.getLogger(JavaBridge.class);

    //call from js:  java.log(values);
    public void log(String text) {
        logger.debug(text);
    }
}
