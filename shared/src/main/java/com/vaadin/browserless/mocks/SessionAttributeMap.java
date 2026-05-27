/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.browserless.mocks;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

/**
 * A live map of all attributes in this session. Modifications to the map will be
 * reflected to the session and vice versa.
 */
public class SessionAttributeMap extends AbstractMap<String, Object> {

    private final HttpSession session;
    private final Set<Entry<String, Object>> entries;

    public SessionAttributeMap(HttpSession session) {
        this.session = session;
        this.entries = new SessionAttributeEntrySet(session);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entries;
    }

    @Override
    public Object get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        return session.getAttribute((String) key);
    }

    @Override
    public Object put(String key, Object value) {
        Object old = session.getAttribute(key);
        session.setAttribute(key, value);
        return old;
    }

    @Override
    public Object remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        Object old = session.getAttribute((String) key);
        session.removeAttribute((String) key);
        return old;
    }

    @Override
    public boolean remove(Object key, Object value) {
        Object current = get(key);
        if (Objects.equals(current, value)) {
            remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return !session.getAttributeNames().hasMoreElements();
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        return session.getAttribute((String) key) != null;
    }

    private static class SessionAttributeEntrySet extends AbstractSet<Entry<String, Object>> {
        private final HttpSession session;

        SessionAttributeEntrySet(HttpSession session) {
            this.session = session;
        }

        @Override
        public int size() {
            int count = 0;
            for (Iterator<String> it = listAttrNames(session).iterator(); it.hasNext(); ) {
                it.next();
                count++;
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return !session.getAttributeNames().hasMoreElements();
        }

        @Override
        public boolean add(Entry<String, Object> element) {
            boolean modified = !Objects.equals(session.getAttribute(element.getKey()), element.getValue());
            if (modified) {
                session.setAttribute(element.getKey(), element.getValue());
            }
            return modified;
        }

        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return new SessionAttributeEntrySetIterator(session);
        }

        @Override
        public boolean remove(Object element) {
            if (!(element instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) element;
            if (contains(e)) {
                session.removeAttribute((String) e.getKey());
                return true;
            }
            return false;
        }

        @Override
        public boolean contains(Object element) {
            if (!(element instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) element;
            return Objects.equals(session.getAttribute((String) e.getKey()), e.getValue());
        }
    }

    private static class SessionAttributeEntrySetIterator implements Iterator<Entry<String, Object>> {
        private final HttpSession session;
        /**
         * Copy the attribute names, otherwise [remove] would throw [ConcurrentModificationException].
         */
        private final Iterator<String> attrNames;
        private String lastAttributeName;

        SessionAttributeEntrySetIterator(HttpSession session) {
            this.session = session;
            this.attrNames = listAttrNames(session).iterator();
        }

        @Override
        public boolean hasNext() {
            return attrNames.hasNext();
        }

        @Override
        public Entry<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastAttributeName = attrNames.next();
            return new SimpleEntry<>(lastAttributeName, session.getAttribute(lastAttributeName));
        }

        @Override
        public void remove() {
            if (lastAttributeName == null) {
                throw new IllegalStateException();
            }
            session.removeAttribute(lastAttributeName);
            lastAttributeName = null;
        }
    }

    private static List<String> listAttrNames(HttpSession session) {
        return new ArrayList<>(Collections.list(session.getAttributeNames()));
    }
}
