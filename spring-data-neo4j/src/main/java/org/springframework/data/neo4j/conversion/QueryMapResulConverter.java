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
package org.springframework.data.neo4j.conversion;

import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public class QueryMapResulConverter<T> implements ResultConverter<Map<String, Object>, T> {
    private final Neo4jOperations template;

    public QueryMapResulConverter(Neo4jOperations template) {
        this.template = template;
    }

    @Override
    public T convert(Map<String, Object> value, Class<T> type) {
        final Map<String, Object> valueCopy = value;


        T resultProxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                ResultColumn column = method.getAnnotation(ResultColumn.class);
                TypeInformation<Object> returnType = ClassTypeInformation.fromReturnTypeOf(method);
                Object columnValue = valueCopy.get(column.value());

                Object result;
                if (returnType.isCollectionLike())
                    result = template.convert((Iterable) columnValue).to(returnType.getActualType().getType());
                else
                    result = template.convert(columnValue, returnType.getType());


                return result;
            }
        });
        return resultProxy;
    }
}