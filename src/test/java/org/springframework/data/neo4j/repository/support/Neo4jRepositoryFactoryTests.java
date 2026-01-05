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
package org.springframework.data.neo4j.repository.support;

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
import org.springframework.data.neo4j.integration.shared.common.ThingWithAllCypherTypes;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithAllAdditionalTypes;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCompositeProperties;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryCreationException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jRepositoryFactoryTests {

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

	/**
	 * Test failure and success to ensure that
	 * {@link Neo4jRepositoryFactorySupport#assertIdentifierType(Class, Class)} gets used.
	 */
	@Nested
	class IdentifierTypeCheck {

		@Spy
		private Neo4jRepositoryFactory neo4jRepositoryFactory = new Neo4jRepositoryFactory(null, null);

		private Neo4jEntityInformation entityInformation;

		private RepositoryInformation metadata;

		@BeforeEach
		void setup() {

			this.metadata = mock(RepositoryInformation.class);
			this.entityInformation = mock(Neo4jEntityInformation.class);

			doReturn(this.entityInformation).when(this.neo4jRepositoryFactory)
				.getEntityInformation(Mockito.any(RepositoryMetadata.class));
		}

		@Test
		void matchingClassTypes() {
			given(this.entityInformation.getIdType()).willReturn(Long.class);
			Class repositoryIdentifierClass = Long.class;
			given(this.metadata.getIdType()).willReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> this.neo4jRepositoryFactory.getTargetRepository(this.metadata))
				.hasMessageContaining("Target type must not be null")
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void mismatchingClassTypes() {
			given(this.entityInformation.getIdType()).willReturn(Long.class);
			Class repositoryIdentifierClass = String.class;
			given(this.metadata.getIdType()).willReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> this.neo4jRepositoryFactory.getTargetRepository(this.metadata)).hasMessage(
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

			this.mappingContext = new Neo4jMappingContext();
			this.mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(ThingWithAllAdditionalTypes.class,
					ThingWithAllCypherTypes.class, ThingWithCompositeProperties.class)));
			this.repositoryFactory = new Neo4jRepositoryFactory(Mockito.mock(Neo4jTemplate.class), this.mappingContext);
		}

		@Test
		void validateIgnoreCaseShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(InvalidIgnoreCase.class))
				.withMessageMatching(
						".+ derive query for .*: Only the case of String based properties can be ignored within the following keywords: \\[IsNotLike, NotLike, IsLike, Like, IsStartingWith, StartingWith, StartsWith, IsEndingWith, EndingWith, EndsWith, IsNotContaining, NotContaining, NotContains, IsContaining, Containing, Contains, IsNot, Not, Is, Equals]");
		}

		@Test
		void validateTemporalShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(InvalidTemporal.class))
				.withMessageMatching(
						".+ derive query for .*: The keywords \\[IsAfter, After] work only with properties with one of the following types: \\[class java.time.Instant, class java.time.LocalDate, class java.time.LocalDateTime, class java.time.LocalTime, class java.time.OffsetDateTime, class java.time.OffsetTime, class java.time.ZonedDateTime]");
		}

		@Test
		void validateCollectionShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(InvalidCollection.class))
				.withMessageMatching(
						".+ derive query for .*: The keywords \\[IsEmpty, Empty] work only with collection properties");
		}

		@Test
		void validateSpatialShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(InvalidSpatial.class))
				.withMessageMatching(".+ derive query for .* \\[IsNear, Near] works only with spatial properties");
		}

		@Test
		void validateNotACompositePropertyShouldWork() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(DerivedWithComposite.class))
				.withMessageMatching(
						".+ derive query for .*: Derived queries are not supported for composite properties");
		}

		@Test // GH-2281
		void validateDeleteReturnType() {

			assertThatExceptionOfType(QueryCreationException.class)
				.isThrownBy(() -> this.repositoryFactory.getRepository(InvalidDeleteBy.class))
				.withMessageMatching(
						"A derived delete query can only return the number of deleted nodes as a long or void");
		}

	}

}
