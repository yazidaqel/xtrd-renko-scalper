package io.xtrd;

import io.xtrd.trading.events.IEvent;
import io.xtrd.trading.events.MarketDataEvent;
import io.xtrd.trading.events.PriceEvent;
import io.xtrd.trading.events.TradesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;


public class EventProcessor {
    private final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private BlockingQueue<IEvent> queue = new LinkedBlockingQueue<>();
    private Thread worker;
    private Map<Class, Consumer> consumers = new HashMap<>();

    public void start() {
        worker = ThreadHelper.startThread(worker, "Event processor", new Processor(), logger);
    }

    public void stop() {
        ThreadHelper.stopThread(worker, logger);
    }

    public void clear() {
        queue.clear();
    }

    public void putEvent(IEvent event) {
        queue.add(event);
    }

    public void addConsumer(Class clazz, Consumer consumer) {
        Consumer consumerForClass = consumers.get(clazz);
        if (consumerForClass == null) {
            consumers.put(clazz, consumer);
        } else {
            //add consumer to the chain
            consumers.put(clazz, consumerForClass.andThen(consumer));
        }

    }

    class Processor implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    IEvent event = queue.take();
                    Class eventClass = event.getClass();
                    Consumer consumer = consumers.get(eventClass);
                    if (consumer != null) {
                        consumer.accept(event);
                        if (logger.isDebugEnabled()) {
                            if (!(eventClass == TradesEvent.class || eventClass == PriceEvent.class || eventClass == MarketDataEvent.class)) {
                                logger.debug("Event type: {} processed: {}", event.getClass(), event);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    logger.debug("Thread {} interrupted, exiting", Thread.currentThread().getName());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
