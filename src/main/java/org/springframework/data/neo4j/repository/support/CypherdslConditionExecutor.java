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

import java.util.Collection;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.SortItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * An interface that can be added to any repository so that queries can be enriched by {@link Condition conditions} of the
 * Cypher-DSL. This interface behaves the same as the {@link org.springframework.data.querydsl.QuerydslPredicateExecutor}.
 *
 * @author Michael J. Simons
 * @param <T> Type of the domain
 * @since 6.1
 */
@API(status = API.Status.STABLE, since = "6.1")
public interface CypherdslConditionExecutor<T> {

	Optional<T> findOne(Condition condition);

	Collection<T> findAll(Condition condition);

	Collection<T> findAll(Condition condition, Sort sort);

	Collection<T> findAll(Condition condition, SortItem... sortItems);

	Collection<T> findAll(SortItem... sortItems);

	Page<T> findAll(Condition condition, Pageable pageable);

	long count(Condition condition);

	boolean exists(Condition condition);
}

