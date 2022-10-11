package io.xtrd;

import org.slf4j.Logger;

class ThreadHelper {

    private ThreadHelper() {
    }

    public static Thread startThread(Thread thread, String name, Runnable task, Logger logger) {
        if (thread == null) {
            thread = new Thread(task);
            thread.setName(name);
            thread.start();
        } else {
            logger.warn("{} thread already started", name);
        }
        return thread;
    }

    public static void stopThread(Thread thread, Logger logger) {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(5000);
                logger.info("{} service stopped", thread.getName());
            } catch (InterruptedException e) {
                logger.warn("Error during jointing interrupted thread {}", thread.getName());
            }
        } else {
            logger.warn("Thread already stopped");
        }
    }
}
