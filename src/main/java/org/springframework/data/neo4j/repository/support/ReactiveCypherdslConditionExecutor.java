/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.SortItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort;

/**
 * An interface that can be added to any repository so that queries can be enriched by
 * {@link Condition conditions} of the Cypher-DSL. This interface behaves the same as the
 * {@link org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor}.
 *
 * @param <T> the type of the domain
 * @author Niklas Krieger
 * @author Michael J. Simons
 * @since 6.3.3
 */
@API(status = API.Status.STABLE, since = "6.3.3")
public interface ReactiveCypherdslConditionExecutor<T> {

	Mono<T> findOne(Condition condition);

	Flux<T> findAll(Condition condition);

	Flux<T> findAll(Condition condition, Sort sort);

	Flux<T> findAll(Condition condition, SortItem... sortItems);

	Flux<T> findAll(SortItem... sortItems);

	Mono<Long> count(Condition condition);

	Mono<Boolean> exists(Condition condition);

}
