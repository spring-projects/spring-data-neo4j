package org.springframework.datastore.graph.neo4j.template.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Converter
{
    private final Map<String, Method> valueOfs = new HashMap<String, Method>();

    public Object convert(final String typeName, final String value)
    {
        if (value == null) return null;
        if (typeName == null)
            throw new IllegalArgumentException("TypeName must not be null");
        try
        {
            Method valueOf = valueOfs.get(typeName);
            if (valueOf == null)
            {
                final Class type = Class.forName(typeName.contains(".") ? typeName : "java.lang." + typeName);
                valueOf = type.getMethod("valueOf", String.class);
                valueOfs.put(typeName, valueOf);
            }
            return valueOf.invoke(null, value);
        } catch (Exception e)
        {
            throw new RuntimeException(String.format("Error converting value %s from String to type %s", value, typeName), e);
        }
    }
}
