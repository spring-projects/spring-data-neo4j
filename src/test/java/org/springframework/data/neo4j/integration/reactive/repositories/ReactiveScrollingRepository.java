/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.reactive.repositories;

import java.util.UUID;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.integration.shared.common.ScrollingEntity;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Michael J. Simons
 */
public interface ReactiveScrollingRepository extends ReactiveNeo4jRepository<ScrollingEntity, UUID> {

	Flux<ScrollingEntity> findTop4ByOrderByB();

	Mono<Window<ScrollingEntity>> findTop4By(Sort sort, ScrollPosition position);

	Mono<ScrollingEntity> findFirstByA(String a);

	Flux<ScrollingEntity> findAllByAOrderById(String a);
}
