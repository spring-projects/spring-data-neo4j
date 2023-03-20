/*
 * Copyright 2011-2023 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.integration.shared.common.ScrollingEntity;

/**
 * @author Michael J. Simons
 */
class CypherAdapterUtilsTest {

	@Test
	void shouldCombineSortKeysetProper() {

		var mappingContext = new Neo4jMappingContext();
		var entity = mappingContext.getPersistentEntity(ScrollingEntity.class);
		var n = Constants.NAME_OF_TYPED_ROOT_NODE.apply(entity);

		var condition = CypherAdapterUtils.combineKeysetIntoCondition(entity,
				KeysetScrollPosition.of(Map.of("foobar", "D0", "b", 3, "c", LocalDateTime.of(2023, 3, 19, 14, 21, 8, 716))),
				Sort.by(Sort.Order.asc("b"), Sort.Order.desc("a"), Sort.Order.asc("c"))
		);

		var expected = """
				MATCH (scrollingEntity)
				WHERE (((scrollingEntity.b > $pcdsl01
				      OR (scrollingEntity.b = $pcdsl01
				        AND scrollingEntity.foobar < $pcdsl02))
				    OR (scrollingEntity.foobar = $pcdsl02
				      AND scrollingEntity.c > $pcdsl03))
				  OR (scrollingEntity.b = $pcdsl01
				    AND scrollingEntity.foobar = $pcdsl02
				    AND scrollingEntity.c = $pcdsl03))
				RETURN scrollingEntity""";

		assertThat(Renderer.getRenderer(Configuration.prettyPrinting()).render(Cypher.match(Cypher.anyNode(n)).where(condition).returning(n).build()))
						.isEqualTo(expected);
	}
}
