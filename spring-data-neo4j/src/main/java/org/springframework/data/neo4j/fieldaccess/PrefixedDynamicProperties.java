/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.fieldaccess;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Stores the properties internally with prefixed keys. When using the methods from {@link DynamicProperties} the prefix
 * is dynamically added and removed so that the prefixing is not visible when using the {@link DynamicProperties}
 * interface.
 * <p>
 * The methods *PrefixedProperty() allow to access the prefixed property key/values pairs directly.
 */
public class PrefixedDynamicProperties implements DynamicProperties , Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> map;
    protected final String prefix;

    /**
     * Handles key prefixing
     */
    private static class PrefixUtil {
        private final String prefix;
        private final static String DIVIDER = "-";

        public PrefixUtil(final String prefix) {
            this.prefix = prefix + DIVIDER;
        }

        boolean hasPrefix(final String s) {
            return s.startsWith(prefix);
        }

        public String removePrefix(final String s) {
            if (hasPrefix(s)) {
                return s.substring(prefix.length());
            }
            else {
                return s;
            }
        }

        public static String prefixKey(final String prefix, final String key) {
            return new StringBuilder(prefix).append(DIVIDER).append(key).toString();
        }
    }

    /**
     * Removes a prefix from Strings when iterating over them.
     */
    private static class RemovePrefixIterableWrapper implements Iterable<String> {

        private final Iterable<String> iterable;
        private final String prefix;

        /**
         * 
         * @param iterable
         *            Strings to iterate over
         * @param prefix
         *            the prefix to have removed from the iterated Strings
         */
        RemovePrefixIterableWrapper(final Iterable<String> iterable, final String prefix) {
            this.iterable = iterable;
            this.prefix = prefix;
        }

        @Override
        public Iterator<String> iterator() {
            return new RemovePrefixIteratorWrapper(iterable.iterator(), prefix);
        }

        /**
         * Removes a prefix from Strings when iterating over them.
         */
        private static class RemovePrefixIteratorWrapper implements Iterator<String> {

            private final Iterator<String> it;
            private final PrefixUtil prefixUtil;

            /**
             * 
             * @param it
             *            Strings to iterate over
             * @param prefix
             *            the prefix to have removed from the iterated Strings
             */
            private RemovePrefixIteratorWrapper(final Iterator<String> it, final String prefix) {
                this.prefixUtil = new PrefixUtil(prefix);
                this.it = it;
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            /**
             * Returns the next string with the prefix removed
             */
            @Override
            public String next() {
                return prefixUtil.removePrefix(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }

        }

    }

    /**
     * @param prefix
     *            the prefix to be added internally to the keys
     */
    public PrefixedDynamicProperties(final String prefix) {
        map = new HashMap<String, Object>();
        this.prefix = prefix;
    }

    /**
     * @param prefix
     *            the prefix to be added internally to the keys
     * @param initialCapacity
     *            the initialCapacity of the internal map that holds the properties
     */
    public PrefixedDynamicProperties(final String prefix, final int initialCapacity) {
        map = new HashMap<String, Object>(initialCapacity);
        this.prefix = prefix;
    }

    @Override
    public boolean hasProperty(final String key) {
        return map.containsKey(prefixedKey(key));
    }

    @Override
    public Object getProperty(final String key) {
        return map.get(prefixedKey(key));
    }

    @Override
    public Object getProperty(final String key, final Object defaultValue) {
        if (!hasProperty(key)) {
            return defaultValue;
        }
        return getProperty(key);
    }

    @Override
    public void setProperty(final String key, final Object value) {
        baseSetProperty(key, value);
    }

    private void baseSetProperty(final String key, final Object value) {
        map.put(prefixedKey(key), value);
    }

    private Object baseRemoveProperty(final String key) {
        return map.remove(prefixedKey(key));
    }
    
    @Override
    public Object removeProperty(final String key) {
        return baseRemoveProperty(key);
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        return new RemovePrefixIterableWrapper(map.keySet(), prefix);
    }

    @Override
    public void setPropertiesFrom(final Map<String, Object> propertiesMap) {
        this.map.clear();
        for (String key : propertiesMap.keySet()) {
            baseSetProperty(key, propertiesMap.get(key));
        }
    }

    @Override
    public DynamicProperties createFrom(final Map<String, Object> map) {
        DynamicProperties d = new PrefixedDynamicProperties(prefix, map.size());
        d.setPropertiesFrom(map);
        return d;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = new HashMap<String, Object>(map.size());
        for (String key : getPropertyKeys()) {
            result.put(key, getProperty(key));
        }
        return result;
    }

    /**
     * Set the property with the given key only if the key is prefixed.
     * 
     * @param key
     *            key of the property
     * @param value
     *            value
     * @return <tt>true</tt> if the property has been set or not
     */
    public boolean setPropertyIfPrefixed(final String key, final Object value) {
        if (isPrefixedKey(key)) {
            setPrefixedProperty(key, value);
            return true;
        }
        return false;
    }

    public boolean isPrefixedKey(String key) {
        PrefixUtil util = new PrefixUtil(prefix);
        return util.hasPrefix(key);
    }
    
    private String prefixedKey(final String key) {
        return PrefixUtil.prefixKey(prefix, key);
    }

    public Object getPrefixedProperty(final String key) {
        return map.get(key);
    }

    public void setPrefixedProperty(final String key, final Object value) {
        map.put(key, value);
    }

    public boolean hasPrefixedProperty(final String key) {
        return map.containsKey(key);
    }

    public Set<String> getPrefixedPropertyKeys() {
        return map.keySet();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PrefixedDynamicProperties other = (PrefixedDynamicProperties) obj;
		if (map == null) {
			if (other.map != null) {
				return false;
			}
		} else if (!map.equals(other.map)) {
			return false;
		}
		if (prefix == null) {
			if (other.prefix != null) {
				return false;
			}
		} else if (!prefix.equals(other.prefix)) {
			return false;
		}
		return true;
	}
}
