package io.xtrd.javascript;

import javafx.scene.web.WebEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSChartBridge {
    private final Logger logger = LoggerFactory.getLogger(JSChartBridge.class);
    private final WebEngine webEngine;
    public enum FunctionName {
        appendData, setMarkers, updatePriceLine,addLabel, removeLabel, changePrecision
    }

    public JSChartBridge(WebEngine webEngine) {
        this.webEngine = webEngine;
    }

    public Object call(FunctionName functionName, Object param) {
        String scriptStr;
        if (param == null) {
            scriptStr = functionName.toString().concat("(").concat(")");
        } else if (param instanceof String) {
            scriptStr = functionName.toString().concat("('").concat(param.toString()).concat("')");
        } else {
            scriptStr = functionName.toString().concat("(").concat(param.toString()).concat(")");
        }
        logger.debug(scriptStr);
        return webEngine.executeScript(scriptStr);
    }


}
