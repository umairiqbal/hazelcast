/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.client;

import com.hazelcast.core.IMap;
import com.hazelcast.core.Instance;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MapEntrySetIterator<K, V> implements Iterator<java.util.Map.Entry<K, V>> {
    protected final Iterator it;
    protected final EntryHolder<K, V> proxy;
    protected final Instance.InstanceType instanceType;
    protected volatile Entry<K, V> lastEntry;
    K currentIteratedKey;
    V currentIteratedValue;
    boolean hasNextCalled = false;

    public MapEntrySetIterator(Iterator iterator, EntryHolder<K, V> proxy, Instance.InstanceType instanceType) {
        this.it = iterator;
        this.proxy = proxy;
        this.instanceType = instanceType;
    }

    public boolean hasNext() {
        hasNextCalled = true;
        if (!it.hasNext()) {
            return false;
        }
        final Map.Entry<K, V> entry = (Entry<K, V>) it.next();
        currentIteratedKey = entry.getKey();
        currentIteratedValue = entry.getValue();
        return true;
    }

    public Entry<K, V> next() {
        if (!hasNextCalled) {
            hasNext();
        }
        hasNextCalled = false;
        K key = this.currentIteratedKey;
        V value = this.currentIteratedValue;
        lastEntry = new MapEntry(key, value, proxy);
        return lastEntry;
    }

    public void remove() {
        it.remove();
        proxy.remove(lastEntry.getKey(), lastEntry.getValue());
    }

    protected class MapEntry implements Entry<K, V> {

        private K key;
        private V value;
        private EntryHolder<K, V> proxy;

        public MapEntry(K key, V value, EntryHolder<K, V> proxy) {
            this.key = key;
            this.value = value;
            this.proxy = proxy;
        }

        public K getKey() {
            // TODO Auto-generated method stub
            return key;
        }

        public V getValue() {
            // TODO Auto-generated method stub
            return value;
        }

        public V setValue(V arg0) {
            if (instanceType.equals(Instance.InstanceType.MULTIMAP)) {
                throw new UnsupportedOperationException();
            }
            return (V) ((IMap) proxy).put(key, arg0);
        }
    }

    ;
}
