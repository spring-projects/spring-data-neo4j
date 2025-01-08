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
package org.springframework.data.neo4j.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.repository.query.Neo4jSpelSupport.LiteralReplacement;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 * @soundtrack Red Hot Chili Peppers - Californication
 */
class Neo4jSpelSupportTest {

	@Test // DATAGRAPH-1454
	void literalOfShouldWork() {

		LiteralReplacement literalReplacement = Neo4jSpelSupport.literal("x");

		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.UNSPECIFIED);
		assertThat(literalReplacement.getValue()).isEqualTo("x");

		literalReplacement = Neo4jSpelSupport.literal(null);
		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.UNSPECIFIED);
		assertThat(literalReplacement.getValue()).isEqualTo("");

		literalReplacement = Neo4jSpelSupport.literal("");
		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.UNSPECIFIED);
		assertThat(literalReplacement.getValue()).isEqualTo("");
	}

	@Test // DATAGRAPH-1454
	void orderByShouldWork() {

		LiteralReplacement literalReplacement = Neo4jSpelSupport.orderBy(Sort.by("a").ascending());
		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.SORT);
		assertThat(literalReplacement.getValue()).isEqualTo("ORDER BY a ASC");

		literalReplacement = Neo4jSpelSupport.orderBy(PageRequest.of(1, 2, Sort.by("a").ascending()));
		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.SORT);
		assertThat(literalReplacement.getValue()).isEqualTo("ORDER BY a ASC");

		literalReplacement = Neo4jSpelSupport.orderBy(null);
		assertThat(literalReplacement.getTarget()).isEqualTo(LiteralReplacement.Target.SORT);
		assertThat(literalReplacement.getValue()).isEqualTo("");

		assertThatIllegalArgumentException().isThrownBy(() -> Neo4jSpelSupport.orderBy("a lizard"))
				.withMessageMatching(".+is not a valid order criteria");
	}

	private Map<?, ?> getCacheInstance() throws ClassNotFoundException, IllegalAccessException {
		Class<?> type = Class.forName(
				"org.springframework.data.neo4j.repository.query.Neo4jSpelSupport$StringBasedLiteralReplacement");
		Field cacheField = ReflectionUtils.findField(type, "INSTANCES");
		cacheField.setAccessible(true);
		return (Map<?, ?>) cacheField.get(null);
	}

	private void flushLiteralCache() {
		try {
			Map<?, ?> cache = getCacheInstance();
			cache.clear();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private int getCacheSize() {
		try {
			Map<?, ?> cache = getCacheInstance();
			return cache.size();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test // DATAGRAPH-1454
	void cacheShouldWork() {

		flushLiteralCache();

		LiteralReplacement literalReplacement1 = Neo4jSpelSupport.literal("x");
		LiteralReplacement literalReplacement2 = Neo4jSpelSupport.literal("x");
		assertThat(literalReplacement1).isSameAs(literalReplacement2);
	}

	@Test // GH-2375
	void cacheShouldBeThreadSafe() throws ExecutionException, InterruptedException {

		flushLiteralCache();

		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newWorkStealingPool();

		AtomicBoolean running = new AtomicBoolean();
		AtomicInteger overlaps = new AtomicInteger();

		Collection<Callable<LiteralReplacement>> getReplacementCalls = new ArrayList<>();
		for (int t = 0; t < numThreads; ++t) {
			getReplacementCalls.add(() -> {
				if (!running.compareAndSet(false, true)) {
					overlaps.incrementAndGet();
				}
				Thread.sleep(100); // Make the chances of overlapping a bit bigger
				LiteralReplacement d = Neo4jSpelSupport.literal("x");
				running.compareAndSet(true, false);
				return d;
			});
		}

		Map<LiteralReplacement, Integer> replacements = new IdentityHashMap<>();
		for (Future<LiteralReplacement> getDriverFuture : executor.invokeAll(getReplacementCalls)) {
			replacements.put(getDriverFuture.get(), 1);
		}
		executor.shutdown();

		// Assume things actually had been concurrent
		assumeThat(overlaps.get()).isGreaterThan(0);

		assertThat(getCacheSize()).isEqualTo(1);
		assertThat(replacements).hasSize(1);
	}

	@ParameterizedTest // GH-2279
	@CsvSource({
			"MATCH (n:Something) WHERE n.name = ?#{#name}, MATCH (n:Something) WHERE n.name = ?__HASH__{#name}",
			"MATCH (n:Something) WHERE n.name = :#{#name}, MATCH (n:Something) WHERE n.name = :__HASH__{#name}"
	})
	void shouldQuoteParameterExpressionsCorrectly(String query, String expected) {

		String quoted = Neo4jSpelSupport.potentiallyQuoteExpressionsParameter(query);
		assertThat(quoted).isEqualTo(expected);
	}

	@ParameterizedTest // GH-2279
	@CsvSource({
			"MATCH (n:Something) WHERE n.name = ?__HASH__{#name}, MATCH (n:Something) WHERE n.name = ?#{#name}",
			"MATCH (n:Something) WHERE n.name = :__HASH__{#name}, MATCH (n:Something) WHERE n.name = :#{#name}"
	})
	void shouldUnquoteParameterExpressionsCorrectly(String quoted, String expected) {

		String query = Neo4jSpelSupport.potentiallyUnquoteParameterExpressions(quoted);
		assertThat(query).isEqualTo(expected);
	}

	@Test
	void moreThan10SpelEntriesShouldWork() {

		SpelQueryContext spelQueryContext = StringBasedNeo4jQuery.SPEL_QUERY_CONTEXT;

		StringBuilder template = new StringBuilder("MATCH (user:User) WHERE ");
		String query;
		SpelQueryContext.SpelExtractor spelExtractor;

		class R implements LiteralReplacement {
			private final String value;

			R(String value) {
				this.value = value;
			}

			@Override
			public String getValue() {
				return value;
			}

			@Override
			public Target getTarget() {
				return Target.UNSPECIFIED;
			}
		}

		Map<String, Object> parameters = new HashMap<>();
		for (int i = 0; i <= 20; ++i) {
			template.append("user.name = :#{#searchUser.name} OR ");
			parameters.put("__SpEL__" + i, new R("'x" + i + "'"));
		}
		template.delete(template.length() - 4, template.length());
		spelExtractor = spelQueryContext.parse(template.toString());
		query = spelExtractor.getQueryString();
		Neo4jQuerySupport.QueryContext qc = new Neo4jQuerySupport.QueryContext("n/a", query, parameters);
		assertThat(qc.query).isEqualTo(
				"MATCH (user:User) WHERE user.name = 'x0' OR user.name = 'x1' OR user.name = 'x2' OR user.name = 'x3' OR user.name = 'x4' OR user.name = 'x5' OR user.name = 'x6' OR user.name = 'x7' OR user.name = 'x8' OR user.name = 'x9' OR user.name = 'x10' OR user.name = 'x11' OR user.name = 'x12' OR user.name = 'x13' OR user.name = 'x14' OR user.name = 'x15' OR user.name = 'x16' OR user.name = 'x17' OR user.name = 'x18' OR user.name = 'x19' OR user.name = 'x20'");
	}

	@Test // GH-2279
	void shouldQuoteParameterExpressionsCorrectly() {

		String quoted = Neo4jSpelSupport.potentiallyQuoteExpressionsParameter("MATCH (n:#{#staticLabels}) WHERE n.name = ?#{#name}");
		assertThat(quoted).isEqualTo("MATCH (n:#{#staticLabels}) WHERE n.name = ?__HASH__{#name}");
	}

	@Test // GH-2279
	void shouldReplaceStaticLabels() {

		Neo4jMappingContext schema = new Neo4jMappingContext();
		schema.setInitialEntitySet(Collections.singleton(BikeNode.class));

		String query = Neo4jSpelSupport.renderQueryIfExpressionOrReturnQuery(
				"MATCH (n:#{#staticLabels}) WHERE n.name = ?#{#name} OR n.name = :?#{#name} RETURN n",
				new Neo4jMappingContext(), (EntityMetadata<BikeNode>) () -> BikeNode.class, new SpelExpressionParser());

		assertThat(query).isEqualTo("MATCH (n:`Bike`:`Gravel`:`Easy Trail`) WHERE n.name = ?#{#name} OR n.name = :?#{#name} RETURN n");

	}

	@Node(primaryLabel = "Bike", labels = {"Gravel", "Easy Trail"})
	static class BikeNode {
		@Id String id;
	}
}
