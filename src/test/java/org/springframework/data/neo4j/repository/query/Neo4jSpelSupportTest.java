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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.query.Neo4jSpelSupport.LiteralReplacement;
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
				.withMessageMatching(".+is not a valid order criteria.");
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
}
