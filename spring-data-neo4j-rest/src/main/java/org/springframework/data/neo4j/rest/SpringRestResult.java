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
package org.springframework.data.neo4j.rest;

import org.neo4j.rest.graphdb.util.ConvertedResult;
import org.neo4j.rest.graphdb.util.ResultConverter;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;

import java.util.Iterator;


class SpringRestResult<T> implements Result<T> {
    org.neo4j.rest.graphdb.util.QueryResult<T> queryResult;

    SpringRestResult(org.neo4j.rest.graphdb.util.QueryResult<T> queryResult) {
        this.queryResult = queryResult;
    }

    @Override
    public <R> EndResult<R> to(final Class<R> type) {
        return new SpringEndResult<R>(queryResult.to(type));
    }

    public <R> EndResult<R> to(Class<R> type, final org.springframework.data.neo4j.conversion.ResultConverter<T, R> converter) {
        ConvertedResult<R> result = queryResult.to(type, new ResultConverter<T, R>() {
            @Override
            public R convert(T value, Class<R> type) {
                return converter.convert(value,type);
            }
        });
        return new SpringEndResult<R>(result);
    }

    public void handle(org.springframework.data.neo4j.conversion.Handler<T> handler) {
        queryResult.handle(new SpringHandler<T>(handler));
    }

    @Override
    public Iterator<T> iterator() {
        return queryResult.iterator();
    }



    @SuppressWarnings("unchecked")
    @Override
    public T single() {
       return (T) to(Object.class).single();
    }
}
