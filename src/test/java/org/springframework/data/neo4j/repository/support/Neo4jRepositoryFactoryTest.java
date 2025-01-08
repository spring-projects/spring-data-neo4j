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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllCypherTypes;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCompositeProperties;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryCreationException;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jRepositoryFactoryTest {

	/**
	 * Test failure and success to ensure that {@link Neo4jRepositoryFactorySupport#assertIdentifierType(Class, Class)}
	 * gets used.
	 */
	@Nested
	class IdentifierTypeCheck {
		@Spy private Neo4jRepositoryFactory neo4jRepositoryFactory = new Neo4jRepositoryFactory(null, null);
		private Neo4jEntityInformation entityInformation;
		private RepositoryInformation metadata;

		@BeforeEach
		void setup() {

			metadata = mock(RepositoryInformation.class);
			entityInformation = mock(Neo4jEntityInformation.class);

			doReturn(entityInformation).when(neo4jRepositoryFactory).getEntityInformation(Mockito.any());
		}

		@Test
		void matchingClassTypes() {
			when(entityInformation.getIdType()).thenReturn(Long.class);
			Class repositoryIdentifierClass = Long.class;
			when(metadata.getIdType()).thenReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> neo4jRepositoryFactory.getTargetRepository(metadata))
					.hasMessageContaining("Target type must not be null").isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void mismatchingClassTypes() {
			when(entityInformation.getIdType()).thenReturn(Long.class);
			Class repositoryIdentifierClass = String.class;
			when(metadata.getIdType()).thenReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> neo4jRepositoryFactory.getTargetRepository(metadata))
					.hasMessage(
							"The repository id type class java.lang.String differs from the entity id type class java.lang.Long")
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DerivedQueryCheck {

		private Neo4jMappingContext mappingContext;
		private Neo4jRepositoryFactory repositoryFactory;

		@BeforeAll
		void prepareContext() {

			mappingContext = new Neo4jMappingContext();
			mappingContext.setInitialEntitySet(new HashSet<>(
					Arrays.asList(ThingWithAllAdditionalTypes.class,
							ThingWithAllCypherTypes.class,
							ThingWithCompositeProperties.class)));
			repositoryFactory = new Neo4jRepositoryFactory(Mockito.mock(Neo4jTemplate.class), mappingContext);
		}

		@Test
		void validateIgnoreCaseShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(InvalidIgnoreCase.class))
					.withMessageMatching("Could not create query for .*: Only the case of String based properties can be ignored within the following keywords: \\[IsNotLike, NotLike, IsLike, Like, IsStartingWith, StartingWith, StartsWith, IsEndingWith, EndingWith, EndsWith, IsNotContaining, NotContaining, NotContains, IsContaining, Containing, Contains, IsNot, Not, Is, Equals]");
		}

		@Test
		void validateTemporalShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(InvalidTemporal.class))
					.withMessageMatching("Could not create query for .*: The keywords \\[IsAfter, After] work only with properties with one of the following types: \\[class java.time.Instant, class java.time.LocalDate, class java.time.LocalDateTime, class java.time.LocalTime, class java.time.OffsetDateTime, class java.time.OffsetTime, class java.time.ZonedDateTime]");
		}

		@Test
		void validateCollectionShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(InvalidCollection.class))
					.withMessageMatching("Could not create query for .*: The keywords \\[IsEmpty, Empty] work only with collection properties");
		}

		@Test
		void validateSpatialShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(InvalidSpatial.class))
					.withMessageMatching("Could not create query for .* \\[IsNear, Near] works only with spatial properties");
		}

		@Test
		void validateNotACompositePropertyShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(DerivedWithComposite.class))
					.withMessageMatching("Could not create query for .*: Derived queries are not supported for composite properties");
		}

		@Test // GH-2281
		void validateDeleteReturnType() {

			assertThatExceptionOfType(QueryCreationException.class).isThrownBy(() -> repositoryFactory.getRepository(InvalidDeleteBy.class))
					.withMessageMatching("Could not create query for .*: A derived delete query can only return the number of deleted nodes as a long or void");
		}
	}

	interface InvalidIgnoreCase extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {

		Optional<ThingWithAllAdditionalTypes> findOneByAnIntIgnoreCase(int anInt);
	}

	interface InvalidTemporal extends Neo4jRepository<ThingWithAllAdditionalTypes, Long> {

		Optional<ThingWithAllAdditionalTypes> findOneByAnIntAfter(int anInt);
	}

	interface InvalidCollection extends Neo4jRepository<ThingWithAllCypherTypes, Long> {

		Optional<ThingWithAllCypherTypes> findOneByALongIsEmpty();
	}

	interface InvalidSpatial extends Neo4jRepository<ThingWithAllCypherTypes, Long> {

		Optional<ThingWithAllCypherTypes> findOneByALongIsNear(Point point);
	}

	interface InvalidDeleteBy extends Neo4jRepository<ThingWithAllCypherTypes, Long> {

		Optional<ThingWithAllCypherTypes> deleteAllBy(Point point);
	}

	interface DerivedWithComposite extends Neo4jRepository<ThingWithCompositeProperties, Long> {

		Optional<ThingWithCompositeProperties> findOneByCustomTypeMapTrue();
	}
}
