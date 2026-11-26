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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.simple.SimpleServiceProvider;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J provider for the tests that writes through slf4j-simple as usual and
 * also hands every event to the open {@link LogCapture}s.
 */
public final class CapturingServiceProvider implements SLF4JServiceProvider {

    private final SimpleServiceProvider delegate = new SimpleServiceProvider();
    private CapturingLoggerFactory loggerFactory;

    @Override
    public void initialize() {
        delegate.initialize();
        loggerFactory = new CapturingLoggerFactory(delegate.getLoggerFactory());
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return delegate.getMarkerFactory();
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return delegate.getMDCAdapter();
    }

    @Override
    public String getRequestedApiVersion() {
        return delegate.getRequestedApiVersion();
    }

    static final class CapturingLoggerFactory implements ILoggerFactory {

        private final ILoggerFactory delegate;
        private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

        private CapturingLoggerFactory(ILoggerFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public Logger getLogger(String name) {
            return loggers.computeIfAbsent(name,
                    key -> new CapturingLogger(key, delegate.getLogger(key)));
        }
    }

    private static final class CapturingLogger extends LegacyAbstractLogger {

        private final transient Logger delegate;

        private CapturingLogger(String name, Logger delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        private boolean isEnabled(Level level) {
            return LogCapture.isCapturing(name, level)
                    || delegate.isEnabledForLevel(level);
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
            if (delegate.isEnabledForLevel(level)) {
                LoggingEventBuilder event = delegate.atLevel(level)
                        .setMessage(pattern).setCause(throwable);
                if (marker != null) {
                    event.addMarker(marker);
                }
                if (arguments != null) {
                    for (Object argument : arguments) {
                        event.addArgument(argument);
                    }
                }
                event.log();
            }
        }
    }
}
