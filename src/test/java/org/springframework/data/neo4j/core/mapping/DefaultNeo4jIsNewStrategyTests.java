/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.support.IsNewStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class DefaultNeo4jIsNewStrategyTests {

	@Mock
	Neo4jPersistentEntity<?> entityMetaData;

	@Mock
	Neo4jPersistentProperty idProperty;

	@Mock
	Neo4jPersistentProperty versionProperty;

	static class DummyIdGenerator implements IdGenerator<Void> {

		@Override
		public Void generateId(String primaryLabel, Object entity) {
			return null;
		}

	}

	@Nested
	class InternallyGenerated {

		@Test
		void shouldDealWithNonPrimitives() {
			Object a = new Object();
			Object b = new Object();

			IdDescription idDescription = IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE);
			doReturn(Long.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> null).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> Long.valueOf(1))
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void shouldDealWithPrimitives() {
			Object a = new Object();
			Object b = new Object();
			Object c = new Object();

			IdDescription idDescription = IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE);
			doReturn(long.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> -1L).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> 0L).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(b);
			doReturn((IdentifierAccessor) () -> 1L).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(c);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
			assertThat(strategy.isNew(c)).isFalse();
		}

	}

	@Nested
	class ExternallyGenerated {

		@Test
		void shouldDealWithNonPrimitives() {

			Object a = new Object();
			Object b = new Object();
			IdDescription idDescription = IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE,
					DummyIdGenerator.class, null, "na");
			doReturn(String.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> null).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> "4711").when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getIdentifierAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void doesntNeedToDealWithPrimitives() {

			IdDescription idDescription = IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE,
					DummyIdGenerator.class, null, "na");
			doReturn(long.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();

			assertThatIllegalArgumentException()
				.isThrownBy(() -> DefaultNeo4jIsNewStrategy.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData))
				.withMessage(
						"Cannot use org.springframework.data.neo4j.core.mapping.DefaultNeo4jIsNewStrategy with externally generated, primitive ids");
		}

	}

	@Nested
	class Assigned {

		@Test
		void shouldAlwaysTreatEntitiesAsNewWithoutVersion() {
			Object a = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "na");
			doReturn(String.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
		}

		@Test
		void shouldDealWithVersion() {
			Object a = new Object();
			Object b = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "na");

			doReturn(String.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(String.class).when(DefaultNeo4jIsNewStrategyTests.this.versionProperty).getType();

			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.versionProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getVersionProperty();

			PersistentPropertyAccessor aa = mock(PersistentPropertyAccessor.class);
			doReturn(null).when(aa).getProperty(DefaultNeo4jIsNewStrategyTests.this.versionProperty);
			doReturn(aa).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getPropertyAccessor(a);

			PersistentPropertyAccessor ab = mock(PersistentPropertyAccessor.class);
			doReturn("A version").when(ab).getProperty(DefaultNeo4jIsNewStrategyTests.this.versionProperty);
			doReturn(ab).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getPropertyAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);

			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void shouldDealWithPrimitiveVersion() {
			Object a = new Object();
			Object b = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "na");

			doReturn(String.class).when(DefaultNeo4jIsNewStrategyTests.this.idProperty).getType();
			doReturn(int.class).when(DefaultNeo4jIsNewStrategyTests.this.versionProperty).getType();

			doReturn(idDescription).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getIdDescription();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.idProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getRequiredIdProperty();
			doReturn(DefaultNeo4jIsNewStrategyTests.this.versionProperty)
				.when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData)
				.getVersionProperty();

			PersistentPropertyAccessor aa = mock(PersistentPropertyAccessor.class);
			doReturn(0).when(aa).getProperty(DefaultNeo4jIsNewStrategyTests.this.versionProperty);
			doReturn(aa).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getPropertyAccessor(a);

			PersistentPropertyAccessor ab = mock(PersistentPropertyAccessor.class);
			doReturn(1).when(ab).getProperty(DefaultNeo4jIsNewStrategyTests.this.versionProperty);
			doReturn(ab).when(DefaultNeo4jIsNewStrategyTests.this.entityMetaData).getPropertyAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy
				.basedOn(DefaultNeo4jIsNewStrategyTests.this.entityMetaData);

			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

	}

}
