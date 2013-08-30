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
package org.springframework.data.neo4j.support.conversion;

import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.mapping.MappingPolicy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
* @author mh
* @since 10.11.11
*/
public class QueryResultProxy implements InvocationHandler {
    private final Map<String, Object> map;
    private final MappingPolicy mappingPolicy;
    private final ResultConverter converter;
    private final ResultColumnValueExtractor resultColumnValueExtractor;

    public QueryResultProxy(Map<String, Object> map, MappingPolicy mappingPolicy, ResultConverter converter) {
        this.map = map;
        this.mappingPolicy = mappingPolicy;
        this.converter = converter;
        this.resultColumnValueExtractor = new ResultColumnValueExtractor(map,mappingPolicy,converter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        if (method.getName().equals("equals") && params!=null && params.length == 1) {
            return equalsInternal(proxy, params[0]);
        }

        if (method.getName().equals("hashCode") && (params==null || params.length == 0)) {
           return map.hashCode();
        }

        return resultColumnValueExtractor.extractFromMethod(method);

    }


    private boolean equalsInternal(Object me, Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != me.getClass()) {
            return false;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(other);
        if (!(handler instanceof QueryResultProxy)) return false;
        return ((QueryResultProxy) handler).map.equals(map);
    }
}
