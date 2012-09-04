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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Array;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 05.11.11
 */
public class PropertyConverter {
    private final ConversionService conversionService;
    private final Neo4jPersistentProperty property;
    private final TypeInformation<?> typeInformation;
    private final Class<?> targetType;

    public PropertyConverter(ConversionService conversionService, Neo4jPersistentProperty property) {
        this.conversionService = conversionService;
        this.property = property;
        this.typeInformation = property.getTypeInformation();
        targetType = property.getPropertyType();
    }

    public Object serializePropertyValue(final Object newVal) {
        if (newVal == null) return null;
        final TypeInformation<?> typeInformation = property.getTypeInformation();
        if (typeInformation.isCollectionLike()) {
            return serializeCollection(newVal, conversionService, typeInformation, targetType);
        }
        return conversionService.convert(newVal, targetType);
    }

    public Object deserializePropertyValue(final Object newVal) {
        if (newVal == null) return null;
        if (typeInformation.isCollectionLike() && isCollectionLike(newVal)) {
            return deserializeCollection(newVal, conversionService, typeInformation);
        }
        return conversionService.convert(newVal, typeInformation.getType());
    }

    private boolean isCollectionLike(Object val) {
        return val != null && (val.getClass().isArray() || Collection.class.isAssignableFrom(val.getClass()));
    }

    private Object serializeCollection(Object newVal, ConversionService conversionService, TypeInformation<?> typeInformation, final Class<?> targetType) {
        final List<Object> values = convertCollection(conversionService, targetType, toCollection(newVal));
        return values.toArray((Object[]) Array.newInstance(targetType, values.size()));
    }

    @SuppressWarnings("unchecked")
    private Object deserializeCollection(Object newVal, ConversionService conversionService, TypeInformation<?> typeInformation) {
        final Class<?> actualType = typeInformation.getActualType().getType();
        final List<Object> result = convertCollection(conversionService, actualType, toCollection(newVal));
        final Class<?> fieldType = typeInformation.getType();
        if (fieldType.isArray()) {
            return result.toArray((Object[]) Array.newInstance(actualType, result.size()));
        }
        if (Set.class.isAssignableFrom(fieldType)) return new LinkedHashSet<Object>(result);
        return result;
    }

    private List<Object> convertCollection(ConversionService conversionService, Class<?> targetType, Iterable<?> values) {
        final List<Object> result = new ArrayList<Object>();
        for (Object value : values) {
            result.add(conversionService.convert(value, targetType));
        }
        return result;
    }

    private Iterable<?> toCollection(Object newVal) {
        if (newVal.getClass().isArray()) {
            return asList((Object[]) newVal);
        } else {
            return (Iterable<?>) newVal;
        }
    }

    boolean isObjectOrSupportedType(final Object value, Neo4jPersistentProperty property) {
        return property.getType().equals(Object.class) && property.isNeo4jPropertyValue(value);
    }
}
