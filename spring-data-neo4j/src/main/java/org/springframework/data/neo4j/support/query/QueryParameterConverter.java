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
package org.springframework.data.neo4j.support.query;

import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;

import java.lang.reflect.Array;
import java.util.*;

public class QueryParameterConverter {
    public Map<String, Object> convert(Map<String, Object> parameters) {
        if (parameters == null) return Collections.emptyMap();

        HashMap<String, Object> convertedParameters = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            convertedParameters.put(entry.getKey(), convertParameter(entry.getValue()));
        }

        return convertedParameters;
    }

    private Object convertParameter(Object parameter) {
        if (parameter == null) return null;

        if (parameter.getClass().isEnum())
            return new Neo4jConversionServiceFactoryBean.EnumToStringConverter().convert((Enum) parameter);

        if (parameter instanceof Date)
            return new Neo4jConversionServiceFactoryBean.DateToStringConverter().convert((Date) parameter);

        if (parameter.getClass().isArray())
            return convertArray(parameter);

        if (parameter instanceof Iterable)
            return convertIterable((Iterable) parameter);

        return parameter;
    }

    private Object convertArray(Object parameter) {
        Class<?> componentType = parameter.getClass().getComponentType();

        if (componentType.isPrimitive())
            return convertArrayOfPrimitive(parameter);

        return convertArray((Object[]) parameter);
    }

    private Object convertArrayOfPrimitive(Object parameter) {
        int length = Array.getLength(parameter);

        Object convertedValues = Array.newInstance(parameter.getClass().getComponentType(), length);

        for (int i = 0; i < length; i++)
            Array.set(convertedValues, i, convertParameter(Array.get(parameter, i)));

        return convertedValues;
    }

    private Object convertArray(Object[] parameter) {
        ArrayList<Object> convertedValues = new ArrayList<Object>();

        for (Object o : parameter) {
            convertedValues.add(convertParameter(o));
        }

        Class<?> componentType = parameter.getClass().getComponentType();

        if (componentType == String.class)
            return convertedValues.toArray((String[]) Array.newInstance(String.class, convertedValues.size()));

        if (componentType == Object.class)
            return convertedValues.toArray((Object[]) Array.newInstance(Object.class, convertedValues.size()));

        if (convertedValues.size() > 0)
            return convertedValues.toArray((Object[]) Array.newInstance(convertedValues.get(0).getClass(), convertedValues.size()));

        throw new IllegalArgumentException(String.format("Cannot determine converted parameter type (%s/ %s)", parameter, parameter.getClass()));
    }

    private Object convertIterable(Iterable parameter) {
        ArrayList<Object> convertedValues = new ArrayList<Object>();

        for (Object o : parameter) {
            convertedValues.add(convertParameter(o));
        }

        return convertedValues;
    }
}