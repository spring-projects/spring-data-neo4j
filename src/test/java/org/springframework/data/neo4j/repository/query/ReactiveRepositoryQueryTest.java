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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Point;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.support.Neo4jEvaluationContextExtension;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for
 * <ul>
 * <li>{@link ReactiveStringBasedNeo4jQuery}</li>
 * </ul>
 *
 * @author Michael J. Simons
 * @soundtrack Red Hot Chili Peppers - Stadium Arcadium
 */
@ExtendWith(MockitoExtension.class)
final class ReactiveRepositoryQueryTest {

	private static final RepositoryMetadata TEST_REPOSITORY_METADATA = new DefaultRepositoryMetadata(
			TestRepository.class);

	private static final ProjectionFactory PROJECTION_FACTORY = new SpelAwareProxyProjectionFactory();

	@Mock(answer = Answers.RETURNS_MOCKS)
	private Neo4jMappingContext neo4jMappingContext;

	@Mock
	private ReactiveNeo4jOperations neo4jOperations;

	@Mock
	private ProjectionFactory projectionFactory;

	@Nested
	@ExtendWith(LogbackCapturingExtension.class)
	class ReactiveStringBasedNeo4jQueryTest {

		@Test
		void spelQueryContextShouldBeConfiguredCorrectly() {

			SpelQueryContext spelQueryContext = ReactiveStringBasedNeo4jQuery.SPEL_QUERY_CONTEXT;

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

			Neo4jQueryMethod method = reactiveNeo4jQueryMethod("annotatedQueryWithoutTemplate");
			assertThatExceptionOfType(MappingException.class)
					.isThrownBy(() -> ReactiveStringBasedNeo4jQuery
							.create(neo4jOperations, neo4jMappingContext, ReactiveQueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory))
					.withMessage("Expected @Query annotation to have a value, but it did not.");
		}

		@Test // DATAGRAPH-1440
		void shouldWarnWhenUsingSortedAndCustomQuery(LogbackCapture logbackCapture) {

			Neo4jQueryMethod method = reactiveNeo4jQueryMethod("findAllExtendedEntitiesWithCustomQuery", Sort.class);
			ReactiveStringBasedNeo4jQuery query =
					ReactiveStringBasedNeo4jQuery
							.create(neo4jOperations, neo4jMappingContext,
									ReactiveQueryMethodEvaluationContextProvider.DEFAULT, method, projectionFactory);

			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { Sort.by("name").ascending() });

			query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity()
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

			Neo4jQueryMethod method = reactiveNeo4jQueryMethod("orderBySpel", Pageable.class);
			ReactiveStringBasedNeo4jQuery query =
					ReactiveStringBasedNeo4jQuery
							.create(neo4jOperations, neo4jMappingContext,
									new ReactiveExtensionAwareQueryMethodEvaluationContextProvider(
											context.getBeanFactory()), method, projectionFactory);

			Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { PageRequest.of(1, 1, Sort.by("name").ascending()) });
			PreparedQuery pq = query.prepareQuery(
					TestEntity.class,
					Collections.emptyMap(),
					parameterAccessor,
					Neo4jQueryType.DEFAULT,
					() -> (typeSystem, mapAccessor) -> new TestEntity()
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

			Neo4jQueryMethod method = reactiveNeo4jQueryMethod("makeStaticThingsDynamic", String.class, String.class,
					String.class, String.class, Sort.class);
			ReactiveStringBasedNeo4jQuery query =
					ReactiveStringBasedNeo4jQuery.create(neo4jOperations, neo4jMappingContext,
							new ReactiveExtensionAwareQueryMethodEvaluationContextProvider(context.getBeanFactory()),
							method, projectionFactory);

			String s = Mono.fromSupplier(() -> {
				Neo4jParameterAccessor parameterAccessor = new Neo4jParameterAccessor(
						(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
						new Object[] { "A valid ", "dynamic Label", "dyn prop", "static value",
								Sort.by("name").ascending() });
				PreparedQuery pq = query.prepareQuery(
						TestEntity.class,
						Collections.emptyMap(),
						parameterAccessor,
						Neo4jQueryType.DEFAULT,
						() -> (typeSystem, mapAccessor) -> new TestEntity()
				);
				return pq.getQueryFragmentsAndParameters().getCypherQuery();
			}).block();
			assertThat(s)
					.isEqualTo(
							"MATCH (n:`A valid dynamic Label`) SET n.`dyn prop` = 'static value' RETURN n ORDER BY name ASC SKIP $skip LIMIT $limit");
		}

		@Test
		void shouldBindParameters() {

			Neo4jQueryMethod method = reactiveNeo4jQueryMethod("annotatedQueryWithValidTemplate", String.class,
					String.class);

			ReactiveStringBasedNeo4jQuery repositoryQuery = spy(
					ReactiveStringBasedNeo4jQuery.create(neo4jOperations,
							neo4jMappingContext, ReactiveQueryMethodEvaluationContextProvider.DEFAULT,
							method, projectionFactory));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Map<String, Object> resolveParameters = repositoryQuery.bindParameters(new Neo4jParameterAccessor(
					(Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
					new Object[] { "A String", "Another String" }));

			assertThat(resolveParameters).containsEntry("0", "A String").containsEntry("1", "Another String");
		}

		@Test
		void shouldResolveNamedParameters() {

			Neo4jQueryMethod method = ReactiveRepositoryQueryTest
					.reactiveNeo4jQueryMethod("findByDontDoThisInRealLiveNamed",
							Point.class, String.class, String.class);

			ReactiveStringBasedNeo4jQuery repositoryQuery = spy(
					ReactiveStringBasedNeo4jQuery.create(neo4jOperations,
							neo4jMappingContext, ReactiveQueryMethodEvaluationContextProvider.DEFAULT,
							method, projectionFactory));

			// skip conversion
			doAnswer(invocation -> invocation.getArgument(0)).when(repositoryQuery).convertParameter(any());

			Point thePoint = Values.point(4223, 1, 2).asPoint();
			Map<String, Object> resolveParameters = repositoryQuery
					.bindParameters(
							new Neo4jParameterAccessor((Neo4jQueryMethod.Neo4jParameters) method.getParameters(),
									new Object[] { thePoint, "TheName", "TheFirstName" }));

			assertThat(resolveParameters).hasSize(8).containsEntry("0", thePoint).containsEntry("location", thePoint)
					.containsEntry("1", "TheName").containsEntry("name", "TheName").containsEntry("2", "TheFirstName")
					.containsEntry("firstName", "TheFirstName").containsEntry("__SpEL__0", "TheFirstName")
					.containsEntry("__SpEL__1", "TheNameTheFirstName");
		}
	}

	private static Method queryMethod(String name, Class<?>... parameters) {

		return ReflectionUtils.findMethod(TestRepository.class, name, parameters);
	}

	private static ReactiveNeo4jQueryMethod reactiveNeo4jQueryMethod(String name, Class<?>... parameters) {

		return new ReactiveNeo4jQueryMethod(queryMethod(name, parameters),
				TEST_REPOSITORY_METADATA, PROJECTION_FACTORY);
	}

	private interface TestRepository extends ReactiveCrudRepository<TestEntity, Long> {

		@Query("MATCH (n:Test) WHERE n.name = $0 OR n.name = $1")
		Flux<TestEntity> annotatedQueryWithValidTemplate(String name, String anotherName);

		@Query(value = "MATCH (n:`:#{literal(#aDynamicLabelPt1 + #aDynamicLabelPt2)}`) "
					   + "SET n.`:#{literal(#aDynamicProperty)}` = :#{literal('''' + #enforcedLiteralValue + '''')} "
					   + "RETURN n :#{orderBy(#sort)} SKIP $skip LIMIT $limit"
		)
		Flux<TestEntity> makeStaticThingsDynamic(
				@Param("aDynamicLabelPt1") String aDynamicLabelPt1,
				@Param("aDynamicLabelPt2") String aDynamicLabelPt2,
				@Param("aDynamicProperty") String aDynamicProperty,
				@Param("enforcedLiteralValue") String enforcedLiteralValue,
				Sort sort
		);

		@Query(value = "MATCH (n:Test) RETURN n :#{ orderBy (#pageable.sort)} SKIP $skip LIMIT $limit")
		Flux<TestEntity> orderBySpel(Pageable page);

		@Query
		Flux<TestEntity> annotatedQueryWithoutTemplate();

		@Query("MATCH (n:Test) RETURN n SKIP $skip LIMIT $limit")
		List<ExtendedTestEntity> findAllExtendedEntitiesWithCustomQuery(Sort sort);

		@Query("MATCH (n:Test) WHERE n.name = $name AND n.firstName = :#{#firstName} AND n.fullName = ?#{#name + #firstName} AND p.location = $location return n")
		Mono<TestEntity> findByDontDoThisInRealLiveNamed(@Param("location") org.neo4j.driver.types.Point location,
				@Param("name") String name, @Param("firstName") String aFirstName);
	}

	private ReactiveRepositoryQueryTest() {
	}
}
