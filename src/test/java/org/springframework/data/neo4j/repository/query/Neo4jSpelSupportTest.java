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

import java.util.Collections;

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
import org.springframework.expression.spel.standard.SpelExpressionParser;

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

	@Test // DATAGRAPH-1454
	void cacheShouldWork() {

		// Make sure we flush this before...
		for (int i = 0; i < 16; ++i) {
			LiteralReplacement literalReplacement = Neo4jSpelSupport.literal("y" + i);
		}

		LiteralReplacement literalReplacement1 = Neo4jSpelSupport.literal("x");
		LiteralReplacement literalReplacement2 = Neo4jSpelSupport.literal("x");
		assertThat(literalReplacement1).isSameAs(literalReplacement2);
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
