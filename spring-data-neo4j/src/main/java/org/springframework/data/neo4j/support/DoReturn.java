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

package org.springframework.data.neo4j.support;

/**
 * Wrapper for return values of FieldAccessor methods.
 * DoReturn indicates that the wrapping aspect should not proceed to the original field access but instead return immediately.
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DoReturn<T> {
    private final T value;

    private DoReturn(T value) {
        this.value = value;
    }

    /**
     * static factory method
     * @param value
     * @param <T>
     * @return
     */
    public static <T> DoReturn<T> doReturn(T value) {
        return new DoReturn<T>(value);
    }

    public static Object unwrap(final Object value) {
        return (value instanceof DoReturn) ? ((DoReturn) value).value : value;
    }
}
