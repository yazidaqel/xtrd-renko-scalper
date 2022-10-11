package io.xtrd;

import com.google.gson.Gson;
import com.sun.javafx.webkit.WebConsoleListener;
import io.xtrd.fix.QuickFIXApplication;
import io.xtrd.javascript.JSChartBridge;
import io.xtrd.javascript.JavaBridge;
import io.xtrd.javascript.Marker;
import io.xtrd.trading.*;
import io.xtrd.trading.events.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.javafx.scene.layout.MigPane;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Consumer;


public class MainApp extends Application {
    private static Properties config;
    private final Logger logger = LoggerFactory.getLogger(MainApp.class);
    //communication with the Javascript engine
    private final JavaBridge javaBridge = new JavaBridge();
    private QuickFIXApplication fixApplication;
    private int lastMarkerId;
    private boolean isChartInitialized;
    private EventProcessor eventProcessor;
    private Symbol subscriptionSymbol;


    public static Properties getConfigurationFile(String configName) throws Exception {
        Properties config = new Properties();
        config.load(new FileReader(configName));
        return config;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("No config provided");
        }
        config = getConfigurationFile(args[0]);
        Platform.setImplicitExit(true);
        launch(args);
    }

    private void logVersions(WebView web){
        logger.info("Java Version:   {}", System.getProperty("java.runtime.version"));
        logger.info("JavaFX Version: {}", System.getProperty("javafx.runtime.version"));
        logger.info("OS:             {}, {}", System.getProperty("os.name"), System.getProperty("os.arch"));
        logger.info("User Agent:     {}", web.getEngine().getUserAgent());
    }

    private void startFixEngine() throws Exception {
        if (config.getProperty(ApiConstants.CONFIG_SANDBOX_EXECUTION,"false").equals("false")) {
            fixApplication = new QuickFIXApplication(eventProcessor);
        } else {
            fixApplication = new QuickFIXApplication(eventProcessor, true);
        }
        fixApplication.setUserName(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_USERNAME));
        fixApplication.setPassword(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_PASSWORD));
        fixApplication.setFixAccount(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_ACCOUNT_ID));
        fixApplication.start(config.getProperty(ApiConstants.CONFIG_FIX_SESSION_CONFIG));
        fixApplication.logon();
    }

    private void stopFixEngine() throws Exception {
        if (fixApplication != null) {
            fixApplication.logout();
            fixApplication.stop();
        }
    }


    @Override
    public void stop() throws Exception {
        stopFixEngine();
        eventProcessor.stop();
        super.stop();
    }

    public void start(Stage stage) throws Exception {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        WebEngine webEngine = webView.getEngine();
        //check webkit version
        int webKitVersion;
        try {
            String userAgent = webEngine.getUserAgent();
            int index = userAgent.indexOf(ApiConstants.SAFARI_AGENT_STRING);
            webKitVersion = Integer.parseInt(userAgent.substring(index + 7, index + 10));
        } finally {
        }
        if (webKitVersion<ApiConstants.MINIMUM_WEBKIT_VERSION) {
            logVersions(webView);
            logger.error("Current WebKit version {} does not support ECMAScript6 and CSS3. Please, update jdk.",webKitVersion);
            throw new Exception("Current WebKit version does not support ECMAScript6 and CSS3");
        }
        initWebEngine(webEngine);
        JSChartBridge jsChartBridge = new JSChartBridge(webEngine);
        WebConsoleListener.setDefaultListener((webViewX, message, lineNumber, sourceId) -> {
            if (logger.isDebugEnabled()) {
                logger.debug("{} [at {}]", message, lineNumber);
            }
        });

        eventProcessor = new EventProcessor();
        eventProcessor.start();

        MigPane layout = createLayout(stage, webView, jsChartBridge);
        Scene scene = new Scene(layout, 850, 650);
        scene.getStylesheets().add("/styles/styles.css");
        stage.setScene(scene);
        stage.show();
        // now load the page
        URL url = new File("src/main/resources/html/renko.html").toURI().toURL();
        webEngine.load(url.toString());
        stage.getIcons().add(new Image(MainApp.class.getResourceAsStream("/images/icon.png")));
        stage.setTitle("Renko scalper (disconnected)");
    }


    private MigPane createLayout(Stage stage, WebView webView, JSChartBridge jsChartBridge) {
        MigPane layout = new MigPane();
        layout.add(new Label("Renko size:"), "align label,span, split 8");
        TextField renkoSizeText = new TextField(config.getProperty(ApiConstants.CONFIG_RENKO_SIZE, ""));
        renkoSizeText.setMaxWidth(70);
        renkoSizeText.setAlignment(Pos.CENTER_RIGHT);
        layout.add(renkoSizeText);
        layout.add(new Label("Order size:"), "align label,span");
        TextField orderSizeText = new TextField(config.getProperty(ApiConstants.CONFIG_ORDER_SIZE, ""));
        orderSizeText.setMaxWidth(70);
        orderSizeText.setAlignment(Pos.CENTER_RIGHT);
        layout.add(orderSizeText);
        layout.add(new Label("Symbol:"), "align label,span");
        ComboBox<Symbol> subscriptionComboBox = new ComboBox<Symbol>();
        ObservableList<Symbol> items = FXCollections.observableArrayList(new ArrayList<>());
        subscriptionComboBox.setItems(items);
        subscriptionComboBox.setDisable(true);
        layout.add(subscriptionComboBox, "grow");
        Button startButton = new Button("Start");
        startButton.setDisable(true);
        Button connectButton = new Button("Connect");
        layout.add(connectButton, "right, top");
        layout.add(startButton, "right, top");
        layout.add(webView, "push, span, grow, wrap");

        subscriptionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                startButton.setDisable(false);
            }
        });
        startButton.setOnAction(getStartButtonHandler(startButton, renkoSizeText, orderSizeText, subscriptionComboBox, jsChartBridge));
        connectButton.setOnAction(getConnectButtonHandler(connectButton));

        Consumer<SecurityListEvent> securityListConsumer = (list) -> Platform.runLater(() -> {
            items.clear();
            items.addAll(list.getSecurityList());
            subscriptionComboBox.setDisable(false);
        });
        eventProcessor.addConsumer(SecurityListEvent.class, securityListConsumer);
        eventProcessor.addConsumer(SessionStatus.class, status -> {
            if (status == SessionStatus.CONNECTED) {
                if (subscriptionSymbol == null) {
                    eventProcessor.putEvent(new SecurityListRequestEvent());
                } else {
                    //reconnection
                    //subscribe to market data
                    eventProcessor.putEvent(new SubscribeEvent(subscriptionSymbol));
                }
                Platform.runLater(() -> stage.setTitle("Renko scalper (connected)"));
            } else if (status == SessionStatus.DISCONNECTED){
                Platform.runLater(() -> stage.setTitle("Renko scalper (disconnected)"));
            }
        });

        Gson gson = new Gson();
        //chart consume bricks
        eventProcessor.addConsumer(Brick.class, brick -> Platform.runLater(() -> jsChartBridge.call(JSChartBridge.FunctionName.appendData, gson.toJson(brick))));
        //chart consume orders
        eventProcessor.addConsumer(OrderDrawEvent.class, getChartOrdersConsumer(jsChartBridge));
        //chart consume prices
        Consumer<PriceEvent> priceConsumer = priceEvent -> Platform.runLater(() -> {
            if (!isChartInitialized) {
                isChartInitialized = true;
                jsChartBridge.call(JSChartBridge.FunctionName.appendData, gson.toJson(new Brick(System.currentTimeMillis() - 1000L, priceEvent.getPrice(), priceEvent.getPrice())));
            }
            jsChartBridge.call(JSChartBridge.FunctionName.updatePriceLine, priceEvent.getPrice());
        });
        eventProcessor.addConsumer(PriceEvent.class, priceConsumer);
        return layout;
    }

    private void initWebEngine(WebEngine webEngine) {
        // set up the listener
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // set an interface object named 'javaBridge' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("java", javaBridge);
                webEngine.executeScript("console.log = function(message)\n" +
                        "{\n" +
                        "    java.log(message);\n" +
                        "};");
            }
        });
    }

    EventHandler<ActionEvent> getConnectButtonHandler(Button connectButton) {
        return e -> {
            try {
                connectButton.setDisable(true);
                startFixEngine();
            } catch (Exception exception) {
                logger.error("Connection error: ", exception);
            }
        };
    }


    EventHandler<ActionEvent> getStartButtonHandler(Button startButton, TextField renkoSizeText, TextField orderSizeText, ComboBox<Symbol> subscriptionComboBox, JSChartBridge jsChartBridge) {
        return e -> {
            startButton.setDisable(true);
            subscriptionSymbol = subscriptionComboBox.getValue();
            BigDecimal brickSize = new BigDecimal(renkoSizeText.getText());
            BigDecimal orderSize = new BigDecimal(orderSizeText.getText());
            jsChartBridge.call(JSChartBridge.FunctionName.changePrecision, subscriptionSymbol.getPricePower());

            OMS oms = new OMS(subscriptionSymbol, brickSize, orderSize, eventProcessor);
            RenkoModel renkoModel = new RenkoModel(brickSize, eventProcessor);

            String priceSourceStr = config.getProperty(ApiConstants.CONFIG_PRICE_SOURCE);
            PriceSource priceSource = PriceSource.TRADES;
            if (priceSourceStr != null) {
                priceSource = PriceSource.valueOf(priceSourceStr);
            }
            if (priceSource == PriceSource.TRADES) {
                //take prices from the trades
                Consumer<TradesEvent> tradesConsumer = event -> {
                    for (MarketData trade : event.getMarketData()) {
                        eventProcessor.putEvent(new PriceEvent(trade.getPrice()));
                    }
                };
                eventProcessor.addConsumer(TradesEvent.class, tradesConsumer);
            } else {
                //take prices from the top of the book
                Book book = new Book(eventProcessor);
                book.setTOBConsumerType(book.getType(priceSource));
            }
            //subscribe to market data
            eventProcessor.putEvent(new SubscribeEvent(subscriptionSymbol));
        };
    }

    Consumer<OrderDrawEvent> getChartOrdersConsumer(JSChartBridge bridge) {
        HashMap<Order, Marker> markers = new HashMap<>();
        return event -> Platform.runLater(() -> {
            Order order = event.getOrder();
            if (event.getOperation() == OrderOperation.add) {
                Marker marker;
                if (order.getSide() == Side.Buy) {
                    marker = new Marker(lastMarkerId++, order.getPrice(), order.getLinkedLimitBrick().getTime(), order.toJS(), Marker.Type.buyLimit);
                } else {
                    marker = new Marker(lastMarkerId++, order.getPrice(), order.getLinkedLimitBrick().getTime(), order.toJS(), Marker.Type.sellLimit);
                }
                bridge.call(JSChartBridge.FunctionName.addLabel, marker);
                markers.put(order, marker);
            } else if (event.getOperation() == OrderOperation.delete) {
                Marker marker = markers.get(order);
                if (marker != null) {
                    bridge.call(JSChartBridge.FunctionName.removeLabel, marker.getId());
                }
            } else if (event.getOperation() == OrderOperation.fill) {
                Marker marker = markers.get(order);
                if (marker != null) {
                    bridge.call(JSChartBridge.FunctionName.removeLabel, marker.getId());
                }
                long markerTime = order.getLinkedExecBrick() == null ? order.getLinkedLimitBrick().getTime() : order.getLinkedExecBrick().getTime();
                if (order.getSide() == Side.Buy) {
                    marker = new Marker(lastMarkerId++, order.getPrice(), markerTime, order.toJS(), Marker.Type.buy);
                } else {
                    marker = new Marker(lastMarkerId++, order.getPrice(), markerTime, order.toJS(), Marker.Type.sell);
                }
                bridge.call(JSChartBridge.FunctionName.addLabel, marker);
                markers.put(order, marker);
            }
        });
    }
}