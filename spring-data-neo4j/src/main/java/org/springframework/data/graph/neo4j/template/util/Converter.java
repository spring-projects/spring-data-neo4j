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

package org.springframework.data.graph.neo4j.template.util;

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
