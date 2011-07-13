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

import org.springframework.data.domain.Page;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author mh
 * @since 13.06.11
 */
public class GenericTypeExtractor {
    public static Class<?> resolveReturnedType(Method method) {
        return resolveConcreteType(method.getReturnType(), method.getGenericReturnType());
    }

    public static Class<?> resolveFieldType(Field field) {
        return resolveConcreteType(field.getType(), field.getGenericType());
    }

    public static Class<?> resolveConcreteType(Class<?> type, final Type genericType) {
        if (Iterable.class.isAssignableFrom(type) || Page.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType returnType = (ParameterizedType) genericType;
                Type componentType = returnType.getActualTypeArguments()[0];

                return componentType instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) componentType).getRawType()
                        : (Class<?>) componentType;
            } else {
                return Object.class;
            }
        }

        return type;
    }
}
