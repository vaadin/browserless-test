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

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J provider for the tests, registered through {@code META-INF/services} so
 * that it is used whatever runs the tests. It writes events at INFO level and
 * above to the standard error stream, and also hands every event to the open
 * {@link LogCapture}s.
 */
public final class CapturingServiceProvider implements SLF4JServiceProvider {

    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new BasicMDCAdapter();
    private final CapturingLoggerFactory loggerFactory = new CapturingLoggerFactory();

    @Override
    public void initialize() {
        // Nothing to set up
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    static final class CapturingLoggerFactory implements ILoggerFactory {

        private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

        private CapturingLoggerFactory() {
        }

        @Override
        public Logger getLogger(String name) {
            return loggers.computeIfAbsent(name, CapturingLogger::new);
        }
    }

    private static final class CapturingLogger extends LegacyAbstractLogger {

        private static final Level DEFAULT_LEVEL = Level.INFO;

        private CapturingLogger(String name) {
            this.name = name;
        }

        private boolean isEnabled(Level level) {
            return level.toInt() >= DEFAULT_LEVEL.toInt()
                    || LogCapture.isCapturing(name, level);
        }

        @Override
        public boolean isTraceEnabled() {
            return isEnabled(Level.TRACE);
        }

        @Override
        public boolean isDebugEnabled() {
            return isEnabled(Level.DEBUG);
        }

        @Override
        public boolean isInfoEnabled() {
            return isEnabled(Level.INFO);
        }

        @Override
        public boolean isWarnEnabled() {
            return isEnabled(Level.WARN);
        }

        @Override
        public boolean isErrorEnabled() {
            return isEnabled(Level.ERROR);
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker,
                String pattern, Object[] arguments, Throwable throwable) {
            LogCapture.record(name, level, pattern, arguments, throwable);
            if (level.toInt() >= DEFAULT_LEVEL.toInt()) {
                PrintStream err = System.err;
                err.println("[" + Thread.currentThread().getName() + "] "
                        + level + " " + name + " - " + MessageFormatter
                                .basicArrayFormat(pattern, arguments));
                if (throwable != null) {
                    throwable.printStackTrace(err);
                }
                err.flush();
            }
        }
    }
}
