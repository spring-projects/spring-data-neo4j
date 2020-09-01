/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.Test;
import org.neo4j.ogm.metadata.MetaData;
import org.springframework.data.neo4j.mapping.datagraph1212.AssignedWithPrimitiveVersion;
import org.springframework.data.neo4j.mapping.datagraph1212.AssignedWithVersion;
import org.springframework.data.neo4j.mapping.datagraph1212.AssignedWithoutVersion;
import org.springframework.data.neo4j.mapping.datagraph1212.ExternallyGeneratedNonPrimitive;
import org.springframework.data.neo4j.mapping.datagraph1212.ExternallyGeneratedPrimitive;
import org.springframework.data.neo4j.mapping.datagraph1212.InternallyGeneratedNonPrimitive;
import org.springframework.data.neo4j.mapping.datagraph1212.InternallyGeneratedPrimitive;
import org.springframework.data.support.IsNewStrategy;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Helping Hands… Live & Acoustic At The Masonic
 */
public class DefaultNeo4jIsNewStrategyTests {

	MetaData metaData = new MetaData("org.springframework.data.neo4j.mapping.datagraph1212");

	Neo4jMappingContext mappingContext = new Neo4jMappingContext(metaData);

	@Test
	public void shouldDealWithNonPrimitiveInternalIds() {
		InternallyGeneratedNonPrimitive a = new InternallyGeneratedNonPrimitive(null);
		InternallyGeneratedNonPrimitive b = new InternallyGeneratedNonPrimitive(1L);

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(mappingContext.getPersistentEntity(InternallyGeneratedNonPrimitive.class), metaData);
		assertThat(strategy.isNew(a)).isTrue();
		assertThat(strategy.isNew(b)).isFalse();
	}

	@Test
	public void shouldDealWithPrimitiveInternalIds() {
		InternallyGeneratedPrimitive a = new InternallyGeneratedPrimitive(-1L);
		InternallyGeneratedPrimitive b = new InternallyGeneratedPrimitive(0L);
		InternallyGeneratedPrimitive c = new InternallyGeneratedPrimitive(1L);

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(mappingContext.getPersistentEntity(InternallyGeneratedPrimitive.class), metaData);
		assertThat(strategy.isNew(a)).isTrue();
		assertThat(strategy.isNew(b)).isFalse();
		assertThat(strategy.isNew(c)).isFalse();
	}

	@Test
	public void shouldDealWithNonPrimitiveExternalIds() {
		ExternallyGeneratedNonPrimitive a = new ExternallyGeneratedNonPrimitive(null);
		ExternallyGeneratedNonPrimitive b = new ExternallyGeneratedNonPrimitive(UUID.randomUUID());

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(mappingContext.getPersistentEntity(ExternallyGeneratedNonPrimitive.class), metaData);
		assertThat(strategy.isNew(a)).isTrue();
		assertThat(strategy.isNew(b)).isFalse();
	}

	@Test
	public void doesntNeedToDealWithPrimitiveExternalIds() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> DefaultNeo4jIsNewStrategy
						.basedOn(mappingContext.getPersistentEntity(ExternallyGeneratedPrimitive.class), metaData))
				.withMessage(
						"Cannot use org.springframework.data.neo4j.mapping.DefaultNeo4jIsNewStrategy with externally generated, primitive ids.");
	}

	@Test
	public void shouldAlwaysTreatEntitiesAsNewWithoutVersionAndAssignedIds() {

		Neo4jMappingContext context = new Neo4jMappingContext(metaData);

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(context.getPersistentEntity(AssignedWithoutVersion.class), metaData);
		assertThat(strategy.isNew(new AssignedWithoutVersion())).isTrue();
		assertThat(strategy.isNew(new AssignedWithoutVersion("someId"))).isTrue();
	}

	@Test
	public void shouldDealWithVersionAndAssignedIds() {

		AssignedWithVersion a = new AssignedWithVersion();
		AssignedWithVersion b = new AssignedWithVersion("someId");
		AssignedWithVersion c = new AssignedWithVersion("someId", 1);

		Neo4jMappingContext context = new Neo4jMappingContext(metaData);

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(context.getPersistentEntity(AssignedWithVersion.class), metaData);

		assertThat(strategy.isNew(a)).isTrue();
		assertThat(strategy.isNew(b)).isTrue();
		assertThat(strategy.isNew(c)).isFalse();
	}

	@Test
	public void shouldDealWithPrimitiveVersionAndAssignedIds() {
		AssignedWithPrimitiveVersion a = new AssignedWithPrimitiveVersion();
		AssignedWithPrimitiveVersion b = new AssignedWithPrimitiveVersion("someId");
		AssignedWithPrimitiveVersion c = new AssignedWithPrimitiveVersion("someId", 1);

		Neo4jMappingContext context = new Neo4jMappingContext(metaData);

		IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(context.getPersistentEntity(AssignedWithPrimitiveVersion.class), metaData);

		assertThat(strategy.isNew(a)).isTrue();
		assertThat(strategy.isNew(b)).isTrue();
		assertThat(strategy.isNew(c)).isFalse();

	}
}
