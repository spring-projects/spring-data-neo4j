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
package org.springframework.data.neo4j.integration.issues.gh2328;

import java.util.UUID;

import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

/**
 * @author Michael J. Simons
 */
public interface ReactiveEntity2328Repository extends ReactiveNeo4jRepository<Entity2328, UUID> {

	// Without a custom query, repository creation would fail with
	// Could not create query for
	// public abstract org.springframework.data.neo4j.integration.issues.gh2328.SomeEntity
	// org.springframework.data.neo4j.integration.issues.gh2328.GH2328IT$SomeRepository.getSomeEntityViaNamedQuery()!
	// Reason: No property getSomeEntityViaNamedQuery found for type SomeEntity!;
	Mono<Entity2328> getSomeEntityViaNamedQuery();

}
