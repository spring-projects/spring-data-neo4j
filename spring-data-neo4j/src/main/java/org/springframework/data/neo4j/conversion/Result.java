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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.mapping.MappingPolicy;

/**
* @author mh
* @since 28.06.11
*/
public interface Result<T> extends Iterable<T> {
    <R> Result<R> to(Class<R> type);
    <R> Result<R> to(Class<R> type, ResultConverter<T, R> resultConverter);
    Result<T> with(MappingPolicy mappingPolicy);

    T single();

    T singleOrNull();

    void handle(Handler<T> handler);

    <C extends Iterable<T>> C as(Class<C> container);Slice<T> slice(int page, int pageSize);

    Slice<T> slice(Pageable page);

    void finish();
}
