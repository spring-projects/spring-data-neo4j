/*
 * Copyright 2011-2021 the original author or authors.
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Point;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.repository.support.Neo4jEvaluationContextExtension;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
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

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private Neo4jMappingContext neo4jMappingContext;

	@Mock
	private Neo4jOperations neo4jOperations;

	@Mock
	private ProjectionFactory projectionFactory;

	@ParameterizedTest
	@ValueSource(strings = {
			"RETURN 1 SKIP $skip LIMIT $limit",
			"RETURN 1 sKip $skip limit $limit",
			"match(n) return              $\n"
			+ "       skip                 \n"
			+ "       skip                $\n"
			+ "       skip                 \n"
			+ "       LIMIT $ limit",
			"MATCH (n) RETURN n Skip $skip LIMIT /* No */ $ "
			+ " /* NO */ limit",
			"MATCH (n) RETURN n Skip $skip LIMIT /* No */$/* NO */ limit",
			"MATCH (n) RETURN n Skip $skip LIMIT // No, no\n"
			+ " $ /* really, not a */ limit"
	})
	void shouldDetectValidSkipAndLimitPlaceholders(String template) {

		assertThat(StringBasedNeo4jQuery.hasSkipAndLimitKeywordsAndPlaceholders(template)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"RETURN 1 SKIP $SKIP LIMIT $LIMIT",
			"RETURN 1 skip $skiP limit $lImit",
			"RETURN 1 skip // $skiP limit $lImit",
			"RETURN 1 skip $skiP limit // $lImit",
	})
	void shouldDetectValidSkipAndLimitPlaceholdersNegative(String template) {

		assertThat(StringBasedNeo4jQuery.hasSkipAndLimitKeywordsAndPlaceholders(template)).isFalse();
	}

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
					.isThrownBy(
							() -> reactiveNeo4jQueryMethod("findAllByNameStartingWith", String.class, Pageable.class));
		}
	}

	@Nested
	class Neo4jQueryLookupStrategyTest {

		@Test
		void shouldSelectPartTreeNeo4jQuery() {

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(neo4jOperations,
					neo4jMappingContext,
					QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy.resolveQuery(queryMethod("findById", Object.class),
					TEST_REPOSITORY_METADATA, PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(PartTreeNeo4jQuery.class);
		}

		@Test
		void shouldSelectStringBasedNeo4jQuery() {

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(neo4jOperations,
					neo4jMappingContext, QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy.resolveQuery(queryMethod("annotatedQueryWithValidTemplate"),
					TEST_REPOSITORY_METADATA, PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(StringBasedNeo4jQuery.class);
		}

		@Test
		void shouldSelectStringBasedNeo4jQueryForNamedQuery() {

			final String namedQueryName = "TestEntity.findAllByANamedQuery";
			when(namedQueries.hasQuery(namedQueryName)).thenReturn(true);
			when(namedQueries.getQuery(namedQueryName)).thenReturn("MATCH (n) RETURN n");

			final Neo4jQueryLookupStrategy lookupStrategy = new Neo4jQueryLookupStrategy(neo4jOperations,
					neo4jMappingContext, QueryMethodEvaluationContextProvider.DEFAULT);

			RepositoryQuery query = lookupStrategy
					.resolveQuery(queryMethod("findAllByANamedQuery"), TEST_REPOSITORY_METADATA,
							PROJECTION_FACTORY, namedQueries);
			assertThat(query).isInstanceOf(StringBasedNeo4jQuery.class);
		}
	}

	@Nested
	@ExtendWith(LogbackCapturingExtension.class)
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
					.isEqualTo(
							"MATCH (user:User) WHERE user.name = $__SpEL__0 and user.middleName = $__SpEL__1 RETURN user");

			template = "MATCH (user:User) WHERE user.name=?#{[0]} and user.name=:#{[0]} RETURN user";
			spelExtractor = spelQueryContext.parse(template);
			query = spelExtractor.getQueryString();

			assertThat(query)
					.isEqualTo("MATCH (user:User) WHERE user.name=$__SpEL__0 and user.name=$__SpEL__1 RETURN user");
		}

		@Test
		void shouldDetectInvalidAnnotation() {

			Neo4jQueryMethod method = neo4jQueryMethod("annotatedQueryWithoutTemplate");
			assertThatExceptionOfType(MappingException.class)
					.isThrownBy(() -> StringBasedNeo4jQuery
							.create(neo4jOperations, neo4jMappingContext,
									QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory))
					.withMessage("Expected @Query annotation to have a value, but it did not.");
		}

		@Test // DATAGRAPH-1409
		void shouldDetectMissingCountQuery() {

			Neo4jQueryMethod method = neo4jQueryMethod("missingCountQuery", Pageable.class);
			assertThatExceptionOfType(MappingException.class)
					.isThrownBy(() -> StringBasedNeo4jQuery
							.create(neo4jOperations, neo4jMappingContext,
									QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory))
					.withMessage("Expected paging query method to have a count query!");
		}

		@Test // DATAGRAPH-1409
		void shouldAllowMissingCountOnSlicedQuery(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = neo4jQueryMethod("missingCountQueryOnSlice", Pageable.class);
			StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
					QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(s -> s.matches(
							"(?s)You provided a string based query returning a slice for '.*\\.missingCountQueryOnSlice'\\. You might want to consider adding a count query if more slices than you expect are returned\\."));
		}

		@Test // DATAGRAPH-1440
		void shouldDetectMissingPlaceHoldersOnPagedQuery(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = neo4jQueryMethod("missingPlaceHoldersOnPage", Pageable.class);
			StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
					QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(s -> s.matches(
							"(?s)The custom query.*MATCH \\(n:Page\\) return n.*for '.*\\.missingPlaceHoldersOnPage' is supposed to work with a page or slicing query but does not have the required parameter placeholders `\\$skip` and `\\$limit`\\..*"));
		}

		@Test // DATAGRAPH-1440
		void shouldDetectMissingPlaceHoldersOnSlicedQuery(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = neo4jQueryMethod("missingPlaceHoldersOnSlice", Pageable.class);
			StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
					QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(s -> s.matches(
							"(?s)The custom query.*MATCH \\(n:Slice\\) return n.*is supposed to work with a page or slicing query but does not have the required parameter placeholders `\\$skip` and `\\$limit`\\..*"));
		}

		@Test // DATAGRAPH-1440
		void shouldWarnWhenUsingSortedAndCustomQuery(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = neo4jQueryMethod("findAllExtendedEntitiesWithCustomQuery", Sort.class);
			AbstractNeo4jQuery query =
					StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
							QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);

			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] {Sort.by("name").ascending() });

			query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity(),
					UnaryOperator.identity()
			);
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(s -> s.matches(
							".*" + Pattern.quote("Please specify the order in the query itself and use an unsorted request or use the SpEL extension `:#{orderBy(#sort)}`.")  + ".*"))
					.anyMatch(s -> s.matches(
							"(?s).*One possible order clause matching your page request would be the following fragment:.*ORDER BY name ASC"));
		}

		@Test // DATAGRAPH-1440
		void shouldWarnWhenUsingSortedPageable(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = neo4jQueryMethod("noWarningsPerSe", Pageable.class);
			AbstractNeo4jQuery query =
					StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
							QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);
			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { PageRequest.of(1, 1, Sort.by("name").ascending()) });

			query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity(),
					UnaryOperator.identity()
			);
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(s -> s.matches(
							".*" + Pattern
									.quote("Please specify the order in the query itself and use an unsorted request or use the SpEL extension `:#{orderBy(#sort)}`.")
							+ ".*"))
					.anyMatch(s -> s.matches(
							"(?s).*One possible order clause matching your page request would be the following fragment:.*ORDER BY name ASC"));
		}

		@Test // DATAGRAPH-1454
		void orderBySpelShouldWork(LogbackCapture logbackCapture) {

			ConfigurableApplicationContext context = new GenericApplicationContext();
			context.getBeanFactory().registerSingleton(Neo4jEvaluationContextExtension.class.getSimpleName(),
					new Neo4jEvaluationContextExtension());
			context.refresh();

			Neo4jQueryMethod method = neo4jQueryMethod("orderBySpel", Pageable.class);
			StringBasedNeo4jQuery query = StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
							new ExtensionAwareQueryMethodEvaluationContextProvider(context.getBeanFactory()), method, projectionFactory);

			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { PageRequest.of(1, 1, Sort.by("name").ascending()) });
			PreparedQuery pq = query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity(),
					UnaryOperator.identity()
			);
			assertThat(pq.getQueryFragmentsAndParameters().getCypherQuery())
					.isEqualTo("MATCH (n:Test) RETURN n ORDER BY name ASC SKIP $skip LIMIT $limit");
			assertThat(logbackCapture.getFormattedMessages())
					.noneMatch(s -> s.matches(
							".*Please specify the order in the query itself and use an unsorted page request\\..*"))
					.noneMatch(s -> s.matches(
							"(?s).*One possible order clause matching your page request would be the following fragment:.*ORDER BY name ASC"));
		}

		@Test // DATAGRAPH-1454
		void literalReplacementsShouldWork() {

			ConfigurableApplicationContext context = new GenericApplicationContext();
			context.getBeanFactory().registerSingleton(Neo4jEvaluationContextExtension.class.getSimpleName(),
					new Neo4jEvaluationContextExtension());
			context.refresh();

			Neo4jQueryMethod method = neo4jQueryMethod("makeStaticThingsDynamic", String.class, String.class, String.class, String.class, Sort.class);
			StringBasedNeo4jQuery query = StringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
							new ExtensionAwareQueryMethodEvaluationContextProvider(context.getBeanFactory()), method, projectionFactory);

			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { "A valid ", "dynamic Label", "dyn prop", "static value",
							Sort.by("name").ascending() });
			PreparedQuery pq = query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity(),
					UnaryOperator.identity()
			);
			assertThat(pq.getQueryFragmentsAndParameters().getCypherQuery())
					.isEqualTo("MATCH (n:`A valid dynamic Label`) SET n.`dyn prop` = 'static value' RETURN n ORDER BY name ASC SKIP $skip LIMIT $limit");
		}

		@Test
		void shouldBindParameters() {

			Neo4jQueryMethod method = RepositoryQueryTest.neo4jQueryMethod("annotatedQueryWithValidTemplate", String.class,
					String.class);

			StringBasedNeo4jQuery repositoryQuery = spy(StringBasedNeo4jQuery.create(neo4jOperations,
					neo4jMappingContext, QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Map<String, Object> resolveParameters = repositoryQuery.bindParameters(new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(), new Object[] { "A String", "Another String" }), true,
					UnaryOperator.identity());

			assertThat(resolveParameters).containsEntry("0", "A String").containsEntry("1", "Another String");
		}

		@Test
		void shouldResolveNamedParameters() {

			Neo4jQueryMethod method = RepositoryQueryTest.neo4jQueryMethod("findByDontDoThisInRealLiveNamed",
					org.neo4j.driver.types.Point.class, String.class, String.class);

			StringBasedNeo4jQuery repositoryQuery = spy(StringBasedNeo4jQuery.create(neo4jOperations,
					neo4jMappingContext, QueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Point thePoint = Values.point(4223, 1, 2).asPoint();
			Map<String, Object> resolveParameters = repositoryQuery
					.bindParameters(new Neo4jParameterAccessor((Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
							new Object[] { thePoint, "TheName", "TheFirstName" }), true, UnaryOperator.identity());

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
							"findAllExtendedEntities",
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

	private static Method queryMethod(String name, Class<?>... parameters) {

		return ReflectionUtils.findMethod(TestRepository.class, name, parameters);
	}

	private static Neo4jQueryMethod neo4jQueryMethod(String name, Class<?>... parameters) {

		return new Neo4jQueryMethod(queryMethod(name, parameters), TEST_REPOSITORY_METADATA, PROJECTION_FACTORY);
	}

	private static ReactiveNeo4jQueryMethod reactiveNeo4jQueryMethod(String name, Class<?>... parameters) {

		return new ReactiveNeo4jQueryMethod(queryMethod(name, parameters),
				TEST_REPOSITORY_METADATA, PROJECTION_FACTORY);
	}

	private static class TestEntity {
		@Id @GeneratedValue private Long id;

		private String name;
	}

	private static class ExtendedTestEntity extends TestEntity {

		private String otherAttribute;
	}

	private interface TestEntityInterfaceProjection {

		String getName();
	}

	private  static class TestEntityDTOProjection {

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

	private interface TestRepository extends CrudRepository<TestEntity, Long> {

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

		List<ExtendedTestEntity> findAllExtendedEntities();

		@Query("MATCH (n:Test) RETURN n SKIP $skip LIMIT $limit")
		List<ExtendedTestEntity> findAllExtendedEntitiesWithCustomQuery(Sort sort);

		@Query("MATCH (n:Test) RETURN n SKIP $skip LIMIT $limit")
		Page<TestEntity> missingCountQuery(Pageable pageable);

		@Query("MATCH (n:Test) RETURN n SKIP $skip LIMIT $limit")
		Slice<TestEntity> missingCountQueryOnSlice(Pageable pageable);

		@Query(value = "MATCH (n:Test) RETURN n SKIP $skip LIMIT $limit", countQuery = "MATCH (n:Test) RETURN count(n)")
		Slice<TestEntity> noWarningsPerSe(Pageable pageable);

		// The complexity of the queries here doesn't matter, we the tests aim for having the appropriate skip/limits and count queries.
		@Query(value = "MATCH (n:Page) return n", countQuery = "RETURN 1")
		Page<TestEntity> missingPlaceHoldersOnPage(Pageable pageable);

		@Query(value = "MATCH (n:Slice) return n", countQuery = "RETURN 1")
		Slice<TestEntity> missingPlaceHoldersOnSlice(Pageable pageable);

		@Query(value = "MATCH (n:Test) RETURN n :#{ orderBy (#pageable.sort)} SKIP $skip LIMIT $limit", countQuery = "MATCH (n:Test) RETURN count(n)")
		Slice<TestEntity> orderBySpel(Pageable page);

		@Query(value = "MATCH (n:`:#{literal(#aDynamicLabelPt1 + #aDynamicLabelPt2)}`) "
					   + "SET n.`:#{literal(#aDynamicProperty)}` = :#{literal('''' + #enforcedLiteralValue + '''')} "
					   + "RETURN n :#{orderBy(#sort)} SKIP $skip LIMIT $limit"
		)
		List<TestEntity> makeStaticThingsDynamic(
				@Param("aDynamicLabelPt1") String aDynamicLabelPt1,
				@Param("aDynamicLabelPt2") String aDynamicLabelPt2,
				@Param("aDynamicProperty") String aDynamicProperty,
				@Param("enforcedLiteralValue") String enforcedLiteralValue,
				Sort sort
		);
	}

	private RepositoryQueryTest() {
	}
}
