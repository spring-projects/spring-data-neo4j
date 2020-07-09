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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.support.IsNewStrategy;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class DefaultNeo4jIsNewStrategyTest {

	@Mock Neo4jPersistentEntity<?> entityMetaData;

	@Mock Neo4jPersistentProperty idProperty;

	@Mock Neo4jPersistentProperty versionProperty;

	@Nested
	class InternallyGenerated {
		@Test
		void shouldDealWithNonPrimitives() {
			Object a = new Object();
			Object b = new Object();

			IdDescription idDescription = IdDescription.forInternallyGeneratedIds();
			doReturn(Long.class).when(idProperty).getType();
			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> null).when(entityMetaData).getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> Long.valueOf(1)).when(entityMetaData).getIdentifierAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void shouldDealWithPrimitives() {
			Object a = new Object();
			Object b = new Object();
			Object c = new Object();

			IdDescription idDescription = IdDescription.forInternallyGeneratedIds();
			doReturn(long.class).when(idProperty).getType();
			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> -1L).when(entityMetaData).getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> 0L).when(entityMetaData).getIdentifierAccessor(b);
			doReturn((IdentifierAccessor) () -> 1L).when(entityMetaData).getIdentifierAccessor(c);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);
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
			IdDescription idDescription = IdDescription.forExternallyGeneratedIds(DummyIdGenerator.class, null, "na");
			doReturn(String.class).when(idProperty).getType();
			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();
			doReturn((IdentifierAccessor) () -> null).when(entityMetaData).getIdentifierAccessor(a);
			doReturn((IdentifierAccessor) () -> "4711").when(entityMetaData).getIdentifierAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void doesntNeedToDealWithPrimitives() {

			IdDescription idDescription = IdDescription.forExternallyGeneratedIds(DummyIdGenerator.class, null, "na");
			doReturn(long.class).when(idProperty).getType();
			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();

			assertThatIllegalArgumentException().isThrownBy(() -> DefaultNeo4jIsNewStrategy.basedOn(entityMetaData))
					.withMessage(
							"Cannot use org.springframework.data.neo4j.core.mapping.DefaultNeo4jIsNewStrategy with externally generated, primitive ids.");
		}
	}

	@Nested
	class Assigned {

		@Test
		void shouldAlwaysTreatEntitiesAsNewWithoutVersion() {
			Object a = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds("na");
			doReturn(String.class).when(idProperty).getType();
			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);
			assertThat(strategy.isNew(a)).isTrue();
		}

		@Test
		void shouldDealWithVersion() {
			Object a = new Object();
			Object b = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds("na");

			doReturn(String.class).when(idProperty).getType();
			doReturn(String.class).when(versionProperty).getType();

			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();
			doReturn(versionProperty).when(entityMetaData).getVersionProperty();

			PersistentPropertyAccessor aa = mock(PersistentPropertyAccessor.class);
			doReturn(null).when(aa).getProperty(versionProperty);
			doReturn(aa).when(entityMetaData).getPropertyAccessor(a);

			PersistentPropertyAccessor ab = mock(PersistentPropertyAccessor.class);
			doReturn("A version").when(ab).getProperty(versionProperty);
			doReturn(ab).when(entityMetaData).getPropertyAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);

			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}

		@Test
		void shouldDealWithPrimitiveVersion() {
			Object a = new Object();
			Object b = new Object();
			IdDescription idDescription = IdDescription.forAssignedIds("na");

			doReturn(String.class).when(idProperty).getType();
			doReturn(int.class).when(versionProperty).getType();

			doReturn(idDescription).when(entityMetaData).getIdDescription();
			doReturn(idProperty).when(entityMetaData).getRequiredIdProperty();
			doReturn(versionProperty).when(entityMetaData).getVersionProperty();

			PersistentPropertyAccessor aa = mock(PersistentPropertyAccessor.class);
			doReturn(0).when(aa).getProperty(versionProperty);
			doReturn(aa).when(entityMetaData).getPropertyAccessor(a);

			PersistentPropertyAccessor ab = mock(PersistentPropertyAccessor.class);
			doReturn(1).when(ab).getProperty(versionProperty);
			doReturn(ab).when(entityMetaData).getPropertyAccessor(b);

			IsNewStrategy strategy = DefaultNeo4jIsNewStrategy.basedOn(entityMetaData);

			assertThat(strategy.isNew(a)).isTrue();
			assertThat(strategy.isNew(b)).isFalse();
		}
	}

	static class DummyIdGenerator implements IdGenerator<Void> {

		@Override
		public Void generateId(String primaryLabel, Object entity) {
			return null;
		}
	}
}
