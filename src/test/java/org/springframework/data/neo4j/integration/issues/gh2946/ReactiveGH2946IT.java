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
package org.springframework.data.neo4j.integration.issues.gh2946;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author chuuminggg
 */
@Neo4jIntegrationTest
@ExtendWith(LogbackCapturingExtension.class)
class ReactiveGH2946IT {

	private static final Pattern TRAVERSED_IDS = Pattern.compile(":param __id__ => \\[(.*)]");

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver) {
		try (Session session = driver.session()) {
			session.executeWriteWithoutResult(tx -> {
				tx.run("MATCH (n) DETACH DELETE n").consume();
				// r -> a -> b -> c and a back-reference a -> r. When the level starting
				// at {r, b} is processed, a is reachable again via the already known
				// relationship r -> a and must not be traversed a second time.
				tx.run("""
						CREATE (r:GH2946Node {name: 'r'})-[:NEXT]->(a:GH2946Node {name: 'a'}),
						       (a)-[:NEXT]->(r),
						       (a)-[:NEXT]->(b:GH2946Node {name: 'b'}),
						       (b)-[:NEXT]->(:GH2946Node {name: 'c'})
						""").consume();
			});
		}
	}

	@Test
	@Tag("GH-2946")
	void shouldNotTraverseAlreadyVisitedNodesAgain(@Autowired ReactiveGH2946Repository repository,
			LogbackCapture logbackCapture) {

		try {
			logbackCapture.addLogger("org.springframework.data.neo4j.cypher", Level.TRACE);

			repository.findById("r").as(StepVerifier::create).assertNext(r -> {
				assertThat(r.getNext()).extracting(GH2946Node::getName).containsExactly("a");
				GH2946Node a = r.getNext().get(0);
				assertThat(a.getNext()).extracting(GH2946Node::getName).containsExactlyInAnyOrder("r", "b");
				GH2946Node b = a.getNext().stream().filter(n -> "b".equals(n.getName())).findFirst().orElseThrow();
				assertThat(b.getNext()).extracting(GH2946Node::getName).containsExactly("c");
			}).verifyComplete();

			List<List<String>> traversedLevels = new ArrayList<>();
			for (String message : logbackCapture.getFormattedMessages()) {
				Matcher matcher = TRAVERSED_IDS.matcher(message);
				while (matcher.find()) {
					traversedLevels.add(Arrays.asList(matcher.group(1).split(", ")));
				}
			}

			// Levels {a}, {r, b} and {c}
			assertThat(traversedLevels).hasSize(3);
			Map<String, Integer> visits = new HashMap<>();
			traversedLevels.forEach(level -> level.forEach(id -> visits.merge(id, 1, Integer::sum)));
			assertThat(visits).allSatisfy((id, count) -> assertThat(count).as("visits of node %s", id).isOne());
		}
		finally {
			logbackCapture.resetLogLevel();
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
