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

import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.conversion.QueryResultProxy;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.lang.reflect.Proxy;
import java.util.Map;

public class QueryMapResulConverter<T> implements ResultConverter<Map<String, Object>, T> {
    private final Neo4jOperations template;

    public QueryMapResulConverter(Neo4jOperations template) {
        this.template = template;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(Map<String, Object> value, Class<T> type, MappingPolicy mappingPolicy) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{type}, new QueryResultProxy(value,mappingPolicy,template.getDefaultConverter()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(Map<String, Object> value, Class<T> type) {
        return convert(value,type,null);
    }
}