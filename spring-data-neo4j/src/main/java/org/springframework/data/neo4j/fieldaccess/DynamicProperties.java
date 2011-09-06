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

import java.lang.reflect.Field;
import java.util.Map;

import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesFieldAccessorFactory.DynamicPropertiesFieldAccessor;

/**
 * A {@link DynamicProperties} property on a @NodeEntity stores all its properties dynamically
 * on the underlying node itself.
 * <p>
 * This dynamic property only is available inside a transaction, i.e. when the entity has been saved.
 * <p>
 * The key/value pairs of the {@link DynamicProperties} property are stored on the node with the keys
 * prefixed with the property name that is returned by {@link DelegatingFieldAccessorFactory#getNeo4jPropertyName(Field)}. 
 * <pre>
 * &#064;NodeEntity
 * class Person {
 *     String name;
 *     DynamicProperties personalProperties;
 * }
 * 
 * Person p = new Person();
 * p.persist();
 * p.personalProperties.setProperty(&quot;ZIP&quot;, 8000);
 * p.personalProperties.setProperty(&quot;City&quot;, &quot;Zürich&quot;);
 * </pre>
 * results in a node with the properties:
 * <pre>
 * "personalProperties-ZIP" => 8000
 * "personalProperties-City" => "Zürich"
 * </pre>
 */
public interface DynamicProperties {

    /**
     * @param key
     *            the key to be checked
     * @return <tt>true</tt> if a property with the given key exists
     */
    boolean hasProperty(String key);

    /**
     * @param key
     *            key of the property to get
     * @return the property with the given key, or <tt>null</tt> if no such property exists and {@link #hasProperty}
     *         returns <tt>false</tt>
     */
    Object getProperty(String key);

    /**
     * @param key
     *            key of the property to get
     * @param defaultValue
     *            the default value to return if no property with the given key exists
     * @return the property with the given key or defaultValue if no such property exists and {@link #hasProperty}
     *         returns <tt>false</tt>
     */
    Object getProperty(String key, Object defaultValue);

    /**
     * Set the value of the property with the given key to the given value and overwrites it when such a property
     * already exists.
     * 
     * @param key
     *            key of the property
     * @param value
     *            value of the property
     */
    void setProperty(String key, Object value);

    /**
     * Removes the property with the given key
     * 
     * @param key
     * @return the property that has been removed or null if no such property exists and {@link #hasProperty} returns
     *         <tt>false</tt>
     */
    Object removeProperty(String key);

    /**
     * Returns all keys
     * 
     * @return iterable over all keys
     */
    Iterable<String> getPropertyKeys();

    /**
     * @return a map with all properties key/value pairs
     */
    Map<String, Object> asMap();

    /**
     * Sets a property for all key/value pairs in the given map
     * 
     * @param map
     *            that contains the key/value pairs to set
     */
    void setPropertiesFrom(Map<String, Object> map);

    /**
     * Creates a new instance with the properties set from the given map with {@link #setPropertiesFrom(Map)}
     * 
     * @param map
     *            that contains the key/value pairs to set
     * @return a new DynamicProperties instance
     */
    DynamicProperties createFrom(Map<String, Object> map);
}