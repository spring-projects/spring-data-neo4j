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
package org.springframework.data.neo4j.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Point;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for
 * <ul>
 * <li>{@link Neo4jQueryLookupStrategy}</li>
 * <li>{@link Neo4jQueryMethod}</li>
 * <li>{@link StringBasedNeo4jQuery}</li>
 * </ul>
 *
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
final class RepositoryQueryTest {

	private static final String CUSTOM_CYPHER_QUERY = "MATCH (n) return n";

	private static final RepositoryMetadata TEST_REPOSITORY_METADATA = new DefaultRepositoryMetadata(
			TestRepository.class);

	private static final ProjectionFactory PROJECTION_FACTORY = new SpelAwareProxyProjectionFactory();

	@Mock NamedQueries namedQueries;

	@Nested
	class Neo4jQueryMethodTest {

		@Test
		void findQueryAnnotation() {

			Neo4jQueryMethod neo4jQueryMethod = neo4jQueryMethod("annotatedQueryWithValidTemplate");

			Optional<Query> optionalQueryAnnotation = neo4jQueryMethod.getQueryAnnotation();
			assertThat(optionalQueryAnnotation).isPresent();
		}

		@Test
		void streamQueriesShouldBeTreatedAsCollectionQueries() {

			Neo4jQueryMethod neo4jQueryMethod = neo4jQueryMethod("findAllByIdGreaterThan", long.class);

			assumeThat(neo4jQueryMethod.isStreamQuery()).isTrue();
			assertThat(neo4jQueryMethod.isCollectionLikeQuery()).isTrue();
		}

		@Test
		void collectionQueriesShouldBeTreatedAsSuch() {

			Neo4jQueryMethod neo4jQueryMethod = neo4jQueryMethod("findAllByANamedQuery");

			assumeThat(neo4jQueryMethod.isCollectionQuery()).isTrue();
			assertThat(neo4jQueryMethod.isCollectionLikeQuery()).isTrue();
		}

		@Test
		void shouldFailOnMonoOfPageAsReturnType() {
			assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
					.isThrownBy(() -> reactiveNeo4jQueryMethod("findAllByName", String.class, Pageable.class));
		}

		@Test
		void shouldFailForPageableParameterOnMonoOfPageAsReturnType() {
			assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
					.isThrownBy(() -> reactiveNeo4jQueryMethod("findAllByName", String.class, Pageable.class));
		}

		@Test
		void shouldFailForPageableParameterOnMonoOfSliceAsReturnType() {
			assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
					.isThrownBy(() -> reactiveNeo4jQueryMethod("findAllByNameStartingWith", String.class, Pageable.class));
		}
	}

	@Nested
	class Neo4jQueryLookupStrategyTest {

		@Test
		void shouldSelectPartTreeNeo4jQuery() {

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(mock(Neo4jOperations.class),
					mock(Neo4jMappingContext.class, Mockito.RETURNS_DEEP_STUBS), QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy.resolveQuery(queryMethod("findById", Object.class),
					TEST_REPOSITORY_METADATA, PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(PartTreeNeo4jQuery.class);
		}

		@Test
		void shouldSelectStringBasedNeo4jQuery() {

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(mock(Neo4jOperations.class),
					mock(Neo4jMappingContext.class), QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy.resolveQuery(queryMethod("annotatedQueryWithValidTemplate"),
					TEST_REPOSITORY_METADATA, PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(StringBasedNeo4jQuery.class);
		}

		@Test
		void shouldSelectStringBasedNeo4jQueryForNamedQuery() {

			final String namedQueryName = "TestEntity.findAllByANamedQuery";
			when(namedQueries.hasQuery(namedQueryName)).thenReturn(true);
			when(namedQueries.getQuery(namedQueryName)).thenReturn("MATCH (n) RETURN n");

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(mock(Neo4jOperations.class),
					mock(Neo4jMappingContext.class), QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy.resolveQuery(queryMethod("findAllByANamedQuery"), TEST_REPOSITORY_METADATA,
					PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(StringBasedNeo4jQuery.class);
		}
	}

	@Nested
	class StringBasedNeo4jQueryTest {

		@Test
		void spelQueryContextShouldBeConfiguredCorrectly() {

			SpelQueryContext spelQueryContext = StringBasedNeo4jQuery.SPEL_QUERY_CONTEXT;

			String template;
			String query;
			SpelQueryContext.SpelExtractor spelExtractor;

			template = "MATCH (user:User) WHERE user.name = :#{#searchUser.name} and user.middleName = ?#{#searchUser.middleName} RETURN user";

			spelExtractor = spelQueryContext.parse(template);
			query = spelExtractor.getQueryString();

			assertThat(query)
					.isEqualTo("MATCH (user:User) WHERE user.name = $__SpEL__0 and user.middleName = $__SpEL__1 RETURN user");

			template = "MATCH (user:User) WHERE user.name=?#{[0]} and user.name=:#{[0]} RETURN user";
			spelExtractor = spelQueryContext.parse(template);
			query = spelExtractor.getQueryString();

			assertThat(query).isEqualTo("MATCH (user:User) WHERE user.name=$__SpEL__0 and user.name=$__SpEL__1 RETURN user");
		}

		@Test
		void shouldDetectInvalidAnnotation() {

			Neo4jQueryMethod method = neo4jQueryMethod("annotatedQueryWithoutTemplate");
			assertThatExceptionOfType(MappingException.class)
					.isThrownBy(() -> StringBasedNeo4jQuery.create(mock(Neo4jOperations.class), mock(Neo4jMappingContext.class),
							QueryMethodEvaluationContextProvider.DEFAULT, method))
					.withMessage("Expected @Query annotation to have a value, but it did not.");
		}

		@Test
		void shouldBindParameters() {

			Neo4jQueryMethod method = RepositoryQueryTest.neo4jQueryMethod("annotatedQueryWithValidTemplate", String.class,
					String.class);

			StringBasedNeo4jQuery repositoryQuery = spy(StringBasedNeo4jQuery.create(mock(Neo4jOperations.class),
					mock(Neo4jMappingContext.class), QueryMethodEvaluationContextProvider.DEFAULT, method));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Map<String, Object> resolveParameters = repositoryQuery.bindParameters(new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(), new Object[] { "A String", "Another String" }));

			assertThat(resolveParameters).containsEntry("0", "A String").containsEntry("1", "Another String");
		}

		@Test
		void shouldResolveNamedParameters() {

			Neo4jQueryMethod method = RepositoryQueryTest.neo4jQueryMethod("findByDontDoThisInRealLiveNamed",
					org.neo4j.driver.types.Point.class, String.class, String.class);

			StringBasedNeo4jQuery repositoryQuery = spy(StringBasedNeo4jQuery.create(mock(Neo4jOperations.class),
					mock(Neo4jMappingContext.class), QueryMethodEvaluationContextProvider.DEFAULT, method));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Point thePoint = Values.point(4223, 1, 2).asPoint();
			Map<String, Object> resolveParameters = repositoryQuery
					.bindParameters(new Neo4jParameterAccessor((Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
							new Object[] { thePoint, "TheName", "TheFirstName" }));

			assertThat(resolveParameters).hasSize(8).containsEntry("0", thePoint).containsEntry("location", thePoint)
					.containsEntry("1", "TheName").containsEntry("name", "TheName").containsEntry("2", "TheFirstName")
					.containsEntry("firstName", "TheFirstName").containsEntry("__SpEL__0", "TheFirstName")
					.containsEntry("__SpEL__1", "TheNameTheFirstName");
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ResultProcessTest {

		private Stream<Arguments> params() {
			return Stream.of(
					Arguments.of(
							"findAllByANamedQuery",
							false,
							TestEntity.class,
							TestEntity.class
					),
					Arguments.of(
							"findAllInterfaceProjections",
							true,
							TestEntityInterfaceProjection.class,
							TestEntity.class
					),
					Arguments.of(
							"findAllDTOProjections",
							true,
							TestEntityDTOProjection.class,
							TestEntity.class
					),
					Arguments.of(
							"findAllExtendedEntites",
							false,
							ExtendedTestEntity.class,
							ExtendedTestEntity.class
					)
			);
		}

		@ParameterizedTest
		@MethodSource("params")
		void shouldDetectCorrectProjectionBehaviour(String methodName, boolean projecting, Class<?> queryReturnedType, Class<?> domainType) {

			Neo4jQueryMethod method = RepositoryQueryTest.neo4jQueryMethod(methodName);

			ReturnedType returnedType = method.getResultProcessor().getReturnedType();
			assertThat(returnedType.isProjecting()).isEqualTo(projecting);
			assertThat(returnedType.getReturnedType()).isEqualTo(queryReturnedType);
			assertThat(returnedType.getDomainType()).isEqualTo(domainType);
			assertThat(Neo4jQuerySupport.getDomainType(method)).isEqualTo(domainType);
		}
	}

	static Method queryMethod(String name, Class<?>... parameters) {

		return ReflectionUtils.findMethod(TestRepository.class, name, parameters);
	}

	static Neo4jQueryMethod neo4jQueryMethod(String name, Class<?>... parameters) {

		return new Neo4jQueryMethod(ReflectionUtils.findMethod(TestRepository.class, name, parameters),
				TEST_REPOSITORY_METADATA, PROJECTION_FACTORY);
	}

	static ReactiveNeo4jQueryMethod reactiveNeo4jQueryMethod(String name, Class<?>... parameters) {

		return new ReactiveNeo4jQueryMethod(ReflectionUtils.findMethod(TestRepository.class, name, parameters),
				TEST_REPOSITORY_METADATA, PROJECTION_FACTORY);
	}

	static class TestEntity {
		@Id @GeneratedValue private Long id;

		private String name;
	}

	static class ExtendedTestEntity extends TestEntity {

		private String otherAttribute;
	}

	interface TestEntityInterfaceProjection {

		String getName();
	}

	static class TestEntityDTOProjection {

		private String name;

		private Long numberOfRelations;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getNumberOfRelations() {
			return numberOfRelations;
		}

		public void setNumberOfRelations(Long numberOfRelations) {
			this.numberOfRelations = numberOfRelations;
		}
	}

	interface TestRepository extends CrudRepository<TestEntity, Long> {

		@Query("MATCH (n:Test) WHERE n.name = $name AND n.firstName = :#{#firstName} AND n.fullName = ?#{#name + #firstName} AND p.location = $location return n")
		Optional<TestEntity> findByDontDoThisInRealLiveNamed(@Param("location") org.neo4j.driver.types.Point location,
				@Param("name") String name, @Param("firstName") String aFirstName);

		@Query("MATCH (n:Test) WHERE n.name = $0 OR n.name = $1")
		List<TestEntity> annotatedQueryWithValidTemplate(String name, String anotherName);

		@Query(CUSTOM_CYPHER_QUERY)
		List<TestEntity> annotatedQueryWithValidTemplate();

		@Query
		List<TestEntity> annotatedQueryWithoutTemplate();

		List<TestEntity> findAllByANamedQuery();

		Stream<TestEntity> findAllByIdGreaterThan(long id);

		Mono<Page<TestEntity>> findAllByName(String name, Pageable pageable);

		Mono<Slice<TestEntity>> findAllByNameStartingWith(String name, Pageable pageable);

		List<TestEntityInterfaceProjection> findAllInterfaceProjections();

		List<TestEntityDTOProjection> findAllDTOProjections();

		List<ExtendedTestEntity> findAllExtendedEntites();
	}

	private RepositoryQueryTest() {}
}
