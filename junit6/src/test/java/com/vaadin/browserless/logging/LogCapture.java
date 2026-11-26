/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.browserless.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

/**
 * Records the events of one logger while open, so that tests can assert what is
 * logged. The logger is enabled for the given level and above while the capture
 * is open, independent of the level configured for slf4j-simple, which is fixed
 * once per JVM.
 * <p>
 * Requires {@link CapturingServiceProvider} to be the SLF4J provider.
 */
public final class LogCapture implements AutoCloseable {

    /**
     * A logged event, with the message already formatted.
     */
    public record CapturedEvent(Level level, String message,
            Throwable throwable) {
    }

    private static final List<LogCapture> ACTIVE = new CopyOnWriteArrayList<>();

    private final String loggerName;
    private final Level level;
    private final List<CapturedEvent> events = new CopyOnWriteArrayList<>();

    private LogCapture(String loggerName, Level level) {
        this.loggerName = loggerName;
        this.level = level;
    }

    /**
     * Starts capturing the events of the logger of the given class.
     *
     * @param loggerClass
     *            the class the logger is named after
     * @param level
     *            the lowest level to enable and capture
     * @return the capture, to close when done
     */
    public static LogCapture start(Class<?> loggerClass, Level level) {
        if (!(LoggerFactory
                .getILoggerFactory() instanceof CapturingServiceProvider.CapturingLoggerFactory)) {
            throw new IllegalStateException("Log capture needs "
                    + CapturingServiceProvider.class.getName()
                    + " as the SLF4J provider, set the slf4j.provider system property to it");
        }
        LogCapture capture = new LogCapture(loggerClass.getName(), level);
        ACTIVE.add(capture);
        return capture;
    }

    /**
     * Gets the events captured so far, in logging order.
     *
     * @return the captured events
     */
    public List<CapturedEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public void close() {
        ACTIVE.remove(this);
    }

    static boolean isCapturing(String loggerName, Level level) {
        return ACTIVE.stream()
                .anyMatch(capture -> capture.captures(loggerName, level));
    }

    static void record(String loggerName, Level level, String pattern,
            Object[] arguments, Throwable throwable) {
        ACTIVE.stream().filter(capture -> capture.captures(loggerName, level))
                .forEach(capture -> capture.events.add(new CapturedEvent(level,
                        MessageFormatter.basicArrayFormat(pattern, arguments),
                        throwable)));
    }

    private boolean captures(String loggerName, Level level) {
        return this.loggerName.equals(loggerName)
                && level.toInt() >= this.level.toInt();
    }
}
